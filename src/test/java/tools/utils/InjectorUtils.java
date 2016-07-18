package tools.utils;

import ch.jalu.injector.InjectorBuilder;
import ch.jalu.injector.handlers.instantiation.DependencyDescription;
import ch.jalu.injector.handlers.instantiation.Instantiation;
import ch.jalu.injector.handlers.instantiation.InstantiationProvider;

import java.util.HashSet;
import java.util.List;
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
        Instantiation<?> instantiation = getInstantiationMethod(clazz);
        if (instantiation == null) {
            return null;
        }
        Set<Class<?>> dependencies = new HashSet<>();
        for (DependencyDescription description : instantiation.getDependencies()) {
            dependencies.add(description.getType());
        }
        return dependencies;
    }

    /**
     * Returns the instantiation method for the given class.
     *
     * @param clazz the class to process
     * @return the instantiation method for the class, or null if none applicable
     */
    public static Instantiation<?> getInstantiationMethod(Class<?> clazz) {
        List<InstantiationProvider> providers = InjectorBuilder.createInstantiationProviders();
        for (InstantiationProvider provider : providers) {
            Instantiation<?> instantiation = provider.get(clazz);
            if (instantiation != null) {
                return instantiation;
            }
        }
        return null;
    }

}
