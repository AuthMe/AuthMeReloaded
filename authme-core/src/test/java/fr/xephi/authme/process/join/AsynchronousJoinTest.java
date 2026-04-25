package fr.xephi.authme.process.join;

import fr.xephi.authme.TestHelper;
import fr.xephi.authme.data.ProxySessionManager;
import fr.xephi.authme.data.auth.PlayerCache;
import fr.xephi.authme.data.limbo.LimboService;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.platform.DialogAdapter;
import fr.xephi.authme.platform.DialogInputSpec;
import fr.xephi.authme.platform.DialogWindowSpec;
import fr.xephi.authme.process.login.AsynchronousLogin;
import fr.xephi.authme.process.register.AsyncRegister;
import fr.xephi.authme.service.PreJoinDialogService;
import fr.xephi.authme.service.BukkitService;
import fr.xephi.authme.service.CommonService;
import fr.xephi.authme.service.DialogStateService;
import fr.xephi.authme.service.DialogWindowService;
import fr.xephi.authme.service.PluginHookService;
import fr.xephi.authme.service.SessionService;
import fr.xephi.authme.service.ValidationService;
import fr.xephi.authme.service.bungeecord.BungeeSender;
import fr.xephi.authme.settings.WelcomeMessageConfiguration;
import fr.xephi.authme.settings.commandconfig.CommandManager;
import fr.xephi.authme.settings.properties.HooksSettings;
import fr.xephi.authme.settings.properties.RegistrationSettings;
import fr.xephi.authme.settings.properties.RestrictionSettings;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static fr.xephi.authme.service.BukkitServiceTestHelper.setBukkitServiceToScheduleSyncTaskFromOptionallyAsyncTask;
import static fr.xephi.authme.service.BukkitServiceTestHelper.setBukkitServiceToRunTaskLater;
import static fr.xephi.authme.service.BukkitServiceTestHelper.setBukkitServiceToRunTaskOptionallyAsync;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.WARN)
public class AsynchronousJoinTest {

    @InjectMocks
    private AsynchronousJoin asynchronousJoin;

    @Mock
    private DataSource database;
    @Mock
    private CommonService service;
    @Mock
    private LimboService limboService;
    @Mock
    private PluginHookService pluginHookService;
    @Mock
    private BukkitService bukkitService;
    @Mock
    private AsynchronousLogin asynchronousLogin;
    @Mock
    private AsyncRegister asyncRegister;
    @Mock
    private CommandManager commandManager;
    @Mock
    private ValidationService validationService;
    @Mock
    private WelcomeMessageConfiguration welcomeMessageConfiguration;
    @Mock
    private SessionService sessionService;
    @Mock
    private PlayerCache playerCache;
    @Mock
    private BungeeSender bungeeSender;
    @Mock
    private ProxySessionManager proxySessionManager;
    @Mock
    private DialogAdapter dialogAdapter;
    @Mock
    private DialogWindowService dialogWindowService;
    @Mock
    private DialogStateService dialogStateService;
    @Mock
    private PreJoinDialogService preJoinDialogService;

    @BeforeAll
    public static void initLogger() {
        TestHelper.setupLogger();
    }

    @BeforeEach
    public void setUp() {
        setBukkitServiceToScheduleSyncTaskFromOptionallyAsyncTask(bukkitService);
        setBukkitServiceToRunTaskOptionallyAsync(bukkitService);
        setBukkitServiceToRunTaskLater(bukkitService);
    }

    @Test
    public void shouldShowLoginDialogForUnauthenticatedRegisteredPlayer() {
        // given
        Player player = mockPlayer("Bobby");
        setUpRegisteredJoin(player);
        given(playerCache.isAuthenticated("Bobby")).willReturn(false);
        given(service.getProperty(RegistrationSettings.USE_DIALOG_UI)).willReturn(true);
        given(dialogAdapter.isDialogSupported()).willReturn(true);
        given(dialogWindowService.createLoginDialog(player)).willReturn(createDialogSpec("Login", "Login"));

        // when
        asynchronousJoin.processJoin(player);

        // then
        verify(limboService).createLimboPlayer(player, true);
        verify(bukkitService).runTaskLater(eq(player), any(Runnable.class), eq(1L));
        verify(dialogAdapter).showLoginDialog(eq(player), any(DialogWindowSpec.class));
    }

    @Test
    public void shouldNotShowLoginDialogForAlreadyAuthenticatedPlayer() {
        // given
        Player player = mockPlayer("Bobby");
        setUpRegisteredJoin(player);
        given(playerCache.isAuthenticated("Bobby")).willReturn(true);

        // when
        asynchronousJoin.processJoin(player);

        // then
        verify(limboService).createLimboPlayer(player, true);
        verify(dialogAdapter, never()).showLoginDialog(eq(player), any(DialogWindowSpec.class));
    }

