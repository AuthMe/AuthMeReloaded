package fr.xephi.authme.util.datacolumns.predicate;

/**
 * Predicate that always evaluates to true.
 *
 * @param <C> the context type (not used here)
 */
public class AlwaysTruePredicate<C> extends AbstractPredicate<C> {

    @Override
    public Predicate<C> and(Predicate<C> other) {
        return other;
    }

    @Override
    public Predicate<C> or(Predicate<C> other) {
        return this;
    }
}
