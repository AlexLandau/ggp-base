
package org.ggp.base.util.propnet.sancho;

import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.sancho.Factor;

/**
 *  Meta information about a legal move in the context of a particular propnet.
 */
public class ForwardDeadReckonLegalMoveInfo
{
  /**
   * GDL move.
   */
  public Move                         mMove;

  /**
   * Role the move is a move for - using the original GDL role index.
   */
  public int                          mRoleIndex;

  /**
   * Index in the master move list of this move info.  Note that this is now global and shared by all propnets.
   */
  public int                          mMasterIndex;

  /**
   * DOES proposition for this move.
   */
  public ForwardDeadReckonProposition mInputProposition;

  /**
   * Factor to which this move belongs in a factorized game, else null.
   */
  public Factor                       mFactor;

  /**
   * Whether this is an artificial noop inserted during game factorization.
   */
  public boolean                      mIsPseudoNoOp;

  /**
   * Whether this move amounts to a nop-op on the goal-relevant part of the state.
   */
  public boolean                      mIsVirtualNoOp;

  /**
   * Construct a new move info instance.
   */
  public ForwardDeadReckonLegalMoveInfo()
  {
    mIsPseudoNoOp = false;
  }

  /**
   * Construct a new move info instance specifying whether it is a pseudo-noop.
   *
   * @param moveIsPseudoNoOp - whether the move is an artificial noop introduced during factorization.
   */
  public ForwardDeadReckonLegalMoveInfo(boolean moveIsPseudoNoOp)
  {
    this.mIsPseudoNoOp = moveIsPseudoNoOp;
  }

  @Override
  public String toString()
  {
    if ( mMove == null )
    {
      return "NONE";
    }

    return mMove.toString();
  }

  /**
   * @return a representation of this move info that's suitable for storing across matches.
   */
  public String toPersistentString()
  {
    return "Role " + mRoleIndex + " " + toString();
  }
}
