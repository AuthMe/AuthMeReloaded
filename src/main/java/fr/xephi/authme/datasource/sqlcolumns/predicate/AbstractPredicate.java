package fr.xephi.authme.datasource.sqlcolumns.predicate;

public abstract class AbstractPredicate<C> implements Predicate<C> {

    public Predicate<C> and(Predicate other) {
        return new AndPredicate<>(this, other);
    }

    public Predicate<C> or(Predicate<C> other) {
        return new OrPredicate<>(this, other);
    }

}
