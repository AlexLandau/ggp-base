package org.ggp.base.util.statemachine.sancho;

import java.util.Iterator;

import org.ggp.base.util.propnet.sancho.ForwardDeadReckonInternalMachineState;
import org.ggp.base.util.propnet.sancho.ForwardDeadReckonLegalMoveInfo;
import org.ggp.base.util.propnet.sancho.ForwardDeadReckonLegalMoveSet;
import org.ggp.base.util.statemachine.Role;

/**
 * Interface to support semantic filtering on top f an underlying state machine.  This is intended
 * to allow semantic modification to aid search by reducing the search space.  Uses include:
 *    Factors - each factor implements this interface to filter move choices
 *    Latch-guided search - latch analysis can reveal partitionings of the input set such that
 *    the order of choice between partitions is irrelevant, in which case searching one at a time
 *    dramatically reduces branching factor (cf sudoku)
 * @author steve
 *
 */
public interface StateMachineFilter
{
  /**
   * Determine if a given state should be treated as terminal by the search.
   *
   * @param xiState - the state.
   * @param xiStateMachine - a state machine performing any necessary checking.
   *
   * @return virtual terminality.
   */
  boolean isFilteredTerminal(ForwardDeadReckonInternalMachineState xiState,
                             ForwardDeadReckonPropnetRuleEngine xiStateMachine);

  /**
   * Count available legal moves given a raw collection from the state machine
   * @param xiState - state against which the filtering is being performed
   * @param xiMoves - raw legals at state machine level
   * @param role - role being queried
   * @param includeForcedPseudoNoops - whether to include pseudo-noops in consideration
   * @return number of available choices
   */
  int getFilteredMovesSize(ForwardDeadReckonInternalMachineState xiState,
                           ForwardDeadReckonLegalMoveSet xiMoves,
                           Role role,
                           boolean includeForcedPseudoNoops);

  /**
   * Count available legal moves given a raw collection from the state machine
   * @param xiState - state against which the filtering is being performed
   * @param xiMoves - raw legals at state machine level
   * @param roleIndex - index of role being queried
   * @param includeForcedPseudoNoops - whether to include pseudo-noops in consideration
   * @return number of available choices
   */
  int getFilteredMovesSize(ForwardDeadReckonInternalMachineState xiState,
                           ForwardDeadReckonLegalMoveSet xiMoves,
                           int roleIndex,
                           boolean includeForcedPseudoNoops);

  /**
   * Logical iterator for available moves under the filter
   * @param itr - iterator from the underlying state machine
   * @return next choice
   */
  ForwardDeadReckonLegalMoveInfo nextFilteredMove(Iterator<ForwardDeadReckonLegalMoveInfo> itr);
}
