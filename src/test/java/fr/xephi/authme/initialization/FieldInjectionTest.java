package fr.xephi.authme.initialization;

import fr.xephi.authme.initialization.samples.AlphaService;
import fr.xephi.authme.initialization.samples.BadFieldInjection;
import fr.xephi.authme.initialization.samples.BetaManager;
import fr.xephi.authme.initialization.samples.ClassWithAnnotations;
import fr.xephi.authme.initialization.samples.Duration;
import fr.xephi.authme.initialization.samples.FieldInjectionWithAnnotations;
import fr.xephi.authme.initialization.samples.GammaService;
import fr.xephi.authme.initialization.samples.InvalidStaticFieldInjection;
import fr.xephi.authme.initialization.samples.ProvidedClass;
import fr.xephi.authme.initialization.samples.Size;
import org.junit.Test;

import javax.inject.Inject;

import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

/**
 * Test for {@link FieldInjection}.
 */
public class FieldInjectionTest {

    @SuppressWarnings("unchecked")
    @Test
    public void shouldReturnDependencies() {
        // given
        FieldInjection<FieldInjectionWithAnnotations> injection =
            FieldInjection.provide(FieldInjectionWithAnnotations.class).get();

        // when
        Class<?>[] dependencies = injection.getDependencies();
        Class<?>[] annotations = injection.getDependencyAnnotations();

        // then
        assertThat(dependencies, arrayContaining(BetaManager.class, int.class, long.class, ClassWithAnnotations.class));
        assertThat(annotations, arrayContaining((Class<?>) null, Size.class, Duration.class, null));
    }

    @Test
    public void shouldInstantiateClass() {
        // given
        FieldInjection<BetaManager> injection = FieldInjection.provide(BetaManager.class).get();
        ProvidedClass providedClass = new ProvidedClass("");
        AlphaService alphaService = AlphaService.newInstance(providedClass);
        GammaService gammaService = new GammaService(alphaService);

        // when
        BetaManager betaManager = injection.instantiateWith(providedClass, gammaService, alphaService);

        // then
        assertThat(betaManager, not(nullValue()));
        assertThat(betaManager.getDependencies(), arrayContaining(providedClass, gammaService, alphaService));
    }

    @Test
    public void shouldProvideNullForImpossibleFieldInjection() {
        // given / when
        FieldInjection<BadFieldInjection> injection = FieldInjection.provide(BadFieldInjection.class).get();

        // then
        assertThat(injection, nullValue());
    }

    @Test(expected = RuntimeException.class)
    public void shouldForwardExceptionDuringInstantiation() {
        // given
        FieldInjection<ThrowingConstructor> injection = FieldInjection.provide(ThrowingConstructor.class).get();

        // when / when
        injection.instantiateWith(new ProvidedClass(""));
    }

    @Test(expected = RuntimeException.class)
    public void shouldThrowForInvalidFieldValue() {
        // given
        ProvidedClass providedClass = new ProvidedClass("");
        AlphaService alphaService = AlphaService.newInstance(providedClass);
        GammaService gammaService = new GammaService(alphaService);
        FieldInjection<BetaManager> injection = FieldInjection.provide(BetaManager.class).get();

        // when / then
        // Correct order is provided, gamma, alpha
        injection.instantiateWith(providedClass, alphaService, gammaService);
    }

    @Test(expected = NullPointerException.class)
    public void shouldThrowForNullValue() {
        // given
        ProvidedClass providedClass = new ProvidedClass("");
        AlphaService alphaService = AlphaService.newInstance(providedClass);
        FieldInjection<BetaManager> injection = FieldInjection.provide(BetaManager.class).get();

        // when / then
        // Correct order is provided, gamma, alpha
        injection.instantiateWith(providedClass, null, alphaService);
    }

    @Test(expected = RuntimeException.class)
    public void shouldThrowForStaticFieldInjection() {
        // given / when / then
        FieldInjection.provide(InvalidStaticFieldInjection.class).get();
    }

    private static class ThrowingConstructor {
        @SuppressWarnings("unused")
        @Inject
        private ProvidedClass providedClass;

        @SuppressWarnings("unused")
        public ThrowingConstructor() {
            throw new UnsupportedOperationException("Exception in constructor");
        }
    }
}
