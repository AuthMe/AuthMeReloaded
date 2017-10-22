package fr.xephi.authme.datasource.sqlcolumns;

import fr.xephi.authme.data.auth.PlayerAuth;
import fr.xephi.authme.datasource.Columns;
import fr.xephi.authme.datasource.DataSourceResult;
import fr.xephi.authme.datasource.SQLite;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.DatabaseSettings;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * TODO: merge into DataSource
 */
public class SqliteTestExt extends SQLite {

    private Connection connection;
    private String tableName;
    private TypeAdapter typeAdapter;
    private Columns col;

    public SqliteTestExt(Settings settings, Connection connection)  {
        super(settings, connection);
        col = new Columns(settings);
        typeAdapter = new TypeAdapter(settings);
        tableName = settings.getProperty(DatabaseSettings.MYSQL_TABLE);
        this.connection = connection;
    }

    public <T> DataSourceResult<T> retrieve(String name, Column<T> column) {
        String sql = String.format("SELECT %s FROM %s WHERE %s = ?;",
            column.returnName(col), tableName, col.NAME);
        try (PreparedStatement pst = connection.prepareStatement(sql)) {
            pst.setString(1, name);
            try (ResultSet rs = pst.executeQuery()) {
                return rs.next()
                    ? DataSourceResult.of(typeAdapter.get(rs, column))
                    : DataSourceResult.unknownPlayer();
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    public DataSourceValues retrieve(String name, Column<?>... columns) {
        Set<Column<?>> nonEmptyColumns = removeSkippedColumns(columns);
        String sql = "SELECT "
            + nonEmptyColumns.stream()
                .map(column -> column.returnName(col))
                .collect(Collectors.joining(", "))
            + " FROM " + tableName
            + " WHERE " + col.NAME + " = ?;";
        try (PreparedStatement pst = connection.prepareStatement(sql)) {
            pst.setString(1, name);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    DataSourceValues values = new DataSourceValues();
                    for (Column<?> column : columns) {
                        if (column.returnName(col).isEmpty()) {
                            values.put(column, null);
                        } else {
                            values.put(column, typeAdapter.get(rs, column));
                        }
                    }
                    return values;
                } else {
                    return DataSourceValues.unknownPlayer();
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    public <T> boolean update(String name, Column<T> column, T value) {
        String sql = String.format("UPDATE %s SET %s = ? WHERE %s = ?;",
            tableName, column.returnName(col), col.NAME);
        try (PreparedStatement pst = connection.prepareStatement(sql)) {
            pst.setObject(1, value);
            pst.setString(2, name);
            return pst.execute();
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    public boolean update(String name, UpdateValues updateValues) {
        return performUpdate(
            name,
            updateValues.getColumns(),
            updateValues::get);
    }

    public boolean update(PlayerAuth auth, Column<?>... columns) {
        return performUpdate(
            auth.getNickname(),
            Arrays.asList(columns),
            column -> column.fromPlayerAuth(auth));
    }

    private boolean performUpdate(String name, Collection<Column<?>> columns, Function<Column<?>, Object> valueGetter) {
        Set<Column<?>> nonEmptyColumns = removeSkippedColumns(columns);
        if (nonEmptyColumns.isEmpty()) {
            return true;
        }

        String sql = "UPDATE " + tableName + " SET "
            + nonEmptyColumns.stream()
            .map(column -> column.returnName(col) + " = ?")
            .collect(Collectors.joining(", "))
            + " WHERE " + col.NAME + " = ?;";
        try (PreparedStatement pst = connection.prepareStatement(sql)) {
            int index = 1;
            for (Column column : nonEmptyColumns) {
                pst.setObject(index, valueGetter.apply(column));
                ++index;
            }
            pst.setString(index, name);
            return pst.execute();
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    private Set<Column<?>> removeSkippedColumns(Collection<Column<?>> cols) {
        return cols.stream()
            .filter(column -> !column.returnName(col).isEmpty())
            .collect(Collectors.toSet());
    }

    private Set<Column<?>> removeSkippedColumns(Column<?>... cols) {
        return removeSkippedColumns(Arrays.asList(cols));
    }
}
