
package org.ggp.base.util.propnet.sancho;

import java.io.IOException;
import java.io.Writer;

/**
 * The Or class is designed to represent logical OR gates.
 */
@SuppressWarnings("serial")
public final class ForwardDeadReckonOr extends ForwardDeadReckonComponent
                                                                         implements
                                                                         PolymorphicOr
{
  /**
   * Construct a new OR component
   *
   * @param numInputs Number of inputs if known, else -1.  If a specific number (other than -1)
   *        is specified then no subsequent changes to the inputs are permitted
   * @param numOutputs Number of outputs if known, else -1.  If a specific number (other than -1)
   *        is specified then no subsequent changes to the outputs are permitted
   */
  public ForwardDeadReckonOr(int numInputs, int numOutputs)
  {
    super(numInputs, numOutputs);
  }

  @Override
  public void reset(int instanceId)
  {
    super.reset(instanceId);
    state[instanceId] = 0;
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
    //validate();

    if (newState)
    {
      stateVal = ++state[instanceId];
    }
    else
    {
      stateVal = --state[instanceId];
    }

    boolean countNonZero = ((stateVal & opaqueValueMask) != 0);
    if (((stateVal & cachedStateMask) != 0) != countNonZero)
    {
      if ( countNonZero )
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
      int trueInputCount = 0;

      for (ForwardDeadReckonComponent c : inputsArray)
      {
        if (c.getLastPropagatedValue(instanceId))
        {
          trueInputCount++;
          break;
        }
      }

      if ((trueInputCount != 0) != ((state[instanceId] & cachedStateMask) != 0))
      {
        System.out.println("Validation failure for " + toString());
      }
    }
  }

  @Override
  public String toString()
  {
    return "OR";
  }

  @Override
  public void renderAsDot(Writer xiOutput) throws IOException
  {
    renderAsDot(xiOutput, "ellipse", "grey", "OR");
  }
}