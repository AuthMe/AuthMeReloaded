package tools.utils;

import ch.jalu.injector.handlers.instantiation.DependencyDescription;
import ch.jalu.injector.handlers.instantiation.Instantiation;
import ch.jalu.injector.handlers.instantiation.StandardInjectionProvider;

import java.util.HashSet;
import java.util.Set;

/**
 * Utility for operations with the injector.
 */
public final class InjectorUtils {

    private InjectorUtils() {
    }

    /**
     * Returns a class' dependencies as determined by the injector.
     *
     * @param clazz the class to process
     * @return the class' dependencies, or null if no instantiation method found
     */
    public static Set<Class<?>> getDependencies(Class<?> clazz) {
        Instantiation<?> instantiation = new StandardInjectionProvider().safeGet(clazz);
        if (instantiation == null) {
            return null;
        }
        Set<Class<?>> dependencies = new HashSet<>();
        for (DependencyDescription description : instantiation.getDependencies()) {
            dependencies.add(description.getType());
        }
        return dependencies;
    }

}
