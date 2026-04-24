package fr.xephi.authme.process.login;

import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import fr.xephi.authme.TestHelper;
import fr.xephi.authme.data.auth.PlayerAuth;
import fr.xephi.authme.data.auth.PlayerCache;
import fr.xephi.authme.data.captcha.LoginCaptchaManager;
import fr.xephi.authme.data.limbo.LimboMessageType;
import fr.xephi.authme.data.limbo.LimboPlayer;
import fr.xephi.authme.data.limbo.LimboPlayerState;
import fr.xephi.authme.data.limbo.LimboService;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.events.AuthMeAsyncPreLoginEvent;
import fr.xephi.authme.message.MessageKey;
import fr.xephi.authme.permission.PlayerStatePermission;
import fr.xephi.authme.platform.DialogAdapter;
import fr.xephi.authme.platform.DialogInputSpec;
import fr.xephi.authme.platform.DialogWindowSpec;
import fr.xephi.authme.security.PasswordSecurity;
import fr.xephi.authme.service.BukkitService;
import fr.xephi.authme.service.CommonService;
import fr.xephi.authme.service.CancellableTask;
import fr.xephi.authme.service.DialogWindowService;
import fr.xephi.authme.service.SessionService;
import fr.xephi.authme.service.bungeecord.BungeeSender;
import fr.xephi.authme.settings.properties.RegistrationSettings;
import fr.xephi.authme.data.TempbanManager;
import fr.xephi.authme.settings.properties.DatabaseSettings;
import fr.xephi.authme.settings.properties.HooksSettings;
import fr.xephi.authme.settings.properties.PluginSettings;
import fr.xephi.authme.settings.properties.RestrictionSettings;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.stubbing.Answer;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.only;

