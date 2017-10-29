package fr.xephi.authme.datasource.sqlcolumns.predicate;

public class NegatingPredicate<C> extends AbstractPredicate<C> {

    private final Predicate<C> predicate;

    public NegatingPredicate(Predicate<C> predicate) {
        this.predicate = predicate;
    }

    public Predicate<C> getPredicate() {
        return predicate;
    }
}
