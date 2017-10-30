package fr.xephi.authme.util.datacolumns.sqlimplementation;

import fr.xephi.authme.datasource.DataSourceResult;
import fr.xephi.authme.util.datacolumns.Column;
import fr.xephi.authme.util.datacolumns.ColumnsHandler;
import fr.xephi.authme.util.datacolumns.DataSourceValues;
import fr.xephi.authme.util.datacolumns.DependentColumn;
import fr.xephi.authme.util.datacolumns.UpdateValues;
import fr.xephi.authme.util.datacolumns.predicate.Predicate;
import fr.xephi.authme.util.datacolumns.sqlimplementation.PredicateSqlGenerator.WhereClauseResult;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Implementation of {@link ColumnsHandler} for a SQL data source.
 *
 * @param <C> the context type
 * @param <I> the identifier type
 */
public class SqlColumnsHandler<C, I> implements ColumnsHandler<C, I> {

    private final Connection connection;
    private final String tableName;
    private final String idColumn;
    private final TypeAdapter<C> typeAdapter;
    private final PredicateSqlGenerator<C> predicateSqlGenerator;
    private final C context;

    /**
     * Constructor.
     *
     * @param connection connection to the database
     * @param context the context object (for name resolution)
     * @param tableName name of the SQL table
     * @param idColumn the name of the identifier column
     */
    public SqlColumnsHandler(Connection connection, C context, String tableName, String idColumn)  {
        this.context = context;
        this.typeAdapter = new TypeAdapter<>(context);
        this.predicateSqlGenerator = new PredicateSqlGenerator<>(context);
        this.tableName = tableName;
        this.connection = connection;
        this.idColumn = idColumn;
    }

    @Override
    public <T> DataSourceResult<T> retrieve(I identifier, Column<T, C> column) throws SQLException {
        if (!column.isColumnUsed(context)) {
            return DataSourceResult.of(null);
        }
        String sql = "SELECT " + column.resolveName(context) + " FROM " + tableName
            + " WHERE " + idColumn + " = ?;";
        try (PreparedStatement pst = connection.prepareStatement(sql)) {
            pst.setObject(1, identifier);
            try (ResultSet rs = pst.executeQuery()) {
                return rs.next()
                    ? DataSourceResult.of(typeAdapter.get(rs, column))
                    : DataSourceResult.unknownPlayer();
            }
        }
    }

