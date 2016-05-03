package fr.xephi.authme.initialization.samples;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

/**
 * Sample class for testing the execution of @PostConstruct methods.
 */
public class PostConstructTestClass {

    @Inject
    @Size
    private int size;
    @Inject
    private BetaManager betaManager;
    private boolean wasPostConstructCalled = false;
    private boolean wasSecondPostConstructCalled = false;

    @PostConstruct
    protected void setFieldToTrue() {
        wasPostConstructCalled = true;
    }

    @PostConstruct
    public int otherPostConstructMethod() {
        wasSecondPostConstructCalled = true;
        return 42;
    }

    public boolean werePostConstructsCalled() {
        return wasPostConstructCalled && wasSecondPostConstructCalled;
    }

    public BetaManager getBetaManager() {
        return betaManager;
    }
}
