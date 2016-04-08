package org.ggp.base.util.assignments;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.ggp.base.util.AlloyUtils;
import org.ggp.base.util.Immutables;
import org.ggp.base.util.Pair;
import org.ggp.base.util.gdl.GdlUtils;
import org.ggp.base.util.gdl.grammar.GdlConstant;
import org.ggp.base.util.gdl.grammar.GdlDistinct;
import org.ggp.base.util.gdl.grammar.GdlPool;
import org.ggp.base.util.gdl.grammar.GdlRule;
import org.ggp.base.util.gdl.grammar.GdlSentence;
import org.ggp.base.util.gdl.grammar.GdlTerm;
import org.ggp.base.util.gdl.grammar.GdlVariable;
import org.ggp.base.util.gdl.model.SentenceDomainModel;
import org.ggp.base.util.gdl.model.SentenceForm;
import org.ggp.base.util.gdl.model.SentenceFormDomain;
import org.ggp.base.util.graph.DirectedMinimumSpanningTreeFinder;

import com.google.common.base.Preconditions;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;
import com.google.common.collect.Table;
import com.google.common.collect.Tables;

//@NotThreadSafe
//TODO: Change name; this isn't as general as it could be
//TODO: This doesn't currently take into account FunctionInfo and thus is
//      pretty terrible.
public class ComplexAssignmentIterationPlanFactory {
    private final ImmutableList<GdlSentence> positiveConjuncts;
    private final ImmutableMap<GdlSentence, SentenceFormDomain> domains;
    private final ImmutableSet<GdlDistinct> distinctConstraints;
    //What about negative conjuncts? The assignment iterator does at least have to
    //know about them, to know what variables to update

    //	private final List<Table<>
    //Given one variable and its value, list possible values for another variable...
    //	SetMultimap<GdlVariable, GdlConstant> variableDomains;

    public ComplexAssignmentIterationPlanFactory(
            ImmutableList<GdlSentence> positiveConjuncts,
            ImmutableMap<GdlSentence, SentenceFormDomain> domains,
            ImmutableSet<GdlDistinct> distinctConstraints) {
        this.positiveConjuncts = positiveConjuncts;
        this.domains = domains;
        this.distinctConstraints = distinctConstraints;
    }

    public static ComplexAssignmentIterationPlanFactory create(GdlRule rule,
            SentenceDomainModel domainModel) {
        ImmutableList<GdlSentence> positiveConjuncts = rule.getBody().stream()
                .filter(literal -> (literal instanceof GdlSentence))
                .map(literal -> (GdlSentence) literal)
                .collect(Immutables.collectList());
        ImmutableSet<GdlDistinct> distinctConstraints = rule.getBody().stream()
                .filter(literal -> (literal instanceof GdlDistinct))
                .map(literal -> (GdlDistinct) literal)
                .collect(Immutables.collectSet());
        Map<GdlSentence, SentenceFormDomain> domains = Maps.newHashMap();
        for (GdlSentence sentence : positiveConjuncts) {
            SentenceForm form = domainModel.getSentenceForm(sentence);
            domains.put(sentence, domainModel.getDomain(form));
        }
        return new ComplexAssignmentIterationPlanFactory(
                positiveConjuncts,
                ImmutableMap.copyOf(domains),
                distinctConstraints);
    }

    public NewAssignmentIterationPlan create() {
        //Do a sort of search through candidate plans...
        //That, or adapt the existing algorithm for finding a plan
        Set<GdlVariable> variablesToAssign = GdlUtils.getVariables(positiveConjuncts);

        SetMultimap<GdlVariable, GdlConstant> variableDomains = computeVariableDomains(variablesToAssign);

        //From variable/value to list of values for another given variable
        Map<GdlVariable, Table<GdlVariable, GdlConstant, Set<GdlConstant>>> possibleValuesGivenOtherVarByVar =
                computePossibleValuesGivenOtherVarByVar(variablesToAssign);

        limitPossibleValuesByVariableDomains(possibleValuesGivenOtherVarByVar, variableDomains);

        //Now this might be interesting...
        //What would be the thing we want to minimize here?
        //Basically, we'd have some ordering over the variables...
        //And then we'd want to assign each variable besides the first some "predecessor" from
        //earlier in the ordering
        //The predecessor would be used to assign values to the variable when its time comes
        //(so e.g. if it's a function, just one value given)

        /*
         * So we presumably want to minimize the number of value combinations we iterate over...
         * If we ignore variance within variables, that relationship looks something like...
         *
         * for x before y, we check the average domain size of y over the values of x
         *
         * Then we want to find the ordering that minimizes the product of these...
         *
         * This is a directed variant of the minimal spanning tree problem.
         *
         * We'll use an implementation of the Chu-Liu/Edmonds algorithm:
         * http://en.wikipedia.org/wiki/Edmonds%27_algorithm
         */

        GdlVariable dummyInitialVariable = createNewVariable(variablesToAssign);
        Set<GdlVariable> varsForDmst = Sets.newHashSet(variablesToAssign);
        varsForDmst.add(dummyInitialVariable);
        System.out.println("Positive conjuncts are: " + positiveConjuncts);
        Map<Pair<GdlVariable, GdlVariable>, Double> weights = computeWeights(variableDomains, possibleValuesGivenOtherVarByVar, dummyInitialVariable);
        System.out.println("weights: " + weights);
        Map<GdlVariable, GdlVariable> varSources = DirectedMinimumSpanningTreeFinder.findDmst(varsForDmst, dummyInitialVariable, weights);

        replaceWithDummyVarWherePossible(varSources, weights, dummyInitialVariable);

        //TODO: Turn this into an iteration plan
        return createPlan(varSources, dummyInitialVariable, variableDomains, possibleValuesGivenOtherVarByVar);

        //TODO: I'm worried this will do poorly where things like sums are involved, relative to the old approach...

        //Worth noting: This approach is not suited for a prover that's expected
        //to answer certain queries where certain values are filled in and others aren't.
        //Those will have different optimal plans (per query).


    }

