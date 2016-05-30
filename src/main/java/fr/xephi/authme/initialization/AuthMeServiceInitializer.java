package fr.xephi.authme.initialization;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import fr.xephi.authme.settings.NewSetting;

import javax.annotation.PostConstruct;
import javax.inject.Provider;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
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
        objects.put(getClass(), this);
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
            return clazz.cast(objects.get(clazz));
        }

        // First time we come across clazz, need to instantiate it. Validate that we can do so
        validatePackage(clazz);
        validateInstantiable(clazz);

        // Add the clazz to the list of traversed classes in a new Set, so each path we take has its own Set.
        traversedClasses = new HashSet<>(traversedClasses);
        traversedClasses.add(clazz);
        T object = instantiate(clazz, traversedClasses);
        storeObject(object);
        return object;
    }

    /**
     * Performs a reload on all applicable instances which are registered.
     * Requires that the {@link NewSetting settings} instance be registered.
     * <p>
     * Note that the order in which these classes are reloaded is not guaranteed.
     */
    public void performReloadOnServices() {
        NewSetting settings = (NewSetting) objects.get(NewSetting.class);
        if (settings == null) {
            throw new IllegalStateException("Settings instance is null");
        }
        for (Object object : objects.values()) {
            if (object instanceof Reloadable) {
                ((Reloadable) object).reload();
            } else if (object instanceof SettingsDependent) {
                ((SettingsDependent) object).loadSettings(settings);
            }
        }
    }

    /**
     * Instantiates the given class by locating its @Inject elements and retrieving
     * or instantiating the required instances.
     *
     * @param clazz the class to instantiate
     * @param traversedClasses collection of classes already traversed
     * @param <T> the class' type
     * @return the instantiated object
     */
    private <T> T instantiate(Class<T> clazz, Set<Class<?>> traversedClasses) {
        Injection<T> injection = firstNotNull(
            ConstructorInjection.provide(clazz), FieldInjection.provide(clazz), InstantiationFallback.provide(clazz));
        if (injection == null) {
            throw new IllegalStateException("Did not find injection method for " + clazz + ". Make sure you have "
                + "a constructor with @Inject or fields with @Inject. Fields with @Inject require "
                + "the default constructor");
        }

        validateInjectionHasNoCircularDependencies(injection.getDependencies(), traversedClasses);
        Object[] dependencies = resolveDependencies(injection, traversedClasses);
        T object = injection.instantiateWith(dependencies);
        executePostConstructMethod(object);
        return object;
    }

    /**
     * Resolves the dependencies for the given class instantiation, i.e. returns a collection that satisfy
     * the class' dependencies by retrieving elements or instantiating them where necessary.
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
            if (annotations[i] == null) {
                values[i] = get(dependencies[i], traversedClasses);
            } else {
                Object value = objects.get(annotations[i]);
                if (value == null) {
                    throw new IllegalStateException("Value for field with @" + annotations[i].getSimpleName()
                        + " must be registered beforehand");
                }
                values[i] = value;
            }
        }
        return values;
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

    /**
     * Executes an object's method annotated with {@link PostConstruct} if present.
     * Throws an exception if there are multiple such methods, or if the method is static.
     *
     * @param object the object to execute the post construct method for
     */
    private static void executePostConstructMethod(Object object) {
        Method postConstructMethod = getAndValidatePostConstructMethod(object.getClass());
        if (postConstructMethod != null) {
            try {
                postConstructMethod.setAccessible(true);
                postConstructMethod.invoke(object);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new UnsupportedOperationException("Error executing @PostConstruct method", e);
            }
        }
    }

    private static void validateInstantiable(Class<?> clazz) {
        if (clazz.isEnum() || clazz.isInterface() || Modifier.isAbstract(clazz.getModifiers())) {
            throw new IllegalStateException("Class " + clazz.getSimpleName() + " cannot be instantiated");
        }
    }

    /**
     * Validate and locate the given class' post construct method. Returns {@code null} if none present.
     *
     * @param clazz the class to search
     * @return post construct method, or null
     */
    private static Method getAndValidatePostConstructMethod(Class<?> clazz) {
        Method postConstructMethod = null;
        for (Method method : clazz.getDeclaredMethods()) {
            if (method.isAnnotationPresent(PostConstruct.class)) {
                if (postConstructMethod != null) {
                    throw new IllegalStateException("Multiple methods with @PostConstruct on " + clazz);
                } else if (method.getParameterTypes().length > 0 || Modifier.isStatic(method.getModifiers())) {
                    throw new IllegalStateException("@PostConstruct method may not be static or have any parameters. "
                        + "Invalid method in " + clazz);
                } else if (method.getReturnType() != void.class) {
                    throw new IllegalStateException("@PostConstruct method must have return type void. "
                        + "Offending class: " + clazz);
                } else {
                    postConstructMethod = method;
                }
            }
        }
        return postConstructMethod;
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
