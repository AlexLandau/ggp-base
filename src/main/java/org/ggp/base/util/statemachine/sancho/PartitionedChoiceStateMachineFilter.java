package org.ggp.base.util.statemachine.sancho;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.ggp.base.util.propnet.sancho.ForwardDeadReckonInternalMachineState;
import org.ggp.base.util.propnet.sancho.ForwardDeadReckonLegalMoveInfo;
import org.ggp.base.util.propnet.sancho.ForwardDeadReckonLegalMoveSet;
import org.ggp.base.util.propnet.sancho.ForwardDeadReckonPropositionInfo;
import org.ggp.base.util.statemachine.Role;

/**
 * @author steve
 * State machine search filter for use in games where the legal moves are strictly partitioned
 * into partitions within which each move is a choice, and those partitions can be assessed
 * Separately and in any order
 * State machine search filter for use in puzzles exhibiting the following constraints:
 * 1) The solution goal condition requires a specific set of base propositions to all be true (or false)
 * 2) The base propositions in (1) are latched once set
 * 3) Each base proposition in the above set
 */
public class PartitionedChoiceStateMachineFilter implements StateMachineFilter
{
  private final List<PartitionInfo> partitions = new ArrayList<>();
  private ForwardDeadReckonLegalMoveSet activePartition = null;
  private ForwardDeadReckonLegalMoveSet activeMoveSet = null;
  private final ForwardDeadReckonLegalMoveSet activePartitionBuffer;
  private final ForwardDeadReckonInternalMachineState activeState;

  private class PartitionInfo
  {
    final ForwardDeadReckonLegalMoveSet moves;
    final ForwardDeadReckonPropositionInfo enablingProposition;
    final ForwardDeadReckonPropositionInfo disablingProposition;

    PartitionInfo(ForwardDeadReckonLegalMoveSet xiMoves,
                  ForwardDeadReckonPropositionInfo xiEnablingProposition,
                  ForwardDeadReckonPropositionInfo xiDisablingProposition)
    {
      moves = xiMoves;
      enablingProposition = xiEnablingProposition;
      disablingProposition = xiDisablingProposition;
    }

    boolean isActive(ForwardDeadReckonInternalMachineState xiState)
    {
      return ( (enablingProposition == null || xiState.contains(enablingProposition)) &&
               (disablingProposition == null || !xiState.contains(disablingProposition)) );
    }
  }

  /**
   * Constructor
   * @param xiMachine underlying state machine
   */
  public PartitionedChoiceStateMachineFilter(ForwardDeadReckonPropnetRuleEngine xiMachine)
  {
    activeState = xiMachine.createEmptyInternalState();
    activePartitionBuffer = new ForwardDeadReckonLegalMoveSet(xiMachine.getFullPropNet().getActiveLegalProps(0));
    activeMoveSet = new ForwardDeadReckonLegalMoveSet(xiMachine.getFullPropNet().getActiveLegalProps(0));

    //  We only support this on puzzles currently
    assert(xiMachine.getRoles().size()==1);
  }

  /**
   * Add a partition definition, which is a subset of legal moves, not overlapping any other partition
   * @param partitionMoves  Moves belonging to the new partition
   * @param enablingProposition Base prop that enables the partition (may be null)
   * @param disablingProposition Base prop that disables the partition (may be null)
   */
  public void addPartition(ForwardDeadReckonLegalMoveSet partitionMoves,
                           ForwardDeadReckonPropositionInfo enablingProposition,
                           ForwardDeadReckonPropositionInfo disablingProposition)
  {
    partitions.add(new PartitionInfo(partitionMoves, enablingProposition, disablingProposition));
  }

  private void determineActiveMoveSet(ForwardDeadReckonInternalMachineState xiState)
  {
    int smallestSize = Integer.MAX_VALUE;
    PartitionInfo smallestPartition = null;

    for(PartitionInfo candidate : partitions)
    {
      if ( candidate.isActive(xiState))
      {
        activePartitionBuffer.copy(candidate.moves);
        activePartitionBuffer.intersect(activeMoveSet);

        int size = activePartitionBuffer.getNumChoices(0);
        if ( size < smallestSize )
        {
          smallestSize = size;
          smallestPartition = candidate;
        }
      }
    }

    if ( smallestPartition == null )
    {
      activePartition = null;
    }
    else
    {
      activePartitionBuffer.copy(smallestPartition.moves);
      activePartitionBuffer.intersect(activeMoveSet);

      activePartition = activePartitionBuffer;
    }
  }

  @Override
  public boolean isFilteredTerminal(ForwardDeadReckonInternalMachineState xiState,
                                    ForwardDeadReckonPropnetRuleEngine xiStateMachine)
  {
    //  If this is not the state we have cached the active set for then calculate it
    if ( !activeState.equals(xiState))
    {
      activeState.copy(xiState);
      activeMoveSet.copy(xiStateMachine.getLegalMoveSet(xiState));

      determineActiveMoveSet(xiState);
    }

    //  If any partition has no choices consider the state terminal (it is not possible
    //  to win from it)
    if ( activePartition != null && activePartition.getNumChoices(0) == 0 )
    {
      return true;
    }

    return xiStateMachine.isTerminal(xiState);
  }

  @Override
  public int getFilteredMovesSize(ForwardDeadReckonInternalMachineState xiState,
                                  ForwardDeadReckonLegalMoveSet xiMoves,
                                  Role xiRole,
                                  boolean xiIncludeForcedPseudoNoops)
  {
    if ( !activeMoveSet.equals(xiMoves))
    {
      activeMoveSet.copy(xiMoves);

      determineActiveMoveSet(xiState);
    }

    //  activePartition can only be null in an underlying terminal state, in which
    //  we should no b asked about legal moves
    assert(activePartition != null);

    return activePartition.getNumChoices(xiRole);
  }

  @Override
  public int getFilteredMovesSize(ForwardDeadReckonInternalMachineState xiState,
                                  ForwardDeadReckonLegalMoveSet xiMoves,
                                  int xiRoleIndex,
                                  boolean xiIncludeForcedPseudoNoops)
  {
    if ( !activeMoveSet.equals(xiMoves))
    {
      activeMoveSet.copy(xiMoves);

      determineActiveMoveSet(xiState);
    }

    //  activePartition can only be null in an underlying terminal state, in which
    //  we should no b asked about legal moves
    assert(activePartition != null);

    return activePartition.getNumChoices(xiRoleIndex);
  }

  @Override
  public ForwardDeadReckonLegalMoveInfo nextFilteredMove(Iterator<ForwardDeadReckonLegalMoveInfo> xiItr)
  {
    ForwardDeadReckonLegalMoveInfo candidate;

    //  activePartition can only be null in an underlying terminal state, in which
    //  we should no b asked about legal moves
    assert(activePartition != null);

    do
    {
      candidate = xiItr.next();
    } while(!activePartition.isLegalMove(0, candidate));

    return candidate;
  }
}
