package org.ggp.base.util;

import java.util.concurrent.TimeUnit;

import org.ggp.base.util.gdl.grammar.GdlSentence;
import org.ggp.base.util.gdl.model.SentenceForm;
import org.ggp.base.util.gdl.model.SimpleSentenceForm;

import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableSortedSet;


public class PerfLogger {
    public static enum Perf {
        WORKING_SET_INIT, UP_TO_TERMINAL, BEFORE_LEGALS_AND_GOALS, BEFORE_GOALS_AND_NEXTS, BEFORE_AND_INCLUDING_GOALS, ADD_NEWLY_TERMINAL_GOAL_VALUES, WRITE_NEXTS_INTO_TRUES, NEXT_NODES, BEFORE_NEXTS, RANDOM_MOVE_SELECTION_AND_SETTING, UP_TO_LEGALS, LEGAL_MOVE_COUNTING, RANDOM_MOVE_INDEX_SELECTION, ZERO_OUT_INPUT_RANGE, SET_SELECTED_INPUTS, REMOVE_UNREACHABLE_INIT, REMOVE_UNREACHABLE_PROPAGATE, REMOVE_UNREACHABLE_OPTIMIZE_AWAY, REMOVE_UNREACHABLE_PREP_OPTIMIZE_AWAY,
        FG_CONSTRUCT,
        FG_CLEAR_REMOVABLES,
        FG_FIND_TO_COMBINE,
        FG_COMBINE,
        FG_SORT,
        FG_ITERATION,
        FG_REMOVE_AFTER_NO_MATCHES,
        CTK_LEGAL, CTK_NEXT, CTK_TERMINAL, CTK_GOALS, SC_LEGAL, SC_NEXT, SC_TERMINAL, SC_GOALS,
        GET_GOAL_ALL, IS_TERMINAL_ALL, GET_LEGAL_ALL, GET_NEXT_ALL, GET_LEGAL_COMPILED, GET_NEXT_COMPILED, GET_GOAL_COMPILED, IS_TERMINAL_COMPILED, NATIVE_TO_GDL_MOVES, GDL_TO_NATIVE_MOVES, GET_GOAL_COMPILED2, IS_TERMINAL_COMPILED2, GET_LEGAL_COMPILED2, GET_NEXT_COMPILED2

    }

    private static final Counter<Perf> PERF_LOGS = Counter.create();

    public static class PerfToken implements AutoCloseable {
        private final Perf category;
        private final Stopwatch watch; //Benefit from including the state here

        private PerfToken(Perf category) {
            this.category = category;
            this.watch = Stopwatch.createStarted();
        }

        @Override
        public void close() {
            PerfLogger.record(this);
        }
    }

    public static PerfToken getToken(Perf category) {
        return new PerfToken(category);
    }

    public static void record(PerfToken token) {
        long totalTime = token.watch.stop().elapsed(TimeUnit.NANOSECONDS);
        PERF_LOGS.add(token.category, totalTime);
    }

    public static boolean hasLogs() {
        return !PERF_LOGS.isEmpty();
    }

    public static void printAndClear() {
        for (Perf category : ImmutableSortedSet.copyOf(PERF_LOGS.keySet())) {
            System.out.println(category + ": " + PERF_LOGS.get(category) / 1_000_000L);
        }
        for (SentenceForm sentence : ASK_COUNTER.keySet()) {
            System.out.println(sentence + ": " + ASK_COUNTER.get(sentence) / 1_000_000L);
        }
        PERF_LOGS.clear();
        ASK_COUNTER.clear();
    }

    private static final Counter<SentenceForm> ASK_COUNTER = Counter.create();
    public static void addToAskCounter(GdlSentence varRenamedSentence, long nanos) {
        ASK_COUNTER.add(SimpleSentenceForm.create(varRenamedSentence), nanos);
    }
}
