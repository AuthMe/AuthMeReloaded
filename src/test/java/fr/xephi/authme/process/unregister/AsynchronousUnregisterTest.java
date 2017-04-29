package fr.xephi.authme.process.unregister;

import fr.xephi.authme.TestHelper;
import fr.xephi.authme.data.auth.PlayerAuth;
import fr.xephi.authme.data.auth.PlayerCache;
import fr.xephi.authme.data.limbo.LimboService;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.events.AbstractUnregisterEvent;
import fr.xephi.authme.message.MessageKey;
import fr.xephi.authme.security.PasswordSecurity;
import fr.xephi.authme.security.crypts.HashedPassword;
import fr.xephi.authme.service.BukkitService;
import fr.xephi.authme.service.CommonService;
import fr.xephi.authme.service.TeleportationService;
import fr.xephi.authme.settings.commandconfig.CommandManager;
import fr.xephi.authme.settings.properties.RegistrationSettings;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.function.Function;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

/**
 * Test for {@link AsynchronousUnregister}.
 */
@RunWith(MockitoJUnitRunner.class)
public class AsynchronousUnregisterTest {

    @InjectMocks
    private AsynchronousUnregister asynchronousUnregister;

    @Mock
    private DataSource dataSource;
    @Mock
    private CommonService service;
    @Mock
    private PasswordSecurity passwordSecurity;
    @Mock
    private PlayerCache playerCache;
    @Mock
    private BukkitService bukkitService;
    @Mock
    private LimboService limboService;
    @Mock
    private TeleportationService teleportationService;
    @Mock
    private CommandManager commandManager;

    @BeforeClass
    public static void initLogger() {
        TestHelper.setupLogger();
    }

    @Test
    public void shouldRejectWrongPassword() {
        // given
        Player player = mock(Player.class);
        String name = "Bobby";
        given(player.getName()).willReturn(name);
        PlayerAuth auth = mock(PlayerAuth.class);
        given(playerCache.getAuth(name)).willReturn(auth);
        HashedPassword password = new HashedPassword("password", "in_auth_obj");
        given(auth.getPassword()).willReturn(password);
        String userPassword = "pass";
        given(passwordSecurity.comparePassword(userPassword, password, name)).willReturn(false);

        // when
        asynchronousUnregister.unregister(player, userPassword);

        // then
        verify(service).send(player, MessageKey.WRONG_PASSWORD);
        verify(passwordSecurity).comparePassword(userPassword, password, name);
        verifyZeroInteractions(dataSource, limboService, teleportationService, bukkitService);
        verify(player, only()).getName();
    }

    @Test
    public void shouldPerformUnregister() {
        // given
        Player player = mock(Player.class);
        String name = "Frank21";
        given(player.getName()).willReturn(name);
        given(player.isOnline()).willReturn(true);
        PlayerAuth auth = mock(PlayerAuth.class);
        given(playerCache.getAuth(name)).willReturn(auth);
        HashedPassword password = new HashedPassword("password", "in_auth_obj");
        given(auth.getPassword()).willReturn(password);
        String userPassword = "pass";
        given(passwordSecurity.comparePassword(userPassword, password, name)).willReturn(true);
        given(dataSource.removeAuth(name)).willReturn(true);
        given(service.getProperty(RegistrationSettings.FORCE)).willReturn(true);

        // when
        asynchronousUnregister.unregister(player, userPassword);

        // then
        verify(service).send(player, MessageKey.UNREGISTERED_SUCCESS);
        verify(passwordSecurity).comparePassword(userPassword, password, name);
        verify(dataSource).removeAuth(name);
        verify(playerCache).removePlayer(name);
        verify(teleportationService).teleportOnJoin(player);
        verify(bukkitService).scheduleSyncTaskFromOptionallyAsyncTask(any(Runnable.class));
        verifyCalledUnregisterEventFor(player);
        verify(commandManager).runCommandsOnUnregister(player);
    }

    @Test
    public void shouldPerformUnregisterAndNotApplyBlindEffect() {
        // given
        Player player = mock(Player.class);
        String name = "Frank21";
        given(player.getName()).willReturn(name);
        given(player.isOnline()).willReturn(true);
        PlayerAuth auth = mock(PlayerAuth.class);
        given(playerCache.getAuth(name)).willReturn(auth);
        HashedPassword password = new HashedPassword("password", "in_auth_obj");
        given(auth.getPassword()).willReturn(password);
        String userPassword = "pass";
        given(passwordSecurity.comparePassword(userPassword, password, name)).willReturn(true);
        given(dataSource.removeAuth(name)).willReturn(true);
        given(service.getProperty(RegistrationSettings.FORCE)).willReturn(true);

        // when
        asynchronousUnregister.unregister(player, userPassword);

        // then
        verify(service).send(player, MessageKey.UNREGISTERED_SUCCESS);
        verify(passwordSecurity).comparePassword(userPassword, password, name);
        verify(dataSource).removeAuth(name);
        verify(playerCache).removePlayer(name);
        verify(teleportationService).teleportOnJoin(player);
        verify(bukkitService).scheduleSyncTaskFromOptionallyAsyncTask(any(Runnable.class));
        verifyCalledUnregisterEventFor(player);
        verify(commandManager).runCommandsOnUnregister(player);
    }

