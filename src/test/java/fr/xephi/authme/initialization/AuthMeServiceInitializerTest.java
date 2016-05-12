package fr.xephi.authme.initialization;

import fr.xephi.authme.initialization.samples.AlphaService;
import fr.xephi.authme.initialization.samples.BadFieldInjection;
import fr.xephi.authme.initialization.samples.BetaManager;
import fr.xephi.authme.initialization.samples.CircularClasses;
import fr.xephi.authme.initialization.samples.ClassWithAbstractDependency;
import fr.xephi.authme.initialization.samples.ClassWithAnnotations;
import fr.xephi.authme.initialization.samples.Duration;
import fr.xephi.authme.initialization.samples.FieldInjectionWithAnnotations;
import fr.xephi.authme.initialization.samples.GammaService;
import fr.xephi.authme.initialization.samples.InstantiationFallbackClasses;
import fr.xephi.authme.initialization.samples.InvalidClass;
import fr.xephi.authme.initialization.samples.InvalidPostConstruct;
import fr.xephi.authme.initialization.samples.InvalidStaticFieldInjection;
import fr.xephi.authme.initialization.samples.PostConstructTestClass;
import fr.xephi.authme.initialization.samples.ProvidedClass;
import fr.xephi.authme.initialization.samples.Size;
import fr.xephi.authme.settings.NewSetting;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Test for {@link AuthMeServiceInitializer}.
 */
public class AuthMeServiceInitializerTest {

    private static final String ALLOWED_PACKAGE = "fr.xephi.authme.initialization";

    private AuthMeServiceInitializer initializer;

    @Before
    public void setInitializer() {
        initializer = new AuthMeServiceInitializer(ALLOWED_PACKAGE);
        initializer.register(ProvidedClass.class, new ProvidedClass(""));
    }

    @Test
    public void shouldInitializeElements() {
        // given / when
        BetaManager betaManager = initializer.get(BetaManager.class);

        // then
        assertThat(betaManager, not(nullValue()));
        for (Object o : betaManager.getDependencies()) {
            assertThat(o, not(nullValue()));
        }
    }

    @Test(expected = RuntimeException.class)
    public void shouldThrowForInvalidPackage() {
        // given / when / then
        initializer.get(InvalidClass.class);
    }

    @Test(expected = RuntimeException.class)
    public void shouldThrowForUnregisteredPrimitiveType() {
        // given / when / then
        initializer.get(int.class);
    }

    @Test
    public void shouldPassValueByAnnotation() {
        // given
        int size = 12;
        long duration = -15482L;
        initializer.provide(Size.class, size);
        initializer.provide(Duration.class, duration);

        // when
        ClassWithAnnotations object = initializer.get(ClassWithAnnotations.class);

        // then
        assertThat(object, not(nullValue()));
        assertThat(object.getSize(), equalTo(size));
        assertThat(object.getDuration(), equalTo(duration));
        // some sample check to make sure we only have one instance of GammaService
        assertThat(object.getGammaService(), equalTo(initializer.get(BetaManager.class).getDependencies()[1]));
    }

    @Test(expected = RuntimeException.class)
    public void shouldRecognizeCircularReferences() {
        // given / when / then
        initializer.get(CircularClasses.Circular3.class);
    }

    @Test(expected = RuntimeException.class)
    public void shouldThrowForUnregisteredAnnotation() {
        // given
        initializer.provide(Size.class, 4523);

        // when / then
        initializer.get(ClassWithAnnotations.class);
    }

    @Test(expected = RuntimeException.class)
    public void shouldThrowForFieldInjectionWithNoDefaultConstructor() {
        // given / when / then
        initializer.get(BadFieldInjection.class);
    }

    @Test
    public void shouldInjectFieldsWithAnnotationsProperly() {
        // given
        initializer.provide(Size.class, 2809375);
        initializer.provide(Duration.class, 13095L);

        // when
        FieldInjectionWithAnnotations result = initializer.get(FieldInjectionWithAnnotations.class);

        // then
        assertThat(result.getSize(), equalTo(2809375));
        assertThat(result.getDuration(), equalTo(13095L));
        assertThat(result.getBetaManager(), not(nullValue()));
        assertThat(result.getClassWithAnnotations(), not(nullValue()));
        assertThat(result.getClassWithAnnotations().getGammaService(),
            equalTo(result.getBetaManager().getDependencies()[1]));
    }

    @Test(expected = RuntimeException.class)
    public void shouldThrowForAnnotationAsKey() {
        // given / when / then
        initializer.get(Size.class);
    }

    @Test(expected = RuntimeException.class)
    public void shouldThrowForSecondRegistration() {
        // given / when / then
        initializer.register(ProvidedClass.class, new ProvidedClass(""));
    }

