
package org.ggp.base.util.propnet.sancho;

import java.io.IOException;
import java.io.Serializable;
import java.io.Writer;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public abstract class LearningComponent extends BidirectionalPropagationComponent implements Serializable
{
  private static final long              serialVersionUID = 352527824700221111L;
  /** The inputs to the component. */
  protected List<LearningComponent>      inputs;
  /** The outputs of the component. */
  protected final Set<LearningComponent> outputs;

  protected boolean                      dirty;
  protected boolean                      cachedValue;

  public static int                      getCount;
  public static int                      dirtyCount;

  private long                           signature        = 0;

  /**
   * Creates a new Component with no inputs or outputs.
   */
  public LearningComponent()
  {
    this.inputs = new LinkedList<>();
    this.outputs = new HashSet<>();

    dirty = true;
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
    if (!inputs.contains(input))
    {
      inputs.add((LearningComponent)input);
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
    outputs.add((LearningComponent)output);
  }

  /**
   * Getter method.
   *
   * @return The inputs to the component.
   */
  @Override
  public Collection<? extends PolymorphicComponent> getInputs()
  {
    return inputs;
  }

  @Override
  public void removeInput(PolymorphicComponent input)
  {
    inputs.remove(input);
  }

  @Override
  public void removeAllInputs()
  {
    inputs.clear();
  }

  @Override
  public void removeAllOutputs()
  {
    outputs.clear();
  }

  @Override
  public void removeOutput(PolymorphicComponent output)
  {
    outputs.remove(output);
  }

  @Override
  public void crystalize()
  {
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
    assert inputs.size() == 1;
    return inputs.iterator().next();
  }

  /**
   * Getter method.
   *
   * @return The outputs of the component.
   */
  @Override
  public Collection<? extends PolymorphicComponent> getOutputs()
  {
    return outputs;
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
    assert outputs.size() == 1;
    return outputs.iterator().next();
  }

  /**
   * Gets the value of the Component.
   *
   * @return The value of the Component.
   */
  @Override
  public boolean getValue()
  {
    getCount++;

    if (dirty)
    {
      dirty = false;
      cachedValue = getValueInternal();
    }

    return cachedValue;
  }

  @Override
  public boolean isDirty()
  {
    return dirty;
  }

  @Override
  public void setDirty(boolean from, BidirectionalPropagationComponent source)
  {
    dirtyCount++;

    if (!dirty)
    {
      dirty = true;

      if (!(this instanceof PolymorphicTransition))
      {
        for (LearningComponent output : outputs)
        {
          output.setDirty(cachedValue, this);
        }
      }
    }
  }

  @Override
  public void reset(boolean disable)
  {
    if (this instanceof PolymorphicConstant)
    {
      dirty = false;
      cachedValue = getValueInternal();
    }
    else
    {
      if (disable)
      {
        dirty = false;
        cachedValue = false;
      }
      else
      {
        dirty = true;
      }
    }
  }

  protected class EncapsulatedCost
  {
    private int cost = 0;

    public int getCost()
    {
      return cost;
    }

    public void incrementCost()
    {
      cost++;
    }
  }

  protected abstract boolean getValueAndCost(EncapsulatedCost aggregatedCost);

  /**
   * Calculates the value of the Component.
   *
   * @return The value of the Component.
   */
  @Override
  protected abstract boolean getValueInternal();

  public void Optimize()
  {
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
  public void setSignature(long signature)
  {
    this.signature = signature;
  }

  @Override
  public long getSignature()
  {
    return signature;
  }
}
