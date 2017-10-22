package fr.xephi.authme.datasource.sqlcolumns;

import fr.xephi.authme.datasource.SqlDataSourceUtils;

import java.sql.ResultSet;
import java.sql.SQLException;


public class TypeAdapter<C> {

    private C col;

    TypeAdapter(C col) {
        this.col = col;
    }

    public <T> T get(ResultSet rs, Column<T, C> column) throws SQLException {
        return createResultSetGetter(column.getType(), rs).getValue(column.resolveName(col));
    }

    private static <T> ResultSetGetter<T> createResultSetGetter(Type<T> type, ResultSet rs) {
        if (type == Type.STRING) {
            return cast(rs::getString);
        } else if (type == Type.LONG) {
            return cast(c -> SqlDataSourceUtils.getNullableLong(rs, c));
        } else {
            throw new IllegalStateException("Unhandled type '" + type + "'");
        }
    }

    private static <T> ResultSetGetter cast(ResultSetGetter<T> s) {
        return s;
    }

    @FunctionalInterface
    private interface ResultSetGetter<T> {
        T getValue(String column) throws SQLException;
    }
}
