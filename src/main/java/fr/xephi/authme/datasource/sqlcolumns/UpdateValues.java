package fr.xephi.authme.datasource.sqlcolumns;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public final class UpdateValues {

    private final Map<Column<?>, Object> values;

    private UpdateValues(Map<Column<?>, Object> map) {
        this.values = map;
    }

    public Set<Column<?>> getColumns() {
        return values.keySet();
    }

    @SuppressWarnings("unchecked")
    public <T> T get(Column<T> column) {
        return (T) values.computeIfAbsent(column, c -> {
            throw new IllegalArgumentException("No value available for column '" + c + "'");
        });
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private Map<Column<?>, Object> map = new HashMap<>();

        public <T> Builder put(Column<T> column, T value) {
            map.put(column, value);
            return this;
        }

        public UpdateValues build() {
            return new UpdateValues(map);
        }
    }

}
