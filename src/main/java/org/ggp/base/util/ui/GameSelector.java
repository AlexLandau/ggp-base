package org.ggp.base.util.ui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JComboBox;

import org.ggp.base.util.game.CloudGameRepository;
import org.ggp.base.util.game.Game;
import org.ggp.base.util.game.GameRepository;
import org.ggp.base.util.game.SimpleLocalGameRepository;
import org.ggp.base.util.game.TestGameRepository;


/**
 * GameSelector is a pair of widgets for selecting a game repository
 * and then choosing a game from that game repository. Currently this
 * is a little rough, and could use some polish, but it provides all
 * of the important functionality: you can load games both from local
 * storage and from game repositories on the web.
 *
 * @author Sam Schreiber
 */
public class GameSelector implements ActionListener {
    JComboBox<NamedItem> theGameList;
    JComboBox<String> theRepositoryList;

    GameRepository theSelectedRepository;
    Map<String, GameRepository> theCachedRepositories;

    class NamedItem {
        public final String theKey;
        public final String theName;

        public NamedItem(String theKey, String theName) {
            this.theKey = theKey;
            this.theName = theName;
        }

        @Override
        public String toString() {
            return theName;
        }
    }

    public GameSelector() {
        theGameList = new JComboBox<NamedItem>();
        theGameList.addActionListener(this);

        theRepositoryList = new JComboBox<String>();
        theRepositoryList.addActionListener(this);

        theCachedRepositories = new HashMap<String, GameRepository>();
        theRepositoryList.addItem("games.ggp.org/base");
        theRepositoryList.addItem("games.ggp.org/dresden");
        theRepositoryList.addItem("games.ggp.org/stanford");
        theRepositoryList.addItem("Test");
        theRepositoryList.addItem("Local");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == theRepositoryList) {
            String theRepositoryName = theRepositoryList.getSelectedItem().toString();
            if (theRepositoryName.equals("Test")) {
                theSelectedRepository = new TestGameRepository();
            } else if (theRepositoryName.equals("Local")) {
                theSelectedRepository = SimpleLocalGameRepository.getLocalBaseRepo();
            } else if (theCachedRepositories.containsKey(theRepositoryName)) {
                theSelectedRepository = theCachedRepositories.get(theRepositoryName);
            } else {
                theSelectedRepository = new CloudGameRepository(theRepositoryName);
                theCachedRepositories.put(theRepositoryName, theSelectedRepository);
            }
            repopulateGameList();
        }
    }

    public GameRepository getSelectedGameRepository() {
        return theSelectedRepository;
    }

    public void repopulateGameList() {
        GameRepository theRepository = getSelectedGameRepository();
        List<String> theKeyList = new ArrayList<String>(theRepository.getGameKeys());
        Collections.sort(theKeyList);
        theGameList.removeAllItems();
        for (String theKey : theKeyList) {
            try {
                Game theGame = theRepository.getGame(theKey);
                if (theGame == null) {
                    continue;
                }
                String theName = theGame.getName();
                if (theName == null) {
                    theName = theKey;
                }
                if (theName.length() > 24)
                    theName = theName.substring(0, 24) + "...";
                theGameList.addItem(new NamedItem(theKey, theName));
            } catch (RuntimeException e) {
                //Ignore; it just wasn't a game
            }
        }
    }

    public JComboBox<String> getRepositoryList() {
        return theRepositoryList;
    }

    public JComboBox<NamedItem> getGameList() {
        return theGameList;
    }

    public Game getSelectedGame() {
        try {
            return getSelectedGameRepository().getGame(((NamedItem)theGameList.getSelectedItem()).theKey);
        } catch(Exception e) {
            return null;
        }
    }
}