    private void replaceWithDummyVarWherePossible(
            Map<GdlVariable, GdlVariable> varSources,
            Map<Pair<GdlVariable, GdlVariable>, Double> weights,
            GdlVariable dummyInitialVariable) {
        for (GdlVariable dependentVar : ImmutableList.copyOf(varSources.keySet())) {
            //			if (varSources.get(key))
            GdlVariable requiredVar = varSources.get(dependentVar);
            if (requiredVar != dummyInitialVariable) {
                double weightUsed = weights.get(Pair.of(requiredVar, dependentVar));
                double dummyWeight = weights.get(Pair.of(dummyInitialVariable, dependentVar));
                //				System.out.println("Weights are " + (weightUsed - dummyWeight));
                if (weightUsed >= dummyWeight) {
                    varSources.put(dependentVar, dummyInitialVariable);
                }
            }
        }
    }

    private NewAssignmentIterationPlan createPlan(
            Map<GdlVariable, GdlVariable> varSources,
            GdlVariable dummyInitialVariable,
            SetMultimap<GdlVariable, GdlConstant> variableDomains,
            Map<GdlVariable, Table<GdlVariable, GdlConstant, Set<GdlConstant>>> possibleValuesGivenOtherVarByVar) {
        System.out.println("varSources: " + varSources);
        //TODO: Replace references with dummyInitialVariable if it doesn't change the weights?
        List<GdlVariable> assignmentOrder = toOrdering(varSources, dummyInitialVariable);
        List<AssignmentStrategy> strategies = Lists.newArrayList();
        //		for (GdlVariable curVar : assignmentOrder) {
        for (int varIndex = 0; varIndex < assignmentOrder.size(); varIndex++) {
            GdlVariable curVar = assignmentOrder.get(varIndex);
            //Create a strategy
            final AssignmentStrategy strategy;
            if (varSources.get(curVar) == dummyInitialVariable) {
                List<Integer> definedIndices = ImmutableList.of(varIndex);
                List<ImmutableList<GdlConstant>> partialAssignments =
                        variableDomains.get(curVar).stream()
                        .map(constant -> ImmutableList.of(constant))
                        .collect(Immutables.collectList());
                //Just use the variable domain
                strategy = SimpleAssignmentStrategy.create(definedIndices, partialAssignments);
            } else {
                GdlVariable dependentVar = varSources.get(curVar);
                List<Integer> dependentIndices = ImmutableList.of(assignmentOrder.indexOf(dependentVar));
                List<Integer> definedIndices = ImmutableList.of(varIndex);

                //Get from possibleVars...
                Map<List<GdlConstant>, List<List<GdlConstant>>> contents = Maps.newHashMap();
                Map<GdlConstant, Set<GdlConstant>> possibleValues = possibleValuesGivenOtherVarByVar.get(curVar).row(dependentVar);
                for (Entry<GdlConstant, Set<GdlConstant>> entry : possibleValues.entrySet()) {
                    contents.put(ImmutableList.of(entry.getKey()),
                            entry.getValue().stream().map(constant -> ImmutableList.of(constant)).collect(Immutables.collectList()));
                }

                //Use something that depends on the ___
                strategy = DependentAssignmentStrategy.create(dependentIndices, definedIndices, contents);
            }
            strategies.add(strategy);
        }
        //		if (strategies.isEmpty()) {
        //			System.out.println("Strategies are empty, conjuncts are: " + positiveConjuncts);
        //		}
        return ComplexAssignmentIterationPlan.create(assignmentOrder, strategies);
    }

