package fr.xephi.authme.process.register.executors;

import fr.xephi.authme.TestHelper;
import fr.xephi.authme.data.auth.PlayerAuth;
import fr.xephi.authme.message.MessageKey;
import fr.xephi.authme.process.SyncProcessManager;
import fr.xephi.authme.process.login.AsynchronousLogin;
import fr.xephi.authme.security.PasswordSecurity;
import fr.xephi.authme.security.crypts.HashedPassword;
import fr.xephi.authme.service.BukkitService;
import fr.xephi.authme.service.CommonService;
import fr.xephi.authme.service.ValidationService;
import fr.xephi.authme.service.ValidationService.ValidationResult;
import fr.xephi.authme.settings.properties.PluginSettings;
import fr.xephi.authme.settings.properties.RegistrationSettings;
import org.bukkit.entity.Player;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static fr.xephi.authme.AuthMeMatchers.equalToHash;
import static fr.xephi.authme.AuthMeMatchers.hasAuthBasicData;
import static fr.xephi.authme.service.BukkitServiceTestHelper.setBukkitServiceToScheduleSyncDelayedTaskWithDelay;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Test for {@link PasswordRegisterExecutor}.
 */
@RunWith(MockitoJUnitRunner.class)
public class PasswordRegisterExecutorTest {

    @InjectMocks
    private PasswordRegisterExecutor executor;

    @Mock
    private ValidationService validationService;
    @Mock
    private CommonService commonService;
    @Mock
    private PasswordSecurity passwordSecurity;
    @Mock
    private BukkitService bukkitService;
    @Mock
    private SyncProcessManager syncProcessManager;
    @Mock
    private AsynchronousLogin asynchronousLogin;

    @Test
    public void shouldCheckPasswordValidity() {
        // given
        String password = "myPass";
        String name = "player040";
        given(validationService.validatePassword(password, name)).willReturn(new ValidationResult());
        Player player = mockPlayerWithName(name);
        PasswordRegisterParams params = PasswordRegisterParams.of(player, password, null);

        // when
        boolean result = executor.isRegistrationAdmitted(params);

        // then
        assertThat(result, equalTo(true));
        verify(validationService).validatePassword(password, name);
    }

    @Test
    public void shouldDetectInvalidPasswordAndInformPlayer() {
        // given
        String password = "myPass";
        String name = "player040";
        given(validationService.validatePassword(password, name)).willReturn(
            new ValidationResult(MessageKey.PASSWORD_CHARACTERS_ERROR, "[a-z]"));
        Player player = mockPlayerWithName(name);
        PasswordRegisterParams params = PasswordRegisterParams.of(player, password, null);

        // when
        boolean result = executor.isRegistrationAdmitted(params);

        // then
        assertThat(result, equalTo(false));
        verify(validationService).validatePassword(password, name);
        verify(commonService).send(player, MessageKey.PASSWORD_CHARACTERS_ERROR, "[a-z]");
    }

    @Test
    public void shouldCreatePlayerAuth() {
        // given
        given(passwordSecurity.computeHash(anyString(), anyString())).willAnswer(
            invocation -> new HashedPassword(invocation.getArgument(0)));
        Player player = mockPlayerWithName("S1m0N");
        TestHelper.mockIpAddressToPlayer(player, "123.45.67.89");
        PasswordRegisterParams params = PasswordRegisterParams.of(player, "pass", "mail@example.org");

        // when
        PlayerAuth auth = executor.buildPlayerAuth(params);

        // then
        assertThat(auth, hasAuthBasicData("s1m0n", "S1m0N", "mail@example.org", null));
        assertThat(auth.getRegistrationIp(), equalTo("123.45.67.89"));
        assertIsCloseTo(auth.getRegistrationDate(), System.currentTimeMillis(), 500);
        assertThat(auth.getPassword(), equalToHash("pass"));
    }

    @Test
    public void shouldLogPlayerIn() {
        // given
        given(commonService.getProperty(RegistrationSettings.FORCE_LOGIN_AFTER_REGISTER)).willReturn(false);
        given(commonService.getProperty(PluginSettings.USE_ASYNC_TASKS)).willReturn(false);
        Player player = mock(Player.class);
        PasswordRegisterParams params = PasswordRegisterParams.of(player, "pass", "mail@example.org");
        setBukkitServiceToScheduleSyncDelayedTaskWithDelay(bukkitService);

        // when
        executor.executePostPersistAction(params);

        // then
        verify(asynchronousLogin).forceLogin(player);
        verify(syncProcessManager).processSyncPasswordRegister(player);
    }

    @Test
    public void shouldNotLogPlayerIn() {
        // given
        given(commonService.getProperty(RegistrationSettings.FORCE_LOGIN_AFTER_REGISTER)).willReturn(true);
        Player player = mock(Player.class);
        PasswordRegisterParams params = PasswordRegisterParams.of(player, "pass", "mail@example.org");

        // when
        executor.executePostPersistAction(params);

        // then
        verifyNoInteractions(bukkitService, asynchronousLogin);
        verify(syncProcessManager).processSyncPasswordRegister(player);
    }

    private static Player mockPlayerWithName(String name) {
        Player player = mock(Player.class);
        given(player.getName()).willReturn(name);
        return player;
    }

    private static void assertIsCloseTo(long value1, long value2, long tolerance) {
        assertThat(Math.abs(value1 - value2), not(greaterThan(tolerance)));
    }
}
