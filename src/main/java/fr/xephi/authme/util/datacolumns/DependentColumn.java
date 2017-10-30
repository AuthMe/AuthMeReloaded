package fr.xephi.authme.util.datacolumns;

/**
 * Column whose value may be derived from an external object.
 */
public interface DependentColumn<T, C, D> extends Column<T, C> {

    /**
     * Gets the value associated with this column from the given dependent object.
     * This is typically used when you have an object representing your data source
     * layout; implementing this method will allow you to pass such an object with
     * the columns of your wish to specify which values should be changed / inserted.
     *
     * @param dependent the dependent to get the value from
     * @return the value from the dependent, may be null
     */
    T getValueFromDependent(D dependent);

}
