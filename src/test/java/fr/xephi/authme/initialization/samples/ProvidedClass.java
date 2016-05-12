package fr.xephi.authme.initialization.samples;

import fr.xephi.authme.initialization.Reloadable;

import javax.inject.Inject;

/**
 * Sample - class that is always provided to the initializer beforehand.
 */
public class ProvidedClass implements Reloadable {

    private boolean wasReloaded = false;

    @Inject
    public ProvidedClass() {
        throw new IllegalStateException("Should never be called (tests always provide this class)");
    }

    public ProvidedClass(String manualConstructor) {
    }

    @Override
    public void reload() {
        wasReloaded = true;
    }

    public boolean getWasReloaded() {
        return wasReloaded;
    }
}
