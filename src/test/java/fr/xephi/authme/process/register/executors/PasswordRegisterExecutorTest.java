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
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static fr.xephi.authme.AuthMeMatchers.equalToHash;
import static fr.xephi.authme.AuthMeMatchers.hasAuthBasicData;
import static fr.xephi.authme.AuthMeMatchers.hasAuthLocation;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

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
    @Ignore // TODO #792: last IP should be NULL + check registration info
    public void shouldCreatePlayerAuth() {
        // given
        given(passwordSecurity.computeHash(anyString(), anyString())).willAnswer(
            invocation -> new HashedPassword(invocation.getArgument(0)));
        Player player = mockPlayerWithName("S1m0N");
        TestHelper.mockPlayerIp(player, "123.45.67.89");
        World world = mock(World.class);
        given(world.getName()).willReturn("someWorld");
        given(player.getLocation()).willReturn(new Location(world, 48, 96, 144, 1.1f, 0.28f));
        PasswordRegisterParams params = PasswordRegisterParams.of(player, "pass", "mail@example.org");

        // when
        PlayerAuth auth = executor.buildPlayerAuth(params);

        // then
        assertThat(auth, hasAuthBasicData("s1m0n", "S1m0N", "mail@example.org", "123.45.67.89"));
        assertThat(auth, hasAuthLocation(48, 96, 144, "someWorld", 1.1f, 0.28f));
        assertThat(auth.getPassword(), equalToHash("pass"));
    }

    @Test
    public void shouldLogPlayerIn() {
        // given
        given(commonService.getProperty(RegistrationSettings.FORCE_LOGIN_AFTER_REGISTER)).willReturn(false);
        given(commonService.getProperty(PluginSettings.USE_ASYNC_TASKS)).willReturn(false);
        Player player = mock(Player.class);
        PasswordRegisterParams params = PasswordRegisterParams.of(player, "pass", "mail@example.org");

        // when
        executor.executePostPersistAction(params);

        // then
        TestHelper.runSyncDelayedTaskWithDelay(bukkitService);
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
        verifyZeroInteractions(bukkitService, asynchronousLogin);
        verify(syncProcessManager).processSyncPasswordRegister(player);
    }

    private static Player mockPlayerWithName(String name) {
        Player player = mock(Player.class);
        given(player.getName()).willReturn(name);
        return player;
    }
}
