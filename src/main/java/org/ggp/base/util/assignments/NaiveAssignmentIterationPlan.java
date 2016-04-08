package org.ggp.base.util.assignments;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.concurrent.Immutable;

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
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;

//Odometer-style iteration only
@Immutable
public class NaiveAssignmentIterationPlan implements NewAssignmentIterationPlan {
    private final ImmutableList<GdlVariable> variables;
    private final ImmutableListMultimap<GdlVariable, GdlConstant> valuesByVariable;

    private NaiveAssignmentIterationPlan(ImmutableList<GdlVariable> variables,
            ImmutableListMultimap<GdlVariable, GdlConstant> valuesByVariable) {
        this.variables = variables;
        this.valuesByVariable = valuesByVariable;
    }

    public static NaiveAssignmentIterationPlan create(
            List<GdlVariable> variables,
            SetMultimap<GdlVariable, GdlConstant> valuesByVariable) {
        return new NaiveAssignmentIterationPlan(ImmutableList.copyOf(variables), ImmutableListMultimap.copyOf(valuesByVariable));
    }

    public static NaiveAssignmentIterationPlan create(GdlRule rule, SentenceDomainModel domainModel) {
        List<GdlVariable> variables = GdlUtils.getVariables(rule);
        Preconditions.checkState(variables.size() == Sets.newHashSet(variables).size());
        SetMultimap<GdlVariable, GdlConstant> valuesByVariable = HashMultimap.create();
        for (GdlLiteral literal : rule.getBody()) {
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
        }
        return create(variables, valuesByVariable);
    }

    @Override
    public NewAssignmentIterator getIterator() {
        return new NaiveAssignmentIterator();
    }

    private class NaiveAssignmentIterator implements NewAssignmentIterator {
        //		private final Odometer<GdlSentence> odometer;
        private final Iterator<List<GdlConstant>> delegate;

        public NaiveAssignmentIterator() {
            List<Set<GdlConstant>> sets = Lists.newArrayList();
            for (GdlVariable var : variables) {
                sets.add(ImmutableSet.copyOf(valuesByVariable.get(var)));
            }
            this.delegate = Sets.cartesianProduct(sets).iterator();
        }

        @Override
        public boolean hasNext() {
            return delegate.hasNext();
        }

        @Override
        public Map<GdlVariable, GdlConstant> next() {
            List<GdlConstant> list = delegate.next();
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
            System.out.println("Unsatisfiable literals: " + unsatisfiableLiterals);
        }
    }
}
