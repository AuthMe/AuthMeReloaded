package fr.xephi.authme.util.datacolumns;

/**
 * Default implementations of {@link ColumnType}.
 *
 * @param <T> the type
 */
public final class StandardTypes<T> implements ColumnType<T> {

    /** String type. */
    public static final ColumnType<String> STRING = new StandardTypes<>(String.class);

    /** Long type. */
    public static final ColumnType<Long> LONG = new StandardTypes<>(Long.class);

    /** Integer type. */
    public static final ColumnType<Integer> INTEGER = new StandardTypes<>(Integer.class);

    /** Boolean type. */
    public static final ColumnType<Boolean> BOOLEAN = new StandardTypes<>(Boolean.class);


    private final Class<T> clazz;

    private StandardTypes(Class<T> clazz) {
        this.clazz = clazz;
    }

    @Override
    public Class<T> getClazz() {
        return clazz;
    }
}
