package fr.xephi.authme.runner;

import fr.xephi.authme.initialization.Injection;

import java.lang.reflect.Field;

/**
 * Contains an injection and the field it's for.
 */
class PendingInjection {

    private final Field field;
    private final Injection<?> injection;

    public PendingInjection(Field field, Injection<?> injection) {
        this.field = field;
        this.injection = injection;
    }

    /**
     * Returns the injection to perform.
     *
     * @return the injection
     */
    public Injection<?> getInjection() {
        return injection;
    }

    /**
     * Returns the field the constructed object should be assigned to.
     *
     * @return the field in the test class
     */
    public Field getField() {
        return field;
    }

}
