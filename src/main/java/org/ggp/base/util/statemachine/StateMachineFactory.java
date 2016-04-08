package org.ggp.base.util.statemachine;

import java.util.List;

import org.ggp.base.util.game.Game;
import org.ggp.base.util.gdl.grammar.Gdl;
import org.ggp.base.util.ruleengine.StateMachineRuleEngine;
import org.ggp.base.util.ruleengine.StdRuleEngine;


public interface StateMachineFactory<T extends StateMachine> {
    public default T buildInitializedForGame(Game game) {
        return buildInitializedForRules(game.getRules());
    }

    public T buildInitializedForRules(List<Gdl> rules);

    public default StdRuleEngine buildEngineForGame(Game game) {
        return StateMachineRuleEngine.wrap(buildInitializedForGame(game));
    }
}
