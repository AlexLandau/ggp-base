package org.ggp.base.test;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.ggp.base.util.game.Game;
import org.ggp.base.util.game.GameRepository;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;
import org.ggp.base.util.statemachine.implementation.prover.ProverStateMachine;
import org.ggp.base.util.statemachine.implementation.prover.ProverStateMachineFactory;
import org.junit.Test;

public class Chess200Test {
    @Test
    public void test() throws TransitionDefinitionException, MoveDefinitionException {
        GameRepository repo = GameRepository.getDefaultRepository();
        Game game = repo.getGame("chess_200");
        ProverStateMachine sm = ProverStateMachineFactory.createNormal().buildInitializedForGame(game);

        MachineState state = sm.getInitialState();
        try {
            state = checkMovesAreLegalThenApply(sm, state, "(move pawn e 2 e 4)", "noop");
            state = checkMovesAreLegalThenApply(sm, state, "noop", "(move pawn e 7 e 6)");
            state = checkMovesAreLegalThenApply(sm, state, "(move pawn e 4 e 5)", "noop");
            state = checkMovesAreLegalThenApply(sm, state, "noop", "(move knight b 8 a 6)");
            state = checkMovesAreLegalThenApply(sm, state, "(move king e 1 e 2)", "noop");
            state = checkMovesAreLegalThenApply(sm, state, "noop", "(move knight a 6 b 8)");
            state = checkMovesAreLegalThenApply(sm, state, "(move king e 2 d 3)", "noop");
            state = checkMovesAreLegalThenApply(sm, state, "noop", "(move knight b 8 a 6)");
            state = checkMovesAreLegalThenApply(sm, state, "(move king d 3 c 4)", "noop");
            state = checkMovesAreLegalThenApply(sm, state, "noop", "(move pawn d 7 d 5)");
            // En passant captures should work when in check
            state = checkMovesAreLegalThenApply(sm, state, "(move pawn e 5 d 6)", "noop");
        } catch (RuntimeException e) {
            throw new RuntimeException("Moves legal in state were: " + sm.getLegalMovesByRole(state), e);
        }
    }

    private MachineState checkMovesAreLegalThenApply(StateMachine sm, MachineState state, String... moveNames) throws MoveDefinitionException, TransitionDefinitionException {
        List<List<Move>> legalMovesByRole = sm.getLegalMovesByRole(state);
        List<Move> moves = moves(moveNames);
        for (int i = 0; i < moves.size(); i++) {
            List<Move> legalMoves = legalMovesByRole.get(i);
            Move chosenMove = moves.get(i);
            if (!legalMoves.contains(chosenMove)) {
                throw new RuntimeException("Moves legal in state were " + legalMoves + ", but chose move " + chosenMove);
            }
        }
        return sm.getNextState(state, moves);
    }

    private List<Move> moves(String... moveNames) {
        return Arrays.stream(moveNames).map(Move::create).collect(Collectors.toList());
    }
}
