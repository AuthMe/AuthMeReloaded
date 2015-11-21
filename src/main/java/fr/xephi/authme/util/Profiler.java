package fr.xephi.authme.util;

import java.text.DecimalFormat;

/**
 */
@SuppressWarnings("UnusedDeclaration")
public class Profiler {

    /** Defines the past time in milliseconds. */
    private long time = 0;
    /** Defines the time in milliseconds the profiler last started at. */
    private long start = -1;

    /**
     * Constructor. This won't start the profiler immediately.
     */
    public Profiler() {
        this(false);
    }

    /**
     * Constructor.
     *
     * @param start True to immediately start the profiler.
     */
    public Profiler(boolean start) {
        // Should the timer be started
        if(start)
            start();
    }

    /**
     * Start the profiler.
     *
    
     * @return True if the profiler was started, false otherwise possibly due to an error.
     * True will also be returned if the profiler was started already. */
    public boolean start() {
        // Make sure the timer isn't started already
        if(isActive())
            return true;

        // Set the start time
        this.start = System.currentTimeMillis();
        return true;
    }

    /**
     * This will start the profiler if it's not active, or will stop the profiler if it's currently active.
     *
    
     * @return True if the profiler has been started, false if the profiler has been stopped. */
    public boolean pause() {
        // Toggle the profiler state
        if(isStarted())
            stop();
        else
            start();

        // Return the result
        return isStarted();
    }

    /**
     * Stop the profiler if it's active.
     *
    
     * @return True will be returned if the profiler was stopped while it was active. False will be returned if the
     * profiler was stopped already. */
    public boolean stop() {
        // Make sure the profiler is active
        if(!isActive())
            return false;

        // Stop the profiler, calculate the passed time
        this.time += System.currentTimeMillis() - this.start;
        this.start = -1;
        return true;
    }

    /**
     * Check whether the profiler has been started. The profiler doesn't need to be active right now.
     *
    
     * @return True if the profiler was started, false otherwise. */
    public boolean isStarted() {
        return isActive() || this.time > 0;
    }

    /**
     * Check whether the profiler is currently active.
     *
    
     * @return True if the profiler is active, false otherwise. */
    public boolean isActive() {
        return this.start >= 0;
    }

    /**
     * Get the passed time in milliseconds.
     *
    
     * @return The passed time in milliseconds. */
    public long getTime() {
        // Check whether the profiler is currently active
        if(isActive())
            return this.time + (System.currentTimeMillis() - this.start);
        return this.time;
    }

    /**
     * Get the passed time in a formatted string.
     *
    
     * @return The passed time in a formatted string. */
    public String getTimeFormatted() {
        // Get the passed time
        long time = getTime();

        // Return the time if it's less than one millisecond
        if(time <= 0)
            return "<1 ms";

        // Return the time in milliseconds
        if(time < 1000)
            return time + " ms";

        // Convert the time into seconds with a single decimal
        double timeSeconds = ((double) time) / 1000;
        DecimalFormat df = new DecimalFormat("#0.0");
        return df.format(timeSeconds) + " s";
    }
}
