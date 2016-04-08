package org.ggp.base.util.concurrency;

public class ConcurrencyUtils {
    private ConcurrencyUtils() {
    }

    private static final boolean COMPLAIN_WHEN_NOT_CHECKING_OFTEN_ENOUGH = false;
    private static final int MAX_TIME_BETWEEN_CHECKS_MILLISECONDS = 1000;

    /**
     * If the thread has been interrupted, throws an InterruptedException.
     */
    public static void checkForInterruption() throws InterruptedException {
        if (COMPLAIN_WHEN_NOT_CHECKING_OFTEN_ENOUGH) {
            if (lastTimeCalled == 0) {
                lastTimeCalled = System.currentTimeMillis();
            }
            long timeTaken = System.currentTimeMillis() - lastTimeCalled;
            if (timeTaken > MAX_TIME_BETWEEN_CHECKS_MILLISECONDS) {
                new Throwable("Took more than 1 second since last time this was called: " + timeTaken).printStackTrace();
            }
            lastTimeCalled = System.currentTimeMillis();
        }

        if (Thread.currentThread().isInterrupted()) {
            throw new InterruptedException();
        }
    }
    private static long lastTimeCalled = 0;
}
