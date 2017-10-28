package fr.xephi.authme.datasource.sqlcolumns;

/**
 * C.
 */
public interface ColumnType<T> {

    /**
     * @return 40
     */
    Class<T> getClazz();

}
