
package org.ggp.base.util.propnet.sancho;

import java.io.IOException;
import java.io.Writer;

/**
 * The Not class is designed to represent logical NOT gates.
 */
@SuppressWarnings("serial")
public final class ForwardDeadReckonNot extends ForwardDeadReckonComponent
                                                                          implements
                                                                          PolymorphicNot
{
  /**
   * Construct a new NOT component
   *
   * @param numOutputs Number of outputs if known, else -1.  If a specific number (other than -1)
   *        is specified then no subsequent changes to the outputs are permitted
   */
  public ForwardDeadReckonNot(int numOutputs)
  {
    super(1, numOutputs);
  }

  @Override
  public void setKnownChangedState(boolean newState,
                                   int instanceId,
                                   ForwardDeadReckonComponent source)
  {
    if ( !newState )
    {
      state[instanceId] |= cachedStateMask;
    }
    else
    {
      state[instanceId] &= ~cachedStateMask;
    }

    propagate(instanceId);
  }

  @Override
  public void reset(int instanceId)
  {
    super.reset(instanceId);
    state[instanceId] |= cachedStateMask;
  }

  @Override
  public void validate()
  {
    for (int instanceId = 0; instanceId < state.length; instanceId++)
    {
      if (((state[instanceId] & cachedStateMask) != 0) != !inputsArray[0]
          .getLastPropagatedValue(instanceId))
      {
        System.out.println("Validation failure for " + toString());
      }
    }
  }

  @Override
  public String toString()
  {
    return "NOT";
  }

  @Override
  public void renderAsDot(Writer xiOutput) throws IOException
  {
    renderAsDot(xiOutput, "invtriangle", "grey", "NOT");
  }
}