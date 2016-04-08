package org.ggp.base.util.gdl.transforms;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.ggp.base.util.Immutables;
import org.ggp.base.util.gdl.GdlUtils;
import org.ggp.base.util.gdl.GdlVisitor;
import org.ggp.base.util.gdl.GdlVisitors;
import org.ggp.base.util.gdl.grammar.Gdl;
import org.ggp.base.util.gdl.grammar.GdlConstant;
import org.ggp.base.util.gdl.grammar.GdlDistinct;
import org.ggp.base.util.gdl.grammar.GdlLiteral;
import org.ggp.base.util.gdl.grammar.GdlNot;
import org.ggp.base.util.gdl.grammar.GdlOr;
import org.ggp.base.util.gdl.grammar.GdlPool;
import org.ggp.base.util.gdl.grammar.GdlRule;
import org.ggp.base.util.gdl.grammar.GdlSentence;
import org.ggp.base.util.gdl.grammar.GdlTerm;
import org.ggp.base.util.gdl.grammar.GdlVariable;
import org.ggp.base.util.gdl.model.SentenceForm;
import org.ggp.base.util.gdl.model.SimpleSentenceForm;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multiset;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;

/**
 * This transformation changes a game description into an equivalent
 * game description in which each rule has at most two conjuncts.
 *
 * This would normally cause lots of sentence types that are
 * duplicates of each other, so this also deduplicates those.
 *
 * This preserves the ordering in which sentences work in prover
 * implementations like the ProverStateMachine.
 *
 */
public class ConjunctDualizer {
    public static List<Gdl> apply(List<Gdl> description) {
        description = GdlCleaner.run(description);
        description = DeORer.run(description);
        description = DistinctAndNotMover.run(description);

        SentenceNameFactory snf = SentenceNameFactory.create(description);

        List<Gdl> newRules = Lists.newArrayList();
        for (Gdl gdl : description) {
            if (gdl instanceof GdlSentence) {
                newRules.add(gdl);
            } else {
                GdlRule rule = (GdlRule) gdl;
                newRules.addAll(splitRule(rule, snf));
            }
        }

        //Repeat deduplication until convergence
        //TODO: Perf test to see if this is slow
        while (true) {
            List<Gdl> mergedRules = mergeIdenticalSingletonSentences(newRules);
            if (mergedRules.equals(newRules)) {
                break;
            }
            newRules = mergedRules;
        }
        return newRules;
    }

    private static List<Gdl> mergeIdenticalSingletonSentences(List<Gdl> rules) {
        //First just identify the singleton sentences
        Set<GdlConstant> singletonSentenceNames = getSingletonSentenceNames(rules);

        //TODO: At some point, we could actually consider handling sentences with
        //more than one name... but those wouldn't have been introduced here, and
        //would instead indicate redundancy in the original game description.

        System.out.println("Singleton sentence names: " + singletonSentenceNames);

        //TODO: Actually sort out and deduplicate these rules
        //How do we know two are duplicates?
        //1) Same type: C, C-C, C-D, or C-N
        //2) Sentence forms in the body are the same
        //3) Constants in the same places in the body
        //4) Variables in the body correspond
        //5) Variables in head are same (or one is subset of other, see below)

        //So we probably want to create some kind of key object for each of these
        //and put the rules in a multimap and see what comes up...

        //If two rules have the same key, we can merge them.
        SetMultimap<DualizedRuleKey, GdlRule> rulesByRuleKey = HashMultimap.create();

        for (Gdl gdl : rules) {
            if (gdl instanceof GdlRule) {
                GdlRule rule = (GdlRule) gdl;
                if (singletonSentenceNames.contains(rule.getHead().getName())) {
                    //                    System.out.println("Making key for rule: " + rule);
                    rulesByRuleKey.put(createKey(rule), rule);
                }
            }
        }

        List<DeduplicationPlan> deduplications = Lists.newArrayList();

        //Look for rules with matching keys; so far just report them
        for (DualizedRuleKey key : rulesByRuleKey.keySet()) {
            Set<GdlRule> ruleSet = rulesByRuleKey.get(key);
            if (ruleSet.size() > 1) {
                System.out.println("These rules can be deduplicated:");
                for (GdlRule rule : ruleSet) {
                    System.out.println("  " + rule);
                }
                deduplications.add(DeduplicationPlan.create(ruleSet));
            }
        }
        //This looks pretty good now
        //What's next is to do the actual deduplication
        //Pick one to be canonical, remove the rules for the others,
        //and replace the others' appearances in other rules

        //What's awkward is that the order in the sentence bodies could differ
        //Also need to deal with cases where the head is not the form we expect
        //(all separate variables, no functions)

        //If the number of variables in the head differ, replace the rule for the
        //one with fewer variables (if it's a subset)

        //        if (deduplications.isEmpty()) {
        //            return rules;
        //        }
        //Apply the deduplications...
        for (DeduplicationPlan deduplication : deduplications) {
            rules = deduplication.apply(rules);
        }
        return rules;
    }

    private static class DeduplicationPlan {
        private final ImmutableSet<GdlConstant> removedSentenceNames;
        private final SentenceForm newSentenceForm;
        private final ImmutableMap<GdlConstant, Function<List<GdlTerm>, List<GdlTerm>>>
        tupleTransformations;

        private DeduplicationPlan(ImmutableSet<GdlConstant> removedSentenceNames, SentenceForm newSentenceForm,
                ImmutableMap<GdlConstant, Function<List<GdlTerm>, List<GdlTerm>>> tupleTransformations) {
            Preconditions.checkArgument(tupleTransformations.keySet().equals(removedSentenceNames));
            this.removedSentenceNames = removedSentenceNames;
            this.newSentenceForm = newSentenceForm;
            this.tupleTransformations = tupleTransformations;
        }

