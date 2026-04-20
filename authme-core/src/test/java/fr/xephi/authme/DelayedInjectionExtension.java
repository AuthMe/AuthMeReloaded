package fr.xephi.authme;

import ch.jalu.injector.Injector;
import ch.jalu.injector.InjectorBuilder;
import ch.jalu.injector.context.ResolutionContext;
import ch.jalu.injector.exceptions.InjectorException;
import ch.jalu.injector.handlers.Handler;
import ch.jalu.injector.handlers.instantiation.Resolution;
import ch.jalu.injector.handlers.instantiation.SimpleResolution;
import ch.jalu.injector.handlers.postconstruct.PostConstructMethodInvoker;
import ch.jalu.injector.testing.BeforeInjecting;
import ch.jalu.injector.testing.InjectDelayed;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import javax.inject.Inject;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * JUnit 5 equivalent of Jalu Injector's DelayedInjectionRunner.
 */
public class DelayedInjectionExtension implements BeforeEachCallback, AfterEachCallback {

    private static final ExtensionContext.Namespace NAMESPACE =
        ExtensionContext.Namespace.create(DelayedInjectionExtension.class);

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        Object testInstance = context.getRequiredTestInstance();
        AutoCloseable mocks = MockitoAnnotations.openMocks(testInstance);
        context.getStore(NAMESPACE).put(testInstance, mocks);

        invokeBeforeInjectingMethods(testInstance);
        injectDelayedFields(testInstance);
    }

    @Override
    public void afterEach(ExtensionContext context) throws Exception {
        Object testInstance = context.getRequiredTestInstance();
        AutoCloseable mocks = context.getStore(NAMESPACE).remove(testInstance, AutoCloseable.class);
        if (mocks != null) {
            mocks.close();
        }
    }

    private static void invokeBeforeInjectingMethods(Object testInstance) throws ReflectiveOperationException {
        for (Method method : getAnnotatedMethods(testInstance.getClass(), BeforeInjecting.class)) {
            method.setAccessible(true);
            method.invoke(testInstance);
        }
    }

    private static void injectDelayedFields(Object testInstance) throws ReflectiveOperationException {
        List<Field> delayedFields = getAnnotatedFields(testInstance.getClass(), InjectDelayed.class);
        if (delayedFields.isEmpty()) {
            return;
        }

        Injector injector = new InjectorBuilder()
            .addHandlers(
                new TestAnnotationResolver(testInstance.getClass(), testInstance),
                new MockDependencyHandler(testInstance.getClass(), testInstance, delayedFields),
                new PostConstructMethodInvoker())
            .addHandlers(InjectorBuilder.createInstantiationProviders(""))
            .create();

        for (Field field : delayedFields) {
            field.setAccessible(true);
            if (field.get(testInstance) != null) {
                throw new IllegalStateException("Field with @InjectDelayed must be null on startup. Field '"
                    + field.getName() + "' is not null");
            }
            field.set(testInstance, injector.getSingleton(field.getType()));
        }
    }

    private static List<Method> getAnnotatedMethods(Class<?> type, Class<? extends Annotation> annotationType) {
        List<Method> methods = new ArrayList<>();
        Class<?> current = type;
        while (current != null && current != Object.class) {
            for (Method method : current.getDeclaredMethods()) {
                if (method.isAnnotationPresent(annotationType)) {
                    methods.add(0, method);
                }
            }
            current = current.getSuperclass();
        }
        return methods;
    }

    private static List<Field> getAnnotatedFields(Class<?> type, Class<? extends Annotation> annotationType) {
        List<Field> fields = new ArrayList<>();
        Class<?> current = type;
        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                if (field.isAnnotationPresent(annotationType)) {
                    fields.add(field);
                }
            }
            current = current.getSuperclass();
        }
        return fields;
    }

    private static final class MockDependencyHandler implements Handler {

        private final Class<?> testClass;
        private final Object target;
        private final Set<Class<?>> fieldsToInject;

        private boolean areMocksRegistered;

        private MockDependencyHandler(Class<?> testClass, Object target, List<Field> delayedFields) {
            this.testClass = testClass;
            this.target = target;
            this.fieldsToInject = new HashSet<>();
            for (Field field : delayedFields) {
                fieldsToInject.add(field.getType());
            }
        }

        @Override
        public Resolution<?> resolve(ResolutionContext context) throws IllegalAccessException {
            Injector injector = context.getInjector();
            if (!areMocksRegistered) {
                registerAllMocks(injector);
                areMocksRegistered = true;
            }

            Class<?> requestedType = context.getIdentifier().getTypeAsClass();
            Object existingObject = injector.getIfAvailable(requestedType);
            if (existingObject != null) {
                return new SimpleResolution<>(existingObject);
            }
            if (fieldsToInject.contains(requestedType)) {
                return null;
            }
            throw new InjectorException("No mock found for '" + requestedType
                + "'. All dependencies of @InjectDelayed must be provided as @Mock or @InjectDelayed fields");
        }

        private void registerAllMocks(Injector injector) throws IllegalAccessException {
            for (Field field : getAnnotatedFields(testClass, Mock.class)) {
                field.setAccessible(true);
                registerMock(injector, field, field.get(target));
            }
        }

        @SuppressWarnings("unchecked")
        private static <T> void registerMock(Injector injector, Field field, Object mock) {
            injector.register((Class<? super T>) field.getType(), (T) mock);
        }
    }

    private static final class TestAnnotationResolver implements Handler {

        private static final Set<Class<? extends Annotation>> IGNORED_ANNOTATIONS = Set.of(
            Inject.class, org.mockito.InjectMocks.class, Mock.class, Spy.class, InjectDelayed.class);

        private final Class<?> testClass;
        private final Object target;

        private TestAnnotationResolver(Class<?> testClass, Object target) {
            this.testClass = testClass;
            this.target = target;
        }

        @Override
        public Resolution<?> resolve(ResolutionContext context) throws IllegalAccessException {
            Class<?> requestedType = context.getIdentifier().getTypeAsClass();
            for (Annotation annotation : context.getIdentifier().getAnnotations()) {
                Object resolvedValue = resolveByAnnotation(annotation.annotationType(), requestedType);
                if (resolvedValue != null) {
                    return new SimpleResolution<>(resolvedValue);
                }
            }
            return null;
        }

        private Object resolveByAnnotation(Class<? extends Annotation> annotationType, Class<?> requestedType)
            throws IllegalAccessException {
            if (IGNORED_ANNOTATIONS.contains(annotationType)) {
                return null;
            }

            for (Field field : getAnnotatedFields(testClass, annotationType)) {
                if (requestedType.isAssignableFrom(field.getType())) {
                    field.setAccessible(true);
                    return field.get(target);
                }
            }
            return null;
        }
    }
}
