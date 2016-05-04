package fr.xephi.authme.initialization.samples;

import javax.inject.Inject;

/**
 * Sample class - attempted field injection on a static member.
 */
public class InvalidStaticFieldInjection {

    @SuppressWarnings("unused")
    @Inject
    private ProvidedClass providedClass;
    @Inject
    protected static AlphaService alphaService;

    InvalidStaticFieldInjection() { }

}
