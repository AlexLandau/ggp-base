
package org.ggp.base.util.propnet.sancho;

import java.io.IOException;
import java.io.Writer;

import org.ggp.base.util.gdl.grammar.GdlSentence;
import org.ggp.base.util.statemachine.Role;

/**
 * Named propositions.
 */
@SuppressWarnings("serial")
public final class ForwardDeadReckonProposition extends ForwardDeadReckonComponent implements PolymorphicProposition
{
  /** The name of the Proposition. */
  private GdlSentence                                    name;
  /** Notification handler to notify on state changes (used for legal moves), if any */
  private ForwardDeadReckonComponentTransitionNotifier[] owningMoveSet  = null;
  /** Trigger index to notify on state changes (if any) */
  private int                                            associatedMoveIndex = -1;
  /** Opaque information higher layers may associate with this component */
  private ForwardDeadReckonPropositionInfo               opaqueInfo     = null;
  /**
   * Values for goal-propositions.
   */
  private Role mGoalRole;
  private int mGoalValue;

  /**
   * Creates a new Proposition with name <tt>name</tt>.
   *
   * @param numOutputs Number of outputs if known, else -1.  If a specific number (other than -1)
   *        is specified then no subsequent changes to the outputs are permitted
   * @param theName
   *          The name of the Proposition.
   */
  public ForwardDeadReckonProposition(int numOutputs, GdlSentence theName)
  {
    super(1, numOutputs);

    this.name = theName;
  }

  /**
   * Retrieve the opaque info et against this component
   * @return opaque info previously set
   */
  public ForwardDeadReckonPropositionInfo getInfo()
  {
    return opaqueInfo;
  }

  @Override
  public void crystalize(int numInstances)
  {
    super.crystalize(numInstances);

    owningMoveSet = new ForwardDeadReckonLegalMoveSet[numInstances];
  }

  /**
   * Set opaque info against this component - not semantically interpreted
   * @param info info to set
   */
  public void setInfo(ForwardDeadReckonPropositionInfo info)
  {
    opaqueInfo = info;
  }

  /**
   * Set an instance of a notification handler to be called when this proposition
   * changes value
   * @param triggerIndex notification index to raise (actually opaque at this level)
   * @param instanceId Instance this notifier is bound for
   * @param activeLegalMovesNotifier notifier to call
   */
  public void setTransitionSet(int triggerIndex,
                               int instanceId,
                               ForwardDeadReckonComponentTransitionNotifier activeLegalMovesNotifier)
  {
    owningMoveSet[instanceId] = activeLegalMovesNotifier;
    associatedMoveIndex = triggerIndex;
  }

  /**
   * Retrieve the trigger index (if any) associated with this proposition
   * @return associated trigger index, or -1 if none
   */
  public int getAssociatedTriggerIndex()
  {
    return associatedMoveIndex;
  }

  /**
   * Getter method.
   *
   * @return The name of the Proposition.
   */
  @Override
  public GdlSentence getName()
  {
    return name;
  }

  /**
   * Setter method. This should only be rarely used; the name of a proposition
   * is usually constant over its entire lifetime.
   */
  @Override
  public void setName(GdlSentence newName)
  {
    name = newName;
  }

  @Override
  public void setKnownChangedState(boolean newState,
                                   int instanceId,
                                   ForwardDeadReckonComponent source)
  {
    if ( newState )
    {
      state[instanceId] |= cachedStateMask;
    }
    else
    {
      state[instanceId] &= ~cachedStateMask;
    }

    if (owningMoveSet[instanceId] != null)
    {
      //ProfileSection methodSection = new ProfileSection("ForwardDeadReckonProposition.stateChange");
      //try
      {
        if (newState)
        {
          owningMoveSet[instanceId].add(associatedMoveIndex);
        }
        else
        {
          owningMoveSet[instanceId].remove(associatedMoveIndex);
        }
      }
      //finally
      //{
      //	methodSection.exitScope();
      //}
    }

    propagate(instanceId);
  }

  /**
   * Setter method.
   *
   * @param value
   *          The new value of the Proposition.
   * @param instanceId
   *          Instance within which the value change is occurring
   */
  public void setValue(boolean value, int instanceId)
  {
    if (((state[instanceId] & cachedStateMask) != 0) != value)
    {
      if ( value )
      {
        state[instanceId] |= cachedStateMask;
      }
      else
      {
        state[instanceId] &= ~cachedStateMask;
      }

      propagate(instanceId);
    }
  }

  @Override
  public String toString()
  {
    return name.toString() + "(" + ((state[0] & cachedStateMask) != 0) + ")";
  }

  @Override
  public void renderAsDot(Writer xiOutput) throws IOException
  {
    renderAsDot(xiOutput, "circle", ((state[0] & cachedStateMask) != 0) ? "red" : "white", name.toString());
  }

  @Override
  public void setValue(boolean value)
  {
    setValue(value, 0);
  }

  /**
   * Set the goal information, to avoid repeated computation.
   *
   * @param xiRole  - the role that this goal applies to.
   */
  public void setGoalInfo(Role xiRole)
  {
    mGoalRole = xiRole;
    mGoalValue = Integer.parseInt(getName().getBody().get(1).toString());
  }

  /**
   * @return the role associated with this goal proposition.
   */
  public Role getGoalRole()
  {
    assert(mGoalRole != null);
    return mGoalRole;
  }

  /**
   * @return the value of this goal proposition.
   */
  public int getGoalValue()
  {
    assert(mGoalRole != null);
    return mGoalValue;
  }
}