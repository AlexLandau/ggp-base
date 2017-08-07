package org.ggp.base.util.statemachine.sancho;

import java.util.Iterator;

import org.ggp.base.util.propnet.sancho.ForwardDeadReckonInternalMachineState;
import org.ggp.base.util.propnet.sancho.ForwardDeadReckonLegalMoveInfo;
import org.ggp.base.util.propnet.sancho.ForwardDeadReckonLegalMoveSet;
import org.ggp.base.util.statemachine.Role;

/**
 * Null default filter that performs direct pass-through to an underlying state machine.
 */
public class NullStateMachineFilter implements StateMachineFilter
{
  @Override
  public boolean isFilteredTerminal(ForwardDeadReckonInternalMachineState xiState,
                                    ForwardDeadReckonPropnetStateMachine xiStateMachine)
  {
    return xiStateMachine.isTerminal(xiState);
  }

  @Override
  public int getFilteredMovesSize(ForwardDeadReckonInternalMachineState xiState,
                                  ForwardDeadReckonLegalMoveSet xiMoves,
                                  Role role,
                                  boolean xiIncludeForcedPseudoNoops)
  {
    return xiMoves.getNumChoices(role);
  }

  @Override
  public int getFilteredMovesSize(ForwardDeadReckonInternalMachineState xiState,
                                  ForwardDeadReckonLegalMoveSet xiMoves,
                                  int xiRoleIndex,
                                  boolean xiIncludeForcedPseudoNoops)
  {

    return xiMoves.getNumChoices(xiRoleIndex);
  }

  @Override
  public ForwardDeadReckonLegalMoveInfo nextFilteredMove(Iterator<ForwardDeadReckonLegalMoveInfo> xiItr)
  {
    return xiItr.next();
  }

}
