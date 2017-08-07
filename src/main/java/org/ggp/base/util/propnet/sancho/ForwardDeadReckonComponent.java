
package org.ggp.base.util.propnet.sancho;

import java.io.IOException;
import java.io.Serializable;
import java.io.Writer;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

/**
 * @author steve
 *  Abstract base class for all components in the ForwardDeadReckon family
 *  of PolymorphicComponents
 */
public abstract class ForwardDeadReckonComponent implements
                                                PolymorphicComponent,
                                                MultiInstanceComponent,
                                                Serializable
{
  private static final long               serialVersionUID   = 352527628564121134L;
  /** The inputs to the component. */
  protected ForwardDeadReckonComponent[]  inputsArray        = null;
  /**
   * The single input if there is only 1 input (else undefined)
   */
  protected ForwardDeadReckonComponent    singleInput        = null;
  /** The outputs of the component. */
  protected ForwardDeadReckonComponent[]  outputsArray       = null;

  /**
   * Propnet within which this component is a member
   */
  protected ForwardDeadReckonPropNet      propNet            = null;

  /**
   * Current number of inputs
   */
  protected int                           inputCount         = 0;
  /**
   * Current number of outputs
   */
  protected int                           outputCount        = 0;

  private Set<ForwardDeadReckonComponent> inputsList;
  private Set<ForwardDeadReckonComponent> outputsList;

  /**
   * Current state value for the component.  Two bits are reserved
   * as flags for last propagated and current states, the remainder
   * is opaque and usable by individual component subclasses as
   * they see fit
   */
  protected int[]                         state;
  /**
   * Mask for flag bit signifying current state
   */
  protected final int                     cachedStateMask = 1<<31;
  /**
   * Mask for flag bit signifying last propagated state
   */
  protected final int                     lastPropagatedStateMask = 1<<30;
  /**
   * Mask for opaque non-flag bits that may be used freely by subclasses
   */
  protected final int                     opaqueValueMask = ~(cachedStateMask | lastPropagatedStateMask);

  /**
   * Unique id for this component.  Opaque at this level
   */
  public int                              id;

  private long                            signature          = 0;

  /**
   * Construct a new component
   *
   * @param numInputs Number of inputs if known, else -1.  If a specific number (other than -1)
   *        is specified then no subsequent changes to the inputs are permitted
   * @param numOutputs Number of outputs if known, else -1.  If a specific number (other than -1)
   *        is specified then no subsequent changes to the outputs are permitted
   */
  public ForwardDeadReckonComponent(int numInputs, int numOutputs)
  {
    if (numInputs < 0 || numOutputs < 0)
    {
      inputsArray = null;
      inputsList = new HashSet<>();
      outputsArray = null;
      outputsList = new HashSet<>();
    }
    else
    {
      inputsArray = new ForwardDeadReckonComponent[numInputs];
      inputsList = null;
      outputsArray = new ForwardDeadReckonComponent[numOutputs];
      outputsList = null;
    }

    state = new int[1];
  }

  @Override
  public void crystalize()
  {
    if (inputsList != null)
    {
      inputsArray = new ForwardDeadReckonComponent[inputsList.size()];

      int index = 0;
      for (ForwardDeadReckonComponent c : inputsList)
      {
        inputsArray[index++] = c;
      }

      inputsList = null;
      inputCount = index;
      if (inputCount > 0)
      {
        singleInput = inputsArray[0];
      }
    }
    if (outputsList != null)
    {
      outputsArray = new ForwardDeadReckonComponent[outputsList.size()];

      int index = 0;
      for (ForwardDeadReckonComponent c : outputsList)
      {
        outputsArray[index++] = c;
      }

      outputsList = null;
      outputCount = index;
    }
  }

  @Override
  public void crystalize(int numInstances)
  {
    crystalize();

    state = new int[numInstances];
  }

  @Override
  public void removeInput(PolymorphicComponent input)
  {
    if (inputsList != null)
    {
      inputsList.remove(input);
      if (singleInput == input)
      {
        singleInput = (inputsList.size() == 0 ? null : inputsList.iterator()
            .next());
      }
    }
    else
    {
      throw new RuntimeException("Attempt to manipuate crystalized component");
    }
  }

  @Override
  public void removeAllInputs()
  {
    if (inputsList != null)
    {
      inputsList.clear();
      singleInput = null;
    }
    else
    {
      throw new RuntimeException("Attempt to manipuate crystalized component");
    }
  }

  @Override
  public void removeAllOutputs()
  {
    if (outputsList != null)
    {
      outputsList.clear();
    }
    else
    {
      throw new RuntimeException("Attempt to manipuate crystalized component");
    }
  }

  @Override
  public void removeOutput(PolymorphicComponent output)
  {
    if (outputsList != null)
    {
      outputsList.remove(output);
    }
    else
    {
      throw new RuntimeException("Attempt to manipuate crystalized component");
    }
  }

  /**
   * Record the parent propNet to which this component belongs
   * @param thePropNet1 parent propNet that owns the component
   */
  public void setPropnet(ForwardDeadReckonPropNet thePropNet1)
  {
    propNet = thePropNet1;
  }

  /**
   * Adds a new input.
   *
   * @param input
   *          A new input.
   */
  @Override
  public void addInput(PolymorphicComponent input)
  {
    assert(input != null);

    if (inputsArray == null)
    {
      inputsList.add((ForwardDeadReckonComponent)input);

      if (singleInput == null)
      {
        singleInput = (ForwardDeadReckonComponent)input;
      }
    }
    else
    {
      if (inputCount == 0)
      {
        singleInput = (ForwardDeadReckonComponent)input;
      }
      inputsArray[inputCount++] = (ForwardDeadReckonComponent)input;
    }
  }

  /**
   * Adds a new output.
   *
   * @param output
   *          A new output.
   */
  @Override
  public void addOutput(PolymorphicComponent output)
  {
    if (outputsArray == null)
    {
      outputsList.add((ForwardDeadReckonComponent)output);
    }
    else
    {
      outputsArray[outputCount++] = (ForwardDeadReckonComponent)output;
    }
  }

  /**
   * Getter method.
   *
   * @return The inputs to the component. Note this should be rarely used in
   *         the finalized state of the propnet
   */
  @Override
  public Collection<? extends PolymorphicComponent> getInputs()
  {
    if (inputsArray == null)
    {
      return inputsList;
    }
    LinkedList<ForwardDeadReckonComponent> result = new LinkedList<>();

    for (int i = 0; i < inputCount; i++)
    {
      result.add(inputsArray[i]);
    }

    return result;
  }

  /**
   * A convenience method, to get a single input. To be used only when the
   * component is known to have exactly one input.
   *
   * @return The single input to the component.
   */
  @Override
  public PolymorphicComponent getSingleInput()
  {
    return singleInput;
  }

  /**
   * Getter method.
   *
   * @return The outputs of the component.
   */
  @Override
  public Collection<? extends PolymorphicComponent> getOutputs()
  {
    if (outputsArray == null)
    {
      return outputsList;
    }

    LinkedList<ForwardDeadReckonComponent> result = new LinkedList<>();

    for (ForwardDeadReckonComponent c : outputsArray)
    {
      result.add(c);
    }

    return result;
  }

  /**
   * A convenience method, to get a single output. To be used only when the
   * component is known to have exactly one output.
   *
   * @return The single output to the component.
   */
  @Override
  public PolymorphicComponent getSingleOutput()
  {
    if (outputsArray == null)
    {
      return outputsList.iterator().next();
    }

    return outputsArray[0];
  }

  /**
   * Gets the value of the Component.
   *
   * @return The value of the Component.
   */
  @Override
  public boolean getValue()
  {
    return (state[0] & cachedStateMask) != 0;
  }

  /**
   * Gets the value of the Component.
   *
   * @return The value of the Component.
   */
  @Override
  public boolean getValue(int instanceId)
  {
    return (state[instanceId] & cachedStateMask) != 0;
  }

  /**
   * Retrieve the state last propagated by this component
   * @param instanceId Instance to retrieve for
   * @return last propagated state for the specified instance
   */
  public boolean getLastPropagatedValue(int instanceId)
  {
    return (state[instanceId] & lastPropagatedStateMask) != 0;
  }

  /**
   * Debug validation routine to check that the component state is
   * self consistent given its inputs
   */
  public void validate()
  {
    for (int instanceId = 0; instanceId < state.length; instanceId++)
    {
      if (inputCount == 1 &&
          !(getSingleInput() instanceof PolymorphicTransition))
      {
        if (((state[instanceId] & cachedStateMask) != 0) != inputsArray[0]
            .getLastPropagatedValue(instanceId))
        {
          System.out.println("Validation failure for " + toString());
        }
      }
    }
  }

  /**
   * Propagate the component's value if it has changed
   * @param instanceId
   */
  public void propagate(int instanceId)
  {
    int stateVal = state[instanceId];
    boolean cachedVal = ((stateVal & cachedStateMask) != 0);
    if (((stateVal & lastPropagatedStateMask) != 0) != cachedVal)
    {
      for (ForwardDeadReckonComponent output : outputsArray)
      {
        output.setKnownChangedState(cachedVal, instanceId, this);
      }

      if ( cachedVal )
      {
        state[instanceId] |= lastPropagatedStateMask;
      }
      else
      {
        state[instanceId] &= ~lastPropagatedStateMask;
      }
    }
  }

  /**
   * Note that a specified input has changed its value.  This
   * component should adjust its own current state accordingly
   * @param newState new state of the changed input (asserted to have changed)
   * @param instanceId Instance that the change is occuring on
   * @param source input component that is chnaging it state
   */
  public abstract void setKnownChangedState(boolean newState,
                                            int instanceId,
                                            ForwardDeadReckonComponent source);

  /**
   * Reset to the all-inputs false state
   * @param instanceId Instance for which the reset is happening
   */
  public void reset(int instanceId)
  {
    state[instanceId] = 0;
  }

  /**
   * Write a representation of the Component in .dot format.
   *
   * @param xiOutput - the output stream.
   * @param shape
   *          The value to use as the <tt>shape</tt> attribute.
   * @param fillcolor
   *          The value to use as the <tt>fillcolor</tt> attribute.
   * @param label
   *          The value to use as the <tt>label</tt> attribute.
   *
   * @throws IOException if there was a problem.
   */
  protected void renderAsDot(Writer xiOutput, String shape, String fillcolor, String label) throws IOException
  {
    xiOutput.write("\"@");
    xiOutput.write(Integer.toHexString(hashCode()));
    xiOutput.write("\"[shape=");
    xiOutput.write(shape);
    xiOutput.write(", style= filled, fillcolor=");
    xiOutput.write(fillcolor);
    xiOutput.write(", label=\"");
    xiOutput.write(label);
    xiOutput.write("\"]; ");

    for (PolymorphicComponent component : getOutputs())
    {
      xiOutput.write("\"@");
      xiOutput.write(Integer.toHexString(hashCode()));
      xiOutput.write("\"->\"@");
      xiOutput.write(Integer.toHexString(component.hashCode()));
      xiOutput.write("\"; ");
    }
  }

  @Override
  public void setSignature(long theSignature1)
  {
    signature = theSignature1;
  }

  @Override
  public long getSignature()
  {
    return signature;
  }
}
