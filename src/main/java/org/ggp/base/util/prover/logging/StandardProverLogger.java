package org.ggp.base.util.prover.logging;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.ggp.base.util.gdl.GdlUtils;
import org.ggp.base.util.gdl.grammar.GdlConstant;
import org.ggp.base.util.gdl.grammar.GdlSentence;
import org.ggp.base.util.gdl.grammar.GdlTerm;
import org.ggp.base.util.gdl.model.SimpleSentenceForm;
import org.ggp.base.util.prover.query.QueryType;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.Multiset;
import com.google.common.collect.Sets;

public class StandardProverLogger implements ProverLogger {
    private final Multiset<QueryType> cacheHits = HashMultiset.create();
    private final Multiset<QueryType> cacheMisses = HashMultiset.create();

    private StandardProverLogger() {
        //Use create() instead
    }

    public static StandardProverLogger create() {
        return new StandardProverLogger();
    }

    @Override
    public void logCacheHit(GdlSentence varRenamedSentence) {
        cacheHits.add(toQueryType(varRenamedSentence));
    }

    @Override
    public void logCacheMiss(GdlSentence varRenamedSentence) {
        cacheMisses.add(toQueryType(varRenamedSentence));
    }

    private QueryType toQueryType(GdlSentence sentence) {
        SimpleSentenceForm form = SimpleSentenceForm.create(sentence);
        List<GdlTerm> tuple = GdlUtils.getTupleFromSentence(sentence);
        Set<Integer> definedSlots = Sets.newHashSet();
        for (int i = 0; i < tuple.size(); i++) {
            if (tuple.get(i) instanceof GdlConstant) {
                definedSlots.add(i);
            }
        }
        return QueryType.create(form, definedSlots, ImmutableMap.of());
    }

    public Multiset<QueryType> getCacheHits() {
        return cacheHits;
    }

    public Multiset<QueryType> getCacheMisses() {
        return cacheMisses;
    }

    public Map<QueryType, QueryTypeStats> getQueryTypeMap() {
        Map<QueryType, QueryTypeStats> result = Maps.newHashMap();
        for (QueryType type : Iterables.concat(cacheHits.elementSet(), cacheMisses.elementSet())) {
            if (result.containsKey(type)) {
                continue;
            }
            int hits = cacheHits.count(type);
            int misses = cacheMisses.count(type);
            result.put(type, new QueryTypeStats(hits, misses));
        }
        return result;
    }

    public static class QueryTypeStats {
        public final int cacheHits;
        public final int cacheMisses;
        private QueryTypeStats(int cacheHits, int cacheMisses) {
            this.cacheHits = cacheHits;
            this.cacheMisses = cacheMisses;
        }
        @Override
        public String toString() {
            return "hits=" + cacheHits + ", misses="
                    + cacheMisses;
        }
    }
}
