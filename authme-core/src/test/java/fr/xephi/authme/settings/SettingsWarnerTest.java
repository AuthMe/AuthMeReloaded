package fr.xephi.authme.settings;

import fr.xephi.authme.TestHelper;
import fr.xephi.authme.settings.properties.EmailSettings;
import fr.xephi.authme.settings.properties.PluginSettings;
import fr.xephi.authme.settings.properties.RestrictionSettings;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.logging.Logger;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.internal.verification.VerificationModeFactory.times;

/**
 * Test for {@link SettingsWarner}.
 */
@ExtendWith(MockitoExtension.class)
class SettingsWarnerTest {

    @InjectMocks
    private SettingsWarner settingsWarner;

    @Mock
    private Settings settings;

    @Test
    void shouldLogWarnings() {
        // given
        Logger logger = TestHelper.setupLogger();
        given(settings.getProperty(RestrictionSettings.FORCE_SINGLE_SESSION)).willReturn(false);
        given(settings.getProperty(EmailSettings.SMTP_PORT)).willReturn(44);
        given(settings.getProperty(EmailSettings.PORT25_USE_TLS)).willReturn(false);
        given(settings.getProperty(PluginSettings.SESSIONS_ENABLED)).willReturn(true);
        given(settings.getProperty(PluginSettings.SESSIONS_TIMEOUT)).willReturn(-5);

        // when
        settingsWarner.logWarningsForMisconfigurations();

        // then
        verify(logger, times(3)).warning(anyString());
    }

    @Test
    void shouldNotLogAnyWarning() {
        // given
        Logger logger = TestHelper.setupLogger();
        given(settings.getProperty(RestrictionSettings.FORCE_SINGLE_SESSION)).willReturn(true);
        given(settings.getProperty(EmailSettings.SMTP_PORT)).willReturn(25);
        given(settings.getProperty(EmailSettings.PORT25_USE_TLS)).willReturn(false);
        given(settings.getProperty(PluginSettings.SESSIONS_ENABLED)).willReturn(false);

        // when
        settingsWarner.logWarningsForMisconfigurations();

        // then
        verifyNoInteractions(logger);
    }
}
