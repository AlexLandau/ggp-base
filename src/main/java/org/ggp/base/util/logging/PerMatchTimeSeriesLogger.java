package org.ggp.base.util.logging;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Map;

import com.google.common.collect.Maps;

/*
 * Good question! I need to write up some documentation. In the meantime, you
 * should build a system that, given a match ID, can retrieve relevant logs
 * for that match in JSON format. Please make sure these logs aren't too large...
 * Time series are fine, logfiles with one line per second aren't.
 */
public class PerMatchTimeSeriesLogger extends PerMatchLogger {
    final Map<Long, Double> data = Maps.newHashMap(); // mutable
    //We want to store info for this match in memory...
    //At the end we want to store it as a JSON file (?)
    public PerMatchTimeSeriesLogger(String matchId, int playerPort, LoggingAspect aspect) {
        super(matchId, playerPort, validateAspect(aspect, LogType.TIME_SERIES));
    }

    public synchronized void logValue(double dataPoint) {
        if (!closed) {
            data.put(System.currentTimeMillis(), dataPoint);
        }
    }

    @Override
    protected synchronized void writeData(BufferedOutputStream out) throws IOException {
        try (ObjectOutputStream oos = new ObjectOutputStream(out)) {
            oos.writeObject(data);
        }
    }
}
