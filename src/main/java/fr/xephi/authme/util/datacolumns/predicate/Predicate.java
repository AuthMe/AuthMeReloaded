package fr.xephi.authme.util.datacolumns.predicate;

/**
 * Predicates to filter entries in a data source.
 *
 * @param <C> the column's context type
 * @see StandardPredicates
 * @see fr.xephi.authme.util.datacolumns.sqlimplementation.PredicateSqlGenerator
 */
public interface Predicate<C> {

    /**
     * Combines this predicate and the provided one as a boolean {@code AND}, i.e. this method returns
     * a predicate which matches entries if both {@code this} predicate and the provided {@code other}
     * return a positive result.
     *
     * @param other the other predicate to use with this one
     * @return predicate evaluating as {@code this && other}
     */
    Predicate<C> and(Predicate<C> other);

    /**
     * Combines this predicate with the provided one as a boolean {@code OR}. In other words,
     * the predicate returned matches all entries which are matched {@code this} and/or the
     * given {@code other} predicate.
     *
     * @param other the other predicate to use with this one
     * @return predicate evaluating as {@code this || other}
     */
    Predicate<C> or(Predicate<C> other);

}
