package fr.xephi.authme.initialization.samples;

import javax.inject.Inject;

/**
 * Sample - invalid class, since Integer parameter type is outside of the allowed package and not annotated.
 */
public class InvalidClass {

    @Inject
    public InvalidClass(AlphaService alphaService, Integer i) {
        throw new IllegalStateException("Should never be called");
    }
}
