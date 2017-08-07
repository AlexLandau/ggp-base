package org.ggp.base.util.propnet.sancho;

public abstract class BidirectionalPropagationComponent implements
                                                       PolymorphicComponent
{

  public abstract boolean isDirty();

  public abstract void setDirty(boolean from,
                                BidirectionalPropagationComponent source);

  public abstract void reset(boolean disable);

  /**
   * Calculates the value of the Component.
   *
   * @return The value of the Component.
   */
  protected abstract boolean getValueInternal();
}