        public static DeduplicationPlan create(Set<GdlRule> ruleSet) {
            //Pick one arbitrarily to be the new canonical one
            Set<GdlRule> oldRules = Sets.newHashSet(ruleSet);
            GdlRule newRule = oldRules.iterator().next();
            oldRules.remove(newRule);
            System.out.println("Will prefer " + newRule.getHead().getName());
            return create(oldRules, newRule);
        }

        private static DeduplicationPlan create(Set<GdlRule> oldRules, GdlRule newRule) {
            SentenceForm newSentenceForm = SimpleSentenceForm.create(newRule.getHead());
            ImmutableList<TermLocator> headToBodyMap = getHeadToBodyMap(newRule);

            Set<GdlConstant> removedSentenceNames = Sets.newHashSet();
            Map<GdlConstant, Function<List<GdlTerm>, List<GdlTerm>>>
            tupleTransformations = Maps.newHashMap();
            for (GdlRule oldRule : oldRules) {
                GdlConstant name = oldRule.getHead().getName();
                removedSentenceNames.add(name);

                Function<List<GdlTerm>, List<GdlTerm>> tupleTransformation =
                        getTupleTransformation(headToBodyMap,
                                getBodyToHeadMap(oldRule));

                tupleTransformations.put(name, tupleTransformation);
            }


            return new DeduplicationPlan(
                    ImmutableSet.copyOf(removedSentenceNames),
                    newSentenceForm,
                    ImmutableMap.copyOf(tupleTransformations));
        }

        private static ImmutableList<TermLocator> getHeadToBodyMap(GdlRule rule) {
            Map<GdlTerm, TermLocator> termMap = getTermMap(rule);

            List<GdlTerm> head = GdlUtils.getTupleFromSentence(rule.getHead());
            return head.stream()
                    .map(termMap::get)
                    .collect(Immutables.collectList());
        }

        private static ImmutableMap<TermLocator, Integer> getBodyToHeadMap(GdlRule rule) {
            Map<TermLocator, GdlTerm> termMap = Maps.newHashMap();

            for (int conjunctNum = 0; conjunctNum < rule.getBody().size(); conjunctNum++) {
                GdlLiteral conjunct = rule.get(conjunctNum);
                if (conjunct instanceof GdlSentence || conjunct instanceof GdlNot) {
                    GdlSentence sentence = getSentence(rule, conjunctNum);
                    List<GdlTerm> bodyTuple = GdlUtils.getTupleFromSentence(sentence);
                    for (int i = 0; i < bodyTuple.size(); i++) {
                        GdlTerm term = bodyTuple.get(i);
                        TermLocator termLocator = new TermLocator(conjunctNum, i);
                        termMap.put(termLocator, term);
                    }
                } else if (conjunct instanceof GdlDistinct) {
                    GdlDistinct distinct = (GdlDistinct) conjunct;
                    termMap.put(new TermLocator(conjunctNum, 0), distinct.getArg1());
                    termMap.put(new TermLocator(conjunctNum, 1), distinct.getArg2());
                } else {
                    throw new RuntimeException();
                }
            }
            List<GdlTerm> head = GdlUtils.getTupleFromSentence(rule.getHead());

            return ImmutableMap.copyOf(Maps.transformValues(termMap, head::indexOf));
        }

        private static Map<GdlTerm, TermLocator> getTermMap(GdlRule rule) {
            Map<GdlTerm, TermLocator> termMap = Maps.newHashMap();

            for (int conjunctNum = 0; conjunctNum < rule.getBody().size(); conjunctNum++) {
                GdlLiteral conjunct = rule.get(conjunctNum);
                if (conjunct instanceof GdlSentence || conjunct instanceof GdlNot) {
                    GdlSentence sentence = getSentence(rule, conjunctNum);
                    List<GdlTerm> bodyTuple = GdlUtils.getTupleFromSentence(sentence);
                    for (int i = 0; i < bodyTuple.size(); i++) {
                        GdlTerm term = bodyTuple.get(i);
                        TermLocator termLocator = new TermLocator(conjunctNum, i);
                        termMap.putIfAbsent(term, termLocator);
                    }
                } else if (conjunct instanceof GdlDistinct) {
                    GdlDistinct distinct = (GdlDistinct) conjunct;
                    termMap.putIfAbsent(distinct.getArg1(), new TermLocator(conjunctNum, 0));
                    termMap.putIfAbsent(distinct.getArg2(), new TermLocator(conjunctNum, 1));
                } else {
                    throw new RuntimeException();
                }
            }
            return termMap;
        }

        //Require the inputs to be immutable because they're referenced by the
        //returned function, which ends up in an immutable object...
        //(Could precompute, but not worth the effort)
        private static Function<List<GdlTerm>, List<GdlTerm>> getTupleTransformation(
                ImmutableList<TermLocator> headToBodyMap,
                ImmutableMap<TermLocator, Integer> oldRuleMap) {
            return new Function<List<GdlTerm>, List<GdlTerm>>() {
                @Override
                public List<GdlTerm> apply(List<GdlTerm> input) {
                    List<GdlTerm> output = Lists.newArrayList();
                    for (int i = 0; i < headToBodyMap.size(); i++) {
                        TermLocator termLocator = headToBodyMap.get(i);
                        Preconditions.checkState(termLocator != null);
                        Integer oldIndex = oldRuleMap.get(termLocator);
                        Preconditions.checkState(oldIndex != null);
                        output.add(input.get(oldIndex));
                    }
                    return output;
                }
            };
        }

