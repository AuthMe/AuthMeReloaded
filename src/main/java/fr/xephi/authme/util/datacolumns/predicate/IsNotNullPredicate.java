package fr.xephi.authme.util.datacolumns.predicate;

import fr.xephi.authme.util.datacolumns.Column;

public class IsNotNullPredicate<C> extends AbstractPredicate<C> {

    private final Column<?, C> column;

    public IsNotNullPredicate(Column<?, C> column) {
        this.column = column;
    }

    public Column<?, C> getColumn() {
        return column;
    }
}
