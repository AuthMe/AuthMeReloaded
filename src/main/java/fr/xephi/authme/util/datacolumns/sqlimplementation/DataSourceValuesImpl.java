package fr.xephi.authme.util.datacolumns.sqlimplementation;

import fr.xephi.authme.util.datacolumns.Column;
import fr.xephi.authme.util.datacolumns.DataSourceValues;

import java.util.HashMap;
import java.util.Map;


class DataSourceValuesImpl implements DataSourceValues {

    private static final DataSourceValuesImpl UNKNOWN_PLAYER = new DataSourceValuesImpl();

    private final Map<Column, Object> values = new HashMap<>();

    public static DataSourceValuesImpl unknownPlayer() {
        return UNKNOWN_PLAYER;
    }

    <T> void put(Column<T, ?> column, Object value) {
        final Class<T> typeClass = column.getType().getClazz();
        if (value == null || typeClass.isInstance(value)) {
            values.put(column, value);
        } else {
            throw new IllegalArgumentException(
                "Value '" + value + "' does not have the correct type for column '" + column + "'");
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(Column<T, ?> column) {
        return (T) values.computeIfAbsent(column, c -> {
           throw new IllegalArgumentException("No value available for column '" + c + "'");
        });
    }

    /**
     * @return whether the player of the associated value exists
     */
    @Override
    public boolean playerExists() {
        return this != UNKNOWN_PLAYER;
    }
}
