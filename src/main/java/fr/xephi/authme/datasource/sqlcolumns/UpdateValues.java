package fr.xephi.authme.datasource.sqlcolumns;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

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

    public static Builder builder() {
        return new Builder();
    }

    public static <C, T> Builder<C> with(Column<T, C> column, T value) {
        return new Builder<C>().and(column, value);
    }

    public static final class Builder<C> {
        private Map<Column<?, C>, Object> map = new HashMap<>();

        public <T> Builder<C> and(Column<T, C> column, T value) {
            map.put(column, value);
            return this;
        }

        public UpdateValues<C> build() {
            return new UpdateValues<>(map);
        }
    }

}
