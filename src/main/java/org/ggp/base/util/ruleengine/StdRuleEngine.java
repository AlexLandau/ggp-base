package org.ggp.base.util.ruleengine;

import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;

public interface StdRuleEngine extends RuleEngine<Move, MachineState> {
    //Helps transition code that used to use StateMachines
}
