package fr.xephi.authme.runner;

import fr.xephi.authme.ReflectionTestUtils;
import fr.xephi.authme.initialization.Injection;
import fr.xephi.authme.initialization.InjectionHelper;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Contains all necessary information to initialize a {@link InjectDelayed} field.
 */
public class PendingInjection {

    private Field field;
    private Object[] dependencies;
    private Injection<?> injection;

    public PendingInjection(Field field, Injection<?> injection, Object[] dependencies) {
        this.field = field;
        this.injection = injection;
        this.dependencies = dependencies;
    }

    /**
     * Constructs an object with the stored injection information.
     *
     * @return the constructed object
     */
    public Object instantiate() {
        Object object = injection.instantiateWith(dependencies);
        executePostConstructMethod(object);
        return object;
    }

    /**
     * Returns the field the constructed object should be assigned to.
     *
     * @return the field in the test class
     */
    public Field getField() {
        return field;
    }

    /**
     * Clears all fields (avoids keeping a reference to all dependencies).
     */
    public void clearFields() {
        field = null;
        dependencies = null;
        injection = null;
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
}
