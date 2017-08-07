
package org.ggp.base.util.propnet.sancho;

import java.io.IOException;
import java.io.Writer;

/**
 * The Constant class is designed to represent nodes with fixed logical values.
 */
@SuppressWarnings("serial")
public final class ForwardDeadReckonConstant extends ForwardDeadReckonComponent implements PolymorphicConstant
{
  /** The value of the constant. */
  private final boolean value;

  /**
   * Creates a new Constant with value <tt>value</tt>.
   *
   * @param numOutputs Number of outputs if known, else -1.  If a specific number (other than -1)
   *        is specified then no subsequent changes to the outputs are permitted
   * @param theValue
   *          The value of the Constant.
   */
  public ForwardDeadReckonConstant(int numOutputs, boolean theValue)
  {
    super(0, numOutputs);
    value = theValue;
  }

  /**
   * Gets the value of the Component.
   *
   * @return The value of the Component.
   */
  @Override
  public boolean getValue()
  {
    return value;
  }

  @Override
  public void setKnownChangedState(boolean newState,
                                   int instanceId,
                                   ForwardDeadReckonComponent source)
  {
    //  Nothing to do here for a constant - this will actually never be called
    assert(false);
  }

  @Override
  public void reset(int instanceId)
  {
    super.reset(instanceId);
    if ( value )
    {
      state[instanceId] |= cachedStateMask;
    }
    else
    {
      state[instanceId] &= ~cachedStateMask;
    }
  }

  @Override
  public String toString()
  {
    return Boolean.toString(value).toUpperCase();
  }

  @Override
  public void renderAsDot(Writer xiOutput) throws IOException
  {
    renderAsDot(xiOutput, "doublecircle", "grey", Boolean.toString(value).toUpperCase());
  }
}