    private List<GdlVariable> toOrdering(
            Map<GdlVariable, GdlVariable> varSources,
            GdlVariable dummyInitialVariable) {
        List<GdlVariable> ordering = Lists.newArrayList();
        Set<GdlVariable> alreadyAdded = Sets.newHashSet(dummyInitialVariable);
        while (ordering.size() < varSources.size()) {
            //TODO: Infinite loop here...
            for (Entry<GdlVariable, GdlVariable> entry : varSources.entrySet()) {
                GdlVariable varToConsider = entry.getKey();
                GdlVariable dependency = entry.getValue();
                if (!alreadyAdded.contains(varToConsider) &&
                        alreadyAdded.contains(dependency)) {
                    ordering.add(varToConsider);
                    alreadyAdded.add(varToConsider);
                }
            }
            //			System.out.println("Ordering: " + ordering);
            //			System.out.println("Dummy variable: " + dummyInitialVariable);
            //			System.out.println("Var sources: " + varSources);
            //			System.out.println("Not already added: " + Sets.difference(alreadyAdded, varSources.keySet()));
        }
        return ordering;
    }

    //Note that we use the log of the average domain size, to minimize the products as opposed to the sums
    private Map<Pair<GdlVariable, GdlVariable>, Double> computeWeights(
            SetMultimap<GdlVariable, GdlConstant> variableDomains,
            Map<GdlVariable, Table<GdlVariable, GdlConstant, Set<GdlConstant>>> possibleValuesGivenOtherVarByVar,
            GdlVariable dummyInitialVariable) {
        // Pairs are <Parent, Child>, i.e. <determining variable, determined variable>
        Map<Pair<GdlVariable, GdlVariable>, Double> weights = Maps.newHashMap();

        //First, get the values for the dummy
        for (GdlVariable childVar : variableDomains.keySet()) {
            int domainSize = variableDomains.get(childVar).size();
            System.out.println("Domain size for " + childVar + ": " + domainSize + " -> " + Math.log(domainSize/1.0));
            weights.put(Pair.of(dummyInitialVariable, childVar), Math.log(domainSize/1.0));
        }

        //Now, for all the variable pairs...
        for (GdlVariable targetVariable : possibleValuesGivenOtherVarByVar.keySet()) {
            Table<GdlVariable, GdlConstant, Set<GdlConstant>> possibleValuesGivenOtherVar = possibleValuesGivenOtherVarByVar.get(targetVariable);
            for (GdlVariable inputVariable : possibleValuesGivenOtherVar.rowKeySet()) {
                System.out.println("Computing average domain size for " + targetVariable + " given fixed " + inputVariable);
                double averageDomainSize = getAverageSize(possibleValuesGivenOtherVar.row(inputVariable));
                System.out.println("Average domain size: " + averageDomainSize + " -> " + Math.log(averageDomainSize));
                weights.put(Pair.of(inputVariable, targetVariable), Math.log(averageDomainSize));
            }
        }
        return weights;
    }

    private double getAverageSize(Map<GdlConstant, Set<GdlConstant>> row) {
        int sum = 0;
        for (Set<GdlConstant> domain : row.values()) {
            System.out.println("Adding domain " + domain);
            sum += domain.size();
        }
        System.out.println("Sum is " + sum + ", size is " + row.size());
        return sum / (double) row.size();
    }

    private GdlVariable createNewVariable(Set<GdlVariable> variablesToAssign) {
        for (int i = 0; true; i++) {
            GdlVariable varToTry = GdlPool.getVariable("?a" + i);
            if (!variablesToAssign.contains(varToTry)) {
                return varToTry;
            }
        }
    }

    private void limitPossibleValuesByVariableDomains(
            Map<GdlVariable, Table<GdlVariable, GdlConstant, Set<GdlConstant>>> possibleValuesGivenOtherVarByVar,
            SetMultimap<GdlVariable, GdlConstant> variableDomains) {
        for (GdlVariable var : possibleValuesGivenOtherVarByVar.keySet()) {
            Table<GdlVariable, GdlConstant, Set<GdlConstant>> possibleValuesGivenOtherVar = possibleValuesGivenOtherVarByVar.get(var);
            Set<GdlConstant> basicDomain = variableDomains.get(var);
            for (Set<GdlConstant> subdomain : possibleValuesGivenOtherVar.values()) {
                subdomain.retainAll(basicDomain);
            }
        }
    }

