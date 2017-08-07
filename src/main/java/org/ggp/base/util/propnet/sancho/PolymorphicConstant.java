
package org.ggp.base.util.propnet.sancho;


/**
 * @author steve
 * Base class for all non-abstract Constant implementations. Needed so that
 * instanceof PolymorphicConstant can be used regardless of the concrete class
 * hierarchy produced by the factory
 */
public abstract interface PolymorphicConstant extends PolymorphicComponent
{
  //  No actual differences from the base class
}
