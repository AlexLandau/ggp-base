package org.ggp.base.util.assignments;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.concurrent.Immutable;

import org.ggp.base.util.AlloyUtils;
import org.ggp.base.util.Immutables;
import org.ggp.base.util.Odometer;
import org.ggp.base.util.gdl.GdlUtils;
import org.ggp.base.util.gdl.grammar.GdlConstant;
import org.ggp.base.util.gdl.grammar.GdlLiteral;
import org.ggp.base.util.gdl.grammar.GdlRule;
import org.ggp.base.util.gdl.grammar.GdlSentence;
import org.ggp.base.util.gdl.grammar.GdlTerm;
import org.ggp.base.util.gdl.grammar.GdlVariable;
import org.ggp.base.util.gdl.model.SentenceDomainModel;
import org.ggp.base.util.gdl.model.SentenceForm;
import org.ggp.base.util.gdl.model.SentenceFormDomain;

import com.google.common.base.Preconditions;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;

//Odometer-style iteration only
@Immutable
public class OdometerAssignmentIterationPlan implements NewAssignmentIterationPlan {
    //TODO: Better perf here if we use List<List<GdlConstant>> in valuesByVar?
    private final ImmutableList<GdlVariable> variables;
    private final ImmutableListMultimap<GdlVariable, GdlConstant> valuesByVariable;

    private final ImmutableMap<GdlLiteral, Integer> varIndexToUpdateByConjunct;

    private OdometerAssignmentIterationPlan(
            ImmutableList<GdlVariable> variables,
            ImmutableListMultimap<GdlVariable, GdlConstant> valuesByVariable,
            ImmutableMap<GdlLiteral, Integer> varIndexToUpdateByConjunct) {
        this.variables = variables;
        this.valuesByVariable = valuesByVariable;
        this.varIndexToUpdateByConjunct = varIndexToUpdateByConjunct;
    }

    public static OdometerAssignmentIterationPlan create(
            List<GdlVariable> variables,
            SetMultimap<GdlVariable, GdlConstant> valuesByVariable,
            Map<GdlLiteral, Integer> varIndexToUpdateByConjunct) {
        return new OdometerAssignmentIterationPlan(ImmutableList.copyOf(variables),
                ImmutableListMultimap.copyOf(valuesByVariable),
                ImmutableMap.copyOf(varIndexToUpdateByConjunct));
    }

    public static NewAssignmentIterationPlan create(GdlRule rule, SentenceDomainModel domainModel) {
        List<GdlVariable> variables = GdlUtils.getVariables(rule);
        if (variables.isEmpty()) {
            return SingletonAssignmentIterationPlan.create();
        }
        //		System.out.println("variables: " + variables);
        Map<GdlVariable, Integer> varsIndex = AlloyUtils.getListIndex(variables);
        Preconditions.checkState(variables.size() == Sets.newHashSet(variables).size());
        SetMultimap<GdlVariable, GdlConstant> valuesByVariable = HashMultimap.create();
        Map<GdlLiteral, Integer> varIndexToUpdateByConjunct = Maps.newHashMap();
        for (GdlLiteral literal : rule.getBody()) {
            //			System.out.println("literal: " + literal);
            if (literal instanceof GdlSentence) {
                //Collect the variables in the positive conjunct's domain
                GdlSentence sentence = (GdlSentence) literal;
                SentenceForm form = domainModel.getSentenceForm(sentence);
                SentenceFormDomain domain = domainModel.getDomain(form);
                List<GdlTerm> tuple = GdlUtils.getTupleFromSentence(sentence);

                for (int i = 0; i < tuple.size(); i++) {
                    GdlTerm term = tuple.get(i);
                    if (term instanceof GdlVariable) {
                        GdlVariable var = (GdlVariable) term;
                        Set<GdlConstant> possibleValues = domain.getDomainForSlot(i);
                        valuesByVariable.putAll(var, possibleValues);
                    }
                }
            }
            List<GdlVariable> varsInLiteral = GdlUtils.getVariables(literal);
            if (!varsInLiteral.isEmpty()) {
                ImmutableSortedSet<Integer> varIndices = varsInLiteral.stream()
                        .map(varsIndex::get)
                        .collect(Immutables.collectSortedSet());
                //Choice of first is based on Odometer class's behavior
                varIndexToUpdateByConjunct.put(literal, varIndices.first());
            }
        }
        return create(variables, valuesByVariable, varIndexToUpdateByConjunct);
    }

    @Override
    public NewAssignmentIterator getIterator() {
        return new OdometerAssignmentIterator();
    }

    private class OdometerAssignmentIterator implements NewAssignmentIterator {
        private final Odometer<GdlConstant> odometer;

        public OdometerAssignmentIterator() {
            List<List<GdlConstant>> values = Lists.newArrayListWithCapacity(variables.size());
            for (GdlVariable var : variables) {
                values.add(valuesByVariable.get(var));
            }
            odometer = Odometer.create(values);
        }

        @Override
        public boolean hasNext() {
            return odometer.hasNext();
        }

        @Override
        public Map<GdlVariable, GdlConstant> next() {
            List<GdlConstant> list = odometer.next();
            return toMap(list);
        }

        private Map<GdlVariable, GdlConstant> toMap(List<GdlConstant> list) {
            Map<GdlVariable, GdlConstant> map = Maps.newHashMapWithExpectedSize(list.size());
            for (int i = 0; i < variables.size(); i++) {
                map.put(variables.get(i), list.get(i));
            }
            return map;
        }

        @Override
        public void skipForward(Set<GdlLiteral> unsatisfiableLiterals,
                Map<GdlVariable, GdlConstant> assignment) {
            for (GdlLiteral literal : unsatisfiableLiterals) {
                int varIndexToUpdate = varIndexToUpdateByConjunct.get(literal);
                if (varIndexToUpdate > 0) {
                    GdlVariable var = variables.get(varIndexToUpdate);
                    GdlConstant value = assignment.get(var);
                    //					System.out.println("Variable ordering: " + variables);
                    //					System.out.println("Assignment was " + assignment);
                    //					System.out.println("Skipping forward in variable " + var + " because of conjunct " + literal);
                    odometer.skipPastValueInSlot(varIndexToUpdate, value);
                }
            }
        }
    }
}
