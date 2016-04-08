package org.ggp.base.util.assignments;

/**
 * Implementations should be immutable.
 */
public interface NewAssignmentIterationPlan {

    NewAssignmentIterator getIterator();
}
