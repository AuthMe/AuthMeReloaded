package fr.xephi.authme.initialization.samples;

import javax.inject.Inject;

/**
 * Sample - class with dependency to ProvidedClass.
 */
public class AlphaService {

    private ProvidedClass providedClass;

    @Inject
    AlphaService(ProvidedClass providedClass) {
        this.providedClass = providedClass;
    }

    public ProvidedClass getProvidedClass() {
        return providedClass;
    }
}
