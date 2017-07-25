package org.ggp.base.util.hashing;

import java.util.Set;

import org.ggp.base.util.gdl.grammar.GdlSentence;

public interface ZHashContext {
    long getHashComponent(GdlSentence sentence);

    default long getHashForFullState(Set<GdlSentence> contents) {
        long hash = 0L;
        for (GdlSentence sentence : contents) {
            hash ^= getHashComponent(sentence);
        }
        return hash;
    }
}
