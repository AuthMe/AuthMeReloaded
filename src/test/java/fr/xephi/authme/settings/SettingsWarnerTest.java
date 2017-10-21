package fr.xephi.authme.settings;

import fr.xephi.authme.TestHelper;
import fr.xephi.authme.settings.properties.EmailSettings;
import fr.xephi.authme.settings.properties.PluginSettings;
import fr.xephi.authme.settings.properties.RestrictionSettings;
import org.junit.Test;

import java.util.logging.Logger;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.internal.verification.VerificationModeFactory.times;

/**
 * Test for {@link SettingsWarner}.
 */
public class SettingsWarnerTest {

    @Test
    public void shouldHaveHiddenConstructorOnly() {
        TestHelper.validateHasOnlyPrivateEmptyConstructor(SettingsWarner.class);
    }

    @Test
    public void shouldLogWarnings() {
        // given
        Logger logger = TestHelper.setupLogger();
        Settings settings = mock(Settings.class);
        given(settings.getProperty(RestrictionSettings.FORCE_SINGLE_SESSION)).willReturn(false);
        given(settings.getProperty(EmailSettings.SMTP_PORT)).willReturn(44);
        given(settings.getProperty(EmailSettings.PORT25_USE_TLS)).willReturn(false);
        given(settings.getProperty(PluginSettings.SESSIONS_ENABLED)).willReturn(true);
        given(settings.getProperty(PluginSettings.SESSIONS_TIMEOUT)).willReturn(-5);

        // when
        SettingsWarner.logWarningsForMisconfigurations(settings);

        // then
        verify(logger, times(3)).warning(anyString());
    }

    @Test
    public void shouldNotLogAnyWarning() {
        Logger logger = TestHelper.setupLogger();
        Settings settings = mock(Settings.class);
        given(settings.getProperty(RestrictionSettings.FORCE_SINGLE_SESSION)).willReturn(true);
        given(settings.getProperty(EmailSettings.SMTP_PORT)).willReturn(25);
        given(settings.getProperty(EmailSettings.PORT25_USE_TLS)).willReturn(false);
        given(settings.getProperty(PluginSettings.SESSIONS_ENABLED)).willReturn(false);
        given(settings.getProperty(PluginSettings.SESSIONS_TIMEOUT)).willReturn(-5);

        // when
        SettingsWarner.logWarningsForMisconfigurations(settings);

        // then
        verifyZeroInteractions(logger);
    }
}
