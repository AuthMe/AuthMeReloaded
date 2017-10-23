package fr.xephi.authme.datasource.sqlcolumns;

/**
 * Column interface. Represents a datum from a data source, e.g. a column in a SQL table.
 *
 * @param <T> the type of the column
 * @param <C> the context type (typically an object holding some configurable values)
 */
public interface Column<T, C> {

    /**
     * Returns the column's name, based on the context.
     *
     * @param context the context to resolve the name with
     * @return the name of the column (may be empty, never null)
     */
    String resolveName(C context);

    /**
     * @return the type of the value represented by this column
     */
    Type<T> getType();

    /**
     * Returns whether this column should be used.
     *
     * @param context the context to resolve the value with
     * @return true if this column should be included in a data source operation,
     *         false if it should be ignored (column doesn't exist in the data source)
     */
    boolean isColumnUsed(C context);

}