    @Test(expected = RuntimeException.class)
    public void shouldThrowForSecondAnnotationRegistration() {
        // given
        initializer.provide(Size.class, 12);

        // when / then
        initializer.provide(Size.class, -8);
    }

    @Test(expected = NullPointerException.class)
    public void shouldThrowForNullValueAssociatedToAnnotation() {
        // given / when / then
        initializer.provide(Duration.class, null);
    }

    @Test(expected = NullPointerException.class)
    public void shouldThrowForRegisterWithNull() {
        // given / when / then
        initializer.register(String.class, null);
    }

    @Test
    public void shouldExecutePostConstructMethod() {
        // given
        initializer.provide(Size.class, 15123);

        // when
        PostConstructTestClass testClass = initializer.get(PostConstructTestClass.class);

        // then
        assertThat(testClass.werePostConstructsCalled(), equalTo(true));
        assertThat(testClass.getBetaManager(), not(nullValue()));
    }

    @Test(expected = RuntimeException.class)
    public void shouldThrowForInvalidPostConstructMethod() {
        // given / when / then
        initializer.get(InvalidPostConstruct.WithParams.class);
    }

    @Test(expected = RuntimeException.class)
    public void shouldThrowForStaticPostConstructMethod() {
        // given / when / then
        initializer.get(InvalidPostConstruct.Static.class);
    }

    @Test(expected = RuntimeException.class)
    public void shouldForwardExceptionFromPostConstruct() {
        // given / when / then
        initializer.get(InvalidPostConstruct.ThrowsException.class);
    }

    @Test(expected = RuntimeException.class)
    public void shouldThrowForAbstractNonRegisteredDependency() {
        // given / when / then
        initializer.get(ClassWithAbstractDependency.class);
    }

    @Test
    public void shouldInstantiateWithImplementationOfAbstractDependency() {
        // given
        ClassWithAbstractDependency.ConcreteDependency concrete = new ClassWithAbstractDependency.ConcreteDependency();
        initializer.register(ClassWithAbstractDependency.AbstractDependency.class, concrete);

        // when
        ClassWithAbstractDependency cwad = initializer.get(ClassWithAbstractDependency.class);

        // then
        assertThat(cwad.getAbstractDependency() == concrete, equalTo(true));
        assertThat(cwad.getAlphaService(), not(nullValue()));
    }

    @Test(expected = RuntimeException.class)
    public void shouldThrowForAlreadyRegisteredClass() {
        // given
        initializer.register(BetaManager.class, new BetaManager());

        // when / then
        initializer.register(BetaManager.class, new BetaManager());
    }

    @Test
    public void shouldCreateNewUntrackedInstance() {
        // given / when
        AlphaService singletonScoped = initializer.get(AlphaService.class);
        AlphaService requestScoped = initializer.newInstance(AlphaService.class);

        // then
        assertThat(singletonScoped.getProvidedClass(), not(nullValue()));
        assertThat(singletonScoped.getProvidedClass(), equalTo(requestScoped.getProvidedClass()));
        assertThat(singletonScoped, not(sameInstance(requestScoped)));
    }

    @Test(expected = RuntimeException.class)
    public void shouldThrowForStaticFieldInjection() {
        // given / when / then
        initializer.newInstance(InvalidStaticFieldInjection.class);
    }

    @Test
    public void shouldFallbackToSimpleInstantiationForPlainClass() {
        // given / when
        InstantiationFallbackClasses.HasFallbackDependency result =
            initializer.get(InstantiationFallbackClasses.HasFallbackDependency.class);

        // then
        assertThat(result, not(nullValue()));
        assertThat(result.getGammaService(), not(nullValue()));
        assertThat(result.getFallbackDependency(), not(nullValue()));
    }

    @Test
    public void shouldPerformReloadOnApplicableInstances() {
        // given
        initializer.provide(Size.class, 12);
        initializer.provide(Duration.class, -113L);
        initializer.register(NewSetting.class, mock(NewSetting.class));

        GammaService gammaService = initializer.get(GammaService.class);
        PostConstructTestClass postConstructTestClass = initializer.get(PostConstructTestClass.class);
        ProvidedClass providedClass = initializer.get(ProvidedClass.class);
        initializer.get(ClassWithAnnotations.class);
        // Assert that no class was somehow reloaded at initialization
        assertThat(gammaService.getWasReloaded() || postConstructTestClass.getWasReloaded()
            || providedClass.getWasReloaded(), equalTo(false));

        // when
        initializer.performReloadOnServices();

        // then
        assertThat(gammaService.getWasReloaded(), equalTo(true));
        assertThat(postConstructTestClass.getWasReloaded(), equalTo(true));
        assertThat(providedClass.getWasReloaded(), equalTo(true));
    }

    @Test(expected = RuntimeException.class)
    public void shouldThrowForNullSetting() {
        // given / when / then
        initializer.performReloadOnServices();
    }

}