    @Test
    public void shouldNotShowDelayedDialogIfPlayerGetsAuthenticatedBeforeTaskRuns() {
        // given
        Player player = mockPlayer("Bobby");
        setUpRegisteredJoin(player);
        given(player.isOnline()).willReturn(true);
        given(playerCache.isAuthenticated("Bobby")).willReturn(false, true);
        given(service.getProperty(RegistrationSettings.USE_DIALOG_UI)).willReturn(true);
        given(dialogAdapter.isDialogSupported()).willReturn(true);
        given(dialogWindowService.createLoginDialog(player)).willReturn(createDialogSpec("Login", "Login"));

        AtomicReference<Runnable> delayedTask = new AtomicReference<>();
        doAnswer(invocation -> {
            delayedTask.set(invocation.getArgument(1));
            return mock(fr.xephi.authme.service.CancellableTask.class);
        }).when(bukkitService).runTaskLater(eq(player), any(Runnable.class), eq(1L));

        // when
        asynchronousJoin.processJoin(player);
        delayedTask.get().run();

        // then
        verify(limboService).createLimboPlayer(player, true);
        verify(dialogAdapter, never()).showLoginDialog(eq(player), any(DialogWindowSpec.class));
    }

    @Test
    public void shouldResumeSessionWithoutOpeningDialog() {
        // given
        Player player = mockPlayer("Bobby");
        setUpRegisteredJoin(player);
        given(sessionService.canResumeSession(player)).willReturn(true);

        // when
        asynchronousJoin.processJoin(player);

        // then
        verify(service).send(player, fr.xephi.authme.message.MessageKey.SESSION_RECONNECTION);
        verify(commandManager).runCommandsOnSessionLogin(player);
        verify(asynchronousLogin).forceLogin(player);
        verify(limboService, never()).createLimboPlayer(player, true);
        verify(dialogAdapter, never()).showLoginDialog(eq(player), any(DialogWindowSpec.class));
    }

    @Test
    public void shouldProcessPendingPreJoinLoginInsteadOfShowingDialog() {
        // given
        Player player = mockPlayer("Bobby");
        setUpRegisteredJoin(player);
        java.util.UUID playerId = java.util.UUID.randomUUID();
        given(player.getUniqueId()).willReturn(playerId);
        given(preJoinDialogService.consumePendingLoginPassword(playerId)).willReturn("hunter2");
        given(preJoinDialogService.consumeSkipPostJoinDialog(playerId)).willReturn(false);
        given(playerCache.isAuthenticated("Bobby")).willReturn(false);

        // when
        asynchronousJoin.processJoin(player);

        // then
        verify(limboService).createLimboPlayer(player, true);
        verify(asynchronousLogin).login(player, "hunter2");
        verify(dialogAdapter, never()).showLoginDialog(eq(player), any(DialogWindowSpec.class));
    }

    @Test
    public void shouldSkipPostJoinDialogWhenPreJoinDialogWasDeferred() {
        // given
        Player player = mockPlayer("Bobby");
        setUpRegisteredJoin(player);
        java.util.UUID playerId = java.util.UUID.randomUUID();
        given(player.getUniqueId()).willReturn(playerId);
        given(preJoinDialogService.consumeSkipPostJoinDialog(playerId)).willReturn(true);
        given(playerCache.isAuthenticated("Bobby")).willReturn(false);
        given(service.getProperty(RegistrationSettings.USE_DIALOG_UI)).willReturn(true);
        given(dialogAdapter.isDialogSupported()).willReturn(true);

        // when
        asynchronousJoin.processJoin(player);

        // then
        verify(limboService).createLimboPlayer(player, true);
        verify(dialogAdapter, never()).showLoginDialog(eq(player), any(DialogWindowSpec.class));
    }

    private void setUpRegisteredJoin(Player player) {
        String normalizedName = player.getName().toLowerCase();
        given(validationService.fulfillsNameRestrictions(player)).willReturn(true);
        given(service.getProperty(RestrictionSettings.UNRESTRICTED_NAMES)).willReturn(Set.of());
        given(service.getProperty(RestrictionSettings.FORCE_SURVIVAL_MODE)).willReturn(false);
        given(service.getProperty(HooksSettings.DISABLE_SOCIAL_SPY)).willReturn(false);
        given(service.getProperty(RestrictionSettings.MAX_JOIN_PER_IP)).willReturn(0);
        given(database.isAuthAvailable(normalizedName)).willReturn(true);
        given(service.getProperty(RestrictionSettings.PROTECT_INVENTORY_BEFORE_LOGIN)).willReturn(false);
        given(sessionService.canResumeSession(player)).willReturn(false);
        given(proxySessionManager.shouldResumeSession(normalizedName)).willReturn(false);
        given(service.getProperty(RestrictionSettings.LOGIN_TIMEOUT)).willReturn(30);
        given(pluginHookService.isEssentialsAvailable()).willReturn(false);
        given(service.getProperty(RegistrationSettings.APPLY_BLIND_EFFECT)).willReturn(false);
    }

    private static Player mockPlayer(String name) {
        Player player = mock(Player.class);
        given(player.getName()).willReturn(name);
        given(player.isOnline()).willReturn(true);
        TestHelper.mockIpAddressToPlayer(player, "127.0.0.1");
        return player;
    }

    private static DialogWindowSpec createDialogSpec(String title, String buttonLabel) {
        return new DialogWindowSpec(title,
            java.util.List.of(new DialogInputSpec("password", "Password", 100)),
            buttonLabel,
            "Cancel",
            false,
            false);
    }
}
