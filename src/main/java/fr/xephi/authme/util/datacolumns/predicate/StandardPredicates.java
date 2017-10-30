package fr.xephi.authme.util.datacolumns.predicate;

import fr.xephi.authme.util.datacolumns.Column;

public class StandardPredicates {

    protected StandardPredicates() {
    }

    public static <T, C> EqualityPredicate<T, C> eq(Column<T, C> column, T requiredValue) {
        return new EqualityPredicate<>(column, requiredValue);
    }

    public static <C> IsNullPredicate<C> isNull(Column<?, C> column) {
        return new IsNullPredicate<>(column);
    }

    public static <C> NegatingPredicate<C> not(Predicate<C> predicate) {
        return new NegatingPredicate<>(predicate);
    }

    public static <C> AndPredicate<C> and(Predicate<C> left, Predicate<C> right) {
        return new AndPredicate<>(left, right);
    }

    public static <C> OrPredicate<C> or(Predicate<C> left, Predicate<C> right) {
        return new OrPredicate<>(left, right);
    }
}