        public List<Gdl> apply(List<Gdl> oldRules) {
            List<Gdl> newRules = Lists.newArrayList();
            for (Gdl gdl : oldRules) {
                if (gdl instanceof GdlRule) {
                    GdlRule rule = (GdlRule) gdl;
                    apply(rule).ifPresent(newRules::add);
                } else {
                    newRules.add(gdl);
                }
            }
            return newRules;
        }

        private Optional<GdlRule> apply(GdlRule rule) {
            //If the head is a removed type, get rid of the rule
            if (removedSentenceNames.contains(rule.getHead().getName())) {
                return Optional.empty();
            }
            //Otherwise, we have to transform body elements if necessary...
            List<GdlLiteral> newBody = rule.getBody().stream()
                    .map(this::applyToConjunct)
                    .collect(Collectors.toList());
            return Optional.of(GdlPool.getRule(rule.getHead(), newBody));
        }

        private GdlLiteral applyToConjunct(GdlLiteral literal) {
            if (literal instanceof GdlSentence) {
                return applyToSentence((GdlSentence) literal);
            } else if (literal instanceof GdlNot) {
                GdlSentence sentence = (GdlSentence) ((GdlNot) literal).getBody();
                return GdlPool.getNot(applyToSentence(sentence));
            } else if (literal instanceof GdlDistinct) {
                return literal;
            } else if (literal instanceof GdlOr) {
                throw new RuntimeException("The DeORer was supposed to have been run");
            }
            throw new RuntimeException("Unknown GdlLiteral type for literal " + literal);
        }

        private GdlLiteral applyToSentence(GdlSentence sentence) {
            if (!removedSentenceNames.contains(sentence.getName())) {
                return sentence;
            }
            //TODO: Figure out how to replace the sentence
            List<GdlTerm> oldTuple = GdlUtils.getTupleFromSentence(sentence);
            List<GdlTerm> newTuple = applyToTuple(sentence.getName(), oldTuple);
            return newSentenceForm.getSentenceFromTuple(newTuple);
        }

        private List<GdlTerm> applyToTuple(GdlConstant oldSentenceName, List<GdlTerm> oldTuple) {
            Function<List<GdlTerm>, List<GdlTerm>> tupleTransformation = tupleTransformations.get(oldSentenceName);
            return tupleTransformation.apply(oldTuple);
        }

    }

    //TODO: Write more tests for this, around constants in the head especially
    private static DualizedRuleKey createKey(GdlRule rule) {
        if (rule.arity() == 1) {
            return SingleConjunctRuleKey.create(rule);
        }
        GdlLiteral secondConjunct = rule.get(1);
        if (secondConjunct instanceof GdlSentence || secondConjunct instanceof GdlNot) {
            return DoubleConjunctRuleKey.create(rule);
            //        } else if (secondConjunct instanceof GdlNot) {
            //            return NegativeConjunctRuleKey.create(rule);
        } else if (secondConjunct instanceof GdlDistinct) {
            return DistinctConjunctRuleKey.create(rule);
        }
        throw new RuntimeException ("Unexpected rule format for " + rule);
    }

    private static interface DualizedRuleKey {
        //We just care about equals/hashCode
    }

    private static class SingleConjunctRuleKey implements DualizedRuleKey {
        private final boolean isNegated;
        private final SentenceForm conjunctForm;
        private final ImmutableMap<Integer, GdlConstant> constants;
        private final ImmutableMap<Integer, Integer> variableEqualities;
        //Specify which variables are or are not in the head
        private final ImmutableSet<Integer> headVarFirstAppearances;

        private SingleConjunctRuleKey(boolean isNegated, SentenceForm conjunctForm,
                ImmutableMap<Integer, GdlConstant> constants, ImmutableMap<Integer, Integer> variableEqualities,
                ImmutableSet<Integer> headVarFirstAppearances) {
            this.isNegated = isNegated;
            this.conjunctForm = conjunctForm;
            this.constants = constants;
            this.variableEqualities = variableEqualities;
            this.headVarFirstAppearances = headVarFirstAppearances;
        }

