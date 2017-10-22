package fr.xephi.authme.datasource.sqlcolumns;

/**
 * Column whose value may be derived from an external object.
 */
public interface DependentColumn<T, C, D> extends Column<T, C> {

    T getFromDependent(D dependent);

}
