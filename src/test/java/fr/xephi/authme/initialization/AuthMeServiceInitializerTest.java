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
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.hamcrest.Matchers.containsString;
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

    // As we test many cases that throw exceptions, we use JUnit's ExpectedException Rule
    // to make sure that we receive the exception we expect
    @Rule
    public ExpectedException expectedException = ExpectedException.none();

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

    @Test
    public void shouldThrowForInvalidPackage() {
        // given / when / then
        expectRuntimeExceptionWith("outside of the allowed packages");
        initializer.get(InvalidClass.class);
    }

    @Test
    public void shouldThrowForUnregisteredPrimitiveType() {
        // given / when / then
        expectRuntimeExceptionWith("Primitive types must be provided");
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

    @Test
    public void shouldRecognizeCircularReferences() {
        // given / when / then
        expectRuntimeExceptionWith("Found cyclic dependency");
        initializer.get(CircularClasses.Circular3.class);
    }

    @Test
    public void shouldThrowForUnregisteredAnnotation() {
        // given
        initializer.provide(Size.class, 4523);

        // when / then
        expectRuntimeExceptionWith("must be registered beforehand");
        initializer.get(ClassWithAnnotations.class);
    }

    @Test
    public void shouldThrowForFieldInjectionWithoutNoArgsConstructor() {
        // given / when / then
        expectRuntimeExceptionWith("Did not find injection method");
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

    @Test
    public void shouldThrowForAnnotationAsKey() {
        // given / when / then
        expectRuntimeExceptionWith("Cannot retrieve annotated elements in this way");
        initializer.get(Size.class);
    }

    @Test
    public void shouldThrowForSecondRegistration() {
        // given / when / then
        expectRuntimeExceptionWith("There is already an object present");
        initializer.register(ProvidedClass.class, new ProvidedClass(""));
    }

    @Test
    public void shouldThrowForSecondAnnotationRegistration() {
        // given
        initializer.provide(Size.class, 12);

        // when / then
        expectRuntimeExceptionWith("already registered");
        initializer.provide(Size.class, -8);
    }

    @Test
    public void shouldThrowForNullValueAssociatedToAnnotation() {
        // given / when / then
        expectedException.expect(NullPointerException.class);
        initializer.provide(Duration.class, null);
    }

    @Test
    public void shouldThrowForRegisterWithNull() {
        // given / when / then
        expectedException.expect(NullPointerException.class);
        initializer.register(String.class, null);
    }

    @Test
    public void shouldExecutePostConstructMethod() {
        // given
        initializer.provide(Size.class, 15123);

        // when
        PostConstructTestClass testClass = initializer.get(PostConstructTestClass.class);

        // then
        assertThat(testClass.wasPostConstructCalled(), equalTo(true));
        assertThat(testClass.getBetaManager(), not(nullValue()));
    }

    @Test
    public void shouldThrowForInvalidPostConstructMethod() {
        // given / when / then
        expectRuntimeExceptionWith("@PostConstruct method may not be static or have any parameters");
        initializer.get(InvalidPostConstruct.WithParams.class);
    }

    @Test
    public void shouldThrowForStaticPostConstructMethod() {
        // given / when / then
        expectRuntimeExceptionWith("@PostConstruct method may not be static or have any parameters");
        initializer.get(InvalidPostConstruct.Static.class);
    }

    @Test
    public void shouldForwardExceptionFromPostConstruct() {
        // given / when / then
        expectRuntimeExceptionWith("Error executing @PostConstruct method");
        initializer.get(InvalidPostConstruct.ThrowsException.class);
    }

    @Test
    public void shouldThrowForMultiplePostConstructMethods() {
        // given / when / then
        expectRuntimeExceptionWith("Multiple methods with @PostConstruct");
        initializer.get(InvalidPostConstruct.MultiplePostConstructs.class);
    }

    @Test
    public void shouldThrowForPostConstructNotReturningVoid() {
        // given / when / then
        expectRuntimeExceptionWith("@PostConstruct method must have return type void");
        initializer.get(InvalidPostConstruct.NotVoidReturnType.class);
    }

    @Test
    public void shouldThrowForAbstractNonRegisteredDependency() {
        // given / when / then
        expectRuntimeExceptionWith("cannot be instantiated");
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

    @Test
    public void shouldThrowForAlreadyRegisteredClass() {
        // given
        initializer.register(BetaManager.class, new BetaManager());

        // when / then
        expectRuntimeExceptionWith("There is already an object present");
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

    @Test
    public void shouldThrowForStaticFieldInjection() {
        // given / when / then
        expectRuntimeExceptionWith("is static but annotated with @Inject");
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

    @Test
    public void shouldThrowForNullSetting() {
        // given / when / then
        expectRuntimeExceptionWith("Settings instance is null");
        initializer.performReloadOnServices();
    }

    @Test
    public void shouldRetrieveExistingInstancesOnly() {
        // given
        initializer.get(GammaService.class);

        // when
        AlphaService alphaService = initializer.getIfAvailable(AlphaService.class);
        BetaManager betaManager = initializer.getIfAvailable(BetaManager.class);

        // then
        // was initialized because is dependency of GammaService
        assertThat(alphaService, not(nullValue()));
        // nothing caused this to be initialized
        assertThat(betaManager, nullValue());
    }

    private void expectRuntimeExceptionWith(String message) {
        expectedException.expect(RuntimeException.class);
        expectedException.expectMessage(containsString(message));
    }

}
