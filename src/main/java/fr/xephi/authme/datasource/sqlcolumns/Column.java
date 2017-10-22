package fr.xephi.authme.datasource.sqlcolumns;

public interface Column<T, C> {

    String resolveName(C context);

    Type<T> getType();

    boolean isColumnUsed(C context);

}
