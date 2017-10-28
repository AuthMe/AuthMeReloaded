package fr.xephi.authme.datasource.sqlcolumns;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Holder class for values associated with a column.
 *
 * @param <C> the context type
 */
public final class UpdateValues<C> {

    private final Map<Column<?, C>, Object> values;

    private UpdateValues(Map<Column<?, C>, Object> map) {
        this.values = map;
    }

    public Set<Column<?, C>> getColumns() {
        return values.keySet();
    }

    @SuppressWarnings("unchecked")
    public <T> T get(Column<T, C> column) {
        return (T) values.computeIfAbsent(column, c -> {
            throw new IllegalArgumentException("No value available for column '" + c + "'");
        });
    }

    /**
     * Creates a new builder with the given column and its value as first entry.
     *
     * @param column the column to save a value for
     * @param value the value to save
     * @param <C> the column's context type
     * @param <T> the column's type
     * @return the created builder
     */
    public static <C, T> Builder<C> with(Column<T, C> column, T value) {
        return new Builder<C>().and(column, value);
    }

    /** Builder class. */
    public static final class Builder<C> {

        private Map<Column<?, C>, Object> map = new HashMap<>();

        /**
         * Adds a new value and returns the builder.
         *
         * @param column the column to save a value for
         * @param value the value to save
         * @param <T> the column's type
         * @return this instance
         */
        public <T> Builder<C> and(Column<T, C> column, T value) {
            map.put(column, value);
            return this;
        }

        /**
         * Creates an {@link UpdateValues} instance with the values provided to this builder.
         *
         * @return the generated values object
         */
        public UpdateValues<C> build() {
            return new UpdateValues<>(map);
        }
    }
}
