package org.ggp.base.util.statemachine.sancho;

import java.util.List;

import org.ggp.base.util.gdl.grammar.Gdl;
import org.ggp.base.util.ruleengine.RuleEngineFactory;
import org.ggp.base.util.ruleengine.StateMachineRuleEngine;
import org.ggp.base.util.ruleengine.StdRuleEngine;
import org.ggp.base.util.statemachine.StateMachine;

public class SanchoRuleEngineFactory implements RuleEngineFactory<StdRuleEngine> {
    public static final SanchoRuleEngineFactory INSTANCE = new SanchoRuleEngineFactory();
    private SanchoRuleEngineFactory() {
        // Singleton
    }

    @Override
    public StdRuleEngine buildEngineForRules(List<Gdl> rules) {
        StateMachine sm = SanchoStateMachineFactory.INSTANCE.buildInitializedForRules(rules);
        return StateMachineRuleEngine.wrap(sm);
    }
}
