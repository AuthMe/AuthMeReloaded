package fr.xephi.authme.util.datacolumns.predicate;

public interface Predicate<C> {

    Predicate<C> and(Predicate<C> other);

    Predicate<C> or(Predicate<C> other);

}
