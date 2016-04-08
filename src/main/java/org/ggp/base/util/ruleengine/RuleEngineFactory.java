package org.ggp.base.util.ruleengine;

import java.util.List;

import org.ggp.base.util.gdl.grammar.Gdl;

public interface RuleEngineFactory<RE extends RuleEngine<?, ?>> {
    RE buildEngineForRules(List<Gdl> rules);
}
