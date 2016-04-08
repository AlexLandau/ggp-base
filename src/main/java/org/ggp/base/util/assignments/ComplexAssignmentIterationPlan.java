package org.ggp.base.util.assignments;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;

import org.ggp.base.util.gdl.GdlUtils;
import org.ggp.base.util.gdl.grammar.GdlConstant;
import org.ggp.base.util.gdl.grammar.GdlLiteral;
import org.ggp.base.util.gdl.grammar.GdlVariable;

import com.google.common.base.Preconditions;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;

/**
 * A {@link NewAssignmentIterationPlan} implementation that incorporates
 * all the possibilities of the old AssignmentIterator:
 *
 * 1) Iteration over sources of conjuncts (i.e. explicit lists of possible
 *    sentences that define multiple variables at once)
 * 2) Functional conjuncts: sentences that, once one (or some) variables
 *    are defined, leave only one possibility for the remaining variable.
 * 3) Normal iteration over the remaining variables.
 *
 * The tricky part will be finding ways to find the right plan (or a
 * "good-enough" plan) quickly enough. However, if we can convert the
 * old code's algorithm, that should be fine for now.
 */
public class ComplexAssignmentIterationPlan implements
NewAssignmentIterationPlan {
    private final ImmutableList<GdlVariable> assignmentOrder;
    private final ImmutableList<AssignmentStrategy> strategies;

    private final ImmutableList<Integer> strategyDefiningVarIndex;
    private final ImmutableList<Integer> indexWithinStrategyForVarIndex;

    //TODO: In constructor, check consistency of strategies, assignment order
    public ComplexAssignmentIterationPlan(
            ImmutableList<GdlVariable> assignmentOrder,
            ImmutableList<AssignmentStrategy> strategies,
            ImmutableList<Integer> strategyDefiningVarIndex,
            ImmutableList<Integer> indexWithinStrategyForVarIndex) {
        this.assignmentOrder = assignmentOrder;
        this.strategies = strategies;
        this.strategyDefiningVarIndex = strategyDefiningVarIndex;
        this.indexWithinStrategyForVarIndex = indexWithinStrategyForVarIndex;
        validatePlan();
    }

    //TODO: Probably want to convert these strategies from some other format
    public static NewAssignmentIterationPlan create(List<GdlVariable> assignmentOrder,
            List<AssignmentStrategy> strategies) {
        if (strategies.isEmpty()) {
            Preconditions.checkArgument(assignmentOrder.isEmpty());
            return SingletonAssignmentIterationPlan.create();
        }

        List<Integer> strategyDefiningVarIndex = Lists.newArrayList();
        List<Integer> indexWithinStrategyForVarIndex = Lists.newArrayList();
        for (int i = 0; i < assignmentOrder.size(); i++) {
            strategyDefiningVarIndex.add(-1);
            indexWithinStrategyForVarIndex.add(-1);
        }

        for (int s = 0; s < strategies.size(); s++) {
            AssignmentStrategy strategy = strategies.get(s);
            List<Integer> definedIndices = strategy.getDefinedIndices();
            for (int i = 0; i < definedIndices.size(); i++) {
                int varIndex = definedIndices.get(i);
                Preconditions.checkState(strategyDefiningVarIndex.get(varIndex) == -1);
                Preconditions.checkState(indexWithinStrategyForVarIndex.get(varIndex) == -1);
                strategyDefiningVarIndex.set(varIndex, s);
                indexWithinStrategyForVarIndex.set(varIndex, i);
            }
        }
        return new ComplexAssignmentIterationPlan(
                ImmutableList.copyOf(assignmentOrder),
                ImmutableList.copyOf(strategies),
                ImmutableList.copyOf(strategyDefiningVarIndex),
                ImmutableList.copyOf(indexWithinStrategyForVarIndex));
    }

    private void validatePlan() {
        //TODO: Add more validation checks
        for (AssignmentStrategy strategy : strategies) {
            Preconditions.checkState(isSorted(strategy.getDependentIndices()));
            Preconditions.checkState(isSorted(strategy.getDefinedIndices()));
        }
    }

    private boolean isSorted(List<Integer> indices) {
        return Ordering.natural().isStrictlyOrdered(indices);
    }

    public class ComplexAssignmentIterator implements NewAssignmentIterator {
        private final List<List<? extends List<GdlConstant>>> partialAssignmentsByStrategy;
        private final List<Integer> currentPartialAssignmentIndex;
        private boolean done = false;

        public ComplexAssignmentIterator() {
            //Go through strategies, make sure they're all fulfilled
            //incrementStrategy will use set()
            partialAssignmentsByStrategy = Lists.newArrayListWithCapacity(strategies.size());
            currentPartialAssignmentIndex = Lists.newArrayListWithCapacity(strategies.size());
            for (int i = 0; i < strategies.size(); i++) {
                partialAssignmentsByStrategy.add(null);
                currentPartialAssignmentIndex.add(-1);
            }
            regenerateStrategy(0);
            if (partialAssignmentsByStrategy.get(0) == null
                    || partialAssignmentsByStrategy.get(0).isEmpty()) {
                done = true;
            }
        }

        private void regenerateStrategy(int strategyIndex) {
            //Collect assignments for the strategy given its predecessors
            AssignmentStrategy strategy = strategies.get(strategyIndex);
            List<GdlConstant> inputs = collectInputsFor(strategy);
            List<? extends List<GdlConstant>> newPartialAssignments = strategy.getPartialAssignments(inputs);
            if (newPartialAssignments.isEmpty()) {
                //Need to recursively backtrack... or will that not quite work?
                int rejectedIndex = strategy.getRejectedIndex(inputs);
                if (rejectedIndex == AssignmentStrategy.NO_INDEX_REJECTED) {
                    //No more assignments are possible
                    done = true;
                    return;
                }
                //Advance the earlier strategy instead
                advanceStrategy(getStrategyToAdvanceFromVarIndex(rejectedIndex));
                return;
            }
            //(resetting indices as we go)
            partialAssignmentsByStrategy.set(strategyIndex, newPartialAssignments);
            currentPartialAssignmentIndex.set(strategyIndex, 0);
            //Then, unless we're the last one, increment the next strategy
            if (strategyIndex + 1 < strategies.size()) {
                regenerateStrategy(strategyIndex + 1);
            }
        }

        private void advanceStrategy(int strategyIndex) {
            //Can we advance?
            int curIndex = currentPartialAssignmentIndex.get(strategyIndex);
            if (curIndex + 1 < partialAssignmentsByStrategy.get(strategyIndex).size()) {
                //Yes, advance
                currentPartialAssignmentIndex.set(strategyIndex, curIndex + 1);
                if (strategyIndex + 1 < strategies.size()) {
                    regenerateStrategy(strategyIndex + 1);
                }
                return;
            } else {
                //No, advance an earlier strategy, if possible
                if (strategyIndex == 0) {
                    done = true;
                    return;
                } else {
                    advanceStrategy(strategyIndex - 1);
                }
            }
        }

        private int getStrategyToAdvanceFromVarIndex(int rejectedVarIndex) {
            return strategyDefiningVarIndex.get(rejectedVarIndex);
        }

        private List<GdlConstant> collectInputsFor(AssignmentStrategy strategy) {
            List<GdlConstant> inputs = Lists.newArrayListWithCapacity(strategy.getDependentIndices().size());
            for (int dependentIndex : strategy.getDependentIndices()) {
                inputs.add(getCurValueForVarIndex(dependentIndex));
            }
            return inputs;
        }

        private GdlConstant getCurValueForVarIndex(int varIndex) {
            int strategyIndex = strategyDefiningVarIndex.get(varIndex);
            int curAssignmentIndex = currentPartialAssignmentIndex.get(strategyIndex);
            int constantIndex = indexWithinStrategyForVarIndex.get(varIndex);
            return partialAssignmentsByStrategy.get(strategyIndex).get(curAssignmentIndex).get(constantIndex);
        }

        @Override
        public boolean hasNext() {
            return !done;
        }

        @Override
        public Map<GdlVariable, GdlConstant> next() {
            Map<GdlVariable, GdlConstant> curAssignment = collectAssignment();
            advanceStrategy(strategies.size() - 1);
            return curAssignment;
        }

        private Map<GdlVariable, GdlConstant> collectAssignment() {
            Map<GdlVariable, GdlConstant> assignment = Maps.newHashMapWithExpectedSize(assignmentOrder.size());
            for (int i = 0; i < assignmentOrder.size(); i++) {
                assignment.put(assignmentOrder.get(i), getCurValueForVarIndex(i));
            }
            return assignment;
        }

        @Override
        public void skipForward(Set<GdlLiteral> unsatisfiableLiterals,
                Map<GdlVariable, GdlConstant> assignment) {
            //			if (true) return;
            for (GdlLiteral literal : unsatisfiableLiterals) {
                //TODO: Does this really make sense?
                //No, only for odometers
                //We should only be skipping until this partial assignment is different somehow
                //				int lastVarIndex = getLastVarIndex(literal);
                List<Integer> varIndicesInLiteral = getVarIndices(literal);
                int lastVarIndex = getMax(varIndicesInLiteral);
                //TODO: Might want to consider multiple strategies for advancement here?
                //Or do we want the last one?
                int strategyIndex = getStrategyToAdvanceFromVarIndex(lastVarIndex);
                //Get partial assignment
                //Need to skip past a partial assignment...
                //TODO: Need to make this the part of the assignment relevant to the
                //literal, not to the strategy
                skipPastPartialAssignmentInIndex(strategyIndex, varIndicesInLiteral, assignment);
            }
        }

        private int getMax(List<Integer> varIndicesInLiteral) {
            int max = -1;
            for (int val : varIndicesInLiteral) {
                if (val > max) {
                    max = val;
                }
            }
            return max;
        }

        //		private Map<GdlVariable, GdlConstant> getPartialAssignmentForStrategy(
        //				int strategyIndex, Map<GdlVariable, GdlConstant> assignment) {
        //			//Figure out all the variables in the strategy index,
        //			// TODO Implement
        //		}

        private void skipPastPartialAssignmentInIndex(int strategyIndex,
                List<Integer> varIndicesInLiteral, Map<GdlVariable, GdlConstant> fullAssignment) {
            //			if (true) return;
            while (!done &&
                    partialAssignmentMatches(varIndicesInLiteral, fullAssignment)
                    /*getCurValueForVarIndex(varIndex) == valueToSkip*/) {
                advanceStrategy(strategyIndex);
            }
        }

        /**
         * Returns true iff all of the var indices listed have the same values in the
         * current assignment as in the given assignment.
         */
        private boolean partialAssignmentMatches(
                List<Integer> varIndicesInLiteral,
                Map<GdlVariable, GdlConstant> fullAssignment) {
            for (int varIndex : varIndicesInLiteral) {
                GdlVariable var = assignmentOrder.get(varIndex);
                if (getCurValueForVarIndex(varIndex) !=
                        fullAssignment.get(var)) {
                    return false;
                }
            }
            return true;
        }

        private boolean partialAssignmentMatches(
                int strategyIndex,
                List<GdlConstant> curPartialAssignmentForStrategy,
                Map<GdlVariable, GdlConstant> fullAssignment) {
            List<Integer> definedIndices = strategies.get(strategyIndex).getDefinedIndices();
            Preconditions.checkArgument(definedIndices.size() == curPartialAssignmentForStrategy.size());
            for (int i = 0; i < curPartialAssignmentForStrategy.size(); i++) {
                //What variable is this?
                int varIndex = definedIndices.get(i);
                GdlVariable var = assignmentOrder.get(varIndex);
                GdlConstant curValue = curPartialAssignmentForStrategy.get(i);
                if (fullAssignment.get(var) != curValue) {
                    return false;
                }
            }
            return true;
        }

        private List<GdlConstant> getCurPartialAssignmentForStrategy(int strategyIndex) {
            int curAssignmentIndex = currentPartialAssignmentIndex.get(strategyIndex);
            return partialAssignmentsByStrategy.get(strategyIndex).get(curAssignmentIndex);
        }

    }

    //	private int getAssignmentOrderIndex(GdlVariable var) {
    //		//TODO: Optimize?
    ////		return Lists.newArrayList(assignmentOrder).indexOf(var);
    ////		assignmentOrder.get(0);
    //		return 0;
    //	}


    private final LoadingCache<GdlLiteral, Integer> lastVarIndexCache =
            CacheBuilder.newBuilder().build(new CacheLoader<GdlLiteral, Integer>() {
                @Override
                public Integer load(GdlLiteral literal) throws Exception {
                    //Oddly, writing it this way causes the MPNF to give bad results
                    //Not going to try to track down the exact bug...
                    //TODO: Test with later versions of Java 8/ecj?
                    //					return GdlUtils.getVariables(literal).stream()
                    //						.mapToInt(assignmentOrder::indexOf)
                    //						.max().getAsInt();
                    List<GdlVariable> variables = GdlUtils.getVariables(literal);
                    int max = -1;
                    for (GdlVariable variable : variables) {
                        int curIndex = assignmentOrder.indexOf(variable);
                        if (curIndex > max) {
                            max = curIndex;
                        }
                    }
                    return max;
                }
            });
    private int getLastVarIndex(GdlLiteral literal) {
        return lastVarIndexCache.getUnchecked(literal);
    }

    private final LoadingCache<GdlLiteral, List<Integer>> varIndicesCache =
            CacheBuilder.newBuilder().build(new CacheLoader<GdlLiteral, List<Integer>>() {
                @Override
                public List<Integer> load(GdlLiteral literal) throws Exception {
                    List<GdlVariable> variables = GdlUtils.getVariables(literal);
                    SortedSet<Integer> indices = Sets.newTreeSet();
                    for (GdlVariable variable : variables) {
                        indices.add(assignmentOrder.indexOf(variable));
                    }
                    return ImmutableList.copyOf(indices);
                }
            });
    private List<Integer> getVarIndices(GdlLiteral literal) {
        return varIndicesCache.getUnchecked(literal);
    }

    @Override
    public NewAssignmentIterator getIterator() {
        return new ComplexAssignmentIterator();
    }

    @Override
    public String toString() {
        return "ComplexAssignmentIterationPlan [assignmentOrder="
                + assignmentOrder + ", strategies=" + strategies + "]";
    }
}
