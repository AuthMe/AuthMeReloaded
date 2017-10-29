package fr.xephi.authme.datasource.sqlcolumns.predicate;

public class OrPredicate<C> extends AbstractPredicate<C> {

    private final Predicate<C> left;
    private final Predicate<C> right;

    public OrPredicate(Predicate<C> left, Predicate<C> right) {
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
