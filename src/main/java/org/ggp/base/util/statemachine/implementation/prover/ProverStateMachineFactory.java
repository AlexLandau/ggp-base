package org.ggp.base.util.statemachine.implementation.prover;

import java.util.List;

import org.ggp.base.util.gdl.grammar.Gdl;
import org.ggp.base.util.gdl.transforms.ConjunctDualizer;
import org.ggp.base.util.statemachine.StateMachineFactory;

public class ProverStateMachineFactory implements StateMachineFactory<ProverStateMachine> {
    private final boolean experimental;
    private final boolean dualized;

    public ProverStateMachineFactory(boolean experimental, boolean dualized) {
        this.experimental = experimental;
        this.dualized = dualized;
    }

    public static ProverStateMachineFactory createNormal() {
        return new ProverStateMachineFactory(false, false);
    }

    public static ProverStateMachineFactory createExperimental() {
        return new ProverStateMachineFactory(true, false);
    }

    public static ProverStateMachineFactory createDualized() {
        return new ProverStateMachineFactory(false, true);
    }

    @Override
    public ProverStateMachine buildInitializedForRules(List<Gdl> rules) {
        ProverStateMachine sm = new ProverStateMachine(experimental);
        if (dualized) {
            rules = ConjunctDualizer.apply(rules);
        }
        sm.initialize(rules);
        return sm;
    }
}
