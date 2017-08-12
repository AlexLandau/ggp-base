package org.ggp.base.util.statemachine.sancho;

import java.util.List;

import org.ggp.base.util.gdl.grammar.Gdl;
import org.ggp.base.util.statemachine.StateMachineFactory;

public class SanchoStateMachineFactory implements StateMachineFactory<ForwardDeadReckonPropnetRuleEngine> {
    public static final String VERSION = "2017.08.06";
    public static final SanchoStateMachineFactory INSTANCE = new SanchoStateMachineFactory();

    private SanchoStateMachineFactory() {
        // Singleton
    }

    @Override
    public ForwardDeadReckonPropnetRuleEngine buildInitializedForRules(List<Gdl> rules) {
        ForwardDeadReckonPropnetRuleEngine machine =
                new ForwardDeadReckonPropnetRuleEngine();
        machine.initialize(rules);
        machine.enableGreedyRollouts(false, true);

        machine.optimizeStateTransitionMechanism(System.currentTimeMillis()+5000);

        return machine;
    }
}
