
package org.ggp.base.util.propnet.sancho;


/**
 * @author steve
 * Base class for all non-abstract And implementations. Needed so that
 * instanceof PolymorphicAnd can be used regardless of the concrete class
 * hierarchy produced by the factory
 */
public abstract interface PolymorphicAnd extends PolymorphicComponent
{
  //  No actual differences from the base class
}
