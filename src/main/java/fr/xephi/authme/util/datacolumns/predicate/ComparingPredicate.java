package fr.xephi.authme.util.datacolumns.predicate;

import fr.xephi.authme.util.datacolumns.Column;

/**
 * Predicate comparing a column with a given value.
 *
 * @param <T> the column's value type
 * @param <C> the column's context type
 */
public class ComparingPredicate<T, C> extends AbstractPredicate<C> {

    private final Column<T, C> column;
    private final T value;
    private final Type type;

    public ComparingPredicate(Column<T, C> column, T value, Type type) {
        this.column = column;
        this.value = value;
        this.type = type;
    }

    public Column<?, C> getColumn() {
        return column;
    }

    public Object getValue() {
        return value;
    }

    public Type getType() {
        return type;
    }

    /** The comparison type. */
    public enum Type {
        LESS,
        LESS_EQUALS,
        EQUALS,
        NOT_EQUALS,
        GREATER,
        GREATER_EQUALS
    }
}
