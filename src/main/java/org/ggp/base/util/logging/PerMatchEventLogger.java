package org.ggp.base.util.logging;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Sets;

// A PerMatchLogger for one-off events.
public class PerMatchEventLogger extends PerMatchLogger {
    private final ListMultimap<Long, String> data = ArrayListMultimap.create();

    public PerMatchEventLogger(String matchId, int playerPort, LoggingAspect aspect) {
        super(matchId, playerPort, validateAspect(aspect, LogType.EVENT));
    }

    public synchronized void writeEvent(String eventName) {
        if (!closed) {
            data.put(System.currentTimeMillis(), eventName);
        }
    }

    @Override
    protected synchronized void writeData(BufferedOutputStream out) throws IOException {
        try (Writer writer = new OutputStreamWriter(out)) {
            //Go through the keys in sorted order
            for (long key : Sets.newTreeSet(data.keySet())) {
                for (String event : data.get(key)) {
                    writer.write(key + ": " + event + "\n");
                }
            }
        }
    }
}
