package fr.xephi.authme.datasource.sqlcolumns;

/**
 * Created by Lucas-Keoki on 10/27/2017.
 */
public interface DataSourceValues {

    /**
     * @return whether the player of the associated value exists
     */
    boolean playerExists();

    <T> T get(Column<T, ?> column);
}
