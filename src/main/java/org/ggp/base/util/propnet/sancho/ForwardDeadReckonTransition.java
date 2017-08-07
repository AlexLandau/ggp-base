
package org.ggp.base.util.propnet.sancho;

import java.io.IOException;
import java.io.Writer;

/**
 * The Transition class is designed to represent pass-through gates.
 */
@SuppressWarnings("serial")
public final class ForwardDeadReckonTransition extends ForwardDeadReckonComponent implements PolymorphicTransition
{
  private ForwardDeadReckonComponentTransitionNotifier[]        owningTransitionInfoSet = null;
  private int                                                   associatedPropositionIndex          = -1;

  /**
   * Construct a new TRANSITION component
   *
   * @param numOutputs Number of outputs if known, else -1.  If a specific number (other than -1)
   *        is specified then no subsequent changes to the outputs are permitted
   */
  public ForwardDeadReckonTransition(int numOutputs)
  {
    super(1, numOutputs);
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

    if (owningTransitionInfoSet[instanceId] != null)
    {
      //ProfileSection methodSection = new ProfileSection("ForwardDeadReckonTransition.legalStateChange");
      //try
      {
        if (newState)
        {
          owningTransitionInfoSet[instanceId].add(associatedPropositionIndex);
        }
        else
        {
          owningTransitionInfoSet[instanceId].remove(associatedPropositionIndex);
        }
      }
      //finally
      //{
      //	methodSection.exitScope();
      //}
    }
  }

  /**
   * Set an instance of a notification handler to be called when this transition
   * changes value
   * @param triggerIndex notification index to raise (actually opaque at this level)
   * @param instanceId Instance this notifier is bound for
   * @param propositionTransitionNotifier notifier to call
   */
  public void setTransitionSet(int triggerIndex,
                               int instanceId,
                               ForwardDeadReckonComponentTransitionNotifier propositionTransitionNotifier)
  {
    owningTransitionInfoSet[instanceId] = propositionTransitionNotifier;
    associatedPropositionIndex = triggerIndex;
  }

  /**
   * Retrieve the trigger index (if any) associated with this proposition
   * @return associated trigger index, or -1 if none
   */
  public int getAssociatedPropositionIndex()
  {
    return associatedPropositionIndex;
  }

  @Override
  public void crystalize(int numInstances)
  {
    super.crystalize(numInstances);

    owningTransitionInfoSet = new ForwardDeadReckonInternalMachineState[numInstances];
  }

  @Override
  public String toString()
  {
    return "TRANSITION";
  }

  @Override
  public void renderAsDot(Writer xiOutput) throws IOException
  {
    renderAsDot(xiOutput, "box", "grey", "TRANSITION");
  }
}