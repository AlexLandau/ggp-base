package org.ggp.base.util.ruleengine;

import java.util.List;
import java.util.Set;

import org.ggp.base.util.gdl.grammar.GdlSentence;
import org.ggp.base.util.gdl.grammar.GdlTerm;

import com.google.common.collect.Lists;

public interface Translator<M, S> {
    public GdlTerm getGdlMove(M move);
    public M getNativeMove(GdlTerm move);

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
        for (GdlTerm move : moves) {
            results.add(getNativeMove(move));
        }
        return results;
    }
}