/**
 * Test for {@link AsynchronousLogin}.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.WARN)
public class AsynchronousLoginTest {

    @InjectMocks
    @Spy
    private AsynchronousLogin asynchronousLogin;

    @Mock
    private DataSource dataSource;
    @Mock
    private PlayerCache playerCache;
    @Mock
    private CommonService commonService;
    @Mock
    private LimboService limboService;
    @Mock
    private BukkitService bukkitService;
    @Mock
    private PasswordSecurity passwordSecurity;
    @Mock
    private LoginCaptchaManager loginCaptchaManager;
    @Mock
    private TempbanManager tempbanManager;
    @Mock
    private SessionService sessionService;
    @Mock
    private BungeeSender bungeeSender;
    @Mock
    private DialogAdapter dialogAdapter;
    @Mock
    private DialogWindowService dialogWindowService;

    @BeforeAll
    public static void initLogger() {
        TestHelper.setupLogger();
    }

    @Test
    public void shouldNotForceLoginAlreadyLoggedInPlayer() {
        // given
        String name = "bobby";
        Player player = mockPlayer(name);
        given(playerCache.isAuthenticated(name)).willReturn(true);

        // when
        asynchronousLogin.forceLogin(player);

        // then
        verify(playerCache, only()).isAuthenticated(name);
        verify(commonService).send(player, MessageKey.ALREADY_LOGGED_IN_ERROR);
        verifyNoInteractions(dataSource);
    }

    @Test
    public void shouldNotForceLoginNonExistentUser() {
        // given
        String name = "oscar";
        Player player = mockPlayer(name);
        given(playerCache.isAuthenticated(name)).willReturn(false);
        given(dataSource.getAuth(name)).willReturn(null);

        // when
        asynchronousLogin.forceLogin(player);

        // then
        verify(playerCache, only()).isAuthenticated(name);
        verify(commonService).send(player, MessageKey.UNKNOWN_USER);
        verify(dataSource, only()).getAuth(name);
    }

    @Test
    public void shouldNotForceLoginInactiveUser() {
        // given
        String name = "oscar";
        Player player = mockPlayer(name);
        given(playerCache.isAuthenticated(name)).willReturn(false);
        int groupId = 13;
        PlayerAuth auth = PlayerAuth.builder().name(name).groupId(groupId).build();
        given(dataSource.getAuth(name)).willReturn(auth);
        given(commonService.getProperty(DatabaseSettings.MYSQL_COL_GROUP)).willReturn("group");
        given(commonService.getProperty(HooksSettings.NON_ACTIVATED_USERS_GROUP)).willReturn(groupId);

        // when
        asynchronousLogin.forceLogin(player);

        // then
        verify(playerCache, only()).isAuthenticated(name);
        verify(commonService).send(player, MessageKey.ACCOUNT_NOT_ACTIVATED);
        verify(dataSource, only()).getAuth(name);
    }

    @Test
    public void shouldNotForceLoginUserWithAlreadyOnlineIp() {
        // given
        String name = "oscar";
        String ip = "1.1.1.245";
        Player player = mockPlayer(name);
        TestHelper.mockIpAddressToPlayer(player, ip);
        given(playerCache.isAuthenticated(name)).willReturn(false);
        PlayerAuth auth = PlayerAuth.builder().name(name).build();
        given(dataSource.getAuth(name)).willReturn(auth);
        given(commonService.getProperty(DatabaseSettings.MYSQL_COL_GROUP)).willReturn("");
        doReturn(true).when(asynchronousLogin).hasReachedMaxLoggedInPlayersForIp(any(Player.class), anyString());

        // when
        asynchronousLogin.forceLogin(player);

        // then
        verify(playerCache, only()).isAuthenticated(name);
        verify(commonService).send(player, MessageKey.ALREADY_LOGGED_IN_ERROR);
        verify(dataSource, only()).getAuth(name);
        verify(asynchronousLogin).hasReachedMaxLoggedInPlayersForIp(player, ip);
    }

    @Test
    public void shouldNotForceLoginForCanceledEvent() {
        // given
        String name = "oscar";
        String ip = "1.1.1.245";
        Player player = mockPlayer(name);
        TestHelper.mockIpAddressToPlayer(player, ip);
        given(playerCache.isAuthenticated(name)).willReturn(false);
        PlayerAuth auth = PlayerAuth.builder().name(name).build();
        given(dataSource.getAuth(name)).willReturn(auth);
        given(commonService.getProperty(DatabaseSettings.MYSQL_COL_GROUP)).willReturn("");
        given(commonService.getProperty(PluginSettings.USE_ASYNC_TASKS)).willReturn(true);
        doReturn(false).when(asynchronousLogin).hasReachedMaxLoggedInPlayersForIp(any(Player.class), anyString());
        doAnswer((Answer<Void>) invocation -> {
            ((AuthMeAsyncPreLoginEvent) invocation.getArgument(0)).setCanLogin(false);
            return null;
        }).when(bukkitService).callEvent(any(AuthMeAsyncPreLoginEvent.class));

        // when
        asynchronousLogin.forceLogin(player);

        // then
        verify(playerCache, only()).isAuthenticated(name);
        verify(dataSource, only()).getAuth(name);
        verify(asynchronousLogin).hasReachedMaxLoggedInPlayersForIp(player, ip);
    }


    @Test
    public void shouldPassMaxLoginPerIpCheck() {
        // given
        Player player = mockPlayer("Carl");
        given(commonService.getProperty(RestrictionSettings.MAX_LOGIN_PER_IP)).willReturn(2);
        given(commonService.hasPermission(player, PlayerStatePermission.ALLOW_MULTIPLE_ACCOUNTS)).willReturn(false);
        mockOnlinePlayersInBukkitService("1.1.1.1");

        // when
        boolean result = asynchronousLogin.hasReachedMaxLoggedInPlayersForIp(player, "1.1.1.1");

        // then
        assertThat(result, equalTo(false));
        verify(commonService).hasPermission(player, PlayerStatePermission.ALLOW_MULTIPLE_ACCOUNTS);
        verify(bukkitService).getOnlinePlayers();
    }

    @Test
    public void shouldSkipIpCheckForZeroThreshold() {
        // given
        Player player = mock(Player.class);
        given(commonService.getProperty(RestrictionSettings.MAX_LOGIN_PER_IP)).willReturn(0);

        // when
        boolean result = asynchronousLogin.hasReachedMaxLoggedInPlayersForIp(player, "2.2.2.2");

        // then
        assertThat(result, equalTo(false));
        verifyNoInteractions(bukkitService);
    }

    @Test
    public void shouldSkipIpCheckForPlayerWithMultipleAccountsPermission() {
        // given
        Player player = mock(Player.class);
        given(commonService.getProperty(RestrictionSettings.MAX_LOGIN_PER_IP)).willReturn(1);
        given(commonService.hasPermission(player, PlayerStatePermission.ALLOW_MULTIPLE_ACCOUNTS)).willReturn(true);

        // when
        boolean result = asynchronousLogin.hasReachedMaxLoggedInPlayersForIp(player, "1.1.1.1");

        // then
        assertThat(result, equalTo(false));
        verify(commonService).hasPermission(player, PlayerStatePermission.ALLOW_MULTIPLE_ACCOUNTS);
        verifyNoInteractions(bukkitService);
    }

    @Test
    public void shouldFailIpCheckForIpWithTooManyPlayersOnline() {
        // given
        Player player = mockPlayer("Ian");
        given(commonService.getProperty(RestrictionSettings.MAX_LOGIN_PER_IP)).willReturn(2);
        given(commonService.hasPermission(player, PlayerStatePermission.ALLOW_MULTIPLE_ACCOUNTS)).willReturn(false);
        mockOnlinePlayersInBukkitService("2.2.2.2");

        // when
        boolean result = asynchronousLogin.hasReachedMaxLoggedInPlayersForIp(player, "2.2.2.2");

        // then
        assertThat(result, equalTo(true));
        verify(commonService).hasPermission(player, PlayerStatePermission.ALLOW_MULTIPLE_ACCOUNTS);
        verify(bukkitService).getOnlinePlayers();
    }

    @Test
    public void shouldShowTotpDialogAfterSuccessfulPasswordCheckWhenDialogsAreEnabled() {
        // given
        Player player = mockPlayer("bobby");
        TestHelper.mockIpAddressToPlayer(player, "203.0.113.5");
        PlayerAuth auth = PlayerAuth.builder().name("bobby").totpKey("secret").build();
        LimboPlayer limboPlayer = mock(LimboPlayer.class);
        given(playerCache.isAuthenticated("bobby")).willReturn(false, false);
        given(dataSource.getAuth("bobby")).willReturn(auth);
        given(commonService.getProperty(DatabaseSettings.MYSQL_COL_GROUP)).willReturn("");
        given(commonService.getProperty(RestrictionSettings.MAX_LOGIN_PER_IP)).willReturn(0);
        given(commonService.getProperty(PluginSettings.USE_ASYNC_TASKS)).willReturn(false);
        given(loginCaptchaManager.isCaptchaRequired("bobby")).willReturn(false);
        given(passwordSecurity.comparePassword("hunter2", auth.getPassword(), "bobby")).willReturn(true);
        given(limboService.getLimboPlayer("bobby")).willReturn(limboPlayer);
        given(commonService.getProperty(RegistrationSettings.USE_DIALOG_UI)).willReturn(true);
        given(dialogAdapter.isDialogSupported()).willReturn(true);
        given(player.isOnline()).willReturn(true);
        given(dialogWindowService.createTotpDialog(player)).willReturn(createDialogSpec("2FA", "Verify"));
        doAnswer(invocation -> {
            invocation.<Runnable>getArgument(1).run();
            return mock(CancellableTask.class);
        }).when(bukkitService).runTaskLater(eq(player), any(Runnable.class), eq(1L));

        // when
        asynchronousLogin.login(player, "hunter2");

        // then
        verify(limboService).resetMessageTask(player, LimboMessageType.TOTP_CODE);
        verify(limboPlayer).setState(LimboPlayerState.TOTP_REQUIRED);
        verify(dialogAdapter).showTotpDialog(eq(player), any(DialogWindowSpec.class));
        verify(asynchronousLogin, never()).performLogin(player, auth);
    }

    @Test
    public void shouldNotShowTotpDialogWhenDialogsAreDisabled() {
        // given
        Player player = mockPlayer("bobby");
        TestHelper.mockIpAddressToPlayer(player, "203.0.113.5");
        PlayerAuth auth = PlayerAuth.builder().name("bobby").totpKey("secret").build();
        LimboPlayer limboPlayer = mock(LimboPlayer.class);
        given(playerCache.isAuthenticated("bobby")).willReturn(false);
        given(dataSource.getAuth("bobby")).willReturn(auth);
        given(commonService.getProperty(DatabaseSettings.MYSQL_COL_GROUP)).willReturn("");
        given(commonService.getProperty(RestrictionSettings.MAX_LOGIN_PER_IP)).willReturn(0);
        given(commonService.getProperty(PluginSettings.USE_ASYNC_TASKS)).willReturn(false);
        given(loginCaptchaManager.isCaptchaRequired("bobby")).willReturn(false);
        given(passwordSecurity.comparePassword("hunter2", auth.getPassword(), "bobby")).willReturn(true);
        given(limboService.getLimboPlayer("bobby")).willReturn(limboPlayer);
        given(commonService.getProperty(RegistrationSettings.USE_DIALOG_UI)).willReturn(false);

        // when
        asynchronousLogin.login(player, "hunter2");

        // then
        verify(limboService).resetMessageTask(player, LimboMessageType.TOTP_CODE);
        verify(limboPlayer).setState(LimboPlayerState.TOTP_REQUIRED);
        verify(dialogAdapter, never()).showTotpDialog(eq(player), any(DialogWindowSpec.class));
    }

    private static Player mockPlayer(String name) {
        Player player = mock(Player.class);
        given(player.getName()).willReturn(name);
        return player;
    }

    private static DialogWindowSpec createDialogSpec(String title, String buttonLabel) {
        return new DialogWindowSpec(title,
            List.of(new DialogInputSpec("code", "Code", 16)),
            buttonLabel,
            "Cancel",
            false,
            false);
    }

    private void mockOnlinePlayersInBukkitService(String checkedIp) {
        Player primaryOnline;
        Player primaryOffline;
        Player extraSameIpOnline = mock(Player.class);

        if ("1.1.1.1".equals(checkedIp)) {
            primaryOnline = mockPlayer("albania");
            TestHelper.mockIpAddressToPlayer(primaryOnline, "1.1.1.1");
            given(dataSource.isLogged("albania")).willReturn(true);

            primaryOffline = mockPlayer("brazil");
            TestHelper.mockIpAddressToPlayer(primaryOffline, "1.1.1.1");
            given(dataSource.isLogged("brazil")).willReturn(false);

            TestHelper.mockIpAddressToPlayer(extraSameIpOnline, "2.2.2.2");
        } else {
            primaryOnline = mockPlayer("congo");
            TestHelper.mockIpAddressToPlayer(primaryOnline, "2.2.2.2");
            given(dataSource.isLogged("congo")).willReturn(true);

            primaryOffline = mockPlayer("denmark");
            TestHelper.mockIpAddressToPlayer(primaryOffline, "2.2.2.2");
            given(dataSource.isLogged("denmark")).willReturn(false);

            Player playerE = mockPlayer("ecuador");
            TestHelper.mockIpAddressToPlayer(playerE, "2.2.2.2");
            given(dataSource.isLogged("ecuador")).willReturn(true);
            extraSameIpOnline = playerE;
        }

        Player otherIpPlayer = mock(Player.class);
        TestHelper.mockIpAddressToPlayer(otherIpPlayer, "3.3.3.3");

        List<Player> onlinePlayers = Arrays.asList(primaryOnline, primaryOffline, extraSameIpOnline, otherIpPlayer);
        given(bukkitService.getOnlinePlayers()).willReturn(onlinePlayers);
    }

}


