package fr.xephi.authme.process.quit;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.TestHelper;
import fr.xephi.authme.data.ProxySessionManager;
import fr.xephi.authme.data.VerificationCodeManager;
import fr.xephi.authme.data.auth.PlayerCache;
import fr.xephi.authme.data.limbo.LimboPlayer;
import fr.xephi.authme.data.limbo.LimboPlayerState;
import fr.xephi.authme.data.limbo.LimboService;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.process.SyncProcessManager;
import fr.xephi.authme.service.CommonService;
import fr.xephi.authme.service.DialogStateService;
import fr.xephi.authme.service.PremiumLoginVerifier;
import fr.xephi.authme.service.SessionService;
import fr.xephi.authme.service.TeleportationService;
import fr.xephi.authme.service.ValidationService;
import fr.xephi.authme.settings.SpawnLoader;
import fr.xephi.authme.settings.properties.PluginSettings;
import fr.xephi.authme.settings.properties.RestrictionSettings;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Test for {@link AsynchronousQuit}, focusing on the guarantee that a quitting player is always
 * unauthenticated afterwards (see the offline-mode login-bypass this protects against).
 */
@ExtendWith(MockitoExtension.class)
class AsynchronousQuitTest {

    @InjectMocks
    private AsynchronousQuit asynchronousQuit;

    @Mock
    private AuthMe plugin;
    @Mock
    private DataSource database;
    @Mock
    private CommonService service;
    @Mock
    private PlayerCache playerCache;
    @Mock
    private SyncProcessManager syncProcessManager;
    @Mock
    private SpawnLoader spawnLoader;
    @Mock
    private ValidationService validationService;
    @Mock
    private VerificationCodeManager codeManager;
    @Mock
    private SessionService sessionService;
    @Mock
    private TeleportationService teleportationService;
    @Mock
    private DialogStateService dialogStateService;
    @Mock
    private ProxySessionManager proxySessionManager;
    @Mock
    private PremiumLoginVerifier premiumLoginVerifier;
    @Mock
    private LimboService limboService;

    @BeforeAll
    static void initLogger() {
        TestHelper.setupLogger();
    }

    @Test
    void shouldRemovePlayerFromCacheEvenWhenQuitLocationLookupThrows() {
        // given - a logged-in player quits and the quit-location lookup throws (e.g. Folia, quit while dead)
        Player player = mockPlayer("Bobby");
        TestHelper.mockIpAddressToPlayer(player, "127.0.0.3");
        given(validationService.isUnrestricted("Bobby")).willReturn(false);
        given(playerCache.isAuthenticated("bobby")).willReturn(true);
        given(service.getProperty(RestrictionSettings.SAVE_QUIT_LOCATION)).willReturn(true);
        given(spawnLoader.getPlayerLocationOrSpawn(player))
            .willThrow(new IllegalStateException("Accessing entity state off owning region's thread"));
        given(service.getProperty(PluginSettings.SESSIONS_ENABLED)).willReturn(true);

        // when
        asynchronousQuit.processQuit(player);

        // then - the throw is swallowed, the session is still updated and the player is unauthenticated
        verify(database).updateSession(org.mockito.ArgumentMatchers.any());
        verify(playerCache).removePlayer("bobby");
        verify(database).setUnlogged("bobby");
        verify(database).invalidateCache("bobby");
    }

    @Test
    void shouldDropProxyAndPremiumGrantsOnQuit() {
        // given - an unauthenticated player quits
        Player player = mockPlayer("Bobby");
        given(validationService.isUnrestricted("Bobby")).willReturn(false);
        given(playerCache.isAuthenticated("bobby")).willReturn(false);

        // when
        asynchronousQuit.processQuit(player);

        // then - name-keyed auto-login grants must not survive the disconnect
        verify(proxySessionManager).removeLoginRequest("bobby");
        verify(premiumLoginVerifier).removeVerified("bobby");
        verify(playerCache).removePlayer("bobby");
        // an unauthenticated quit does no session/quit-location DB writes
        verify(database, never()).updateSession(org.mockito.ArgumentMatchers.any());
        verify(database, never()).setUnlogged(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void shouldResetPendingTotpStateOnQuit() {
        // given - the player passed the password check and was waiting for the 2FA code when leaving
        Player player = mockPlayer("Bobby");
        given(validationService.isUnrestricted("Bobby")).willReturn(false);
        LimboPlayer limbo = new LimboPlayer(null, false, Collections.emptyList(), false, 0.2f, 0.1f);
        limbo.setState(LimboPlayerState.TOTP_REQUIRED);
        given(limboService.getLimboPlayer("bobby")).willReturn(limbo);
        given(playerCache.isAuthenticated("bobby")).willReturn(false);

        // when
        asynchronousQuit.processQuit(player);

        // then - the pending 2FA state does not outlive the connection that passed the password check
        assertThat(limbo.getState(), equalTo(LimboPlayerState.PASSWORD_REQUIRED));
    }

    private Player mockPlayer(String name) {
        Player player = mock(Player.class);
        lenient().when(player.getName()).thenReturn(name);
        return player;
    }
}
