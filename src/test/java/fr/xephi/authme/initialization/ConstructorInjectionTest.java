package fr.xephi.authme.initialization;

import fr.xephi.authme.initialization.samples.AlphaService;
import fr.xephi.authme.initialization.samples.BetaManager;
import fr.xephi.authme.initialization.samples.ClassWithAnnotations;
import fr.xephi.authme.initialization.samples.Duration;
import fr.xephi.authme.initialization.samples.GammaService;
import fr.xephi.authme.initialization.samples.InvalidClass;
import fr.xephi.authme.initialization.samples.ProvidedClass;
import fr.xephi.authme.initialization.samples.Size;
import org.junit.Test;

import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

/**
 * Test for {@link ConstructorInjection}.
 */
public class ConstructorInjectionTest {

    @SuppressWarnings("unchecked")
    @Test
    public void shouldReturnDependencies() {
        // given
        Injection<ClassWithAnnotations> injection = ConstructorInjection.provide(ClassWithAnnotations.class).get();

        // when
        Class<?>[] dependencies = injection.getDependencies();
        Class<?>[] annotations = injection.getDependencyAnnotations();

        // then
        assertThat(dependencies, arrayContaining(int.class, GammaService.class, long.class));
        assertThat(annotations, arrayContaining((Class<?>) Size.class, null, Duration.class));
    }

    @Test
    public void shouldInstantiate() {
        // given
        GammaService gammaService = new GammaService(
            AlphaService.newInstance(new ProvidedClass("")));
        Injection<ClassWithAnnotations> injection = ConstructorInjection.provide(ClassWithAnnotations.class).get();

        // when
        ClassWithAnnotations instance = injection.instantiateWith(-112, gammaService, 19L);

        // then
        assertThat(instance, not(nullValue()));
        assertThat(instance.getSize(), equalTo(-112));
        assertThat(instance.getGammaService(), equalTo(gammaService));
        assertThat(instance.getDuration(), equalTo(19L));
    }

    @Test(expected = NullPointerException.class)
    public void shouldThrowForNullValue() {
        // given
        Injection<ClassWithAnnotations> injection = ConstructorInjection.provide(ClassWithAnnotations.class).get();

        // when / then
        injection.instantiateWith(-112, null, 12L);
    }

    @Test(expected = RuntimeException.class)
    public void shouldThrowUponInstantiationError() {
        // given
        AlphaService alphaService = AlphaService.newInstance(new ProvidedClass(""));
        Injection<InvalidClass> injection = ConstructorInjection.provide(InvalidClass.class).get();

        // when
        injection.instantiateWith(alphaService, 5);
    }

    @Test
    public void shouldReturnNullForNoConstructorInjection() {
        // given / when
        Injection<BetaManager> injection = ConstructorInjection.provide(BetaManager.class).get();

        // then
        assertThat(injection, nullValue());
    }
}
