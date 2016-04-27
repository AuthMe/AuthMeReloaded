package fr.xephi.authme.initialization;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;

import javax.annotation.PostConstruct;
import javax.inject.Provider;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Dependency injector of AuthMe: initializes and injects services and tasks.
 * <p>
 * Only constructor and field injection are supported, indicated with the JSR330
 * {@link javax.inject.Inject @Inject} annotation.
 * <p>
 * {@link PostConstruct @PostConstruct} methods are recognized and invoked upon
 * instantiation. Note that the parent classes are <i>not</i> scanned for such methods.
 */
public class AuthMeServiceInitializer {

    private final Set<String> ALLOWED_PACKAGES;
    private final Map<Class<?>, Object> objects;

    /**
     * Constructor.
     *
     * @param allowedPackages list of allowed packages. Only classes whose package
     *        starts with any of the given entries will be instantiated
     */
    public AuthMeServiceInitializer(String... allowedPackages) {
        ALLOWED_PACKAGES = ImmutableSet.copyOf(allowedPackages);
        objects = new HashMap<>();
    }

    /**
     * Retrieves or instantiates an object of the given type.
     *
     * @param clazz the class to retrieve the value for
     * @param <T> the class' type
     * @return object of the class' type
     */
    public <T> T get(Class<T> clazz) {
        return get(clazz, new HashSet<Class<?>>());
    }

    /**
     * Registers an instantiation by its type.
     *
     * @param object the object to register
     * @throws IllegalStateException if an object of the same type has already been registered
     */
    public void register(Object object) {
        if (object instanceof Type) {
            throw new IllegalStateException("You tried to register a Type object: '" + object
                + "'. This likely indicates an error. Please use register(Class<T>, T) if really desired.");
        }
        storeObject(object);
    }

    /**
     * Register an object with a custom class (supertype). Use this for example to specify a
     * concrete implementation of an interface or an abstract class.
     *
     * @param clazz the class to register the object for
     * @param object the object
     * @param <T> the class' type
     */
    public <T> void register(Class<? super T> clazz, T object) {
        if (objects.containsKey(clazz)) {
            throw new IllegalStateException("There is already an object present for " + clazz);
        }
        Preconditions.checkNotNull(object);
        objects.put(clazz, object);
    }

    /**
     * Associate an annotation with a value.
     *
     * @param annotation the annotation
     * @param value the value
     */
    public void provide(Class<? extends Annotation> annotation, Object value) {
        if (objects.containsKey(annotation)) {
            throw new IllegalStateException("Annotation @" + annotation.getClass().getSimpleName()
                + " already registered");
        }
        Preconditions.checkNotNull(value);
        objects.put(annotation, value);
    }

    /**
     * Creates a new instance of the given class and does <i>not</i> keep track of it afterwards,
     * unlike {@link #get(Class)} (singleton scope).
     *
     * @param clazz the class to instantiate
     * @param <T> the class' type
     * @return new instance of class T
     */
    public <T> T newInstance(Class<T> clazz) {
        return instantiate(clazz, new HashSet<Class<?>>());
    }

    /**
     * Returns an instance of the given class or the value associated with an annotation,
     * by retrieving it or by instantiating it if not yet present.
     *
     * @param clazz the class to retrieve a value for
     * @param traversedClasses the list of traversed classes
     * @param <T> the class' type
     * @return instance or associated value (for annotations)
     */
    private <T> T get(Class<T> clazz, Set<Class<?>> traversedClasses) {
        if (Annotation.class.isAssignableFrom(clazz)) {
            throw new UnsupportedOperationException("Cannot retrieve annotated elements in this way!");
        } else if (objects.containsKey(clazz)) {
            return getObject(clazz);
        }

        // First time we come across clazz, need to instantiate it. Add the clazz to the list of traversed
        // classes in a new list, so each path we need to take has its own Set.
        validatePackage(clazz);
        validateInstantiable(clazz);

        traversedClasses = new HashSet<>(traversedClasses);
        traversedClasses.add(clazz);
        return instantiate(clazz, traversedClasses);
    }

    /**
     * Instantiates the given class by locating an @Inject constructor and retrieving
     * or instantiating its parameters.
     *
     * @param clazz the class to instantiate
     * @param traversedClasses collection of classes already traversed
     * @param <T> the class' type
     * @return the instantiated object
     */
    private <T> T instantiate(Class<T> clazz, Set<Class<?>> traversedClasses) {
        Injection<T> injection = firstNotNull(ConstructorInjection.provide(clazz), FieldInjection.provide(clazz));
        if (injection == null) {
            throw new IllegalStateException("Did not find injection method for " + clazz + ". Make sure you have "
                + "a constructor with @Inject or fields with @Inject. Fields with @Inject require "
                + "the default constructor");
        }

        validateInjectionHasNoCircularDependencies(injection.getDependencies(), traversedClasses);
        Object[] dependencies = resolveDependencies(injection, traversedClasses);
        T object = injection.instantiateWith(dependencies);
        storeObject(object);
        executePostConstructMethods(object);
        return object;
    }

