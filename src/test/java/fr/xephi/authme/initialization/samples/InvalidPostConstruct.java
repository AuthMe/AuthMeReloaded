package fr.xephi.authme.initialization.samples;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

/**
 * Class with invalid @PostConstruct method.
 */
public abstract class InvalidPostConstruct {

    public static final class WithParams {
        @Inject
        private AlphaService alphaService;
        @Inject
        private ProvidedClass providedClass;

        WithParams() { }

        @PostConstruct
        public void invalidPostConstr(BetaManager betaManager) {
        }
    }

    public static final class Static {
        @Inject
        Static(BetaManager betaManager) {
            // --
        }

        @PostConstruct
        public static void invalidMethod() {
            // --
        }
    }

    public static final class ThrowsException {
        @Inject
        private ProvidedClass providedClass;

        @PostConstruct
        public void throwingPostConstruct() {
            throw new IllegalStateException("Exception in post construct");
        }
    }

    public static final class NotVoidReturnType {
        @Inject
        private ProvidedClass providedClass;

        @PostConstruct
        public int returnsInt() {
            return 42;
        }
    }

    public static final class MultiplePostConstructs {
        @Inject
        private ProvidedClass providedClass;

        @PostConstruct
        public void postConstruct1() {
            // --
        }
        @PostConstruct
        public void postConstruct2() {
            // --
        }
    }
}
