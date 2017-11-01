package fr.xephi.authme.util.datacolumns.predicate;

/**
 * Predicate combining two predicates as a boolean {@code AND}.
 *
 * @param <C> the context type
 */
public class AndPredicate<C> extends AbstractPredicate<C> {

    private final Predicate<C> left;
    private final Predicate<C> right;

    public AndPredicate(Predicate<C> left, Predicate<C> right) {
        this.left = left;
        this.right = right;
    }

    public Predicate<C> getLeft() {
        return left;
    }

    public Predicate<C> getRight() {
        return right;
    }
}
