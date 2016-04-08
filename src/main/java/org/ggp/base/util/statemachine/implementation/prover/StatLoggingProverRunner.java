package org.ggp.base.util.statemachine.implementation.prover;

import org.ggp.base.util.game.Game;
import org.ggp.base.util.game.GameRepository;
import org.ggp.base.util.prover.logging.StandardProverLogger;
import org.ggp.base.util.statemachine.MachineState;

public class StatLoggingProverRunner {

    public static void main(String[] args) throws Exception {
        GameRepository repo = GameRepository.getDefaultRepository();
        Game game = repo.getGame("speedChess");
        ProverStateMachine sm = new ProverStateMachine(true);
        sm.initialize(game.getRules());

        for (int i = 0; i < 10; i++) {
            MachineState state = sm.getInitialState();
            while (!sm.isTerminal(state)) {
                state = sm.getRandomNextState(state);
            }
            sm.getGoals(state);
        }

        StandardProverLogger logger = sm.getLogger();
        System.out.println(logger.getQueryTypeMap());
    }

}
