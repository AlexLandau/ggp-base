package org.ggp.base.util.logging;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import com.google.common.base.Preconditions;

public abstract class PerMatchLogger implements AutoCloseable {
    protected static enum LogType {
        TIME_SERIES, //Time series of double values
        EVENT, //One-off events that are recorded as log lines
        //Note: One event logger type can handle many different events
    }
    //TODO: Method to get a logger directly from the logging aspect with
    //a match ID and a "MatchObserver" that sees when the match is done.
    //It should handle all the additional threading needed. (???)
    public static enum LoggingAspect {
        OUR_BEST_MOVE_VALUE("ourBestMoveValue", LogType.TIME_SERIES),
        DEPTH_CHARGE_COUNT("depthChargeCount", LogType.TIME_SERIES),
        MEMORY_USAGE("memoryUsage", LogType.TIME_SERIES),
        GC_TIME("gcTime", LogType.TIME_SERIES),
        GAME_STATE("gameState", LogType.EVENT),
        STATE_MACHINE("stateMachine", LogType.EVENT),
        ;
        private final String name;
        private final LogType type;
        LoggingAspect(String name, LogType type) {
            this.name = name;
            this.type = type;
        }
        public String getName() {
            return name;
        }
        public LogType getType() {
            return type;
        }
    }

    protected static LoggingAspect validateAspect(LoggingAspect aspect,
            LogType type) {
        Preconditions.checkArgument(aspect.getType() == type);
        return aspect;
    }

    private static volatile File loggingDir = null;
    private static synchronized File getLoggingDir() {
        if (loggingDir == null) {
            initializeLoggingDir();
        }
        return loggingDir;
    }

    private static synchronized void initializeLoggingDir() {
        loggingDir = new File("permatchlogs");
        if (!loggingDir.exists()) {
            loggingDir.mkdir();
        }
        if (!loggingDir.isDirectory()) {
            loggingDir = null;
            throw new IllegalStateException("Cannot create the logging directory");
        }
    }

    protected final String matchId;
    protected final int playerPort;
    protected final LoggingAspect aspect;
    protected boolean closed = false;
    public PerMatchLogger(String matchId, int playerPort, LoggingAspect aspect) {
        this.matchId = matchId;
        this.playerPort = playerPort;
        this.aspect = aspect;
    }

    public static File getMatchFolder(String matchId, int playerPort) {
        File matchFolder = new File(getLoggingDir(), matchId + "-" + playerPort);
        if (!matchFolder.exists()) {
            matchFolder.mkdirs();
        }
        return matchFolder;
    }

    @Override
    public synchronized void close() {
        if (closed) {
            return;
        }
        File outFile = getOutputFile();
        try {
            try (BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(outFile))) {
                writeData(out);
            }
        } catch (IOException e) {
            e.printStackTrace();
            // Don't propagate the exception
        }
        closed = true;
    }

    protected abstract void writeData(BufferedOutputStream out) throws IOException;

    private File getOutputFile() {
        File matchFolder = getMatchFolder(matchId, playerPort);
        return new File(matchFolder, aspect.getName() + ".log");
    }
}