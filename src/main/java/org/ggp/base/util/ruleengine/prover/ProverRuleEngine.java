package org.ggp.base.util.ruleengine.prover;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

import org.ggp.base.util.gdl.grammar.Gdl;
import org.ggp.base.util.gdl.grammar.GdlConstant;
import org.ggp.base.util.gdl.grammar.GdlRelation;
import org.ggp.base.util.gdl.grammar.GdlSentence;
import org.ggp.base.util.logging.GamerLogger;
import org.ggp.base.util.prover.Prover;
import org.ggp.base.util.prover.aima.AimaProver;
import org.ggp.base.util.prover.logging.StandardProverLogger;
import org.ggp.base.util.ruleengine.GameDescriptionException;
import org.ggp.base.util.ruleengine.RuleEngine;
import org.ggp.base.util.ruleengine.StdTranslator;
import org.ggp.base.util.ruleengine.Translator;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.implementation.prover.query.ProverQueryBuilder;
import org.ggp.base.util.statemachine.implementation.prover.result.ProverResultParser;

import com.google.common.collect.ImmutableList;


public class ProverRuleEngine implements RuleEngine<Move, MachineState>
{
    private final boolean experimental;
    private final MachineState initialState;
    private final Prover prover;
    private final ImmutableList<Role> roles;
    private final @Nullable StandardProverLogger log;

    public ProverRuleEngine(boolean experimental, MachineState initialState,
            Prover prover, ImmutableList<Role> roles, StandardProverLogger log) {
        this.experimental = experimental;
        this.initialState = initialState;
        this.prover = prover;
        this.roles = roles;
        this.log = log;
    }

    public static ProverRuleEngine create(List<Gdl> description, boolean noPreprocessing)
    {
        Prover prover;
        StandardProverLogger log;
        if (noPreprocessing) {
//            log = StandardProverLogger.create();
//            prover = new LoggingAimaProver(description, log);
            log = null;
            prover = AimaProver.createWithoutPreprocessing(description);
        } else {
            log = null;
            prover = AimaProver.create(description);
        }
        ImmutableList<Role> roles = ImmutableList.copyOf(Role.computeRoles(description));
        MachineState initialState = computeInitialState(prover);
        return new ProverRuleEngine(noPreprocessing, initialState, prover, roles, log);
    }

    private static MachineState computeInitialState(Prover prover)
    {
        Set<GdlSentence> results = prover.askAll(ProverQueryBuilder.getInitQuery(), new HashSet<GdlSentence>());
        return new ProverResultParser().toState(results);
    }

    @Override
    public int getGoal(MachineState state, int roleIndex) throws GameDescriptionException
    {
        Role role = roles.get(roleIndex);
        Set<GdlSentence> results = prover.askAll(ProverQueryBuilder.getGoalQuery(role), ProverQueryBuilder.getContext(state));

        if (results.size() != 1)
        {
            GamerLogger.logError("StateMachine", "Got goal results of size: " + results.size() + " when expecting size one. Results were: " + results);
            throw GameDescriptionException.createForGoals(state, role);
        }

        try
        {
            GdlRelation relation = (GdlRelation) results.iterator().next();
            GdlConstant constant = (GdlConstant) relation.get(1);

            return Integer.parseInt(constant.toString());
        }
        catch (Exception e)
        {
            throw GameDescriptionException.createForGoals(state, role);
        }
    }

    @Override
    public MachineState getInitialState()
    {
        return initialState;
    }

    @Override
    public List<Move> getLegalMoves(MachineState state, int roleIndex) throws GameDescriptionException
    {
        Role role = roles.get(roleIndex);
        Set<GdlSentence> results = prover.askAll(ProverQueryBuilder.getLegalQuery(role), ProverQueryBuilder.getContext(state));

        if (results.size() == 0)
        {
            throw GameDescriptionException.createForLegalMoves(state, role);
        }

        return new ProverResultParser().toMoves(results);
    }

    @Override
    public MachineState getNextState(MachineState state, List<Move> moves) throws GameDescriptionException
    {
        Set<GdlSentence> results = prover.askAll(ProverQueryBuilder.getNextQuery(), ProverQueryBuilder.getContext(state, getRoles(), moves));

        for (GdlSentence sentence : results)
        {
            if (!sentence.isGround())
            {
                throw GameDescriptionException.createForTransition(state, moves);
            }
        }

        return new ProverResultParser().toState(results);
    }

    @Override
    public List<Role> getRoles()
    {
        return roles;
    }

    @Override
    public int getNumRoles() {
        return roles.size();
    }

    @Override
    public boolean isTerminal(MachineState state)
    {
        return prover.prove(ProverQueryBuilder.getTerminalQuery(), ProverQueryBuilder.getContext(state));
    }

    public @Nullable StandardProverLogger getLogger() {
        return log;
    }

    @Override
    public Translator<Move, MachineState> getTranslator() {
        return StdTranslator.INSTANCE;
    }
}