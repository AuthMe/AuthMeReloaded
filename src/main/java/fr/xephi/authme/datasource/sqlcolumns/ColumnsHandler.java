package fr.xephi.authme.datasource.sqlcolumns;

import fr.xephi.authme.datasource.DataSourceResult;

/**
 * Handler which performs operations on the data source based on the given
 * columns and values.
 *
 * @param <C> the context type
 * @param <I> the identifier type
 */
@SuppressWarnings("unchecked")
public interface ColumnsHandler<C, I> {

    /**
     * Retrieves the given column from a given row.
     *
     * @param identifier the id of the row to look up
     * @param column the column whose value should be retrieved
     * @param <T> the column type
     * @return the result of the lookup
     */
    <T> DataSourceResult<T> retrieve(I identifier, Column<T, C> column) throws Exception;

    /**
     * Retrieves multiple values from a given row.
     *
     * @param identifier the id of the row to look up
     * @param columns the columns to retrieve
     * @return map-like object with the requested values
     */
    DataSourceValues retrieve(I identifier, Column<?, C>... columns) throws Exception;

    /**
     * Changes a column from a specific row to the given value.
     *
     * @param identifier the id of the row to modify
     * @param column the column to modify
     * @param value the value to set the column to
     * @param <T> the column type
     * @return true upon success, false otherwise
     */
    <T> boolean update(I identifier, Column<T, C> column, T value) throws Exception;

    /**
     * Updates a row to have the given values.
     *
     * @param identifier the id of the row to modify
     * @param updateValues the values to set on the row
     * @return true upon success, false otherwise
     */
    boolean update(I identifier, UpdateValues<C> updateValues) throws Exception;

    /**
     * Updates a row to have the values as retrieved from the dependent object.
     *
     * @param identifier the id of the row to modify
     * @param dependent the dependent to get values from
     * @param columns the columns to update in the row
     * @param <D> the dependent type
     * @return true upon success, false otherwise
     */
    <D> boolean update(I identifier, D dependent, DependentColumn<?, C, D>... columns) throws Exception;

    /**
     * Inserts the given values into a new row.
     *
     * @param updateValues the values to insert
     * @return true upon success, false otherwise
     * @throws IllegalStateException if there is not at least one column that is not skipped
     */
    boolean insert(UpdateValues<C> updateValues) throws Exception;

    /**
     * Inserts the given values into a new row, as taken from the dependent object.
     *
     * @param dependent the dependent to get values from
     * @param columns the columns to insert
     * @param <D> the dependent type
     * @return true upon success, false otherwise
     * @throws IllegalStateException if there is not at least one column that is not skipped
     */
    <D> boolean insert(D dependent, DependentColumn<?, C, D>... columns) throws Exception;

}
