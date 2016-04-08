package org.ggp.base.util.assignments;

import java.util.Collection;
import java.util.List;

import org.ggp.base.util.Immutables;
import org.ggp.base.util.gdl.grammar.GdlConstant;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

public class MultipleAssignmentStrategy implements AssignmentStrategy {
    private final ImmutableList<Integer> definedIndices;
    private final ImmutableList<ImmutableList<GdlConstant>> partialAssignments;

    public MultipleAssignmentStrategy(ImmutableList<Integer> definedIndices,
            ImmutableList<ImmutableList<GdlConstant>> partialAssignments) {
        this.definedIndices = definedIndices;
        this.partialAssignments = partialAssignments;
    }

    public static AssignmentStrategy create(List<Integer> definedIndices,
            Collection<List<GdlConstant>> partialAssignments) {
        return new MultipleAssignmentStrategy(ImmutableList.copyOf(definedIndices),
                partialAssignments.stream()
                .map(ImmutableList::copyOf)
                .collect(Immutables.collectList()));
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
        Preconditions.checkArgument(inputs.isEmpty());
        return partialAssignments;
    }

    @Override
    public int getRejectedIndex(List<GdlConstant> inputs) {
        //TODO: Huh? Different meaning
        //But why is this getting called?
        return AssignmentStrategy.NO_INDEX_REJECTED;
    }

    @Override
    public String toString() {
        return "MultipleAssignmentStrategy [definedIndices=" + definedIndices
                + ", partialAssignments=" + partialAssignments + "]";
    }
}
