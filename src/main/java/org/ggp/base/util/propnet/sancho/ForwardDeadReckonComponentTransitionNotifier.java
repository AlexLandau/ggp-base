package org.ggp.base.util.propnet.sancho;

/**
 * @author steve
 * Interface that can be used to signal transitions of components to an external handler
 * during propnet propagation.  The signaled index is semantically neutral at this level
 * and its interpretation will depend on the context of use.  Generally it will be an index
 * in some bitset (base props for state, legal moves for legal move sets, ...)
 */
public interface ForwardDeadReckonComponentTransitionNotifier
{
  /**
   * @param index - index to signal as becoming true
   */
  void add(int index);

  /**
   * @param index - index to signal as becoming false
   */
  void remove(int index);
}
