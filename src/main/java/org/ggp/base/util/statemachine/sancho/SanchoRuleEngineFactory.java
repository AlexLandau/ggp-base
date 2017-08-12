package org.ggp.base.util.statemachine.sancho;

import java.util.List;

import org.ggp.base.util.gdl.grammar.Gdl;
import org.ggp.base.util.ruleengine.RuleEngineFactory;

public class SanchoRuleEngineFactory implements RuleEngineFactory<ForwardDeadReckonPropnetRuleEngine> {
    public static final String VERSION = "2017.08.12";
    public static final SanchoRuleEngineFactory INSTANCE = new SanchoRuleEngineFactory();
    private SanchoRuleEngineFactory() {
        // Singleton
    }

    @Override
    public ForwardDeadReckonPropnetRuleEngine buildEngineForRules(List<Gdl> rules) {
        ForwardDeadReckonPropnetRuleEngine engine =
                new ForwardDeadReckonPropnetRuleEngine();
        engine.initialize(rules);
        engine.enableGreedyRollouts(false, true);

        engine.optimizeStateTransitionMechanism(System.currentTimeMillis()+5000);

        return engine;
    }
}
