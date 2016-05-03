package fr.xephi.authme.initialization.samples;

import javax.inject.Inject;

/**
 * Test with an abstract class declared as dependency.
 */
public class ClassWithAbstractDependency {

    private final AlphaService alphaService;
    private final AbstractDependency abstractDependency;

    @Inject
    public ClassWithAbstractDependency(AlphaService as, AbstractDependency ad) {
        this.alphaService = as;
        this.abstractDependency = ad;
    }

    public AlphaService getAlphaService() {
        return alphaService;
    }

    public AbstractDependency getAbstractDependency() {
        return abstractDependency;
    }

    public static abstract class AbstractDependency {
    }

    public static final class ConcreteDependency extends AbstractDependency {
    }
}
