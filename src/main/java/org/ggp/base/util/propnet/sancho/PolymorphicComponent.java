
package org.ggp.base.util.propnet.sancho;

import java.io.IOException;
import java.io.Writer;
import java.util.Collection;

/**
 * The root class of the PolymorphicComponent hierarchy, which is designed to
 * represent nodes in a PropNet. The general contract of derived classes is to
 * override all methods. This mirrors the class Component in the basic propnet
 * code.
 */
public interface PolymorphicComponent
{
  /** The inputs to the component. */

  /**
   * Adds a new input.
   *
   * @param input
   *          A new input.
   */
  public abstract void addInput(PolymorphicComponent input);

  /**
   * Adds a new output.
   *
   * @param output
   *          A new output.
   */
  public abstract void addOutput(PolymorphicComponent output);

  /**
   * Getter method.
   *
   * @return The inputs to the component.
   */
  public abstract Collection<? extends PolymorphicComponent> getInputs();

  /**
   * A convenience method, to get a single input. To be used only when the
   * component is known to have exactly one input.
   *
   * @return The single input to the component.
   */
  public abstract PolymorphicComponent getSingleInput();

  /**
   * Getter method.
   *
   * @return The outputs of the component.
   */
  public abstract Collection<? extends PolymorphicComponent> getOutputs();

  /**
   * A convenience method, to get a single output. To be used only when the
   * component is known to have exactly one output.
   *
   * @return The single output to the component.
   */
  public abstract PolymorphicComponent getSingleOutput();

  /**
   * Gets the value of the Component.
   *
   * @return The value of the Component.
   */
  public abstract boolean getValue();

  /**
   * Write a string representation of the Component in .dot format.
   *
   * @param xiOutput - the output stream to write to.
   *
   * @throws IOException if there was a problem writing the component.
   */
  public abstract void renderAsDot(Writer xiOutput) throws IOException;

  /**
   * Remove a specified input.  Valid only before crystalize() is called
   * @param input
   */
  public abstract void removeInput(PolymorphicComponent input);

  /**
   * Remove all inputs.  Valid only before crystalize() is called
   */
  public abstract void removeAllInputs();

  /**
   * Remove all outputs.  Valid only before crystalize() is called
   */
public abstract void removeAllOutputs();

  /**
   * Remove a specified output.  Valid only before crystalize() is called
   * @param output
   */
  public abstract void removeOutput(PolymorphicComponent output);

  /**
   * Crystalize the implementation of this component into its most runtime
   * optimal internal representation.  No chnages to inputs or outputs are
   * permitted once this has been called
   */
  public abstract void crystalize();

  /**
   * Set a signature value for this component.
   * This is opaque at this level, but intended usage is to label
   * the component with a hash of the value it calculates given all
   * of its input network
   * @param signature opaque signature to set
   */
  public abstract void setSignature(long signature);

  /**
   * @return component's signature value
   */
  public abstract long getSignature();
}