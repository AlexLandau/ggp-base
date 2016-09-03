package org.ggp.base.util.ruleengine;

import java.util.List;

import org.ggp.base.util.gdl.grammar.Gdl;
import org.python.google.common.collect.Lists;

import com.google.common.collect.ImmutableList;

public class CompareManyRuleEngineFactory implements RuleEngineFactory<RuleEngine<?,?>> {
    private final static int MEASUREMENT_TIME_MILLIS = 2000;

    private final ImmutableList<RuleEngineFactory<?>> delegates;

    private CompareManyRuleEngineFactory(ImmutableList<RuleEngineFactory<?>> delegates) {
        this.delegates = delegates;
    }

    @Override
    public RuleEngine<?, ?> buildEngineForRules(List<Gdl> rules) {
        RuleEngine<?,?> bestSoFar = null;
        double bestSpeedSoFar = -1.0;
        for (RuleEngineFactory<?> factory : delegates) {
            try {
                RuleEngine<?, ?> engine = factory.buildEngineForRules(rules);
                double speed = measureStateEngineSpeed(engine);
                if (speed > bestSpeedSoFar) {
                    bestSoFar = engine;
                    bestSpeedSoFar = speed;
                }
            } catch (Exception | StackOverflowError e) {
                //Just print and continue
                e.printStackTrace();
            }
        }
        if (bestSoFar == null) {
            throw new RuntimeException("No suitable rule engines were found.");
        }
        return bestSoFar;
    }

    private <Move, State extends RuleEngineState<Move, State>>
    double measureStateEngineSpeed(RuleEngine<Move, State> engine) throws GameDescriptionException {
        long count = 0;
        long startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() - startTime < MEASUREMENT_TIME_MILLIS) {
            State curState = engine.getInitialState();
            if (engine.isTerminal(curState)) {
                //Error case, just return
                return 0.0;
            }
            do {
                curState = engine.getRandomNextState(curState);
                count++;
            } while (!engine.isTerminal(curState));
            //We need to compute this to accurately compare speeds in practice
            engine.getGoals(curState);
        }

        //Time taken might not be neatly MEASUREMENT_TIME_MILLIS, if this is running slowly
        double timeTaken = System.currentTimeMillis() - startTime;
        return count / timeTaken;
    }

    public static RuleEngineFactory<?> create(RuleEngineFactory<?> firstEngine,
            RuleEngineFactory<?>... otherEngines) {
        List<RuleEngineFactory<? extends RuleEngine<?, ?>>> engines = Lists.asList(firstEngine, otherEngines);
        return new CompareManyRuleEngineFactory(ImmutableList.copyOf(engines));
    }

    public static RuleEngineFactory<?> create(Iterable<RuleEngineFactory<?>> engines) {
        return new CompareManyRuleEngineFactory(ImmutableList.copyOf(engines));
    }
}
