package fr.xephi.authme.process.quit;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.TestHelper;
import fr.xephi.authme.data.ProxySessionManager;
import fr.xephi.authme.data.VerificationCodeManager;
import fr.xephi.authme.data.auth.PlayerCache;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.process.SyncProcessManager;
import fr.xephi.authme.service.CommonService;
import fr.xephi.authme.service.DialogStateService;
import fr.xephi.authme.service.SessionService;
import fr.xephi.authme.service.TeleportationService;
import fr.xephi.authme.service.ValidationService;
import fr.xephi.authme.settings.SpawnLoader;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Test for {@link AsynchronousQuit}.
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

    @BeforeAll
    static void initLogger() {
        TestHelper.setupLogger();
    }

    @Test
    void shouldDropQueuedProxyLoginRequestOnQuit() {
        // given
        Player player = mock(Player.class);
        given(player.getName()).willReturn("Bobby");

        // when
        asynchronousQuit.processQuit(player);

        // then
        verify(proxySessionManager).removeLoginRequest("bobby");
        verify(playerCache).removePlayer("bobby");
    }
}
