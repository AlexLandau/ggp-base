package org.ggp.base.util.gdl.transforms;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;

import org.ggp.base.util.game.Game;
import org.ggp.base.util.game.GameRepository;
import org.ggp.base.util.gdl.grammar.Gdl;
import org.ggp.base.util.gdl.grammar.GdlRule;
import org.ggp.base.validator.StaticValidator;
import org.ggp.base.validator.ValidatorException;
import org.ggp.base.validator.ValidatorWarning;
import org.junit.Test;

import com.google.common.collect.ImmutableSet;

public class ConjunctDualizerTest {

    @Test
    public void testConjunctDualizerMakesRulesSmall() {
        GameRepository repo = GameRepository.getDefaultRepository();
        for (String gameKey : repo.getGameKeys()) {
            Game game = repo.getGame(gameKey);
            List<Gdl> transformed = ConjunctDualizer.apply(game.getRules());
            for (Gdl gdl : transformed) {
                if (gdl instanceof GdlRule) {
                    GdlRule rule = (GdlRule) gdl;
                    if (rule.arity() > 2) {
                        fail("A rule was left with arity greater than 2: " + rule + " in game " + gameKey);
                    }
                }
            }
        }
    }


    //Currently it's known that some games may violate the recursion restriction
    //after this transformation has been applied.
    //I suspect these games won't actually cause problems (but need to actually test).
    //Until we move to the GDL validation library for static validation, we must
    //whitelist by game instead of by failure type.
    ImmutableSet<String> GAME_WHITELIST = ImmutableSet.of(
            "2pffa",
            "2pffa_zerosum",
            "3pffa",
            "4pffa",
            "hitori",
            "laikLee_hex",
            "nonogram_5x5_1",
            "nonogram_10x10_1",

            //Here's another known problem type:
            //We can end up replacing
            // ( <= ( orthogonal ?x1 ?y1 ?x2 ?y2 ) ( up ?x1 ?y1 ?x2 ?y2 ) )
            //with
            // ( <= ( orthogonal ?x1 ?y1 ?x2 ?y2 ) ( down ?x1 ?y2 ?x1 ?y1 ) )
            //In other words, we also need to either replace the second occurrence with
            //the second kind of replacement, or collapse the redundant variables (constants?)
            //together elsewhere in the rule.
            "ttcc4",
            "ttcc4_2player",
            "ttcc4_2player_alt",
            "ttcc4_2player_small",
            "tttcc4",
            "crossers3"
            );

    @Test
    public void testConjunctDualizerKeepsGamesValid() throws ValidatorException {
        GameRepository repo = GameRepository.getDefaultRepository();
        for (String gameKey : repo.getGameKeys()) {
            if (GAME_WHITELIST.contains(gameKey)) {
                continue;
            }
            Game game = repo.getGame(gameKey);
            boolean hasWarnings;
            try {
                hasWarnings = !StaticValidator.validateDescription(game.getRules()).isEmpty();
            } catch (ValidatorException e) {
                //Don't bother to check the transformed version
                continue;
            }
            List<Gdl> transformed = ConjunctDualizer.apply(game.getRules());
            try {
                List<ValidatorWarning> newWarnings = StaticValidator.validateDescription(transformed);
                if (!hasWarnings) {
                    assertTrue(newWarnings.isEmpty());
                }
            } catch (ValidatorException e) {
                throw new RuntimeException("Failed on game key " + gameKey, e);
            }
        }
    }

    public static void main(String[] args) {
        Game game = GameRepository.getDefaultRepository().getGame("ttcc4_2player_alt");
        List<Gdl> newRules = ConjunctDualizer.apply(game.getRules());
        for (Gdl gdl : newRules) {
            System.out.println(gdl);
        }
    }

}
