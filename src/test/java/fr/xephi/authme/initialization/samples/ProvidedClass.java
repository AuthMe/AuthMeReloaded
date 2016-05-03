package fr.xephi.authme.initialization.samples;

import javax.inject.Inject;

/**
 * Sample - class that is always provided to the initializer beforehand.
 */
public class ProvidedClass {

    @Inject
    public ProvidedClass() {
        throw new IllegalStateException("Should never be called (tests always provide this class)");
    }

    public ProvidedClass(String manualConstructor) {
    }

}
