package fr.xephi.authme.initialization.samples;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

/**
 * Sample class - tests various situations for the instantiation fallback.
 */
public abstract class InstantiationFallbackClasses {

    public static final class FallbackClass {
        // No @Inject annotations, public no-args constructor
    }

    public static final class HasFallbackDependency {
        @Inject
        private FallbackClass fallbackClass;

        @Inject
        private GammaService gammaService;

        public GammaService getGammaService() {
            return gammaService;
        }

        public FallbackClass getFallbackDependency() {
            return fallbackClass;
        }
    }

    public static final class InvalidFallbackClass {
        private InvalidFallbackClass() {
            // no-args constructor must be public for fallback instantiation
        }
    }

    public static final class InvalidInjectOnMethodClass {
        // We don't support method injection but this should still be detected and an exception returned
        // Only use instantiation fallback if we're sure there isn't some sort of misconfiguration
        @Inject
        public void setGammaService(GammaService gammaService) {
            // --
        }
    }

    // Class with @PostConstruct method should never be instantiated by instantiation fallback
    public static final class ClassWithPostConstruct {
        @PostConstruct
        public void postConstructMethod() {
            // --
        }
    }

}
