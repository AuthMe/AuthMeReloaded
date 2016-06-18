package fr.xephi.authme.runner;

import fr.xephi.authme.ReflectionTestUtils;
import fr.xephi.authme.initialization.Injection;
import fr.xephi.authme.initialization.InjectionHelper;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkField;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.internal.runners.util.FrameworkUsageValidator;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Custom JUnit runner which adds support for {@link InjectDelayed} and {@link BeforeInjecting}.
 * This runner also initializes fields with Mockito's {@link Mock}, {@link org.mockito.Spy} and
 * {@link org.mockito.InjectMocks}.
 * <p>
 * Mockito's {@link Mock} and {@link org.mockito.InjectMocks} are initialized <i>before</i>
 * {@link org.junit.Before} methods are run. This leaves no possibility to initialize some mock
 * behavior before {@link org.mockito.InjectMocks} fields get instantiated.
 * <p>
 * The runner fills this gap by introducing {@link BeforeInjecting}. At the time these methods
 * are run Mockito's annotation will have taken effect but not {@link InjectDelayed}. Fields with
 * this annotation are initialized after {@link BeforeInjecting} methods have been run.
 * <p>
 * Additionally, after a field annotated with {@link InjectDelayed} has been initialized, its
 * {@link javax.annotation.PostConstruct} method will be invoked, if available.
 * <p>
 * Important: It is required to declare all dependencies of classes annotated with
 * {@link InjectDelayed} as {@link Mock} fields. If a dependency is missing, an exception
 * will be thrown.
 */
public class DelayedInjectionRunner extends BlockJUnit4ClassRunner {

    public DelayedInjectionRunner(Class<?> clazz) throws InitializationError {
        super(clazz);
    }

    @Override
    public Statement withBefores(FrameworkMethod method, Object target, Statement statement) {
        // Initialize all Mockito annotations
        MockitoAnnotations.initMocks(target);

        // Call chain normally: let parent handle @Before methods.
        // Note that the chain of statements will be run from the end to the start,
        // so @Before will be run AFTER our custom statements below
        statement = super.withBefores(method, target, statement);

        // Add support for @BeforeInjecting and @InjectDelayed (again, reverse order)
        statement = withDelayedInjects(target, statement);
        return withBeforeInjectings(target, statement);
    }

    @Override
    public void run(final RunNotifier notifier) {
        // add listener that validates framework usage at the end of each test
        notifier.addListener(new FrameworkUsageValidator(notifier));
        super.run(notifier);
    }

    /* Adds a Statement to the chain if @BeforeInjecting methods are present. */
    private Statement withBeforeInjectings(Object target, Statement statement) {
        List<FrameworkMethod> beforeInjectings = getTestClass().getAnnotatedMethods(BeforeInjecting.class);
        return beforeInjectings.isEmpty()
            ? statement
            : new RunBeforeInjectings(statement, beforeInjectings, target);
    }

    /*
     * Adds a Statement to the chain if @InjectDelayed methods are present.
     * If fields have been found, the injection for the type is resolved and stored with the necessary dependencies.
     */
    private Statement withDelayedInjects(Object target, Statement statement) {
        List<FrameworkField> delayedFields = getTestClass().getAnnotatedFields(InjectDelayed.class);
        if (delayedFields.isEmpty()) {
            return statement;
        }

        List<PendingInjection> pendingInjections = new ArrayList<>(delayedFields.size());
        for (FrameworkField field : delayedFields) {
            pendingInjections.add(createPendingInjection(target, field.getField()));
        }
        return new RunDelayedInjects(statement, pendingInjections, target);
    }

    /**
     * Creates a {@link PendingInjection} for the given field's type, using the target's values.
     *
     * @param target the target to get dependencies from
     * @param field the field to prepare an injection for
     * @return the resulting object
     */
    private PendingInjection createPendingInjection(Object target, Field field) {
        final Injection<?> injection = InjectionHelper.getInjection(field.getType());
        if (injection == null) {
            throw new IllegalStateException("No injection method available for field '" + field.getName() + "'");
        }
        final Object[] dependencies = fulfillDependencies(target, injection.getDependencies());
        return new PendingInjection(field, injection, dependencies);
    }

    /**
     * Returns a list of all objects for the given list of dependencies, retrieved from the given
     * target's {@link @Mock} fields.
     *
     * @param target the target to get the required dependencies from
     * @param dependencies the required dependency types
     * @return the resolved dependencies
     */
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
                    + "All dependencies of @InjectDelayed must be provided as @Mock fields");
            }
            resolvedValues[i] = o;
        }
        return resolvedValues;
    }
}