        public static DualizedRuleKey create(GdlRule rule) {
            Preconditions.checkArgument(rule.arity() == 1);
            Set<GdlVariable> headVariables = GdlUtils.getVariablesSet(rule.getHead());
            boolean isNegated = (rule.get(0) instanceof GdlNot);
            GdlSentence sentence = getSentence(rule, 0);
            SimpleSentenceForm form = SimpleSentenceForm.create(sentence);
            List<GdlTerm> tuple = GdlUtils.getTupleFromSentence(sentence);
            Map<GdlVariable, Integer> firstAppearances = Maps.newHashMap();
            Map<Integer, GdlConstant> constants = Maps.newHashMap();
            Map<Integer, Integer> variableEqualities = Maps.newHashMap();
            Set<Integer> headVarFirstAppearances = Sets.newHashSet();
            for (int i = 0; i < tuple.size(); i++) {
                GdlTerm term = tuple.get(i);
                if (term instanceof GdlConstant) {
                    constants.put(i, (GdlConstant) term);
                } else {
                    GdlVariable variable = (GdlVariable) term;
                    if (firstAppearances.containsKey(variable)) {
                        int firstAppearance = firstAppearances.get(variable);
                        variableEqualities.put(i, firstAppearance);
                    } else {
                        firstAppearances.put(variable, i);
                        if (headVariables.contains(variable)) {
                            headVarFirstAppearances.add(i);
                        }
                    }
                }
            }

            return new SingleConjunctRuleKey(isNegated,
                    form,
                    ImmutableMap.copyOf(constants),
                    ImmutableMap.copyOf(variableEqualities),
                    ImmutableSet.copyOf(headVarFirstAppearances));
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((conjunctForm == null) ? 0 : conjunctForm.hashCode());
            result = prime * result + ((constants == null) ? 0 : constants.hashCode());
            result = prime * result + ((headVarFirstAppearances == null) ? 0 : headVarFirstAppearances.hashCode());
            result = prime * result + (isNegated ? 1231 : 1237);
            result = prime * result + ((variableEqualities == null) ? 0 : variableEqualities.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            SingleConjunctRuleKey other = (SingleConjunctRuleKey) obj;
            if (conjunctForm == null) {
                if (other.conjunctForm != null) {
                    return false;
                }
            } else if (!conjunctForm.equals(other.conjunctForm)) {
                return false;
            }
            if (constants == null) {
                if (other.constants != null) {
                    return false;
                }
            } else if (!constants.equals(other.constants)) {
                return false;
            }
            if (headVarFirstAppearances == null) {
                if (other.headVarFirstAppearances != null) {
                    return false;
                }
            } else if (!headVarFirstAppearances.equals(other.headVarFirstAppearances)) {
                return false;
            }
            if (isNegated != other.isNegated) {
                return false;
            }
            if (variableEqualities == null) {
                if (other.variableEqualities != null) {
                    return false;
                }
            } else if (!variableEqualities.equals(other.variableEqualities)) {
                return false;
            }
            return true;
        }

        @Override
        public String toString() {
            return "SingleConjunctRuleKey [conjunctForm=" + conjunctForm + ", constants=" + constants
                    + ", variableEqualities=" + variableEqualities + ", headVarFirstAppearances="
                    + headVarFirstAppearances + "]";
        }
    }

    private static class TermLocator {
        private final int conjunctNum;
        private final int tupleIndex;

        private TermLocator(int conjunctNum, int tupleIndex) {
            this.conjunctNum = conjunctNum;
            this.tupleIndex = tupleIndex;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + conjunctNum;
            result = prime * result + tupleIndex;
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            TermLocator other = (TermLocator) obj;
            if (conjunctNum != other.conjunctNum) {
                return false;
            }
            if (tupleIndex != other.tupleIndex) {
                return false;
            }
            return true;
        }
    }

    private static GdlSentence getSentence(GdlRule rule, int i) {
        GdlLiteral literal = rule.get(i);
        if (literal instanceof GdlSentence) {
            return (GdlSentence) literal;
        } else if (literal instanceof GdlNot) {
            return (GdlSentence) ((GdlNot) literal).getBody();
        }
        throw new RuntimeException();
    }

    private static class DoubleConjunctRuleKey implements DualizedRuleKey {
        private final boolean isFirstNegated;
        private final boolean isSecondNegated;
        private final SentenceForm firstForm;
        private final SentenceForm secondForm;
        private final ImmutableMap<TermLocator, GdlConstant> constants;
        private final ImmutableMap<TermLocator, TermLocator> variableEqualities;
        //Specify which variables are or are not in the head
        private final ImmutableSet<TermLocator> headVarFirstAppearances;

        private DoubleConjunctRuleKey(boolean isFirstNegated, boolean isSecondNegated, SentenceForm firstForm,
                SentenceForm secondForm, ImmutableMap<TermLocator, GdlConstant> constants,
                ImmutableMap<TermLocator, TermLocator> variableEqualities,
                ImmutableSet<TermLocator> headVarFirstAppearances) {
            this.isFirstNegated = isFirstNegated;
            this.isSecondNegated = isSecondNegated;
            this.firstForm = firstForm;
            this.secondForm = secondForm;
            this.constants = constants;
            this.variableEqualities = variableEqualities;
            this.headVarFirstAppearances = headVarFirstAppearances;
        }

