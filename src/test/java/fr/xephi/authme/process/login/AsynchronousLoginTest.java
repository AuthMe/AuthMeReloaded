package fr.xephi.authme.process.login;

import fr.xephi.authme.TestHelper;
import fr.xephi.authme.data.auth.PlayerAuth;
import fr.xephi.authme.data.auth.PlayerCache;
import fr.xephi.authme.data.limbo.LimboService;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.events.AuthMeAsyncPreLoginEvent;
import fr.xephi.authme.message.MessageKey;
import fr.xephi.authme.permission.PlayerStatePermission;
import fr.xephi.authme.service.BukkitService;
import fr.xephi.authme.service.CommonService;
import fr.xephi.authme.settings.properties.DatabaseSettings;
import fr.xephi.authme.settings.properties.HooksSettings;
import fr.xephi.authme.settings.properties.PluginSettings;
import fr.xephi.authme.settings.properties.RestrictionSettings;
import org.bukkit.entity.Player;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import java.util.Arrays;
import java.util.List;

import static fr.xephi.authme.service.BukkitServiceTestHelper.returnGivenOnlinePlayers;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.internal.verification.VerificationModeFactory.only;

/**
 * Test for {@link AsynchronousLogin}.
 */
@RunWith(MockitoJUnitRunner.class)
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

    @BeforeClass
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
        verifyZeroInteractions(dataSource);
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
        String ip = "127.0.12.245";
        Player player = mockPlayer(name);
        TestHelper.mockPlayerIp(player, ip);
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
        String ip = "127.0.12.245";
        Player player = mockPlayer(name);
        TestHelper.mockPlayerIp(player, ip);
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
        mockOnlinePlayersInBukkitService();

        // when
        boolean result = asynchronousLogin.hasReachedMaxLoggedInPlayersForIp(player, "127.0.0.4");

        // then
        assertThat(result, equalTo(false));
        verify(commonService).hasPermission(player, PlayerStatePermission.ALLOW_MULTIPLE_ACCOUNTS);
        verify(bukkitService).getOnlinePlayers();
    }

    @Test
    public void shouldSkipIpCheckForZeroThreshold() {
        // given
        Player player = mockPlayer("Fiona");
        given(commonService.getProperty(RestrictionSettings.MAX_LOGIN_PER_IP)).willReturn(0);

        // when
        boolean result = asynchronousLogin.hasReachedMaxLoggedInPlayersForIp(player, "192.168.0.1");

        // then
        assertThat(result, equalTo(false));
        verifyZeroInteractions(bukkitService);
    }

    @Test
    public void shouldSkipIpCheckForPlayerWithMultipleAccountsPermission() {
        // given
        Player player = mockPlayer("Frank");
        given(commonService.getProperty(RestrictionSettings.MAX_LOGIN_PER_IP)).willReturn(1);
        given(commonService.hasPermission(player, PlayerStatePermission.ALLOW_MULTIPLE_ACCOUNTS)).willReturn(true);

        // when
        boolean result = asynchronousLogin.hasReachedMaxLoggedInPlayersForIp(player, "127.0.0.4");

        // then
        assertThat(result, equalTo(false));
        verify(commonService).hasPermission(player, PlayerStatePermission.ALLOW_MULTIPLE_ACCOUNTS);
        verifyZeroInteractions(bukkitService);
    }

    @Test
    public void shouldFailIpCheckForIpWithTooManyPlayersOnline() {
        // given
        Player player = mockPlayer("Ian");
        given(commonService.getProperty(RestrictionSettings.MAX_LOGIN_PER_IP)).willReturn(2);
        given(commonService.hasPermission(player, PlayerStatePermission.ALLOW_MULTIPLE_ACCOUNTS)).willReturn(false);
        mockOnlinePlayersInBukkitService();

        // when
        boolean result = asynchronousLogin.hasReachedMaxLoggedInPlayersForIp(player, "192.168.0.1");

        // then
        assertThat(result, equalTo(true));
        verify(commonService).hasPermission(player, PlayerStatePermission.ALLOW_MULTIPLE_ACCOUNTS);
        verify(bukkitService).getOnlinePlayers();
    }

    private static Player mockPlayer(String name) {
        Player player = mock(Player.class);
        given(player.getName()).willReturn(name);
        return player;
    }

    private void mockOnlinePlayersInBukkitService() {
        // 127.0.0.4: albania (online), brazil (offline)
        Player playerA = mockPlayer("albania");
        TestHelper.mockPlayerIp(playerA, "127.0.0.4");
        given(dataSource.isLogged(playerA.getName())).willReturn(true);
        Player playerB = mockPlayer("brazil");
        TestHelper.mockPlayerIp(playerB, "127.0.0.4");
        given(dataSource.isLogged(playerB.getName())).willReturn(false);

        // 192.168.0.1: congo (online), denmark (offline), ecuador (online)
        Player playerC = mockPlayer("congo");
        TestHelper.mockPlayerIp(playerC, "192.168.0.1");
        given(dataSource.isLogged(playerC.getName())).willReturn(true);
        Player playerD = mockPlayer("denmark");
        TestHelper.mockPlayerIp(playerD, "192.168.0.1");
        given(dataSource.isLogged(playerD.getName())).willReturn(false);
        Player playerE = mockPlayer("ecuador");
        TestHelper.mockPlayerIp(playerE, "192.168.0.1");
        given(dataSource.isLogged(playerE.getName())).willReturn(true);

        // 192.168.0.0: france (offline)
        Player playerF = mockPlayer("france");
        TestHelper.mockPlayerIp(playerF, "192.168.0.0");

        List<Player> onlinePlayers = Arrays.asList(playerA, playerB, playerC, playerD, playerE, playerF);
        returnGivenOnlinePlayers(bukkitService, onlinePlayers);
    }

}
