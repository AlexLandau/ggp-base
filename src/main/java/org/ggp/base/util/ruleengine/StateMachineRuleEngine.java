package org.ggp.base.util.ruleengine;

import java.util.List;

import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;

public class StateMachineRuleEngine implements StdRuleEngine {
    private final StateMachine delegate;

    private StateMachineRuleEngine(StateMachine delegate) {
        this.delegate = delegate;
    }

    public static StateMachineRuleEngine wrap(StateMachine sm) {
        return new StateMachineRuleEngine(sm);
    }

    @Override
    public MachineState getInitialState() {
        return delegate.getInitialState();
    }

    @Override
    public int getNumRoles() {
        return delegate.getRoles().size();
    }

    @Override
    public List<Role> getRoles() {
        return delegate.getRoles();
    }

    @Override
    public boolean isTerminal(MachineState state) {
        return delegate.isTerminal(state);
    }

    @Override
    public int getGoal(MachineState state, int roleIndex)
            throws GameDescriptionException {
        try {
            return delegate.getGoal(state, getRoles().get(roleIndex));
        } catch (GoalDefinitionException e) {
            throw GameDescriptionException.wrap(e);
        }
    }

    @Override
    public List<Move> getLegalMoves(MachineState state, int roleIndex)
            throws GameDescriptionException {
        try {
            return delegate.getLegalMoves(state, getRoles().get(roleIndex));
        } catch (MoveDefinitionException e) {
            throw GameDescriptionException.wrap(e);
        }
    }

    @Override
    public MachineState getNextState(MachineState state, List<Move> jointMoves)
            throws GameDescriptionException {
        try {
            return delegate.getNextState(state, jointMoves);
        } catch (TransitionDefinitionException e) {
            throw GameDescriptionException.wrap(e);
        }
    }

    @Override
    public Translator<Move, MachineState> getTranslator() {
        return StdTranslator.INSTANCE;
    }
}