    @Test
    public void shouldNotApplyUnregisteredEffectsForNotForcedRegistration() {
        // given
        Player player = mock(Player.class);
        String name = "__FranK";
        given(player.getName()).willReturn(name);
        given(player.isOnline()).willReturn(true);
        String userPassword = "141$$5ad";
        HashedPassword password = new HashedPassword("ttt123");
        PlayerAuth auth = PlayerAuth.builder().name(name).password(password).build();
        given(playerCache.getAuth(name)).willReturn(auth);
        given(passwordSecurity.comparePassword(userPassword, password, name)).willReturn(true);
        given(dataSource.removeAuth(name)).willReturn(true);
        given(service.getProperty(RegistrationSettings.FORCE)).willReturn(false);

        // when
        asynchronousUnregister.unregister(player, userPassword);

        // then
        verify(service).send(player, MessageKey.UNREGISTERED_SUCCESS);
        verify(passwordSecurity).comparePassword(userPassword, password, name);
        verify(dataSource).removeAuth(name);
        verify(playerCache).removePlayer(name);
        verifyZeroInteractions(teleportationService, limboService);
        verify(bukkitService, never()).runTask(any(Runnable.class));
        verifyCalledUnregisterEventFor(player);
        verify(commandManager).runCommandsOnUnregister(player);
    }

    @Test
    public void shouldHandleDatabaseError() {
        // given
        Player player = mock(Player.class);
        String name = "Frank21";
        given(player.getName()).willReturn(name);
        PlayerAuth auth = mock(PlayerAuth.class);
        given(playerCache.getAuth(name)).willReturn(auth);
        HashedPassword password = new HashedPassword("password", "in_auth_obj");
        given(auth.getPassword()).willReturn(password);
        String userPassword = "pass";
        given(passwordSecurity.comparePassword(userPassword, password, name)).willReturn(true);
        given(dataSource.removeAuth(name)).willReturn(false);

        // when
        asynchronousUnregister.unregister(player, userPassword);

        // then
        verify(passwordSecurity).comparePassword(userPassword, password, name);
        verify(dataSource).removeAuth(name);
        verify(service).send(player, MessageKey.ERROR);
        verifyZeroInteractions(teleportationService, bukkitService);
    }

    @Test
    public void shouldNotTeleportOfflinePlayer() {
        // given
        Player player = mock(Player.class);
        String name = "Frank21";
        given(player.getName()).willReturn(name);
        given(player.isOnline()).willReturn(false);
        PlayerAuth auth = mock(PlayerAuth.class);
        given(playerCache.getAuth(name)).willReturn(auth);
        HashedPassword password = new HashedPassword("password", "in_auth_obj");
        given(auth.getPassword()).willReturn(password);
        String userPassword = "pass";
        given(passwordSecurity.comparePassword(userPassword, password, name)).willReturn(true);
        given(dataSource.removeAuth(name)).willReturn(true);

        // when
        asynchronousUnregister.unregister(player, userPassword);

        // then
        verify(passwordSecurity).comparePassword(userPassword, password, name);
        verify(dataSource).removeAuth(name);
        verify(playerCache).removePlayer(name);
        verifyZeroInteractions(teleportationService);
        verifyCalledUnregisterEventFor(player);
    }

    // Initiator known and Player object available
    @Test
    public void shouldPerformAdminUnregister() {
        // given
        Player player = mock(Player.class);
        String name = "Frank21";
        given(player.isOnline()).willReturn(true);
        given(dataSource.removeAuth(name)).willReturn(true);
        given(service.getProperty(RegistrationSettings.FORCE)).willReturn(true);
        CommandSender initiator = mock(CommandSender.class);

        // when
        asynchronousUnregister.adminUnregister(initiator, name, player);

        // then
        verify(service).send(player, MessageKey.UNREGISTERED_SUCCESS);
        verify(service).send(initiator, MessageKey.UNREGISTERED_SUCCESS);
        verify(dataSource).removeAuth(name);
        verify(playerCache).removePlayer(name);
        verify(teleportationService).teleportOnJoin(player);
        verify(bukkitService).scheduleSyncTaskFromOptionallyAsyncTask(any(Runnable.class));
        verifyCalledUnregisterEventFor(player);
        verify(commandManager).runCommandsOnUnregister(player);
    }

    @Test
    public void shouldPerformAdminUnregisterWithoutInitiatorOrPlayer() {
        // given
        String name = "billy";
        given(dataSource.removeAuth(name)).willReturn(true);

        // when
        asynchronousUnregister.adminUnregister(null, name, null);

        // then
        verify(dataSource).removeAuth(name);
        verify(playerCache).removePlayer(name);
        verifyZeroInteractions(teleportationService);
        verifyCalledUnregisterEventFor(null);
    }

    @Test
    public void shouldHandleDatabaseErrorForAdminUnregister() {
        // given
        String name = "TtOoLl";
        CommandSender initiator = mock(CommandSender.class);
        given(dataSource.removeAuth(name)).willReturn(false);

        // when
        asynchronousUnregister.adminUnregister(initiator, name, null);

        // then
        verify(dataSource).removeAuth(name);
        verify(service).send(initiator, MessageKey.ERROR);
        verifyZeroInteractions(playerCache, teleportationService, bukkitService);
    }

    @SuppressWarnings("unchecked")
    private void verifyCalledUnregisterEventFor(Player player) {
        ArgumentCaptor<Function<Boolean, AbstractUnregisterEvent>> eventFunctionCaptor =
            ArgumentCaptor.forClass(Function.class);
        verify(bukkitService).createAndCallEvent(eventFunctionCaptor.capture());
        AbstractUnregisterEvent event = eventFunctionCaptor.getValue().apply(true);
        assertThat(event.getPlayer(), equalTo(player));
    }
}
