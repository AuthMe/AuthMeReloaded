package fr.xephi.authme.initialization.factory;

import java.util.Collection;

/**
 * Injectable object to retrieve and create singletons of a common parent.
 *
 * @param <P> the parent class to which this store is limited to
 */
public interface SingletonStore<P> {

    /**
     * Returns the singleton of the given type, creating it if it hasn't been yet created.
     *
     * @param clazz the class to get the singleton for
     * @param <C> type of the singleton
     * @return the singleton of type {@code C}
     */
    <C extends P> C getSingleton(Class<C> clazz);

    /**
     * Returns all existing singletons of this store's type.
     *
     * @return all registered singletons of type {@code P}
     */
    Collection<P> retrieveAllOfType();

    /**
     * Returns all existing singletons of the given type.
     *
     * @param clazz the type to get singletons for
     * @param <C> class type
     * @return all registered singletons of type {@code C}
     */
    <C extends P> Collection<C> retrieveAllOfType(Class<C> clazz);

}
