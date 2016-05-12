package fr.xephi.authme.initialization.samples;

import fr.xephi.authme.initialization.SettingsDependent;
import fr.xephi.authme.settings.NewSetting;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

/**
 * Sample class for testing the execution of @PostConstruct methods.
 */
public class PostConstructTestClass implements SettingsDependent {

    @Inject
    @Size
    private int size;
    @Inject
    private BetaManager betaManager;
    private boolean wasPostConstructCalled = false;
    private boolean wasSecondPostConstructCalled = false;
    private boolean wasReloaded = false;

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

    @Override
    public void loadSettings(NewSetting settings) {
        if (settings != null) {
            wasReloaded = true;
        }
    }

    public boolean getWasReloaded() {
        return wasReloaded;
    }
}
