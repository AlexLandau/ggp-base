package org.ggp.base.util.statemachine.implementation.prover;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import org.ggp.base.util.gdl.grammar.Gdl;
import org.ggp.base.util.gdl.grammar.GdlConstant;
import org.ggp.base.util.gdl.grammar.GdlRelation;
import org.ggp.base.util.gdl.grammar.GdlSentence;
import org.ggp.base.util.logging.GamerLogger;
import org.ggp.base.util.propnet.architecture.Component;
import org.ggp.base.util.propnet.architecture.PropNet;
import org.ggp.base.util.prover.Prover;
import org.ggp.base.util.prover.aima.AimaProver;
import org.ggp.base.util.prover.logging.LoggingAimaProver;
import org.ggp.base.util.prover.logging.StandardProverLogger;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;
import org.ggp.base.util.statemachine.implementation.prover.query.ProverQueryBuilder;
import org.ggp.base.util.statemachine.implementation.prover.result.ProverResultParser;

import com.google.common.collect.ImmutableList;


public class ProverStateMachine extends StateMachine
{
    private final boolean experimental;
    private volatile MachineState initialState;
    private volatile Prover prover;
    private volatile ImmutableList<Role> roles;
    private volatile @Nullable StandardProverLogger log;

    /**
     * Initialize must be called before using the StateMachine
     */
    public ProverStateMachine() {
        this(false);
    }
    public ProverStateMachine(boolean experimental)
    {
        this.experimental = experimental;
    }

    @Override
    public void initialize(List<Gdl> description)
    {
        if (experimental) {
            this.log = StandardProverLogger.create();
            prover = new LoggingAimaProver(description, log);
        } else {
            prover = new AimaProver(description);
        }
        roles = ImmutableList.copyOf(Role.computeRoles(description));
        initialState = computeInitialState();
    }

    private MachineState computeInitialState()
    {
        Set<GdlSentence> results = prover.askAll(ProverQueryBuilder.getInitQuery(), new HashSet<GdlSentence>());
        return new ProverResultParser().toState(results);
    }

    @Override
    public int getGoal(MachineState state, Role role) throws GoalDefinitionException
    {
        Set<GdlSentence> results = prover.askAll(ProverQueryBuilder.getGoalQuery(role), ProverQueryBuilder.getContext(state));

        if (results.size() != 1)
        {
            GamerLogger.logError("StateMachine", "Got goal results of size: " + results.size() + " when expecting size one. Results were: " + results);
            throw new GoalDefinitionException(state, role);
        }

        try
        {
            GdlRelation relation = (GdlRelation) results.iterator().next();
            GdlConstant constant = (GdlConstant) relation.get(1);

            return Integer.parseInt(constant.toString());
        }
        catch (Exception e)
        {
            throw new GoalDefinitionException(state, role);
        }
    }

    @Override
    public MachineState getInitialState()
    {
        return initialState;
    }

    @Override
    public List<Move> getLegalMoves(MachineState state, Role role) throws MoveDefinitionException
    {
        Set<GdlSentence> results = prover.askAll(ProverQueryBuilder.getLegalQuery(role), ProverQueryBuilder.getContext(state));

        if (results.isEmpty())
        {
            throw new MoveDefinitionException(state, role);
        }

        return new ProverResultParser().toMoves(results);
    }

    @Override
    public MachineState getNextState(MachineState state, List<Move> moves) throws TransitionDefinitionException
    {
        Set<GdlSentence> results = prover.askAll(ProverQueryBuilder.getNextQuery(), ProverQueryBuilder.getContext(state, getRoles(), moves));

        for (GdlSentence sentence : results)
        {
            if (!sentence.isGround())
            {
                throw new TransitionDefinitionException(state, moves);
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
    public boolean isTerminal(MachineState state)
    {
        return prover.prove(ProverQueryBuilder.getTerminalQuery(), ProverQueryBuilder.getContext(state));
    }

    @Override
    public StateMachine getSynchronizedCopy() {
        return this;
    }

    @Override
    public Map<Role, Move> getGebMoves(MachineState state) {
        return Collections.emptyMap();
    }

    @Override
    public MachineState translateState(MachineState state) {
        if (isNative(state)) {
            return state;
        }
        throw new UnsupportedOperationException("Can't translate this MachineState type: " + state.getClass());
    }

    @Override
    public boolean isNative(MachineState state) {
        //Just needs to implement getContents()
        try {
            return state.getContents() != null;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean isPropNetBased() {
        return false;
    }

    @Override
    public PropNet getPropNet() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean getComponentValue(MachineState state, Component component) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getComponentTrueInputsCount(MachineState state,
            Component component) {
        throw new UnsupportedOperationException();
    }

    public @Nullable StandardProverLogger getLogger() {
        return log;
    }
}