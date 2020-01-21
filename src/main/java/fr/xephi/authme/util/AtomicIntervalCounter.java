package fr.xephi.authme.util;

public class AtomicIntervalCounter {
    private final int threshold;
    private final int interval;
    private int count;
    private long lastInsert;

    public AtomicIntervalCounter(int threshold, int interval) {
        this.threshold = threshold;
        this.interval = interval;
        reset();
    }

    public synchronized void reset() {
        count = 0;
        lastInsert = 0;
    }

    public synchronized boolean handle() {
        long now = System.currentTimeMillis();
        if (now - lastInsert > interval) {
            count = 1;
        } else {
            count++;
        }
        if (count > threshold) {
            reset();
            return true;
        }
        lastInsert = now;
        return false;
    }
}
