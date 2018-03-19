package fr.xephi.authme.util.datacolumns.predicate;

import fr.xephi.authme.util.datacolumns.Column;

/**
 * Predicate matching if a column is null.
 *
 * @param <C> the context type
 */
public class IsNullPredicate<C> extends AbstractPredicate<C> {

    private final Column<?, C> column;

    public IsNullPredicate(Column<?, C> column) {
        this.column = column;
    }

    public Column<?, C> getColumn() {
        return column;
    }
}