        public static DualizedRuleKey create(GdlRule rule) {
            boolean isFirstNegated = (rule.get(0) instanceof GdlNot);
            boolean isSecondNegated = (rule.get(1) instanceof GdlNot);
            Set<GdlVariable> headVariables = GdlUtils.getVariablesSet(rule.getHead());
            SimpleSentenceForm firstForm = SimpleSentenceForm.create(getSentence(rule, 0));
            SimpleSentenceForm secondForm = SimpleSentenceForm.create(getSentence(rule, 1));
            Map<GdlVariable, TermLocator> firstAppearances = Maps.newHashMap();
            Map<TermLocator, GdlConstant> constants = Maps.newHashMap();
            Map<TermLocator, TermLocator> variableEqualities = Maps.newHashMap();
            Set<TermLocator> headVarFirstAppearances = Sets.newHashSet();
            for (int conjunctNum = 0; conjunctNum < 2; conjunctNum++) {
                GdlSentence sentence = getSentence(rule, conjunctNum);
                List<GdlTerm> tuple = GdlUtils.getTupleFromSentence(sentence);
                for (int i = 0; i < tuple.size(); i++) {
                    GdlTerm term = tuple.get(i);
                    TermLocator locator = new TermLocator(conjunctNum, i);
                    if (term instanceof GdlConstant) {
                        constants.put(locator, (GdlConstant) term);
                    } else {
                        GdlVariable variable = (GdlVariable) term;
                        if (firstAppearances.containsKey(variable)) {
                            TermLocator firstAppearance = firstAppearances.get(variable);
                            variableEqualities.put(locator, firstAppearance);
                        } else {
                            firstAppearances.put(variable, locator);
                            if (headVariables.contains(variable)) {
                                headVarFirstAppearances.add(locator);
                            }
                        }
                    }
                }

            }

            return new DoubleConjunctRuleKey(isFirstNegated,
                    isSecondNegated,
                    firstForm,
                    secondForm,
                    ImmutableMap.copyOf(constants),
                    ImmutableMap.copyOf(variableEqualities),
                    ImmutableSet.copyOf(headVarFirstAppearances));
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((constants == null) ? 0 : constants.hashCode());
            result = prime * result + ((firstForm == null) ? 0 : firstForm.hashCode());
            result = prime * result + ((headVarFirstAppearances == null) ? 0 : headVarFirstAppearances.hashCode());
            result = prime * result + (isFirstNegated ? 1231 : 1237);
            result = prime * result + (isSecondNegated ? 1231 : 1237);
            result = prime * result + ((secondForm == null) ? 0 : secondForm.hashCode());
            result = prime * result + ((variableEqualities == null) ? 0 : variableEqualities.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            DoubleConjunctRuleKey other = (DoubleConjunctRuleKey) obj;
            if (constants == null) {
                if (other.constants != null) {
                    return false;
                }
            } else if (!constants.equals(other.constants)) {
                return false;
            }
            if (firstForm == null) {
                if (other.firstForm != null) {
                    return false;
                }
            } else if (!firstForm.equals(other.firstForm)) {
                return false;
            }
            if (headVarFirstAppearances == null) {
                if (other.headVarFirstAppearances != null) {
                    return false;
                }
            } else if (!headVarFirstAppearances.equals(other.headVarFirstAppearances)) {
                return false;
            }
            if (isFirstNegated != other.isFirstNegated) {
                return false;
            }
            if (isSecondNegated != other.isSecondNegated) {
                return false;
            }
            if (secondForm == null) {
                if (other.secondForm != null) {
                    return false;
                }
            } else if (!secondForm.equals(other.secondForm)) {
                return false;
            }
            if (variableEqualities == null) {
                if (other.variableEqualities != null) {
                    return false;
                }
            } else if (!variableEqualities.equals(other.variableEqualities)) {
                return false;
            }
            return true;
        }

    }

    //Distinct is weird because we don't care about the order of the 'distinct' entries...
    private static class DistinctConjunctRuleKey implements DualizedRuleKey {
        private final SentenceForm firstForm;
        private final ImmutableMap<TermLocator, GdlConstant> constants;
        private final ImmutableMap<TermLocator, TermLocator> variableEqualities;
        //Specify which variables are or are not in the head
        private final ImmutableSet<TermLocator> headVarFirstAppearances;

        private DistinctConjunctRuleKey(SentenceForm firstForm,
                ImmutableMap<TermLocator, GdlConstant> constants,
                ImmutableMap<TermLocator, TermLocator> variableEqualities,
                ImmutableSet<TermLocator> headVarFirstAppearances) {
            this.firstForm = firstForm;
            this.constants = constants;
            this.variableEqualities = variableEqualities;
            this.headVarFirstAppearances = headVarFirstAppearances;
        }

        public static DualizedRuleKey create(GdlRule rule) {
            Set<GdlVariable> headVariables = GdlUtils.getVariablesSet(rule.getHead());
            SimpleSentenceForm firstForm = SimpleSentenceForm.create((GdlSentence) rule.get(0));
            Map<GdlVariable, TermLocator> firstAppearances = Maps.newHashMap();
            Map<TermLocator, GdlConstant> constants = Maps.newHashMap();
            Map<TermLocator, TermLocator> variableEqualities = Maps.newHashMap();
            Set<TermLocator> headVarFirstAppearances = Sets.newHashSet();
            for (int conjunctNum = 0; conjunctNum < 2; conjunctNum++) {
                //                GdlSentence sentence = (GdlSentence) rule.get(conjunctNum);
                List<GdlTerm> tuple;// = GdlUtils.getTupleFromSentence(sentence);
                if (conjunctNum == 0) {
                    GdlSentence sentence = (GdlSentence) rule.get(conjunctNum);
                    tuple = GdlUtils.getTupleFromSentence(sentence);
                } else {
                    GdlDistinct distinct = (GdlDistinct) rule.get(conjunctNum);
                    tuple = ImmutableList.of(distinct.getArg1(), distinct.getArg2());
                }
                for (int i = 0; i < tuple.size(); i++) {
                    GdlTerm term = tuple.get(i);
                    TermLocator locator;
                    if (conjunctNum == 0) {
                        locator = new TermLocator(conjunctNum, i);
                    } else {
                        //Ignore location within the distinct "tuple" because
                        //swapping the locations doesn't matter
                        locator = new TermLocator(conjunctNum, 0);
                    }
                    if (term instanceof GdlConstant) {
                        constants.put(locator, (GdlConstant) term);
                    } else {
                        GdlVariable variable = (GdlVariable) term;
                        if (firstAppearances.containsKey(variable)) {
                            TermLocator firstAppearance = firstAppearances.get(variable);
                            variableEqualities.put(locator, firstAppearance);
                        } else {
                            //This shouldn't happen in a distinct...
                            Preconditions.checkState(conjunctNum == 0);
                            firstAppearances.put(variable, locator);
                            if (headVariables.contains(variable)) {
                                headVarFirstAppearances.add(locator);
                            }
                        }
                    }
                }

            }

            return new DistinctConjunctRuleKey(firstForm,
                    ImmutableMap.copyOf(constants),
                    ImmutableMap.copyOf(variableEqualities),
                    ImmutableSet.copyOf(headVarFirstAppearances));
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((constants == null) ? 0 : constants.hashCode());
            result = prime * result + ((firstForm == null) ? 0 : firstForm.hashCode());
            result = prime * result + ((headVarFirstAppearances == null) ? 0 : headVarFirstAppearances.hashCode());
            result = prime * result + ((variableEqualities == null) ? 0 : variableEqualities.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            DistinctConjunctRuleKey other = (DistinctConjunctRuleKey) obj;
            if (constants == null) {
                if (other.constants != null) {
                    return false;
                }
            } else if (!constants.equals(other.constants)) {
                return false;
            }
            if (firstForm == null) {
                if (other.firstForm != null) {
                    return false;
                }
            } else if (!firstForm.equals(other.firstForm)) {
                return false;
            }
            if (headVarFirstAppearances == null) {
                if (other.headVarFirstAppearances != null) {
                    return false;
                }
            } else if (!headVarFirstAppearances.equals(other.headVarFirstAppearances)) {
                return false;
            }
            if (variableEqualities == null) {
                if (other.variableEqualities != null) {
                    return false;
                }
            } else if (!variableEqualities.equals(other.variableEqualities)) {
                return false;
            }
            return true;
        }

    }

