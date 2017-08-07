package org.ggp.base.util.statemachine.sancho;

import java.util.Iterator;

import org.ggp.base.util.propnet.sancho.ForwardDeadReckonInternalMachineState;
import org.ggp.base.util.propnet.sancho.ForwardDeadReckonLegalMoveInfo;
import org.ggp.base.util.propnet.sancho.ForwardDeadReckonLegalMoveSet;
import org.ggp.base.util.statemachine.Role;

/**
 * @author steve
 * Utils class that serves to provide a placeholder for static methods used to apply
 * filters
 */
public class StateMachineFilterUtils
{

  /**
   * Get the next move with respect to a specified filter - inserts pseudo-noops if required
   * @param xiFilter - filter to enumerate moves with respect to
   * @param itr - iterator on the collection of all moves
   * @return next move
   */
  public static ForwardDeadReckonLegalMoveInfo nextFilteredMove(StateMachineFilter xiFilter,
                                                          Iterator<ForwardDeadReckonLegalMoveInfo> itr)
  {
    if ( xiFilter == null )
    {
      return itr.next();
    }

    return xiFilter.nextFilteredMove(itr);
  }

  /**
   * @return the number of moves in the specified collection that are valid wih respect to the specified filter.
   *
   * @param xiMoves  - the moves.
   * @param role - role being queried
   * @param xiFilter - the filter.
   * @param includeForcedPseudoNoops - whether to include forced pseudo-noops
   */
  public static int getFilteredSize(ForwardDeadReckonInternalMachineState xiState,
                                    ForwardDeadReckonLegalMoveSet xiMoves,
                                    Role role,
                                    StateMachineFilter xiFilter,
                                    boolean includeForcedPseudoNoops)
  {
    if (xiFilter == null)
    {
      return xiMoves.getNumChoices(role);
    }

    return xiFilter.getFilteredMovesSize(xiState, xiMoves, role, includeForcedPseudoNoops);
  }

  /**
   * @return the number of moves in the specified collection that are valid wih respect to the specified filter.
   *
   * @param xiMoves  - the moves.
   * @param roleIndex - index of the role being queried
   * @param xiFilter - the filter.
   * @param includeForcedPseudoNoops - whether to include forced pseudo-noops
   */
  public static int getFilteredSize(ForwardDeadReckonInternalMachineState xiState,
                                    ForwardDeadReckonLegalMoveSet xiMoves,
                                    int roleIndex,
                                    StateMachineFilter xiFilter,
                                    boolean includeForcedPseudoNoops)
  {
    if (xiFilter == null)
    {
      return xiMoves.getNumChoices(roleIndex);
    }

    return xiFilter.getFilteredMovesSize(xiState, xiMoves, roleIndex, includeForcedPseudoNoops);
  }

}
