package fr.xephi.authme.initialization.samples;

import javax.inject.Inject;

/**
 * Sample class with invalid field injection (requires default constructor).
 */
public class BadFieldInjection {

    @Inject
    private AlphaService alphaService;

    public BadFieldInjection(BetaManager betaManager) {
        throw new IllegalStateException("Should never be called");
    }
}
