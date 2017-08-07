
package org.ggp.base.util.propnet.sancho;


/**
 * @author steve
 * Base class for all non-abstract Or implementations. Needed so that
 * instanceof PolymorphicOr can be used regardless of the concrete class
 * hierarchy produced by the factory
 */
public abstract interface PolymorphicOr extends PolymorphicComponent
{
  //  No actual differences from the base class
}
