package fr.xephi.authme.datasource.sqlcolumns;


public final class Type<T> {

    public static final Type<String> STRING = new Type<>(String.class);

    public static final Type<Long> LONG = new Type<>(Long.class);

    public static final Type<Boolean> BOOLEAN = new Type<>(Boolean.class);


    private final Class<T> clazz;

    private Type(Class<T> clazz) {
        this.clazz = clazz;
    }

    public Class<T> getClazz() {
        return clazz;
    }
}