    //Nearly identical to the DoubleConjunctRuleKey
    //    private static class NegativeConjunctRuleKey implements DualizedRuleKey {
    //        private final SentenceForm firstForm;
    //        private final SentenceForm secondForm;
    //        private final ImmutableMap<TermLocator, GdlConstant> constants;
    //        private final ImmutableMap<TermLocator, TermLocator> variableEqualities;
    //        //Specify which variables are or are not in the head
    //        private final ImmutableSet<TermLocator> headVarFirstAppearances;
    //
    //        private NegativeConjunctRuleKey(SentenceForm firstForm, SentenceForm secondForm,
    //                ImmutableMap<TermLocator, GdlConstant> constants,
    //                ImmutableMap<TermLocator, TermLocator> variableEqualities,
    //                ImmutableSet<TermLocator> headVarFirstAppearances) {
    //            this.firstForm = firstForm;
    //            this.secondForm = secondForm;
    //            this.constants = constants;
    //            this.variableEqualities = variableEqualities;
    //            this.headVarFirstAppearances = headVarFirstAppearances;
    //        }
    //
    //        public static DualizedRuleKey create(GdlRule rule) {
    //            Set<GdlVariable> headVariables = GdlUtils.getVariablesSet(rule.getHead());
    //            SimpleSentenceForm firstForm = SimpleSentenceForm.create((GdlSentence) rule.get(0));
    //            SimpleSentenceForm secondForm = SimpleSentenceForm.create((GdlSentence) ((GdlNot) rule.get(1)).getBody());
    //            Map<GdlVariable, TermLocator> firstAppearances = Maps.newHashMap();
    //            Map<TermLocator, GdlConstant> constants = Maps.newHashMap();
    //            Map<TermLocator, TermLocator> variableEqualities = Maps.newHashMap();
    //            Set<TermLocator> headVarFirstAppearances = Sets.newHashSet();
    //            for (int conjunctNum = 0; conjunctNum < 2; conjunctNum++) {
    //                GdlSentence sentence;
    //                if (conjunctNum == 0) {
    //                    sentence = (GdlSentence) rule.get(conjunctNum);
    //                } else {
    //                    sentence = (GdlSentence) ((GdlNot) rule.get(conjunctNum)).getBody();
    //                }
    //                List<GdlTerm> tuple = GdlUtils.getTupleFromSentence(sentence);
    //                for (int i = 0; i < tuple.size(); i++) {
    //                    GdlTerm term = tuple.get(i);
    //                    TermLocator locator = new TermLocator(conjunctNum, i);
    //                    if (term instanceof GdlConstant) {
    //                        constants.put(locator, (GdlConstant) term);
    //                    } else {
    //                        GdlVariable variable = (GdlVariable) term;
    //                        if (firstAppearances.containsKey(variable)) {
    //                            TermLocator firstAppearance = firstAppearances.get(variable);
    //                            variableEqualities.put(locator, firstAppearance);
    //                        } else {
    //                            firstAppearances.put(variable, locator);
    //                            if (headVariables.contains(variable)) {
    //                                headVarFirstAppearances.add(locator);
    //                            }
    //                        }
    //                    }
    //                }
    //
    //            }
    //
    //            return new NegativeConjunctRuleKey(firstForm,
    //                    secondForm,
    //                    ImmutableMap.copyOf(constants),
    //                    ImmutableMap.copyOf(variableEqualities),
    //                    ImmutableSet.copyOf(headVarFirstAppearances));
    //        }
    //
    //        @Override
    //        public int hashCode() {
    //            final int prime = 31;
    //            int result = 1;
    //            result = prime * result + ((constants == null) ? 0 : constants.hashCode());
    //            result = prime * result + ((firstForm == null) ? 0 : firstForm.hashCode());
    //            result = prime * result + ((headVarFirstAppearances == null) ? 0 : headVarFirstAppearances.hashCode());
    //            result = prime * result + ((secondForm == null) ? 0 : secondForm.hashCode());
    //            result = prime * result + ((variableEqualities == null) ? 0 : variableEqualities.hashCode());
    //            return result;
    //        }
    //
    //        @Override
    //        public boolean equals(Object obj) {
    //            if (this == obj) {
    //                return true;
    //            }
    //            if (obj == null) {
    //                return false;
    //            }
    //            if (getClass() != obj.getClass()) {
    //                return false;
    //            }
    //            DoubleConjunctRuleKey other = (DoubleConjunctRuleKey) obj;
    //            if (constants == null) {
    //                if (other.constants != null) {
    //                    return false;
    //                }
    //            } else if (!constants.equals(other.constants)) {
    //                return false;
    //            }
    //            if (firstForm == null) {
    //                if (other.firstForm != null) {
    //                    return false;
    //                }
    //            } else if (!firstForm.equals(other.firstForm)) {
    //                return false;
    //            }
    //            if (headVarFirstAppearances == null) {
    //                if (other.headVarFirstAppearances != null) {
    //                    return false;
    //                }
    //            } else if (!headVarFirstAppearances.equals(other.headVarFirstAppearances)) {
    //                return false;
    //            }
    //            if (secondForm == null) {
    //                if (other.secondForm != null) {
    //                    return false;
    //                }
    //            } else if (!secondForm.equals(other.secondForm)) {
    //                return false;
    //            }
    //            if (variableEqualities == null) {
    //                if (other.variableEqualities != null) {
    //                    return false;
    //                }
    //            } else if (!variableEqualities.equals(other.variableEqualities)) {
    //                return false;
    //            }
    //            return true;
    //        }
    //    }

