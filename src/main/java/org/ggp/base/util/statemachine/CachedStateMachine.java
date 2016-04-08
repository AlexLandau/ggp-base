package org.ggp.base.util.statemachine;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.ggp.base.util.gdl.grammar.Gdl;
import org.ggp.base.util.gdl.grammar.GdlConstant;
import org.ggp.base.util.gdl.grammar.GdlSentence;
import org.ggp.base.util.gdl.grammar.GdlTerm;
import org.ggp.base.util.propnet.architecture.Component;
import org.ggp.base.util.propnet.architecture.PropNet;
import org.ggp.base.util.statemachine.cache.TtlCache;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;

//TODO: This looks terribly unsynchronized...
public class CachedStateMachine extends StateMachine {

    private final class Entry
    {
        public Map<Role, Integer> goals;
        public Map<Role, List<Move>> moves;
        public Map<List<Move>, MachineState> nexts;
        public Boolean terminal;
        public Map<Role, Move> gebMoves;

        public Entry()
        {
            goals = new HashMap<Role, Integer>();
            moves = new HashMap<Role, List<Move>>();
            nexts = new HashMap<List<Move>, MachineState>();
            terminal = null;
            gebMoves = null;
        }
    }

    private final TtlCache<MachineState, Entry> ttlCache;

    StateMachine internalStateMachine;
    public CachedStateMachine(StateMachine sm)
    {
        internalStateMachine = sm;
        ttlCache = new TtlCache<MachineState, Entry>(1);
    }
    private CachedStateMachine(StateMachine sm, TtlCache<MachineState, Entry> cache) {
        internalStateMachine = sm;
        ttlCache = cache;
    }

    private Entry getEntry(MachineState state)
    {
        if (!ttlCache.containsKey(state))
        {
            ttlCache.put(state, new Entry());
        }

        return ttlCache.get(state);
    }

    @Override
    public int getGoal(MachineState state, Role role) throws GoalDefinitionException
    {
        Entry entry = getEntry(state);
        synchronized (entry)
        {
            if (!entry.goals.containsKey(role))
            {
                entry.goals.put(role, internalStateMachine.getGoal(state, role));
            }

            return entry.goals.get(role);
        }
    }

    @Override
    public List<Move> getLegalMoves(MachineState state, Role role) throws MoveDefinitionException
    {
        Entry entry = getEntry(state);
        synchronized (entry)
        {
            if (!entry.moves.containsKey(role))
            {
                entry.moves.put(role, internalStateMachine.getLegalMoves(state, role));
            }

            return entry.moves.get(role);
        }
    }

    @Override
    public MachineState getNextState(MachineState state, List<Move> moves) throws TransitionDefinitionException
    {
        Entry entry = getEntry(state);
        //It has gotten a NullPointerException here...
        synchronized (entry)
        {
            if (!entry.nexts.containsKey(moves))
            {
                entry.nexts.put(moves, internalStateMachine.getNextState(state, moves));
            }

            return entry.nexts.get(moves);
        }
    }

    @Override
    public boolean isTerminal(MachineState state)
    {
        Entry entry = getEntry(state);
        if(entry == null)
            throw new RuntimeException("How did we get a null entry? Check cache");
        synchronized (entry)
        {
            if (entry.terminal == null)
            {
                entry.terminal = internalStateMachine.isTerminal(state);
            }

            return entry.terminal;
        }
    }

    @Override
    public Map<Role, Move> getGebMoves(MachineState state) {
        Entry entry = getEntry(state);
        synchronized (entry)
        {
            if (entry.gebMoves == null)
            {
                entry.gebMoves = internalStateMachine.getGebMoves(state);
            }

            return entry.gebMoves;
        }
    }

    @Override
    public void doPerMoveWork()
    {
        prune();
    }

    public void prune()
    {
        ttlCache.prune();
    }

    @Override
    public MachineState getInitialState() {
        return internalStateMachine.getInitialState();
    }

    @Override
    public MachineState getMachineStateFromSentenceList(Set<GdlSentence> sentenceList) {
        return internalStateMachine.getMachineStateFromSentenceList(sentenceList);
    }

    @Override
    public Move getMoveFromTerm(GdlTerm term) {
        return internalStateMachine.getMoveFromTerm(term);
    }

    @Override
    public Role getRoleFromConstant(GdlConstant constant) {
        return internalStateMachine.getRoleFromConstant(constant);
    }

    @Override
    public List<Role> getRoles() {
        return internalStateMachine.getRoles();
    }

    @Override
    public StateMachine getSynchronizedCopy() {
        //We want the caches to be linked...
        //This constructor causes the state machines to share their cache
        return new CachedStateMachine(internalStateMachine.getSynchronizedCopy(), ttlCache);
    }

    @Override
    public void initialize(List<Gdl> description) {
        internalStateMachine.initialize(description);
    }

    @Override
    public MachineState translateState(MachineState state) {
        return internalStateMachine.translateState(state);
    }
    @Override
    public boolean isNative(MachineState state) {
        return internalStateMachine.isNative(state);
    }
    @Override
    public boolean isPropNetBased() {
        return internalStateMachine.isPropNetBased();
    }
    @Override
    public PropNet getPropNet() {
        return internalStateMachine.getPropNet();
    }
    @Override
    public boolean getComponentValue(MachineState state, Component component) {
        return internalStateMachine.getComponentValue(state, component);
    }
    @Override
    public int getComponentTrueInputsCount(MachineState state,
            Component component) {
        return internalStateMachine.getComponentTrueInputsCount(state, component);
    }

}
