package fr.xephi.authme.datasource.sqlcolumns.predicate;

import fr.xephi.authme.datasource.sqlcolumns.Column;

public class IsNullPredicate<C> extends AbstractPredicate<C> {

    private final Column<?, C> column;

    public IsNullPredicate(Column<?, C> column) {
        this.column = column;
    }

    public Column<?, C> getColumn() {
        return column;
    }
}
