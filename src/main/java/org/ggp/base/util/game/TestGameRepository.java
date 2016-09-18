package org.ggp.base.util.game;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import org.ggp.base.util.files.FileUtils;


/**
 * Test game repository that provides rulesheet-only access to games with no
 * associated metadata or other resources, to be used only for unit tests.
 *
 * @author Sam
 */
public final class TestGameRepository extends GameRepository {
    @Override
    protected Set<String> getUncachedGameKeys() {
        Set<String> theKeys = new HashSet<String>();
        for(File game : new File("games/test").listFiles()) {
            if(!game.getName().endsWith(".kif")) continue;
            theKeys.add(game.getName().replace(".kif", ""));
        }
        return theKeys;
    }

    @Override
    protected Game getUncachedGame(String theKey) {
        try {
            String rawRulesheet = FileUtils.readFileAsString(new File("games/test/" + theKey + ".kif"));
            if (rawRulesheet == null) {
                throw new IllegalArgumentException("The rulesheet file for game " + theKey + " was not found.");
            }
            return Game.createEphemeralGame(Game.preprocessRulesheet(rawRulesheet));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}