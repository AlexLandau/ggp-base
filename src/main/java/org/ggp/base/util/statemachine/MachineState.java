package org.ggp.base.util.statemachine;

import java.io.Serializable;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.ggp.base.util.gdl.grammar.GdlSentence;
import org.ggp.base.util.hashing.ZHashContext;
import org.ggp.base.util.ruleengine.RuleEngineState;
import org.ggp.base.util.ruleengine.StdTranslator;
import org.ggp.base.util.ruleengine.Translator;

public class MachineState implements Serializable, RuleEngineState<Move, MachineState> {
    private static final long serialVersionUID = 1L;

    public MachineState() {
        this.contents = null;
    }

    /**
     * Starts with a simple implementation of a MachineState. StateMachines that
     * want to do more advanced things can subclass this implementation, but for
     * many cases this will do exactly what we want.
     */
    //TODO: Make this an ImmutableSet in GGP-Base?
    private final Set<GdlSentence> contents;
    public MachineState(Set<GdlSentence> contents)
    {
        this.contents = contents;
    }

    /**
     * getContents returns the GDL sentences which determine the current state
     * of the game being played. Two given states with identical GDL sentences
     * should be identical states of the game.
     */
    public Set<GdlSentence> getContents()
    {
        return contents;
    }

    //NOTE: A StateMachine may have a ZHashContext given to it at construction
    //time. If it does, it may assume that the context is the one passed into it.
    //This means the context passed in should always be the same, over the course
    //of a given match.
    //TODO: Override everywhere appropriate
    public long getZobristHash(ZHashContext zHashContext) {
        return zHashContext.getHashForFullState(getContents());
    }

    public MachineState getCopy() {
        return new MachineState(new HashSet<GdlSentence>(contents));
    }

    /* Utility methods */
    @Override
    public int hashCode()
    {
        return getContents().hashCode();
    }

    @Override
    public String toString()
    {
        Set<GdlSentence> contents = getContents();
        if(contents == null)
            return "(MachineState with null contents)";
        else
            return contents.stream()
                    .sorted(Comparator.comparing(gdl -> gdl.toString()))
                    .collect(Collectors.toList())
                    .toString();
    }

    @Override
    public boolean equals(Object o)
    {
        if ((o != null) && (o instanceof MachineState))
        {
            MachineState state = (MachineState) o;
            return state.getContents().equals(getContents());
        }

        return false;

    }

    @Override
    public Translator<Move, MachineState> getTranslator() {
        return StdTranslator.INSTANCE;
    }
}