package fr.xephi.authme.initialization;

import fr.xephi.authme.initialization.samples.GammaService;
import fr.xephi.authme.initialization.samples.InstantiationFallbackClasses;
import org.junit.Test;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

/**
 * Test for {@link InstantiationFallback}.
 */
public class InstantiationFallbackTest {

    @Test
    public void shouldInstantiateClass() {
        // given
        Injection<InstantiationFallbackClasses.FallbackClass> instantiation =
            InstantiationFallback.provide(InstantiationFallbackClasses.FallbackClass.class).get();

        // when
        InstantiationFallbackClasses.FallbackClass result = instantiation.instantiateWith();

        // then
        assertThat(result, not(nullValue()));
    }

    @Test(expected = RuntimeException.class)
    public void shouldThrowIfArgumentsAreSupplied() {
        // given
        Injection<InstantiationFallbackClasses.FallbackClass> instantiation =
            InstantiationFallback.provide(InstantiationFallbackClasses.FallbackClass.class).get();

        // when / then
        instantiation.instantiateWith("some argument");
    }

    @Test
    public void shouldReturnNullForClassWithInjectMethod() {
        // given / when
        Injection<InstantiationFallbackClasses.InvalidInjectOnMethodClass> instantiation =
            InstantiationFallback.provide(InstantiationFallbackClasses.InvalidInjectOnMethodClass.class).get();

        // then
        assertThat(instantiation, nullValue());
    }

    @Test
    public void shouldReturnNullForMissingNoArgsConstructor() {
        // given / when
        Injection<InstantiationFallbackClasses.InvalidFallbackClass> instantiation =
            InstantiationFallback.provide(InstantiationFallbackClasses.InvalidFallbackClass.class).get();

        // then
        assertThat(instantiation, nullValue());
    }

    @Test
    public void shouldReturnNullForDifferentInjectionType() {
        // given / when
        Injection<GammaService> instantiation = InstantiationFallback.provide(GammaService.class).get();

        // then
        assertThat(instantiation, nullValue());
    }

    @Test
    public void shouldReturnNullForClassWithPostConstruct() {
        // given / when
        Injection<InstantiationFallbackClasses.ClassWithPostConstruct> instantiation =
            InstantiationFallback.provide(InstantiationFallbackClasses.ClassWithPostConstruct.class).get();

        // then
        assertThat(instantiation, nullValue());
    }

}
