package org.ggp.base.util.ruleengine;

import java.util.List;
import java.util.Set;

import org.ggp.base.util.gdl.grammar.GdlSentence;
import org.ggp.base.util.gdl.grammar.GdlTerm;
import org.ggp.base.util.statemachine.Move;

import com.google.common.collect.Lists;

public interface Translator<M, S> {
    public GdlTerm getGdlMove(M move);
    public M getNativeMove(int roleIndex, GdlTerm move);

    public Set<GdlSentence> getGdlState(S state);
    public S getNativeState(Set<GdlSentence> state);

    default List<GdlTerm> getGdlMoves(List<M> moves) {
        List<GdlTerm> results = Lists.newArrayListWithCapacity(moves.size());
        for (M move : moves) {
            results.add(getGdlMove(move));
        }
        return results;
    }
    default List<M> getNativeMoves(List<GdlTerm> moves) {
        List<M> results = Lists.newArrayListWithCapacity(moves.size());
        for (int r = 0; r < moves.size(); r++) {
            GdlTerm move = moves.get(r);
            results.add(getNativeMove(r, move));
        }
        return results;
    }
    default List<M> getNativeMovesFromMoves(List<Move> moves) {
        List<M> results = Lists.newArrayListWithCapacity(moves.size());
        for (int r = 0; r < moves.size(); r++) {
            Move move = moves.get(r);
            results.add(getNativeMove(r, move.getContents()));
        }
        return results;
    }
}
