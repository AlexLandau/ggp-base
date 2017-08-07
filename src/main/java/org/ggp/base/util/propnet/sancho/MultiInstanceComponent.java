
package org.ggp.base.util.propnet.sancho;

/**
 * @author steve
 * Interface supported by components that can be used in a multi-state-instanced
 * propnet.  Such propnets implement a vector of states and use an instanceId to
 * address a particular member of the vector.  This interface allows component
 * values to be accessed at a particular address in the overall state vector.
 * Typically this is used to provide a private state for each thread where multiple
 * threads are accessing the same propnet
 */
public interface MultiInstanceComponent
{
  /**
   * Gets the value of the Component for the specified
   * member of the state vector.
   * @param instanceIndex The instance within the state vector to address
   *
   * @return The value of the Component.
   */
  public abstract boolean getValue(int instanceIndex);

  /**
   * Crystalize the component to a specified state-vector size.  This
   * causes any necessary allocation of resources for the entire vector
   * in regard to this component.  It may only be called once
   * @param numInstances Number of instances comprising the full state vector
   */
  public abstract void crystalize(int numInstances);
}
