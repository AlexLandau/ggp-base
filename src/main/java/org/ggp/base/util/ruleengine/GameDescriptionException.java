package org.ggp.base.util.ruleengine;

import java.util.List;

import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;

public class GameDescriptionException extends Exception {
    private static final long serialVersionUID = 8790238512778576504L;

    public GameDescriptionException() {
        super();
    }

    public GameDescriptionException(String message, Throwable cause) {
        super(message, cause);
    }

    public GameDescriptionException(String message) {
        super(message);
    }

    public GameDescriptionException(Throwable cause) {
        super(cause);
    }

    public static GameDescriptionException createForTransition(MachineState state,
            List<Move> moves) {
        return wrap(new TransitionDefinitionException(state, moves));
    }

    public static GameDescriptionException createForLegalMoves(MachineState state, Role role) {
        return wrap(new MoveDefinitionException(state, role));
    }

    public static GameDescriptionException createForGoals(MachineState state, Role role,
            Exception e) {
        return wrap(new GoalDefinitionException(state, role, e));
    }

    public static GameDescriptionException createForGoals(MachineState state, Role role) {
        return wrap(new GoalDefinitionException(state, role));
    }

    public static GameDescriptionException createForGoals(RuleEngineState<?,?> state, Role role,
            GoalDefinitionException e) {
        return wrap(new GoalDefinitionException(state, role, e));
    }

    public static GameDescriptionException createForGoals(RuleEngineState<?,?> state) {
        return wrap(new GoalDefinitionException(state));
    }

    public static GameDescriptionException wrap(GoalDefinitionException e) {
        return new GameDescriptionException(e);
    }

    public static GameDescriptionException wrap(MoveDefinitionException e) {
        return new GameDescriptionException(e);
    }

    public static GameDescriptionException wrap(TransitionDefinitionException e) {
        return new GameDescriptionException(e);
    }
}
