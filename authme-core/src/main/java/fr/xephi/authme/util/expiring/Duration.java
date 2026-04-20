package fr.xephi.authme.util.expiring;

import java.util.concurrent.TimeUnit;

/**
 * Represents a duration in time, defined by a time unit and a duration.
 */
public class Duration {

    private final long duration;
    private final TimeUnit unit;

    /**
     * Constructor.
     *
     * @param duration the duration
     * @param unit the time unit in which {@code duration} is expressed
     */
    public Duration(long duration, TimeUnit unit) {
        this.duration = duration;
        this.unit = unit;
    }

    /**
     * Creates a Duration object for the given duration and unit in the most suitable time unit.
     * For example, {@code createWithSuitableUnit(120, TimeUnit.SECONDS)} will return a Duration
     * object of 2 minutes.
     * <p>
     * This method only considers the time units days, hours, minutes, and seconds for the objects
     * it creates. Conversion is done with {@link TimeUnit#convert} and so always rounds the
     * results down.
     * <p>
     * Further examples:
     *   <code>createWithSuitableUnit(299, TimeUnit.MINUTES); // 4 hours</code>
     *   <code>createWithSuitableUnit(700, TimeUnit.MILLISECONDS); // 0 seconds</code>
     *
     * @param sourceDuration the duration
     * @param sourceUnit the time unit the duration is expressed in
     * @return Duration object using the most suitable time unit
     */
    public static Duration createWithSuitableUnit(long sourceDuration, TimeUnit sourceUnit) {
        long durationMillis = Math.abs(TimeUnit.MILLISECONDS.convert(sourceDuration, sourceUnit));

        TimeUnit targetUnit;
        if (durationMillis > 1000L * 60L * 60L * 24L) {
            targetUnit = TimeUnit.DAYS;
        } else if (durationMillis > 1000L * 60L * 60L) {
            targetUnit = TimeUnit.HOURS;
        } else if (durationMillis > 1000L * 60L) {
            targetUnit = TimeUnit.MINUTES;
        } else {
            targetUnit = TimeUnit.SECONDS;
        }

        long durationInTargetUnit = targetUnit.convert(sourceDuration, sourceUnit);
        return new Duration(durationInTargetUnit, targetUnit);
    }

    /**
     * @return the duration
     */
    public long getDuration() {
        return duration;
    }

    /**
     * @return the time unit in which the duration is expressed
     */
    public TimeUnit getTimeUnit() {
        return unit;
    }
}
