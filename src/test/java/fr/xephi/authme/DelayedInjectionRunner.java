package fr.xephi.authme;

import fr.xephi.authme.initialization.Injection;
import fr.xephi.authme.initialization.InjectionHelper;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkField;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.internal.runners.util.FrameworkUsageValidator;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Custom JUnit runner which adds support for {@link DelayedInject}, along with Mockito's
 * {@link Mock}, {@link org.mockito.Spy} and {@link org.mockito.InjectMocks}.
 * <p>
 * Unlike Mockito's &#64;InjectMocks, fields annotated with {@link DelayedInject} will be
 * instantiated only right before a method is invoked on them. This allows a developer to
 * define the behavior of mocks the test class depends on. With {@link org.mockito.InjectMocks},
 * fields are instantiated even before {@link org.junit.Before} methods, making it impossible
 * to define behavior before the class is instantiated.
 * <p>
 * Note that it is required to declare all dependencies of classes annotated with
 * {@link DelayedInject} as {@link Mock} fields. If a dependency is missing, an exception
 * will be thrown.
 * <p>
 * Additionally, this runner adds support for {@link javax.annotation.PostConstruct} methods,
 * both for Mockito's &#64;InjectMocks and the custom &#64;DelayedInject.
 */
public class DelayedInjectionRunner extends BlockJUnit4ClassRunner {

    public DelayedInjectionRunner(Class<?> clazz) throws InitializationError {
        super(clazz);
    }

    @Override
    public Statement withBefores(FrameworkMethod method, Object target, Statement statement) {
        // Initialize all mocks
        MockitoAnnotations.initMocks(target);

        // Add support for @DelayedInject and @PostConstruct
        runPostConstructOnInjectMocksFields(target);
        initializeDelayedMocks(target);

        // Send to parent
        return super.withBefores(method, target, statement);
    }

    @Override
    public void run(final RunNotifier notifier) {
        // add listener that validates framework usage at the end of each test
        notifier.addListener(new FrameworkUsageValidator(notifier));
        super.run(notifier);
    }

    private void runPostConstructOnInjectMocksFields(Object target) {
        List<FrameworkField> delayedFields = getTestClass().getAnnotatedFields(InjectMocks.class);
        for (FrameworkField field : delayedFields) {
            Object o = ReflectionTestUtils.getFieldValue(field.getField(), target);
            executePostConstructMethod(o);
        }
    }

    private void initializeDelayedMocks(Object target) {
        List<FrameworkField> delayedFields = getTestClass().getAnnotatedFields(DelayedInject.class);
        for (FrameworkField field : delayedFields) {
            setUpField(target, field.getField());
        }
    }

    private void setUpField(Object target, Field field) {
        final Injection<?> injection = InjectionHelper.getInjection(field.getType());
        if (injection == null) {
            throw new IllegalStateException("No injection method available for field '" + field.getName() + "'");
        }
        final Object[] dependencies = fulfillDependencies(target, injection.getDependencies());

        Object delayedInjectionMock = Mockito.mock(field.getType(),
            new DelayedInstantiatingAnswer(injection, dependencies));
        ReflectionTestUtils.setField(field, target, delayedInjectionMock);
    }

    private Object[] fulfillDependencies(Object target, Class<?>[] dependencies) {
        List<FrameworkField> availableMocks = getTestClass().getAnnotatedFields(Mock.class);
        Map<Class<?>, Object> mocksByType = new HashMap<>();
        for (FrameworkField frameworkField : availableMocks) {
            Field field = frameworkField.getField();
            Object fieldValue = ReflectionTestUtils.getFieldValue(field, target);
            mocksByType.put(field.getType(), fieldValue);
        }

        Object[] resolvedValues = new Object[dependencies.length];
        for (int i = 0; i < dependencies.length; ++i) {
            Object o = mocksByType.get(dependencies[i]);
            if (o == null) {
                throw new IllegalStateException("No mock found for '" + dependencies[i] + "'. "
                    + "All dependencies of @DelayedInject must be provided as @Mock fields");
            }
            resolvedValues[i] = o;
        }
        return resolvedValues;
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

    private static final class DelayedInstantiatingAnswer implements Answer<Object> {

        private final Injection<?> injection;
        private final Object[] dependencies;
        private Object realObject;

        public DelayedInstantiatingAnswer(Injection<?> injection, Object... dependencies) {
            this.injection = injection;
            this.dependencies = dependencies;
        }

        @Override
        public Object answer(InvocationOnMock invocation) throws Throwable {
            if (realObject == null) {
                Object realObject = injection.instantiateWith(dependencies);
                executePostConstructMethod(realObject);
                this.realObject = realObject;
            }

            Method method = invocation.getMethod();
            return ReflectionTestUtils.invokeMethod(method, realObject, invocation.getArguments());
        }
    }

}
