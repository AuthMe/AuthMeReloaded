package tools.utils;

import ch.jalu.injector.context.ObjectIdentifier;
import ch.jalu.injector.handlers.instantiation.Resolution;
import ch.jalu.injector.handlers.instantiation.StandardInjectionProvider;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
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
        Resolution<?> instantiation = new StandardInjectionProvider().safeGet(clazz);
        if (instantiation == null) {
            return null;
        }
        Set<Class<?>> dependencies = new HashSet<>();
        for (ObjectIdentifier description : instantiation.getDependencies()) {
            dependencies.add(convertToClass(description.getType()));
        }
        return dependencies;
    }

    /**
     * Returns the given type as a {@link Class}.
     *
     * @param type the type to convert
     * @return class corresponding to the provided type
     */
    public static Class<?> convertToClass(Type type) {
        if (type instanceof Class<?>) {
            return (Class<?>) type;
        } else if (type instanceof ParameterizedType) {
            Type rawType = ((ParameterizedType) type).getRawType();
            if (rawType instanceof Class<?>) {
                return (Class<?>) rawType;
            } else {
                throw new IllegalStateException("Got raw type '" + rawType + "' of type '"
                    + rawType.getClass() + "' for genericType '" + type + "'");
            }
        }
        Class<?> typeClass = type == null ? null : type.getClass();
        throw new IllegalStateException("Unknown type implementation '" + typeClass + "' for '" + type + "'");
    }

}
