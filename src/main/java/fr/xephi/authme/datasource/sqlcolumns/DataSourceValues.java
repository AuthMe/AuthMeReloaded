package fr.xephi.authme.datasource.sqlcolumns;

import java.util.HashMap;
import java.util.Map;


public class DataSourceValues {

    private static final DataSourceValues UNKNOWN_PLAYER = new DataSourceValues();

    private final Map<Column, Object> values = new HashMap<>();

    public static DataSourceValues unknownPlayer() {
        return UNKNOWN_PLAYER;
    }

    <T> void put(Column<T> column, Object value) {
        final Class<T> typeClass = column.getType().getClazz();
        if (value == null || typeClass.isInstance(value)) {
            values.put(column, value);
        } else {
            throw new IllegalArgumentException(
                "Value '" + value + "' does not have the correct type for column '" + column + "'");
        }
    }

    @SuppressWarnings("unchecked")
    public <T> T get(Column<T> column) {
        return (T) values.computeIfAbsent(column, c -> {
           throw new IllegalArgumentException("No value available for column '" + c + "'");
        });
    }

    /**
     * @return whether the player of the associated value exists
     */
    public boolean playerExists() {
        return this != UNKNOWN_PLAYER;
    }
}
