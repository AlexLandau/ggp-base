
package org.ggp.base.util.propnet.sancho;

import org.ggp.base.util.gdl.grammar.GdlSentence;

/**
 * @author steve
 * Base class for all non-abstract Proposition implementations. Needed so that
 * instanceof PolymorphicProposition can be used regardless of the concrete class
 * hierarchy produced by the factory
 */
public abstract interface PolymorphicProposition extends PolymorphicComponent
{

  /**
   * Getter method.
   *
   * @return The name of the Proposition.
   */
  public abstract GdlSentence getName();

  /**
   * Setter method. This should only be rarely used; the name of a proposition
   * is usually constant over its entire lifetime.
   * @param newName new sentence to label the proposition with
   */
  public abstract void setName(GdlSentence newName);

  /**
   * Setter method.
   *
   * @param value
   *          The new value of the Proposition.
   */
  public abstract void setValue(boolean value);
}
