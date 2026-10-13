package fr.xephi.authme.process.login;

import fr.xephi.authme.data.auth.PlayerCache;
import fr.xephi.authme.data.limbo.LimboPlayer;
import fr.xephi.authme.data.limbo.LimboService;
import fr.xephi.authme.permission.PermissionsManager;
import fr.xephi.authme.platform.DialogAdapter;
import fr.xephi.authme.service.BukkitService;
import fr.xephi.authme.service.CommonService;
import fr.xephi.authme.service.DialogStateService;
import fr.xephi.authme.service.JoinMessageService;
import fr.xephi.authme.service.TeleportationService;
import fr.xephi.authme.service.bungeecord.BungeeSender;
import fr.xephi.authme.settings.WelcomeMessageConfiguration;
import fr.xephi.authme.settings.commandconfig.CommandManager;
import fr.xephi.authme.settings.properties.RestrictionSettings;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Test for {@link ProcessSyncPlayerLogin}.
 */
@ExtendWith(MockitoExtension.class)
class ProcessSyncPlayerLoginTest {

    @InjectMocks
    private ProcessSyncPlayerLogin processSyncPlayerLogin;

    @Mock
    private DialogAdapter dialogAdapter;
    @Mock
    private DialogStateService dialogStateService;
    @Mock
    private BungeeSender bungeeSender;
    @Mock
    private LimboService limboService;
    @Mock
    private BukkitService bukkitService;
    @Mock
    private TeleportationService teleportationService;
    @Mock
    private PlayerCache playerCache;
    @Mock
    private CommandManager commandManager;
    @Mock
    private CommonService commonService;
    @Mock
    private WelcomeMessageConfiguration welcomeMessageConfiguration;
    @Mock
    private JoinMessageService joinMessageService;
    @Mock
    private PermissionsManager permissionsManager;

    private Player player;

    @BeforeEach
    void setUpPlayer() {
        player = mock(Player.class);
        given(player.getName()).willReturn("Bobby");
        given(commonService.getProperty(RestrictionSettings.PROTECT_INVENTORY_BEFORE_LOGIN)).willReturn(false);
    }

    @Test
    void shouldRestoreRevokedOperatorWhenLoggedInWithoutLimbo() {
        // given
        given(limboService.getLimboPlayer(player)).willReturn(null);

        // when
        processSyncPlayerLogin.processPlayerLogin(player, false, Collections.emptyList());

        // then
        verify(limboService).restorePendingOperator(player);
        verify(limboService).restoreSpeedsForAutoLogin(player);
        verify(limboService, never()).restoreData(player);
    }

    @Test
    void shouldRestoreDataFromLimboWhenLoggedInWithLimbo() {
        // given
        given(limboService.getLimboPlayer(player)).willReturn(mock(LimboPlayer.class));

        // when
        processSyncPlayerLogin.processPlayerLogin(player, false, Collections.emptyList());

        // then
        verify(limboService).restoreData(player);
        verify(limboService, never()).restorePendingOperator(player);
    }
}
