package fr.xephi.authme.util.datacolumns.sqlimplementation;

import fr.xephi.authme.util.datacolumns.Column;
import fr.xephi.authme.util.datacolumns.ColumnType;
import fr.xephi.authme.util.datacolumns.StandardTypes;

import java.sql.ResultSet;
import java.sql.SQLException;

class TypeAdapter<C> {

    private final C context;

    TypeAdapter(C context) {
        this.context = context;
    }

    public <T> T get(ResultSet rs, Column<T, C> column) throws SQLException {
        return createResultSetGetter(column.getType()).getValue(rs, column.resolveName(context));
    }

    /**
     * Returns a function based on the input type from which a value can be retrieved
     * from the give result set.
     *
     * @param type the type to create a getter for
     * @param <T> the type
     * @return the getter to use
     */
    protected <T> ResultSetGetter<T> createResultSetGetter(ColumnType<T> type) {
        // TODO: cache items?
        final ResultSetGetter resultSetGetter;
        if (type == StandardTypes.STRING) {
            resultSetGetter = ResultSet::getString;
        } else if (type == StandardTypes.LONG) {
            resultSetGetter = getTypeNullable(ResultSet::getLong);
        } else if (type == StandardTypes.INTEGER) {
            resultSetGetter = getTypeNullable(ResultSet::getInt);
        } else if (type == StandardTypes.BOOLEAN) {
            resultSetGetter = getTypeNullable(ResultSet::getBoolean);
        } else {
            throw new IllegalStateException("Unhandled type '" + type + "'");
        }
        return resultSetGetter;
    }

    private static <T> ResultSetGetter<T> getTypeNullable(ResultSetGetter<T> getter) {
        return (rs, column) -> {
            T value = getter.getValue(rs, column);
            return rs.wasNull() ? null : value;
        };
    }

    @FunctionalInterface
    private interface ResultSetGetter<T> {

        T getValue(ResultSet rs, String column) throws SQLException;

    }
}
