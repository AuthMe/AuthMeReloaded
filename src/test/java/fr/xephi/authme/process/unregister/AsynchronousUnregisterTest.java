package fr.xephi.authme.process.unregister;

import fr.xephi.authme.TestHelper;
import fr.xephi.authme.cache.auth.PlayerAuth;
import fr.xephi.authme.cache.auth.PlayerCache;
import fr.xephi.authme.cache.limbo.LimboCache;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.output.MessageKey;
import fr.xephi.authme.permission.AuthGroupHandler;
import fr.xephi.authme.permission.AuthGroupType;
import fr.xephi.authme.process.ProcessService;
import fr.xephi.authme.security.PasswordSecurity;
import fr.xephi.authme.security.crypts.HashedPassword;
import fr.xephi.authme.settings.properties.RegistrationSettings;
import fr.xephi.authme.settings.properties.RestrictionSettings;
import fr.xephi.authme.task.PlayerDataTaskManager;
import fr.xephi.authme.util.BukkitService;
import fr.xephi.authme.util.TeleportationService;
import org.bukkit.entity.Player;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
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
    private ProcessService service;
    @Mock
    private PasswordSecurity passwordSecurity;
    @Mock
    private PlayerCache playerCache;
    @Mock
    private BukkitService bukkitService;
    @Mock
    private LimboCache limboCache;
    @Mock
    private PlayerDataTaskManager playerDataTaskManager;
    @Mock
    private TeleportationService teleportationService;
    @Mock
    private AuthGroupHandler authGroupHandler;

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
        verifyZeroInteractions(dataSource, playerDataTaskManager, limboCache, authGroupHandler, teleportationService);
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
        given(service.getProperty(RegistrationSettings.APPLY_BLIND_EFFECT)).willReturn(true);
        given(service.getProperty(RestrictionSettings.TIMEOUT)).willReturn(12);

        // when
        asynchronousUnregister.unregister(player, userPassword);

        // then
        verify(service).send(player, MessageKey.UNREGISTERED_SUCCESS);
        verify(passwordSecurity).comparePassword(userPassword, password, name);
        verify(dataSource).removeAuth(name);
        verify(playerCache).removePlayer(name);
        verify(teleportationService).teleportOnJoin(player);
        verify(authGroupHandler).setGroup(player, AuthGroupType.UNREGISTERED);
        verify(bukkitService).runTask(any(Runnable.class));
    }

}
