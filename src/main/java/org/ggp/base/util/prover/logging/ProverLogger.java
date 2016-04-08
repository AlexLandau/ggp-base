package org.ggp.base.util.prover.logging;

import org.ggp.base.util.gdl.grammar.GdlSentence;

public interface ProverLogger {

    void logCacheHit(GdlSentence varRenamedSentence);

    void logCacheMiss(GdlSentence varRenamedSentence);

}
