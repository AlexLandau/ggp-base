
package org.ggp.base.util.propnet.sancho;

import java.io.IOException;
import java.io.Writer;


/**
 * The ForwardDeadReckonAnd class is designed to represent logical AND gates in
 * the ForwardDeadReckon family of PolymorphicComponents
 */
@SuppressWarnings("serial")
public final class ForwardDeadReckonAnd extends ForwardDeadReckonComponent implements PolymorphicAnd
{
  /**
   * Construct a new AND component
   *
   * @param numInputs Number of inputs if known, else -1.  If a specific number (other than -1)
   *        is specified then no subsequent changes to the inputs are permitted
   * @param numOutputs Number of outputs if known, else -1.  If a specific number (other than -1)
   *        is specified then no subsequent changes to the outputs are permitted
   */
  public ForwardDeadReckonAnd(int numInputs, int numOutputs)
  {
    super(numInputs, numOutputs);
  }

  @Override
  public void reset(int instanceId)
  {
    super.reset(instanceId);
    state[instanceId] = inputsArray.length;
  }

  @Override
  public void crystalize(int numInstances)
  {
    super.crystalize(numInstances);
  }

  @Override
  public void setKnownChangedState(boolean newState,
                                   int instanceId,
                                   ForwardDeadReckonComponent source)
  {
    int stateVal;

    if (newState)
    {
      stateVal = --state[instanceId];
    }
    else
    {
      stateVal = ++state[instanceId];
    }

    boolean countIsZero = ((stateVal & opaqueValueMask) == 0);
    if (((state[instanceId] & cachedStateMask) != 0) != countIsZero)
    {
      if ( countIsZero )
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
  public void validate()
  {
    for (int instanceId = 0; instanceId < state.length; instanceId++)
    {
      int falseInputCount = 0;

      for (ForwardDeadReckonComponent c : inputsArray)
      {
        if (!c.getLastPropagatedValue(instanceId))
        {
          falseInputCount++;
          break;
        }
      }

      if ((falseInputCount == 0) != ((state[instanceId] & cachedStateMask) != 0))
      {
        System.out.println("Validation failure for " + toString());
      }
    }
  }

  @Override
  public String toString()
  {
    return "AND";
  }

  @Override
  public void renderAsDot(Writer xiOutput) throws IOException
  {
    renderAsDot(xiOutput, "invhouse", "grey", "AND");
  }
}
