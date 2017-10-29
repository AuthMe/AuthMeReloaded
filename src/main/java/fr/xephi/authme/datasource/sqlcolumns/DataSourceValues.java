package fr.xephi.authme.datasource.sqlcolumns;

/**
 *
 */
public interface DataSourceValues {

    /**
     * @return whether the player of the associated value exists
     */
    boolean playerExists();

    <T> T get(Column<T, ?> column);
}
