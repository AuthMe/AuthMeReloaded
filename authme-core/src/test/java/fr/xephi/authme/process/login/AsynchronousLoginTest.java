package fr.xephi.authme.process.login;

import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
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
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
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

    private static Player mockPlayer(String name) {
        Player player = mock(Player.class);
        given(player.getName()).willReturn(name);
        return player;
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


