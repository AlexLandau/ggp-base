package org.ggp.base.util.ruleengine;

import java.util.Set;

import org.ggp.base.util.gdl.grammar.GdlSentence;
import org.ggp.base.util.gdl.grammar.GdlTerm;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;

public class StdTranslator implements Translator<Move, MachineState> {
    public static final StdTranslator INSTANCE = new StdTranslator();

    @Override
    public GdlTerm getGdlMove(Move move) {
        return move.getContents();
    }

    @Override
    public Move getNativeMove(GdlTerm move) {
        return new Move(move);
    }

    @Override
    public Set<GdlSentence> getGdlState(MachineState state) {
        return state.getContents();
    }

    @Override
    public MachineState getNativeState(Set<GdlSentence> state) {
        return new MachineState(state);
    }
}
