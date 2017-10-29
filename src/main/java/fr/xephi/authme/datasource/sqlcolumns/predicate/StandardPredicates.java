package fr.xephi.authme.datasource.sqlcolumns.predicate;

import com.sun.org.apache.xpath.internal.operations.Neg;
import fr.xephi.authme.datasource.sqlcolumns.Column;

public class StandardPredicates {

    public static <T, C> EqualityPredicate<T, C> eq(Column<T, C> column, T requiredValue) {
        return new EqualityPredicate<>(column, requiredValue);
    }

    public static <C> NegatingPredicate<C> not(Predicate<C> predicate) {
        return new NegatingPredicate<>(predicate);
    }
}
