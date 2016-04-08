package org.ggp.base.util.ruleengine;

import java.util.Set;

import org.ggp.base.util.gdl.grammar.GdlSentence;

public interface RuleEngineState<Move, State extends RuleEngineState<Move, State>> {
    /**
     * It is recommended that every state return the same Translator instance
     * when this is called, so that there is exactly one Translator per
     * originating RuleEngine. RuleEngine#toNativeState relies on this to
     * operate efficiently.
     */
    Translator<Move, State> getTranslator();

    //TODO: Fix the warning here; casting shouldn't be necessary, I think
    default Set<GdlSentence> toGdlState() {
        return getTranslator().getGdlState((State) this);
    }
}
