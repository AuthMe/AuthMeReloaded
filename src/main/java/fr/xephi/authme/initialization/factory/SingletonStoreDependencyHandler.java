package fr.xephi.authme.initialization.factory;

import ch.jalu.injector.Injector;
import ch.jalu.injector.context.ResolvedInstantiationContext;
import ch.jalu.injector.handlers.dependency.DependencyHandler;
import ch.jalu.injector.handlers.instantiation.DependencyDescription;
import ch.jalu.injector.utils.ReflectionUtils;

import java.util.Collection;

/**
 * Dependency handler that builds {@link SingletonStore} objects.
 */
public class SingletonStoreDependencyHandler implements DependencyHandler {

    @Override
    public Object resolveValue(ResolvedInstantiationContext<?> context, DependencyDescription dependencyDescription) {
        if (dependencyDescription.getType() == SingletonStore.class) {
            Class<?> genericType = ReflectionUtils.getGenericType(dependencyDescription.getGenericType());
            if (genericType == null) {
                throw new IllegalStateException("Singleton store fields must have concrete generic type. "
                    + "Cannot get generic type for field in '" + context.getMappedClass() + "'");
            }

            return new SingletonStoreImpl<>(genericType, context.getInjector());
        }
        return null;
    }

    private static final class SingletonStoreImpl<P> implements SingletonStore<P> {

        private final Injector injector;
        private final Class<P> parentClass;

        SingletonStoreImpl(Class<P> parentClass, Injector injector) {
            this.parentClass = parentClass;
            this.injector = injector;
        }

        @Override
        public <C extends P> C getSingleton(Class<C> clazz) {
            if (parentClass.isAssignableFrom(clazz)) {
                return injector.getSingleton(clazz);
            }
            throw new IllegalArgumentException(clazz + " not child of " + parentClass);
        }

        @Override
        public Collection<P> retrieveAllOfType() {
            return retrieveAllOfType(parentClass);
        }

        @Override
        public <C extends P> Collection<C> retrieveAllOfType(Class<C> clazz) {
            if (parentClass.isAssignableFrom(clazz)) {
                return injector.retrieveAllOfType(clazz);
            }
            throw new IllegalArgumentException(clazz + " not child of " + parentClass);
        }
    }
}