    /**
     * Resolves the dependencies for the given constructor, i.e. returns a collection that satisfy
     * the constructor's parameter types by retrieving elements or instantiating them where necessary.
     *
     * @param injection the injection parameters
     * @param traversedClasses collection of traversed classes
     * @return array with the parameters to use in the constructor
     */
    private Object[] resolveDependencies(Injection<?> injection, Set<Class<?>> traversedClasses) {
        Class<?>[] dependencies = injection.getDependencies();
        Class<?>[] annotations = injection.getDependencyAnnotations();
        Object[] values = new Object[dependencies.length];
        for (int i = 0; i < dependencies.length; ++i) {
            if (annotations[i] != null) {
                Object value = objects.get(annotations[i]);
                if (value == null) {
                    throw new IllegalStateException("Value for field with @" + annotations[i].getSimpleName()
                        + " must be registered beforehand");
                }
                values[i] = value;
            } else {
                values[i] = get(dependencies[i], traversedClasses);
            }
        }
        return values;
    }


    /**
     * Internal method to retrieve an object from the objects map for <b>non-annotation classes</b>.
     * In such cases, the type of the entry always corresponds to the key, i.e. the entry of key
     * {@code Class<T>} is guaranteed to be of type {@code T}.
     * <p>
     * To retrieve values identified with an annotation, use {@code objects.get(clazz)} directly.
     * We do not know or control the type of the value of keys of annotation classes.
     *
     * @param clazz the class to retrieve the implementation of
     * @param <T> the type
     * @return the implementation
     */
    private <T> T getObject(Class<T> clazz) {
        Object o = objects.get(clazz);
        if (o == null) {
            throw new NullPointerException("No instance of " + clazz + " available");
        }
        return clazz.cast(o);
    }

    /**
     * Stores the given object with its class as key. Throws an exception if the key already has
     * a value associated to it.
     *
     * @param object the object to store
     */
    private void storeObject(Object object) {
        if (objects.containsKey(object.getClass())) {
            throw new IllegalStateException("There is already an object present for " + object.getClass());
        }
        Preconditions.checkNotNull(object);
        objects.put(object.getClass(), object);
    }

    /**
     * Validates that none of the dependencies' types are present in the given collection
     * of traversed classes. This prevents circular dependencies.
     *
     * @param dependencies the dependencies of the class
     * @param traversedClasses the collection of traversed classes
     */
    private static void validateInjectionHasNoCircularDependencies(Class<?>[] dependencies,
                                                                   Set<Class<?>> traversedClasses) {
        for (Class<?> clazz : dependencies) {
            if (traversedClasses.contains(clazz)) {
                throw new IllegalStateException("Found cyclic dependency - already traversed '" + clazz
                    + "' (full traversal list: " + traversedClasses + ")");
            }
        }
    }

    /**
     * Validates the package of a parameter type to ensure that it is part of the allowed packages.
     * This ensures that we don't try to instantiate things that are beyond our reach in case some
     * external parameter type has not been registered.
     *
     * @param clazz the class to validate
     */
    private void validatePackage(Class<?> clazz) {
        if (clazz.getPackage() == null) {
            throw new IllegalStateException("Primitive types must be provided explicitly (or use an annotation).");
        }
        String packageName = clazz.getPackage().getName();
        for (String allowedPackage : ALLOWED_PACKAGES) {
            if (packageName.startsWith(allowedPackage)) {
                return;
            }
        }
        throw new IllegalStateException("Class " + clazz + " with package " + packageName + " is outside of the "
            + "allowed packages. It must be provided explicitly or the package must be passed to the constructor.");
    }

    private static void executePostConstructMethods(Object object) {
        for (Method method : object.getClass().getDeclaredMethods()) {
            if (method.isAnnotationPresent(PostConstruct.class)) {
                if (method.getParameterCount() == 0) {
                    try {
                        method.setAccessible(true);
                        method.invoke(object);
                    } catch (IllegalAccessException | InvocationTargetException e) {
                        throw new UnsupportedOperationException(e);
                    }
                } else {
                    throw new IllegalStateException("@PostConstruct methods must have an empty parameter list. " +
                        "Found parameters in " + method + " belonging to " + object.getClass());
                }
            }
        }
    }

    private static void validateInstantiable(Class<?> clazz) {
        if (clazz.isEnum() || clazz.isInterface() || Modifier.isAbstract(clazz.getModifiers())) {
            throw new IllegalStateException("Class " + clazz.getSimpleName() + " cannot be instantiated");
        }
    }

    @SafeVarargs
    private static <T> Injection<T> firstNotNull(Provider<? extends Injection<T>>... providers) {
        for (Provider<? extends Injection<T>> provider : providers) {
            Injection<T> object = provider.get();
            if (object != null) {
                return object;
            }
        }
        return null;
    }
}
