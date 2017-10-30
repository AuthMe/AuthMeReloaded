package fr.xephi.authme.util.datacolumns.predicate;

import fr.xephi.authme.util.datacolumns.Column;

public class EqualityPredicate<T, C> extends AbstractPredicate<C> {

    private final Column<T, C> column;
    private final T object;

    public EqualityPredicate(Column<T, C> column, T object) {
        this.column = column;
        this.object = object;
    }

    public Column<T, C> getColumn() {
        return column;
    }

    public T getObject() {
        return object;
    }
}
