package fr.xephi.authme.initialization.samples;

import javax.inject.Inject;

/**
 * Classes with circular dependencies.
 */
public abstract class CircularClasses {

    public static final class Circular1 {
        @Inject
        public Circular1(AlphaService alphaService, Circular3 circular3) {
            // --
        }
    }

    public static final class Circular2 {
        @Inject
        public Circular2(Circular1 circular1) {
            // --
        }
    }

    public static final class Circular3 {
        @Inject
        public Circular3(Circular2 circular2, BetaManager betaManager) {
            // --
        }
    }
}
