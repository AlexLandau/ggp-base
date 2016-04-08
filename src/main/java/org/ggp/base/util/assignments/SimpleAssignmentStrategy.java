package org.ggp.base.util.assignments;

import java.util.List;

import org.ggp.base.util.gdl.grammar.GdlConstant;

import com.google.common.collect.ImmutableList;

public class SimpleAssignmentStrategy implements AssignmentStrategy {
    private final ImmutableList<Integer> definedIndices;
    private final ImmutableList<ImmutableList<GdlConstant>> partialAssignments;

    public SimpleAssignmentStrategy(ImmutableList<Integer> definedIndices,
            ImmutableList<ImmutableList<GdlConstant>> partialAssignments) {
        this.definedIndices = definedIndices;
        this.partialAssignments = partialAssignments;
    }

    public static SimpleAssignmentStrategy create(List<Integer> definedIndices,
            List<ImmutableList<GdlConstant>> partialAssignments) {
        return new SimpleAssignmentStrategy(
                ImmutableList.copyOf(definedIndices),
                ImmutableList.copyOf(partialAssignments));
    }

    @Override
    public List<Integer> getDependentIndices() {
        return ImmutableList.of();
    }

    @Override
    public List<Integer> getDefinedIndices() {
        return definedIndices;
    }

    @Override
    public List<? extends List<GdlConstant>> getPartialAssignments(
            List<GdlConstant> inputs) {
        return partialAssignments;
    }

    @Override
    public int getRejectedIndex(List<GdlConstant> inputs) {
        return NO_INDEX_REJECTED;
    }

    @Override
    public String toString() {
        return "SimpleAssignmentStrategy [definedIndices=" + definedIndices
                + ", partialAssignments=" + partialAssignments + "]";
    }

}
