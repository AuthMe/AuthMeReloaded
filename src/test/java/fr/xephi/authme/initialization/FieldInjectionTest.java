package fr.xephi.authme.initialization;

import fr.xephi.authme.initialization.samples.AlphaService;
import fr.xephi.authme.initialization.samples.BetaManager;
import fr.xephi.authme.initialization.samples.ClassWithAnnotations;
import fr.xephi.authme.initialization.samples.Duration;
import fr.xephi.authme.initialization.samples.FieldInjectionWithAnnotations;
import fr.xephi.authme.initialization.samples.GammaService;
import fr.xephi.authme.initialization.samples.ProvidedClass;
import fr.xephi.authme.initialization.samples.Size;
import org.junit.Test;

import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

/**
 * Test for {@link FieldInjection}.
 */
public class FieldInjectionTest {

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

}
