package org.ggp.base.util;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class Timer {
    private final AtomicLong nanos = new AtomicLong();

    public static Timer create() {
        return new Timer();
    }

    public long getNanos() {
        return nanos.get();
    }

    public long get(TimeUnit unit) {
        return unit.convert(nanos.get(), TimeUnit.NANOSECONDS);
    }

    /**
     * Adds a single "lap" of time to the timer. Intended to be used as follows:
     *
     * try (Lap l = timer.startLap()) {
     *     // ...
     * }
     *
     * The contents of the try block will be timed, and the result will be added
     * to the total time on this timer. Note that the time won't be updated until
     * the try block is exited.
     */
    public Lap startLap() {
        return new Lap();
    }

    public class Lap implements AutoCloseable {
        private final long startTime;
        private Lap() {
            //Only instantiable by the Timer
            this.startTime = System.nanoTime();
        }

        @Override
        public void close() {
            long elapsed = System.nanoTime() - startTime;
            nanos.addAndGet(elapsed);
        }
    }
}
