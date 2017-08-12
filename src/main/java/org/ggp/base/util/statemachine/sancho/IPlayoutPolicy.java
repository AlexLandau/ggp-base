package org.ggp.base.util.statemachine.sancho;

import org.ggp.base.util.propnet.sancho.ForwardDeadReckonInternalMachineState;
import org.ggp.base.util.propnet.sancho.ForwardDeadReckonLegalMoveInfo;
import org.ggp.base.util.propnet.sancho.ForwardDeadReckonLegalMoveSet;

/**
 * Interface for playout policies, which can influence move selection during
 * playouts
 * @author steve
 */
public interface IPlayoutPolicy
{
  /**
   * @param stateMachine
   * @return a clone suitable for operation with the indicated state machine instance
   */
  public IPlayoutPolicy cloneFor(ForwardDeadReckonPropnetRuleEngine stateMachine);
  /**
   * @param state - current state with respect to which subsequent queries will be made
   * @param legalMoves  - moves available in this state
   * @param factor  - factor this playout is occuring in
   * @param moveIndex - index in the move/state history trace of the move to be selected
   * @param moveHistory - trace of moves played thus far in the playout, or null if the policy has indicated
   *                      it does not require move history
   * @param stateHistory - trace of states reached thus far in the playout, or null if the policy has indicated
   *                       it does not require state history
   */
  public void noteCurrentState(ForwardDeadReckonInternalMachineState state,
                               ForwardDeadReckonLegalMoveSet legalMoves,
                               StateMachineFilter factor,
                               int moveIndex,
                               ForwardDeadReckonLegalMoveInfo[] moveHistory,
                               ForwardDeadReckonInternalMachineState[] stateHistory);
  /**
   * @return whether this policy requires access to the playout's move trace so far
   */
  public boolean requiresMoveHistory();
  /**
   * @return whether this policy requires access to the playout's state trace so far
   * Note - must return TRUE if implementing either of the isAacceptableMove methods in
   * such a way that they can return false (i.e. - anything but a default unconditional true implementation)
   */
  public boolean requiresStateHistory();
  /**
   * Force-terminate the playout
   * TODO - we need a way to assert some goal values here really but for now
   * will only be using in games where the goals are ok in non-terminal state anyway
   * @return true to terminate the current playour
   */
  public boolean terminatePlayout();
  /**
   * Note a new turn has started in the game
   */
  public void noteNewTurn();
  /**
   * Note that a new playout is beginning
   */
  public void noteNewPlayout();
  /**
   * Note that a playout has completed.  At the point of call the underlying state machine is guaranteed
   * to be in the final state of the playout
   * @param length - of the playout in joint moves
   * @param moves - chooser moves at each joint move
   * @param states - states before each move played
   */
  public void noteCompletePlayout(int length, ForwardDeadReckonLegalMoveInfo[] moves, ForwardDeadReckonInternalMachineState[] states);
  /**
   * Select a specific move to play
   * @param roleIndex - role for which the move is to be selected
   * @return selected move, or null if no preference
   */
  public ForwardDeadReckonLegalMoveInfo selectMove(int roleIndex);
  /**
   * @param candidate
   * @param roleIndex role for which the acceptability is being determined
   * @return whether the candidate is acceptable to the policy
   * Note - if no available move is considered acceptable then a random one will be played
   */
  public boolean isAcceptableMove(ForwardDeadReckonLegalMoveInfo candidate,
                                  int roleIndex);
  /**
   * @param toState - state that the current move would result in
   * @param roleIndex role for which the acceptability is being determined
   * @return whether the resulting state is acceptable to the policy
   * Note - if no immediately reachable state is considered acceptable then a random move will be played
   */
  public boolean isAcceptableState(ForwardDeadReckonInternalMachineState toState,
                                   int roleIndex);

  /**
   * @param popDepth number of (consecutive) pops already performed to reach this point
   * @return whether to pop back up the playout stack another level
   */
  public boolean popStackOnAllUnacceptableMoves(int popDepth);
}
