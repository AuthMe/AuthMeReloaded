package fr.xephi.authme.data;

import fr.xephi.authme.permission.PermissionsManager;
import fr.xephi.authme.permission.PlayerPermission;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.ProtectionSettings;
import org.bukkit.entity.Player;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Test for {@link QuickCommandsProtectionManager}.
 */
@RunWith(MockitoJUnitRunner.class)
public class QuickCommandsProtectionManagerTest {

    @Mock
    private Settings settings;

    @Mock
    private PermissionsManager permissionsManager;

    @Test
    public void shouldAllowCommand() {
        // given
        String playername = "PlayerName";
        Player player = mockPlayerWithName(playername);
        given(settings.getProperty(ProtectionSettings.QUICK_COMMANDS_DENIED_BEFORE_MILLISECONDS)).willReturn(0);
        given(permissionsManager.hasPermission(player, PlayerPermission.QUICK_COMMANDS_PROTECTION)).willReturn(true);

        String name = "TestName";

        QuickCommandsProtectionManager qcpm = createQuickCommandsProtectioneManager();
        qcpm.processJoin(player);

        // when
        boolean test1 = qcpm.isAllowed(name);
        boolean test2 = qcpm.isAllowed(playername);

        // then
        assertThat(test1, equalTo(true));
        assertThat(test2, equalTo(true));
    }

    @Test
    public void shouldDenyCommand() {
        // given
        String name = "TestName1";
        Player player = mockPlayerWithName(name);
        given(settings.getProperty(ProtectionSettings.QUICK_COMMANDS_DENIED_BEFORE_MILLISECONDS)).willReturn(5000);

        QuickCommandsProtectionManager qcpm = createQuickCommandsProtectioneManager();
        given(permissionsManager.hasPermission(player, PlayerPermission.QUICK_COMMANDS_PROTECTION)).willReturn(true);
        qcpm.processJoin(player);

        // when
        boolean test = qcpm.isAllowed(name);

        // then
        assertThat(test, equalTo(false));
    }

    private QuickCommandsProtectionManager createQuickCommandsProtectioneManager() {
        return new QuickCommandsProtectionManager(settings, permissionsManager);
    }

    private static Player mockPlayerWithName(String name) {
        Player player = mock(Player.class);
        given(player.getName()).willReturn(name);
        return player;
    }

}
