package fr.xephi.authme.service;

import fr.xephi.authme.ReflectionTestUtils;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.SecuritySettings;
import fr.xephi.authme.util.expiring.ExpiringMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static fr.xephi.authme.AuthMeMatchers.stringWithLength;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.BDDMockito.given;

/**
 * Test for {@link RecoveryCodeService}.
 */
@ExtendWith(MockitoExtension.class)
class RecoveryCodeServiceTest {

    private RecoveryCodeService recoveryCodeService;

    @Mock
    private Settings settings;

    @BeforeEach
    void initService() {
        given(settings.getProperty(SecuritySettings.RECOVERY_CODE_HOURS_VALID)).willReturn(4);
        given(settings.getProperty(SecuritySettings.RECOVERY_CODE_LENGTH)).willReturn(5);
        given(settings.getProperty(SecuritySettings.RECOVERY_CODE_MAX_TRIES)).willReturn(3);

        recoveryCodeService = new RecoveryCodeService(settings);
    }

    @Test
    void shouldBeDisabledForNonPositiveLength() {
        assertThat(recoveryCodeService.isRecoveryCodeNeeded(), equalTo(true));

        // given
        given(settings.getProperty(SecuritySettings.RECOVERY_CODE_LENGTH)).willReturn(0);

        // when
        recoveryCodeService.reload(settings);

        // then
        assertThat(recoveryCodeService.isRecoveryCodeNeeded(), equalTo(false));
    }

    @Test
    void shouldGenerateAndStoreCode() {
        // given
        String name = "Bobbers";

        // when
        recoveryCodeService.generateCode(name);

        // then
        String code = getCodeMap().get(name);
        assertThat(code, stringWithLength(5));
    }

    @Test
    void playerHasTriesLeft() {
        // given
        String player = "Dusty";
        recoveryCodeService.generateCode(player);

        // when
        boolean result = recoveryCodeService.hasTriesLeft(player);

        // then
        assertThat(result, equalTo(true));
    }

    @Test
    void playerHasNoTriesLeft() {
        // given
        String player = "Dusty";
        recoveryCodeService.generateCode(player);
        recoveryCodeService.isCodeValid(player, "1st try");
        recoveryCodeService.isCodeValid(player, "2nd try");
        recoveryCodeService.isCodeValid(player, "3rd try");

        // when
        boolean result = recoveryCodeService.hasTriesLeft(player);

        // then
        assertThat(result, equalTo(false));
    }

    @Test
    void shouldRecognizeCorrectCode() {
        // given
        String player = "dragon";
        String code = recoveryCodeService.generateCode(player);

        // when
        boolean result = recoveryCodeService.isCodeValid(player, code);

        // then
        assertThat(result, equalTo(true));
    }

    @Test
    void shouldRemoveCode() {
        // given
        String player = "Tester";
        String code = recoveryCodeService.generateCode(player);

        // when
        recoveryCodeService.removeCode(player);

        // then
        assertThat(recoveryCodeService.isCodeValid(player, code), equalTo(false));
        assertThat(getCodeMap().get(player), nullValue());
        assertThat(getTriesCounter().get(player), equalTo(0));
    }


    private ExpiringMap<String, String> getCodeMap() {
        return ReflectionTestUtils.getFieldValue(RecoveryCodeService.class, recoveryCodeService, "recoveryCodes");
    }

    private ExpiringMap<String, Integer> getTriesCounter() {
        return ReflectionTestUtils.getFieldValue(RecoveryCodeService.class, recoveryCodeService, "playerTries");
    }
}
