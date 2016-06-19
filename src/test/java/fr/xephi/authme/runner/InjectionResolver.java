package fr.xephi.authme.runner;

import fr.xephi.authme.ReflectionTestUtils;
import fr.xephi.authme.initialization.Injection;
import fr.xephi.authme.initialization.InjectionHelper;
import org.junit.runners.model.FrameworkField;
import org.junit.runners.model.TestClass;
import org.mockito.Mock;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Resolves the dependencies of an injection based on the provided {@link TestClass} and {@link #target target}.
 */
class InjectionResolver {

    private final TestClass testClass;
    private final Object target;
    private final Map<Class<?>, Object> mocksByType;

    public InjectionResolver(TestClass testClass, Object target) {
        this.testClass = testClass;
        this.target = target;
        this.mocksByType = gatherAvailableMocks();
    }

    public Object instantiate(Injection<?> injection) {
        Object[] dependencies = resolveDependencies(injection);
        Object object = injection.instantiateWith(dependencies);
        executePostConstructMethod(object);
        return object;
    }

    /**
     * Returns a list of all objects for the given list of dependencies, retrieved from the given
     * target's {@link Mock} fields.
     *
     * @param injection the injection whose dependencies to gather
     * @return the resolved dependencies
     */
    private Object[] resolveDependencies(Injection<?> injection) {
        final Class<?>[] dependencies = injection.getDependencies();
        final Class<?>[] annotations = injection.getDependencyAnnotations();
        Object[] resolvedValues = new Object[dependencies.length];
        for (int i = 0; i < dependencies.length; ++i) {
            Object dependency = (annotations[i] == null)
                ? resolveDependency(dependencies[i])
                : resolveAnnotation(annotations[i]);
            resolvedValues[i] = dependency;
        }
        return resolvedValues;
    }

    private Object resolveDependency(Class<?> clazz) {
        Object o = mocksByType.get(clazz);
        if (o == null) {
            throw new IllegalStateException("No mock found for '" + clazz + "'. "
                + "All dependencies of @InjectDelayed must be provided as @Mock fields");
        }
        return o;
    }

    private Object resolveAnnotation(Class<?> clazz) {
        Class<? extends Annotation> annotation = (Class<? extends Annotation>) clazz;
        List<FrameworkField> matches = testClass.getAnnotatedFields(annotation);
        if (matches.isEmpty()) {
            throw new IllegalStateException("No field found with @" + annotation.getSimpleName() + " in test class,"
                + "but a dependency in an @InjectDelayed field is using it");
        } else if (matches.size() > 1) {
            throw new IllegalStateException("You cannot have multiple fields with @" + annotation.getSimpleName());
        }
        return ReflectionTestUtils.getFieldValue(matches.get(0).getField(), target);
    }

    /**
     * Executes the class' PostConstruct method if available. Validates that all rules for
     * {@link javax.annotation.PostConstruct} are met.
     *
     * @param object the object whose PostConstruct method should be run, if available
     * @see InjectionHelper#getAndValidatePostConstructMethod
     */
    private static void executePostConstructMethod(Object object) {
        Method postConstructMethod = InjectionHelper.getAndValidatePostConstructMethod(object.getClass());
        if (postConstructMethod != null) {
            ReflectionTestUtils.invokeMethod(postConstructMethod, object);
        }
    }

    private Map<Class<?>, Object> gatherAvailableMocks() {
        List<FrameworkField> availableMocks = testClass.getAnnotatedFields(Mock.class);
        Map<Class<?>, Object> mocksByType = new HashMap<>();
        for (FrameworkField frameworkField : availableMocks) {
            Field field = frameworkField.getField();
            Object fieldValue = ReflectionTestUtils.getFieldValue(field, target);
            mocksByType.put(field.getType(), fieldValue);
        }
        return mocksByType;
    }
}
