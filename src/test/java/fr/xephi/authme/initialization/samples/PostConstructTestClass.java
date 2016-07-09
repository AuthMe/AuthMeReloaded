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
    private boolean wasReloaded = false;

    @PostConstruct
    public void postConstructMethod() {
        wasPostConstructCalled = true;
    }

    public boolean wasPostConstructCalled() {
        return wasPostConstructCalled;
    }

    public BetaManager getBetaManager() {
        return betaManager;
    }

    @Override
    public void reload(NewSetting settings) {
        if (settings != null) {
            wasReloaded = true;
        }
    }

    public boolean getWasReloaded() {
        return wasReloaded;
    }
}
