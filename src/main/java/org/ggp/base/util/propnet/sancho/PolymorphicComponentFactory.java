
package org.ggp.base.util.propnet.sancho;

import java.util.Set;

import org.ggp.base.util.gdl.grammar.GdlSentence;
import org.ggp.base.util.propnet.architecture.PropNet;
import org.ggp.base.util.statemachine.Role;

/**
 * @author steve
 * Factory for polymorphic components.  Particular derived classes will
 * create component families specific to that derived type
 */
public abstract class PolymorphicComponentFactory
{
  /**
   * Create a new PolymorphicPropnet
   * @param basicPropNet ggp-base propnet to topologically clone
   * @return PolymorphicPropNet with topology matching the input
   */
  public abstract PolymorphicPropNet createPropNet(PropNet basicPropNet);

  /**
   * Create a new PolymorphicPropnet
   * @param propNet Polymorphic propnet (not necessarily of the same concrete implementation class) to topologically clone
   * @return PolymorphicPropNet with topology matching the input
   */
  public abstract PolymorphicPropNet createPropNet(PolymorphicPropNet propNet);

  /**
   * Create a new PolymorphicPropnet
   * @param roles The roles of the game the propnet forms a statemachine for
   * @param components The components (already wired together) comprising the propnet
   * @return PolymorphicPropNet encapsulating the provided network of components
   */
  public abstract PolymorphicPropNet createPropNet(Role[] roles,
                                                   Set<PolymorphicComponent> components);

  /**
   * Create a new And component
   * @param numInputs Number of inputs - may be -1 to specify an uncrystalized initial state
   * @param numOutputs Number of outputs - may be -1 to specify an uncrystalized initial state
   * @return new component
   */
  public abstract PolymorphicAnd createAnd(int numInputs, int numOutputs);

  /**
   * Create a new Or component
   * @param numInputs Number of inputs - may be -1 to specify an uncrystalized initial state
   * @param numOutputs Number of outputs - may be -1 to specify an uncrystalized initial state
   * @return new component
   */
  public abstract PolymorphicOr createOr(int numInputs, int numOutputs);

  /**
   * Create a new Not component
   * @param numOutputs Number of outputs - may be -1 to specify an uncrystalized initial state
   * @return new component
   */
  public abstract PolymorphicNot createNot(int numOutputs);

  /**
   * Create a new Constant component
   * @param numOutputs Number of outputs - may be -1 to specify an uncrystalized initial state
   * @param value whether the constanht is true of false
   * @return new component
   */
  public abstract PolymorphicConstant createConstant(int numOutputs,
                                                     boolean value);

  /**
   * Create a new Proposition component
   * @param numOutputs Number of outputs - may be -1 to specify an uncrystalized initial state
   * @param name Sentence to label the new proposition with
   * @return new component
   */
  public abstract PolymorphicProposition createProposition(int numOutputs,
                                                           GdlSentence name);

  /**
   * Create a new Transition component
   * @param numOutputs Number of outputs - may be -1 to specify an uncrystalized initial state
   * @return new component
   */
  public abstract PolymorphicTransition createTransition(int numOutputs);
}
