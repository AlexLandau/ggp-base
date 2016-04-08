package org.ggp.base.util.game;

import java.io.File;
import java.io.FileFilter;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.ggp.base.util.files.FileUtils;

import com.google.common.base.Preconditions;

import external.JSON.JSONObject;

public class SimpleLocalGameRepository extends GameRepository {
    private static final FileFilter DIR_FILTER = new FileFilter() {
        @Override
        public boolean accept(File file) {
            return file.isDirectory();
        }
    };
    private final File theRootDirectory;

    public SimpleLocalGameRepository(File theRootDirectory) {
        Preconditions.checkArgument(theRootDirectory.isDirectory());
        this.theRootDirectory = theRootDirectory;
    }

    public static SimpleLocalGameRepository getLocalBaseRepo() {
        //TODO: Make sure this is the right place across locations
        //Basically, expect that the ggp-repository repo is checked out adjacent to this repo
        return new SimpleLocalGameRepository(new File("../ggp-repository"));
    }

    @Override
    protected Game getUncachedGame(String theKey) {
        try {
            File gameDir = new File(theRootDirectory, theKey);
            Preconditions.checkArgument(gameDir.isDirectory());

            File metadataFile = new File(gameDir, "METADATA");
            String metadataText = FileUtils.readFileAsString(metadataFile);
            JSONObject metadata = new JSONObject(metadataText);

            String theName = metadata.optString("gameName", theKey);
            String descriptionFilename = metadata.optString("description", null);
            String rulesheetFilename = metadata.optString("rulesheet", null);
            String xsltStylesheetFilename = metadata.optString("stylesheet", null);
            String jsStylesheetFilename = metadata.optString("jsStylesheet", null);

            String theDescription = "";
            if (descriptionFilename != null) {
                File descriptionFile = new File(gameDir, descriptionFilename);
                theDescription = FileUtils.readFileAsString(descriptionFile);
            }
            String theRepositoryURL = "";
            String theXsltStylesheet = "";
            if (xsltStylesheetFilename != null) {
                File xsltStylesheetFile = new File(gameDir, xsltStylesheetFilename);
                theXsltStylesheet = FileUtils.readFileAsString(xsltStylesheetFile);
            }
            String theJsStylesheet = "";
            if (jsStylesheetFilename != null) {
                File jsStylesheetFile = new File(gameDir, jsStylesheetFilename);
                theJsStylesheet = FileUtils.readFileAsString(jsStylesheetFile);
            }
            String theRulesheet = "";
            if (rulesheetFilename != null) {
                File rulesheetDir = getDirectoryWithMaxVersion(gameDir);
                File rulesheetFile = new File(rulesheetDir, rulesheetFilename);
                theRulesheet = FileUtils.readFileAsString(rulesheetFile);
                theRulesheet = Game.preprocessRulesheet(theRulesheet);
            }
            return new Game(theKey, theName, theDescription, theRepositoryURL, theXsltStylesheet, theJsStylesheet, theRulesheet);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    //Make it easier to iterate on changing a game and testing it
    @Override
    public Game getGame(String theKey) {
        return getUncachedGame(theKey);
    }

    @Override
    protected Set<String> getUncachedGameKeys() {
        File[] subdirectories = theRootDirectory.listFiles(File::isDirectory);
        return Arrays.stream(subdirectories)
                .map(File::getName)
                .collect(Collectors.toSet());
    }

    private static File getDirectoryWithMaxVersion(File theDir) {
        if (!theDir.exists() || !theDir.isDirectory()) {
            return null;
        }

        int maxVersion = 0;
        File dirWithMaxVersion = theDir;
        File[] children = theDir.listFiles(DIR_FILTER);
        for (File dir : children) {
            String s = dir.getName();
            if (s.startsWith("v")) {
                int nVersion = Integer.parseInt(s.substring(1));
                if (nVersion > maxVersion) {
                    maxVersion = nVersion;
                    dirWithMaxVersion = dir;
                }
            }
        }
        return dirWithMaxVersion;
    }
}
