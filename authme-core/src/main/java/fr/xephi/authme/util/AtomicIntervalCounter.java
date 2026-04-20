package fr.xephi.authme.util;

/**
 * A thread-safe interval counter, allows to detect if an event happens more than 'threshold' times
 * in the given 'interval'.
 */
public class AtomicIntervalCounter {
    private final int threshold;
    private final int interval;
    private int count;
    private long lastInsert;

    /**
     * Constructs a new counter.
     *
     * @param threshold the threshold value of the counter.
     * @param interval the counter interval in milliseconds.
     */
    public AtomicIntervalCounter(int threshold, int interval) {
        this.threshold = threshold;
        this.interval = interval;
        reset();
    }

    /**
     * Resets the counter count.
     */
    public synchronized void reset() {
        count = 0;
        lastInsert = 0;
    }

    /**
     * Increments the counter and returns true if the current count has reached the threshold value
     * in the given interval, this will also reset the count value.
     *
     * @return true if the count has reached the threshold value.
     */
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