    @Override
    public DataSourceValues retrieve(I identifier, Column<?, C>... columns) throws SQLException {
        Set<Column<?, C>> nonEmptyColumns = removeSkippedColumns(columns);
        String sql = "SELECT " + commaSeparatedList(nonEmptyColumns)
            + " FROM " + tableName + " WHERE " + idColumn + " = ?;";
        try (PreparedStatement pst = connection.prepareStatement(sql)) {
            pst.setObject(1, identifier);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    DataSourceValuesImpl values = new DataSourceValuesImpl();
                    for (Column<?, C> column : columns) {
                        if (nonEmptyColumns.contains(column)) {
                            values.put(column, typeAdapter.get(rs, column));
                        } else {
                            values.put(column, null);
                        }
                    }
                    return values;
                } else {
                    return DataSourceValuesImpl.unknownPlayer();
                }
            }
        }
    }

    @Override
    public <T> boolean update(I identifier, Column<T, C> column, T value) throws SQLException {
        if (!column.isColumnUsed(context)) {
            return true;
        }
        String sql = "UPDATE " + tableName + " SET " + column.resolveName(context)
            + " WHERE " + idColumn + " = ?;";
        try (PreparedStatement pst = connection.prepareStatement(sql)) {
            pst.setObject(1, value);
            pst.setObject(2, identifier);
            return performUpdateAction(pst);
        }
    }

    @Override
    public boolean update(I identifier, UpdateValues<C> updateValues) throws SQLException {
        return performUpdate(
            identifier,
            updateValues.getColumns(),
            updateValues::get);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <D> boolean update(I identifier, D dependent, DependentColumn<?, C, D>... columns) throws SQLException {
        return performUpdate(
            identifier,
            Arrays.asList(columns),
            column -> column.getValueFromDependent(dependent));
    }

    @Override
    public boolean insert(UpdateValues<C> updateValues) throws SQLException {
        return performInsert(updateValues.getColumns(), updateValues::get);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <D> boolean insert(D dependent, DependentColumn<?, C, D>... columns) throws SQLException {
        return performInsert(Arrays.asList(columns), column -> column.getValueFromDependent(dependent));
    }

    @Override
    public int count(Predicate<C> predicate) throws SQLException {
        WhereClauseResult whereResult = predicateSqlGenerator.generateWhereClause(predicate);
        String sql = "SELECT COUNT(1) FROM " + tableName + " WHERE " + whereResult.getGeneratedSql();
        try (PreparedStatement pst = connection.prepareStatement(sql)) {
            bindValues(pst, 1, whereResult.getBindings());
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
                throw new IllegalStateException("Could not fetch count for SQL '" + sql + "'");
            }
        }
    }

    private <E extends Column<?, C>> boolean performUpdate(I identifier, Collection<E> columns,
                                                           Function<E, Object> valueGetter) throws SQLException{
        Set<E> nonEmptyColumns = removeSkippedColumns(columns);
        if (nonEmptyColumns.isEmpty()) {
            return true;
        }

        String sql = "UPDATE " + tableName + " SET "
            + commaSeparatedList(nonEmptyColumns, colName -> colName + " = ?")
            + " WHERE " + idColumn + " = ?;";
        try (PreparedStatement pst = connection.prepareStatement(sql)) {
            int index = bindValues(pst, 1, columns, valueGetter);
            pst.setObject(index, identifier);
            return performUpdateAction(pst);
        }
    }

    private <E extends Column<?, C>> boolean performInsert(Collection<E> columns,
                                                           Function<E, Object> valueGetter) throws SQLException {
        Set<E> nonEmptyColumns = removeSkippedColumns(columns);
        if (nonEmptyColumns.isEmpty()) {
            throw new IllegalStateException("Cannot perform insert when all columns are empty: " + columns);
        }

        String sql = "INSERT INTO " + tableName + " (" + commaSeparatedList(nonEmptyColumns) + ") "
            + "VALUES(" + commaSeparatedList(nonEmptyColumns, "?") + ");";
        try (PreparedStatement pst = connection.prepareStatement(sql)) {
            bindValues(pst, 1, nonEmptyColumns, valueGetter);
            return performUpdateAction(pst);
        }
    }

    /**
     * Wraps {@link PreparedStatement#executeUpdate()} for UPDATE and INSERT statements and returns a boolean
     * based on its return value.
     *
     * @param pst the prepared statement
     * @return true if one row was updated, false otherwise
     * @throws IllegalStateException if more than one row was updated (should never happen)
     * @throws SQLException on SQL errors
     */
    protected boolean performUpdateAction(PreparedStatement pst) throws SQLException {
        int count = pst.executeUpdate();
        if (count == 1) {
            return true;
        } else if (count == 0) {
            return false;
        } else {
            throw new IllegalStateException("Found " + count + " rows updated/inserted by statement, expected only 1");
        }
    }

    /*
     * Creates a comma-separated list with the given columns' names.
     */
    private String commaSeparatedList(Collection<? extends Column<?, C>> columns) {
        return commaSeparatedList(columns, Function.identity());
    }

    private String commaSeparatedList(Collection<? extends Column<?, ?>> columns, String value) {
        return IntStream.range(0, columns.size())
            .mapToObj(i -> value)
            .collect(Collectors.joining(", "));
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

    /**
     * Binds the values of the given columns with the provided {@code valueGetter} to the PreparedStatement,
     * starting from the given index (to allow values to be bound before this method is called).
     * The ending index is returned (to allow more values to be bound after calling this method).
     *
     * @param pst the prepared statement
     * @param startIndex the index at which value binding should begin
     * @param columns the columns
     * @param valueGetter function to look up the value to bind, based on the column
     * @param <E> the column extension type
     * @return the index at which binding should continue (if applicable)
     */
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

    private int bindValues(PreparedStatement pst, int startIndex, Collection<Object> bindings) throws SQLException {
        int index = startIndex;
        for (Object binding : bindings) {
            pst.setObject(index, binding);
            ++index;
        }
        return index;
    }

    /**
     * Returns a Set of columns without any that should be skipped
     * (as determined by {@link Column#isColumnUsed}.
     *
     * @param cols the columns to filter
     * @param <E> the column extension type
     * @return set with all columns to use
     */
    private <E extends Column<?, C>> Set<E> removeSkippedColumns(Collection<E> cols) {
        return cols.stream()
            .filter(column -> column.isColumnUsed(context))
            .collect(Collectors.toSet());
    }

    @SafeVarargs
    private final Set<Column<?, C>> removeSkippedColumns(Column<?, C>... cols) {
        return removeSkippedColumns(Arrays.asList(cols));
    }
}
