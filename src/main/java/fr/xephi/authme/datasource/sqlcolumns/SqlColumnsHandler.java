package fr.xephi.authme.datasource.sqlcolumns;

import fr.xephi.authme.datasource.DataSourceResult;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class SqlColumnsHandler<C> implements ColumnsHandler<C, String> {

    private Connection connection;
    private String tableName;
    private TypeAdapter<C> typeAdapter;
    private C context;
    private String idColumn;

    /**
     * Constructor.
     *
     * @param connection connection to the database
     * @param context the context object (for name resolution)
     * @param tableName name of the SQL table
     * @param idColumn the identifier column
     */
    public SqlColumnsHandler(Connection connection, C context, String tableName, String idColumn)  {
        this.context = context;
        this.typeAdapter = new TypeAdapter<>(context);
        this.tableName = tableName;
        this.connection = connection;
        this.idColumn = idColumn;
    }

    @Override
    public <T> DataSourceResult<T> retrieve(String identifier, Column<T, C> column) {
        // TODO: handle empty column
        String sql = "SELECT " + column.resolveName(context) + " FROM " + tableName
            + " WHERE " + idColumn + " = ?;";
        try (PreparedStatement pst = connection.prepareStatement(sql)) {
            pst.setString(1, identifier);
            try (ResultSet rs = pst.executeQuery()) {
                return rs.next()
                    ? DataSourceResult.of(typeAdapter.get(rs, column))
                    : DataSourceResult.unknownPlayer();
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public DataSourceValues retrieve(String identifier, Column<?, C>... columns) {
        Set<Column<?, C>> nonEmptyColumns = removeSkippedColumns(columns);
        String sql = "SELECT " + commaSeparatedList(nonEmptyColumns)
            + " FROM " + tableName + " WHERE " + idColumn + " = ?;";
        try (PreparedStatement pst = connection.prepareStatement(sql)) {
            pst.setString(1, identifier);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    DataSourceValues values = new DataSourceValues();
                    for (Column<?, C> column : columns) {
                        if (nonEmptyColumns.contains(column)) {
                            values.put(column, typeAdapter.get(rs, column));
                        } else {
                            values.put(column, null);
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

    @Override
    public <T> boolean update(String identifier, Column<T, C> column, T value) {
        // TODO: handle optional column
        String sql = String.format("UPDATE %s SET %s = ? WHERE %s = ?;",
            tableName, column.resolveName(context), idColumn);
        try (PreparedStatement pst = connection.prepareStatement(sql)) {
            pst.setObject(1, value);
            pst.setString(2, identifier);
            return pst.execute();
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public boolean update(String identifier, UpdateValues<C> updateValues) {
        return performUpdate(
            identifier,
            updateValues.getColumns(),
            updateValues::get);
    }

    public <D> boolean update(String identifier, D auth, DependentColumn<?, C, D>... columns) {
        return performUpdate(
            identifier,
            Arrays.asList(columns),
            column -> column.getFromDependent(auth));
    }

    private <E extends Column<?, C>> boolean performUpdate(String identifier, Collection<E> columns,
                                                           Function<E, Object> valueGetter) {
        Set<E> nonEmptyColumns = removeSkippedColumns(columns);
        if (nonEmptyColumns.isEmpty()) {
            return true;
        }

        String sql = "UPDATE " + tableName + " SET "
            + commaSeparatedList(nonEmptyColumns, colName -> colName + " = ?")
            + " WHERE " + idColumn + " = ?;";
        try (PreparedStatement pst = connection.prepareStatement(sql)) {
            int index = bindValues(pst, 1, columns, valueGetter);
            pst.setString(index, identifier);
            return pst.execute();
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    /*
     * Creates a comma-separated list with the given columns' names.
     */
    private String commaSeparatedList(Collection<? extends Column<?, C>> columns) {
        return commaSeparatedList(columns, Function.identity());
    }

    /*
     * Creates a comma-separated list with the result of the provided function, to which each column's name is given.
     * Typically used to generate a portion of an SQL query, e.g. {@code name -> name + " = ?"} to yield something like
     * "col1 = ?, col2 = ?, col3 = ?" from three columns.
     */
    private String commaSeparatedList(Collection<? extends Column<?, C>> columns,
                                      Function<String, String> columnNameToSql) {
        return columns.stream()
            .map(column -> column.resolveName(context))
            .map(columnNameToSql)
            .collect(Collectors.joining(", "));
    }

    private <E extends Column<?, C>> int bindValues(PreparedStatement pst,
                                                    int startIndex,
                                                    Collection<E> columns,
                                                    Function<E, Object> valueGetter) throws SQLException {
        int index = startIndex;
        for (E column : columns) {
            pst.setObject(index, valueGetter.apply(column));
            ++index;
        }
        return index;
    }

    private <E extends Column<?, C>> Set<E> removeSkippedColumns(Collection<E> cols) {
        return cols.stream()
            .filter(column -> !column.isColumnUsed(context))
            .collect(Collectors.toSet());
    }

    private Set<Column<?, C>> removeSkippedColumns(Column<?, C>... cols) {
        return removeSkippedColumns(Arrays.asList(cols));
    }
}
