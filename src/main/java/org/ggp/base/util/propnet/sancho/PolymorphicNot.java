
package org.ggp.base.util.propnet.sancho;


/**
 * @author steve
 * Base class for all non-abstract Not implementations. Needed so that
 * instanceof PolymorphicNot can be used regardless of the concrete class
 * hierarchy produced by the factory
 */
public abstract interface PolymorphicNot extends PolymorphicComponent
{
  //  No actual differences from the base class
}
