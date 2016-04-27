package fr.xephi.authme.initialization;

/**
 * Common interface for all injection methods.
 *
 * @param <T> the type of the concerned object
 */
interface Injection<T> {

    /**
     * Returns the dependencies that must be provided to instantiate the given item.
     *
     * @return list of dependencies
     * @see #instantiateWith
     */
    Class<?>[] getDependencies();

    /**
     * Returns the annotation on each dependency if available. The indices of this
     * array correspond to the ones of {@link #getDependencies()}. If no annotation
     * is available, {@code null} is stored. If multiple annotations are present, only
     * one is stored (no guarantee on which one).
     *
     * @return annotation for each dependency
     */
    Class<?>[] getDependencyAnnotations();

    /**
     * Creates a new instance with the given values as dependencies. The given values
     * must correspond to {@link #getDependencies()} in size, order and type.
     *
     * @param values the values to set for the dependencies
     * @return resulting object
     */
    T instantiateWith(Object... values);
}
