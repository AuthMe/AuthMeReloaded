package fr.xephi.authme.initialization.samples;

import javax.inject.Inject;

/**
 * Sample - field injection, including custom annotations.
 */
public class FieldInjectionWithAnnotations {

    @Inject
    private BetaManager betaManager;
    @Inject
    @Size
    private int size;
    @Duration
    @Inject
    private long duration;
    @Inject
    protected ClassWithAnnotations classWithAnnotations;

    FieldInjectionWithAnnotations() {
    }

    public BetaManager getBetaManager() {
        return betaManager;
    }

    public int getSize() {
        return size;
    }

    public long getDuration() {
        return duration;
    }

    public ClassWithAnnotations getClassWithAnnotations() {
        return classWithAnnotations;
    }

}
