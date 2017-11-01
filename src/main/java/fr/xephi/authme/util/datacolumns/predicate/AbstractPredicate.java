package fr.xephi.authme.util.datacolumns.predicate;

/**
 * Common base for predicates.
 *
 * @param <C> the context type
 */
public abstract class AbstractPredicate<C> implements Predicate<C> {

    @Override
    public Predicate<C> and(Predicate<C> other) {
        return new AndPredicate<>(this, other);
    }

    @Override
    public Predicate<C> or(Predicate<C> other) {
        return new OrPredicate<>(this, other);
    }

}
