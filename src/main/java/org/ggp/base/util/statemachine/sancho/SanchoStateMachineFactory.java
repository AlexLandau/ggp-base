package org.ggp.base.util.statemachine.sancho;

import java.util.List;

import org.ggp.base.util.gdl.grammar.Gdl;
import org.ggp.base.util.statemachine.StateMachineFactory;

public class SanchoStateMachineFactory implements StateMachineFactory<ForwardDeadReckonPropnetStateMachine> {
    public static final String VERSION = "2017.08.06";

    @Override
    public ForwardDeadReckonPropnetStateMachine buildInitializedForRules(List<Gdl> rules) {
        ForwardDeadReckonPropnetStateMachine machine =
                new ForwardDeadReckonPropnetStateMachine();
        machine.initialize(rules);
        machine.enableGreedyRollouts(false, true);

        machine.optimizeStateTransitionMechanism(System.currentTimeMillis()+5000);

        return machine;
    }
}
