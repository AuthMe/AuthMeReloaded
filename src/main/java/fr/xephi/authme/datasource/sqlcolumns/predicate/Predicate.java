package fr.xephi.authme.datasource.sqlcolumns.predicate;

public interface Predicate<C> {

    Predicate<C> and(Predicate<C> other);

    Predicate<C> or(Predicate<C> other);

}
