package fr.xephi.authme.output;

/**
 * Log level.
 */
public enum LogLevel {

    /** Info: general messages. */
    INFO(3),

    /** Fine: more detailed messages that may still be interesting to plugin users. */
    FINE(2),

    /** Debug: very detailed messages for debugging. */
    DEBUG(1);

    private int value;

    /**
     * Constructor.
     *
     * @param value the log level; the higher the number the more "important" the level.
     *              A log level enables its number and all above.
     */
    LogLevel(int value) {
        this.value = value;
    }

    /**
     * Return whether the current log level includes the given log level.
     *
     * @param level the level to process
     * @return true if the level is enabled, false otherwise
     */
    public boolean includes(LogLevel level) {
        return value <= level.value;
    }
}