    public static Set<GdlConstant> getSingletonSentenceNames(List<Gdl> rules) {
        Multiset<GdlConstant> sentenceNameCounts = HashMultiset.create();
        Set<GdlConstant> nonRuleSentenceNames = Sets.newHashSet();
        for (Gdl gdl : rules) {
            if (gdl instanceof GdlRule) {
                GdlRule rule = (GdlRule) gdl;
                sentenceNameCounts.add(rule.getHead().getName());
            } else {
                GdlSentence sentence = (GdlSentence) gdl;
                nonRuleSentenceNames.add(sentence.getName());
            }
        }

        Set<GdlConstant> singletonSentenceNames = Sets.newHashSet();
        for (GdlConstant name : sentenceNameCounts.elementSet()) {
            if (sentenceNameCounts.count(name) == 1
                    && !nonRuleSentenceNames.contains(name)) {
                singletonSentenceNames.add(name);
            }
        }
        singletonSentenceNames.remove(GdlPool.BASE);
        singletonSentenceNames.remove(GdlPool.DOES);
        singletonSentenceNames.remove(GdlPool.GOAL);
        singletonSentenceNames.remove(GdlPool.INIT);
        singletonSentenceNames.remove(GdlPool.INPUT);
        singletonSentenceNames.remove(GdlPool.LEGAL);
        singletonSentenceNames.remove(GdlPool.NEXT);
        singletonSentenceNames.remove(GdlPool.ROLE);
        singletonSentenceNames.remove(GdlPool.TERMINAL);
        singletonSentenceNames.remove(GdlPool.TRUE);
        return singletonSentenceNames;
    }

    //    private static List<Gdl> mergeIdenticalRules(List<Gdl> oldRules, SentenceNameFactory snf) {
    //        //Note: Order of the conjuncts is important! (probably?)
    //        SetMultimap<ImmutableList<GdlLiteral>, GdlRule> rulesByNormalizedConjuncts =
    //                groupRulesByNormalizedConjuncts(oldRules);
    //
    //        Set<Gdl> newRules = Sets.newHashSet();
    //        newRules.addAll(GdlUtils.getSentences(oldRules));
    //        //First, where there's overlap, create a new one-off rule that has the normalized
    //        //conjuncts, plus some single-conjunct mapping rules from the old _ to the new _
    //        Map<GdlSentence, GdlSentence> replacementsToApply = Maps.newHashMap();
    //        for (ImmutableList<GdlLiteral> key : rulesByNormalizedConjuncts.keySet()) {
    //            Set<GdlRule> matchingRules = rulesByNormalizedConjuncts.get(key);
    //            //Matching rules?
    //            if (matchingRules.size() == 1) {
    //                newRules.add(Iterables.getOnlyElement(matchingRules));
    //            } else {
    //                GdlConstant newName = snf.getNewName("anon");
    ////              List<GdlLiteral> newRuleBody = key;
    //                //Steal the name and variables from one of the ___
    //
    ////              for () {
    ////
    ////              }
    //            }
    //        }
    //
    //        //Second, identify and replace any one-off rules that just have a single conjunct
    //        //(and the head is not a keyword)
    //        //What do we mean by "one-off"? We mean the head has only one appearance... (?)
    //        //Or that the body conjunct has only one appearance in the body of a rule?
    //        //Which is the right approach?
    //        //I'm pretty sure we're looking for cases where the BODY is a one-off; appears in one
    //        //rule in the body, appears in one rule in the head, and the rule where it appears in the
    //        //body doesn't have a keyword for the head
    //        //(but what about potentially excessive "forks"?)
    //
    //        return getRulesInMostlyOriginalOrder(oldRules, newRules);
    //    }
    //
    //    private static SetMultimap<ImmutableList<GdlLiteral>, GdlRule> groupRulesByNormalizedConjuncts(
    //            List<Gdl> oldRules) {
    //        // TODO: Implement
    //        return null;
    //    }
    //
    //    private static List<Gdl> getRulesInMostlyOriginalOrder(List<Gdl> oldRules,
    //            Set<Gdl> result) {
    //        List<Gdl> finalResult = Lists.newArrayList();
    //        for (Gdl gdl : oldRules) {
    //            if (result.contains(gdl)) {
    //                finalResult.add(gdl);
    //            }
    //        }
    //        Set<Gdl> oldRulesSet = Sets.newHashSet(oldRules);
    //        for (Gdl gdl : result) {
    //            if (!oldRulesSet.contains(gdl)) {
    //                finalResult.add(gdl);
    //            }
    //        }
    //        return finalResult;
    //    }
    //
    //    private static List<GdlRule> splitRule(GdlRule rule, SentenceNameFactory snf) {
    //        //Basically do the following operations in order...
    //        //Add the current conjunct...
    //        //If at any time we're "holding onto" a variable that is not used later, get rid of it
    //        //Can we reverse that? Start at the end, work backwards
    //        //When we see a variable that isn't used in the head or a later conjunct, turn everything
    //        //before that point into its own rule
    //        Set<GdlVariable> varsInUse = Sets.newHashSet();
    //        varsInUse.addAll(GdlUtils.getVariables(rule.getHead()));
    //        varsInUse.addAll(GdlUtils.getVariables(rule.get(rule.getBody().size() - 1)));
    //        for (int i = rule.getBody().size() - 2; i >= 0; i--) {
    //            List<GdlVariable> varsInConjunct = GdlUtils.getVariables(rule.get(i));
    //            if (!varsInUse.containsAll(varsInConjunct)) {
    //                //Split the rule!
    //                GdlConstant newSentenceName = snf.getNewName(rule);
    //                Set<GdlVariable> varsUpToConjunct = Sets.newHashSet();
    //                for (int j = 0; j <= i; j++) {
    //                    varsUpToConjunct.addAll(GdlUtils.getVariables(rule.get(j)));
    //                }
    //                //Fix the ordering
    //                List<GdlVariable> varsToOutput = Lists.newArrayList(
    //                        Sets.intersection(varsInUse, varsUpToConjunct));
    //                //Head of the new rule, first sentence of the old rule
    //                GdlSentence intermediarySentence = GdlPool.getRelation(newSentenceName, varsToOutput);
    //                List<GdlLiteral> oldRuleBody = Lists.newArrayList();
    //                List<GdlLiteral> newRuleBody = Lists.newArrayList();
    //                oldRuleBody.add(intermediarySentence);
    //                for (int j = 0; j < rule.getBody().size(); j++) {
    //                    if (j <= i) {
    //                        newRuleBody.add(rule.get(j));
    //                    } else {
    //                        oldRuleBody.add(rule.get(j));
    //                    }
    //                }
    //                GdlRule oldRule = GdlPool.getRule(rule.getHead(), oldRuleBody);
    //                GdlRule newRule = GdlPool.getRule(intermediarySentence, newRuleBody);
    //                List<GdlRule> splitRules = splitRule(newRule, snf);
    //                splitRules.add(oldRule);
    //                return splitRules;
    //            }
    //        }
    //        return Lists.newArrayList(rule);
    //    }

