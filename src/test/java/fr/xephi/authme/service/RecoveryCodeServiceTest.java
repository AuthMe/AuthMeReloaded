package fr.xephi.authme.service;

import ch.jalu.injector.testing.BeforeInjecting;
import ch.jalu.injector.testing.DelayedInjectionRunner;
import ch.jalu.injector.testing.InjectDelayed;
import fr.xephi.authme.ReflectionTestUtils;
import fr.xephi.authme.service.RecoveryCodeService.ExpiringEntry;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.SecuritySettings;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import java.util.Map;

import static fr.xephi.authme.AuthMeMatchers.stringWithLength;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;

/**
 * Test for {@link RecoveryCodeService}.
 */
@RunWith(DelayedInjectionRunner.class)
public class RecoveryCodeServiceTest {

    @InjectDelayed
    private RecoveryCodeService recoveryCodeService;

    @Mock
    private Settings settings;

    @BeforeInjecting
    public void initSettings() {
        given(settings.getProperty(SecuritySettings.RECOVERY_CODE_HOURS_VALID)).willReturn(4);
        given(settings.getProperty(SecuritySettings.RECOVERY_CODE_LENGTH)).willReturn(5);
    }

    @Test
    public void shouldBeDisabledForNonPositiveLength() {
        assertThat(recoveryCodeService.isRecoveryCodeNeeded(), equalTo(true));

        // given
        given(settings.getProperty(SecuritySettings.RECOVERY_CODE_LENGTH)).willReturn(0);

        // when
        recoveryCodeService.reload(settings);

        // then
        assertThat(recoveryCodeService.isRecoveryCodeNeeded(), equalTo(false));
    }

    @Test
    public void shouldGenerateAndStoreCode() {
        // given
        String name = "Bobbers";

        // when
        recoveryCodeService.generateCode(name);

        // then
        ExpiringEntry entry = getCodeMap().get(name);
        assertThat(entry.getCode(), stringWithLength(5));
    }

    @Test
    public void shouldNotConsiderExpiredCode() {
        // given
        String player = "Cat";
        String code = "11F235";
        setCodeInMap(player, code, System.currentTimeMillis() - 500);

        // when
        boolean result = recoveryCodeService.isCodeValid(player, code);

        // then
        assertThat(result, equalTo(false));
    }

    @Test
    public void shouldRecognizeCorrectCode() {
        // given
        String player = "dragon";
        String code = recoveryCodeService.generateCode(player);

        // when
        boolean result = recoveryCodeService.isCodeValid(player, code);

        // then
        assertThat(result, equalTo(true));
    }

    @Test
    public void shouldRemoveCode() {
        // given
        String player = "Tester";
        String code = recoveryCodeService.generateCode(player);

        // when
        recoveryCodeService.removeCode(player);

        // then
        assertThat(recoveryCodeService.isCodeValid(player, code), equalTo(false));
        assertThat(getCodeMap().get(player), nullValue());
    }


    private Map<String, ExpiringEntry> getCodeMap() {
        return ReflectionTestUtils.getFieldValue(RecoveryCodeService.class, recoveryCodeService, "recoveryCodes");
    }

    private void setCodeInMap(String player, String code, long expiration) {
        Map<String, ExpiringEntry> map = getCodeMap();
        map.put(player, new ExpiringEntry(code, expiration));
    }
}
