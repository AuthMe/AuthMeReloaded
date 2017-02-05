package fr.xephi.authme.initialization.factory;

/**
 * Injectable factory that creates new instances of a certain type.
 *
 * @param <P> the parent type to which the factory is limited to
 */
public interface Factory<P> {

    /**
     * Creates an instance of the given class.
     *
     * @param clazz the class to instantiate
     * @param <C> the class type
     * @return new instance of the class
     */
    <C extends P> C newInstance(Class<C> clazz);

}
