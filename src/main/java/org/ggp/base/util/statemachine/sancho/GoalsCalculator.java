package org.ggp.base.util.statemachine.sancho;

import org.ggp.base.util.propnet.sancho.ForwardDeadReckonInternalMachineState;
import org.ggp.base.util.statemachine.Role;

/**
 * @author steve
 *  Interface through which goal calculations may be injected, bypassing the use
 *  of teh state machine
 */
public interface GoalsCalculator
{
  /**
   * @return name of the calculator (its type in human-readable manner)
   */
  String getName();

  /**
   * @return a functional clone which can be used in a thread safe manner
   */
  GoalsCalculator createThreadSafeReference();

  /**
   * @param xiState - state to calculate goals in
   * @param role - role for which the score is required
   * @return goal value for specified role in specified state
   */
  int getGoalValue(ForwardDeadReckonInternalMachineState xiState, Role role);

  /**
   * @param xiState - state to test for latched scores in
   * @return true if the scores are now latched for all roles
   */
  boolean scoresAreLatched(ForwardDeadReckonInternalMachineState xiState);
}
