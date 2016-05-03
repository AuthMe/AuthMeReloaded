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

    /**
     * Creates a new instance (for instantiations in tests).
     *
     * @param providedClass .
     * @return created instance
     */
    public static AlphaService newInstance(ProvidedClass providedClass) {
        return new AlphaService(providedClass);
    }
}
