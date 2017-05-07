package fr.xephi.authme.datasource;

/**
 * Wraps a value and allows to specify whether a value is missing or the player is not registered.
 */
public final class DataSourceResult<T> {

    /** Instance used when a player does not exist. */
    private static final DataSourceResult UNKNOWN_PLAYER = new DataSourceResult<>(null);
    private final T value;

    private DataSourceResult(T value) {
        this.value = value;
    }

    /**
     * Returns a {@link DataSourceResult} for the given value.
     *
     * @param value the value to wrap
     * @param <T> the value's type
     * @return DataSourceResult object for the given value
     */
    public static <T> DataSourceResult<T> of(T value) {
        return new DataSourceResult<>(value);
    }

    /**
     * Returns a {@link DataSourceResult} specifying that the player does not exist.
     *
     * @param <T> the value type
     * @return data source result for unknown player
     */
    public static <T> DataSourceResult<T> unknownPlayer() {
        return UNKNOWN_PLAYER;
    }

    /**
     * @return whether the player of the associated value exists
     */
    public boolean playerExists() {
        return this != UNKNOWN_PLAYER;
    }

    /**
     * Returns the value. It is {@code null} if the player is unknown. It is also {@code null}
     * if the player exists but does not have the value defined.
     *
     * @return the value, or null
     */
    public T getValue() {
        return value;
    }
}