    private static List<GdlRule> splitRule(GdlRule rule, SentenceNameFactory snf) {
        List<GdlRule> rules = Lists.newArrayList();

        while (rule.arity() > 2) {
            //Rewrite in terms of other stuff...
            //"rule" is going to become 0 through n - 1
            //and we're going to add to "rules" new head and n => old head
            int prefixArity = rule.arity() - 1;
            List<GdlLiteral> prefixBody = ImmutableList.copyOf(Iterables.limit(rule.getBody(), prefixArity));
            GdlLiteral lastConjunct = rule.get(prefixArity);
            //Variables to put in the new head are (in prefix body) and (in old head or last conjunct)
            Set<GdlVariable> varsInPrefixBody = GdlUtils.getVariables(prefixBody);
            Set<GdlVariable> varsInOldHead = GdlUtils.getVariablesSet(rule.getHead());
            Set<GdlVariable> varsInLastConjunct = GdlUtils.getVariablesSet(lastConjunct);
            Set<GdlVariable> newHeadVars = Sets.intersection(varsInPrefixBody, Sets.union(varsInOldHead, varsInLastConjunct));
            List<GdlVariable> sortedNewHeadVars = ImmutableList.copyOf(newHeadVars);

            GdlConstant newName = snf.getNewName(rule);
            GdlSentence newHead = createSentence(newName, sortedNewHeadVars);

            //Add the new 2-arity rule
            GdlRule newSmallRule = GdlPool.getRule(rule.getHead(), ImmutableList.of(newHead, lastConjunct));
            rules.add(newSmallRule);

            //And iterate on the remainder of the rule
            rule = GdlPool.getRule(newHead, prefixBody);
        }
        rules.add(rule);
        return rules;
    }

    private static GdlSentence createSentence(GdlConstant name, List<GdlVariable> body) {
        if (body.isEmpty()) {
            return GdlPool.getProposition(name);
        } else {
            return GdlPool.getRelation(name, body);
        }
    }

    private static class SentenceNameFactory {
        private final Set<GdlConstant> existingNames;

        private SentenceNameFactory(Set<GdlConstant> existingNames) {
            this.existingNames = existingNames;
        }

        public GdlConstant getNewName(GdlRule rule) {
            //TODO: Incorporate all the constants in the rule head
            return getNewName(rule.getHead().getName().toString());
        }

        private GdlConstant getNewName(String baseName) {
            GdlConstant newName;
            int i = 0;
            while (true) {
                newName = GdlPool.getConstant(baseName + "_tmp" + i);
                if (!existingNames.contains(newName)) {
                    break;
                }
                i++;
            }

            existingNames.add(newName);
            return newName;
        }

        public static SentenceNameFactory create(List<Gdl> rules) {
            Set<GdlConstant> existingNames = Sets.newHashSet();
            GdlVisitors.visitAll(rules, new GdlVisitor() {
                @Override
                public void visitSentence(GdlSentence sentence) {
                    existingNames.add(sentence.getName());
                }
            });
            return new SentenceNameFactory(existingNames);
        }
    }
}
