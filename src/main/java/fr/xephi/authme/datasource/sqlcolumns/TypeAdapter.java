package fr.xephi.authme.datasource.sqlcolumns;

import fr.xephi.authme.datasource.Columns;
import fr.xephi.authme.datasource.SqlDataSourceUtils;
import fr.xephi.authme.settings.Settings;

import java.sql.ResultSet;
import java.sql.SQLException;


public class TypeAdapter {

    private Columns col;

    TypeAdapter(Settings settings) {
        this.col = new Columns(settings);
    }

    public <T> T get(ResultSet rs, Column<T> column) throws SQLException {
        return createResultSetGetter(column.getType(), rs).getValue(column.returnName(col));
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
