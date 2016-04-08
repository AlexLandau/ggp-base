package org.ggp.base.util.assignments;

import java.util.List;

import org.ggp.base.util.gdl.grammar.GdlConstant;

/**
 * This is a subcomponent of a ComplexAssignmentIterationPlan.
 */
public interface AssignmentStrategy {
    public static final int NO_INDEX_REJECTED = -1;
    //Takes input variables
    //TODO: Consider making these lists or even arrays instead
    List<Integer> getDependentIndices();
    List<Integer> getDefinedIndices();

    //Results should be sorted in odometer order? Or sorted by receiver?
    List<? extends List<GdlConstant>> getPartialAssignments(List<GdlConstant> inputs);
    //Last method returns empty list? Try this
    int getRejectedIndex(List<GdlConstant> inputs);
}