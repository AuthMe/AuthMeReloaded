package fr.xephi.authme.initialization.samples;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

/**
 * Class with invalid @PostConstruct method.
 */
public abstract class InvalidPostConstruct {

    public static final class WithParams {
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
        @PostConstruct
        public void throwingPostConstruct() {
            throw new IllegalStateException("Exception in post construct");
        }
    }

    public static final class NotVoidReturnType {
        @PostConstruct
        public int returnsInt() {
            return 42;
        }
    }

    public static final class MultiplePostConstructs {
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
