package fr.xephi.authme.initialization.factory;

import ch.jalu.injector.Injector;
import ch.jalu.injector.context.ResolvedInstantiationContext;
import ch.jalu.injector.handlers.dependency.DependencyHandler;
import ch.jalu.injector.handlers.instantiation.DependencyDescription;
import ch.jalu.injector.utils.ReflectionUtils;

/**
 * Dependency handler that builds {@link Factory} objects.
 */
public class FactoryDependencyHandler implements DependencyHandler {

    @Override
    public Object resolveValue(ResolvedInstantiationContext<?> context, DependencyDescription dependencyDescription) {
        if (dependencyDescription.getType() == Factory.class) {
            Class<?> genericType = ReflectionUtils.getGenericType(dependencyDescription.getGenericType());
            if (genericType == null) {
                throw new IllegalStateException("Factory fields must have concrete generic type. " +
                    "Cannot get generic type for field in '" + context.getMappedClass() + "'");
            }

            return new FactoryImpl<>(genericType, context.getInjector());
        }
        return null;
    }

    private static final class FactoryImpl<P> implements Factory<P> {

        private final Injector injector;
        private final Class<P> parentClass;

        FactoryImpl(Class<P> parentClass, Injector injector) {
            this.parentClass = parentClass;
            this.injector = injector;
        }

        @Override
        public <C extends P> C newInstance(Class<C> clazz) {
            if (parentClass.isAssignableFrom(clazz)) {
                return injector.newInstance(clazz);
            }
            throw new IllegalArgumentException(clazz + " not child of " + parentClass);
        }
    }
}
