package fr.xephi.authme.initialization;

import ch.jalu.injector.exceptions.InjectorReflectionException;
import fr.xephi.authme.TestHelper;
import fr.xephi.authme.security.HashAlgorithm;
import org.junit.Test;

import java.util.logging.Logger;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

/**
 * Test for {@link OnStartupTasks}.
 */
public class OnStartupTasksTest {

    @Test
    public void shouldDisplayLegacyJarHint() {
        // given
        Logger logger = TestHelper.setupLogger();
        NoClassDefFoundError noClassDefError = new NoClassDefFoundError("Lcom/google/gson/Gson;");
        ReflectiveOperationException ex2 = new ReflectiveOperationException("", noClassDefError);
        InjectorReflectionException ex = new InjectorReflectionException("", ex2);

        // when
        OnStartupTasks.displayLegacyJarHint(ex);

        // then
        verify(logger).warning("YOU MUST DOWNLOAD THE LEGACY JAR TO USE AUTHME ON YOUR SERVER");
    }

    @Test
    public void shouldNotDisplayLegacyHintForDifferentException() {
        // given
        Logger logger = TestHelper.setupLogger();
        NullPointerException npe = new NullPointerException();

        // when
        OnStartupTasks.displayLegacyJarHint(npe);

        // then
        verifyZeroInteractions(logger);
    }

    @Test
    public void shouldNotDisplayLegacyHintForWrongCause() {
        // given
        Logger logger = TestHelper.setupLogger();
        IllegalAccessException illegalAccessException = new IllegalAccessException("Lcom/google/gson/Gson;");
        ReflectiveOperationException ex2 = new ReflectiveOperationException("", illegalAccessException);
        InjectorReflectionException ex = new InjectorReflectionException("", ex2);

        // when
        OnStartupTasks.displayLegacyJarHint(ex);

        // then
        verifyZeroInteractions(logger);
    }

    @Test
    public void shouldCheckIfHashIsDeprecatedIn54() {
        // given / when / then
        assertThat(OnStartupTasks.isHashDeprecatedIn54(HashAlgorithm.CUSTOM), equalTo(false));
        assertThat(OnStartupTasks.isHashDeprecatedIn54(HashAlgorithm.IPB3), equalTo(false));
        assertThat(OnStartupTasks.isHashDeprecatedIn54(HashAlgorithm.PLAINTEXT), equalTo(false));
        assertThat(OnStartupTasks.isHashDeprecatedIn54(HashAlgorithm.SHA256), equalTo(false));
        assertThat(OnStartupTasks.isHashDeprecatedIn54(HashAlgorithm.WORDPRESS), equalTo(false));

        assertThat(OnStartupTasks.isHashDeprecatedIn54(HashAlgorithm.MD5), equalTo(true));
        assertThat(OnStartupTasks.isHashDeprecatedIn54(HashAlgorithm.SHA512), equalTo(true));
        assertThat(OnStartupTasks.isHashDeprecatedIn54(HashAlgorithm.WHIRLPOOL), equalTo(true));
    }
}
