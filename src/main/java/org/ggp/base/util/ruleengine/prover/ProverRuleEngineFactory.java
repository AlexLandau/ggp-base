package org.ggp.base.util.ruleengine.prover;

import java.util.List;

import org.ggp.base.util.gdl.grammar.Gdl;
import org.ggp.base.util.gdl.transforms.DistinctAndNotMover2;
import org.ggp.base.util.ruleengine.RuleEngineFactory;

public class ProverRuleEngineFactory implements RuleEngineFactory<ProverRuleEngine> {
    private final boolean experimental;

    public ProverRuleEngineFactory(boolean experimental) {
        this.experimental = experimental;
    }

    public static ProverRuleEngineFactory createNormal() {
        return new ProverRuleEngineFactory(false);
    }

    public static ProverRuleEngineFactory createWithDisjuncts() {
        return new ProverRuleEngineFactory(true);
    }

    @Override
    public ProverRuleEngine buildEngineForRules(List<Gdl> rules) {
        if (experimental) {
            rules = DistinctAndNotMover2.run(rules);
        }
        return ProverRuleEngine.create(rules, experimental);
    }
}