    private Map<GdlVariable, Table<GdlVariable, GdlConstant, Set<GdlConstant>>> computePossibleValuesGivenOtherVarByVar(Set<GdlVariable> variables) {
        Map<GdlVariable, Table<GdlVariable, GdlConstant, Set<GdlConstant>>> result = Maps.newHashMap();
        for (GdlVariable targetVariable : variables) {
            Table<GdlVariable, GdlConstant, List<Set<GdlConstant>>> domainsGivenOtherVars = HashBasedTable.create();
            //Okay...
            for (GdlSentence positiveConjunct : positiveConjuncts) {
                List<GdlTerm> tuple = GdlUtils.getTupleFromSentence(positiveConjunct);
                //				if (tuple.contains(targetVariable)) {
                SentenceFormDomain domain = domains.get(positiveConjunct);
                for (int j = 0; j < tuple.size(); j++) {
                    if (tuple.get(j) == targetVariable) {
                        //For each other variable in the sentence...
                        for (int i = 0; i < tuple.size(); i++) {
                            GdlTerm term = tuple.get(i);
                            if (term instanceof GdlVariable && term != targetVariable) {
                                GdlVariable inputVariable = (GdlVariable) term;
                                //If that slot has that value, what can the target variable be?
                                //TODO: Should we just skip this if it's a Cartesian domain?
                                Map<GdlConstant, Set<GdlConstant>> domainsForSlotGivenValuesOfOtherSlot =
                                        domain.getDomainsForSlotGivenValuesOfOtherSlot(j, i);

                                for (Entry<GdlConstant, Set<GdlConstant>> entry : domainsForSlotGivenValuesOfOtherSlot.entrySet()) {
                                    GdlConstant inputVariableValue = entry.getKey();
                                    Set<GdlConstant> outputDomain = entry.getValue();
                                    if (domainsGivenOtherVars.get(inputVariable, inputVariableValue) == null) {
                                        domainsGivenOtherVars.put(inputVariable, inputVariableValue, Lists.newArrayList());
                                    }
                                    domainsGivenOtherVars.get(inputVariable, inputVariableValue).add(outputDomain);
                                }
                            }
                        }
                    }
                }
            }

            Table<GdlVariable, GdlConstant, Set<GdlConstant>> intersectionsGivenOtherVars = HashBasedTable.create(Tables.transformValues(
                    domainsGivenOtherVars, AlloyUtils::intersectAll));
            result.put(targetVariable, intersectionsGivenOtherVars);
        }
        //Do this part after the intersections
        for (GdlDistinct distinct : distinctConstraints) {
            if (distinct.getArg1() instanceof GdlVariable && distinct.getArg2() instanceof GdlVariable) {
                //TODO: Handle this case
                GdlVariable var1 = (GdlVariable) distinct.getArg1();
                GdlVariable var2 = (GdlVariable) distinct.getArg2();
                //It's okay to modify these directly, since we just copied over the table
                removeMatchingValues(result.get(var1).row(var2));
                removeMatchingValues(result.get(var2).row(var1));
            }
        }
        return result;
    }

    private void removeMatchingValues(Map<GdlConstant, Set<GdlConstant>> row) {
        for (Entry<GdlConstant, Set<GdlConstant>> entry : row.entrySet()) {
            entry.getValue().remove(entry.getKey());
        }
    }

    private SetMultimap<GdlVariable, GdlConstant> computeVariableDomains(Set<GdlVariable> variables) {
        ListMultimap<GdlVariable, Set<GdlConstant>> domainsToIntersect = ArrayListMultimap.create();
        for (GdlSentence positiveConjunct : positiveConjuncts) {
            SentenceFormDomain domain = domains.get(positiveConjunct);
            List<GdlTerm> tuple = GdlUtils.getTupleFromSentence(positiveConjunct);
            Preconditions.checkState(tuple.size() == domain.getForm().getTupleSize());
            for (int i = 0; i < tuple.size(); i++) {
                GdlTerm term = tuple.get(i);
                if (term instanceof GdlVariable) {
                    GdlVariable var = (GdlVariable) term;
                    domainsToIntersect.put(var, domain.getDomainForSlot(i));
                }
            }
        }
        //Intersect the domains
        SetMultimap<GdlVariable, GdlConstant> result = HashMultimap.create();
        domainsToIntersect.asMap().forEach((var, domains) -> {
            result.putAll(var, AlloyUtils.intersectAll(domains));
        });

        for (GdlDistinct distinct : distinctConstraints) {
            if (distinct.getArg1() instanceof GdlVariable && distinct.getArg2() instanceof GdlConstant) {
                result.remove(distinct.getArg1(), distinct.getArg2());
            } else if (distinct.getArg2() instanceof GdlVariable && distinct.getArg1() instanceof GdlConstant) {
                result.remove(distinct.getArg2(), distinct.getArg1());
            }
        }
        return result;
    }

}
