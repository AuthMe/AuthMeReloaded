package fr.xephi.authme.datasource.sqlcolumns;

import fr.xephi.authme.datasource.SqlDataSourceUtils;

import java.sql.ResultSet;
import java.sql.SQLException;

public class TypeAdapter<C> {

    private C context;

    TypeAdapter(C context) {
        this.context = context;
    }

    public <T> T get(ResultSet rs, Column<T, C> column) throws SQLException {
        return createResultSetGetter(column.getType(), rs).getValue(column.resolveName(context));
    }

    /**
     * Returns a function based on the input type from which a value can be retrieved
     * from the give result set.
     *
     * @param type the type to create a getter for
     * @param rs the result set to retrieve values from
     * @param <T> the type
     * @return the getter to use
     */
    protected <T> ResultSetGetter<T> createResultSetGetter(Type<T> type, ResultSet rs) {
        // TODO: cache items?
        final ResultSetGetter resultSetGetter;
        if (type == Type.STRING) {
            resultSetGetter = rs::getString;
        } else if (type == Type.LONG) {
            resultSetGetter = c -> SqlDataSourceUtils.getNullableLong(rs, c);
        } else {
            throw new IllegalStateException("Unhandled type '" + type + "'");
        }
        return resultSetGetter;
    }

    @FunctionalInterface
    private interface ResultSetGetter<T> {
        T getValue(String column) throws SQLException;
    }
}
