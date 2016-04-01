package fr.xephi.authme.hooks;

import com.earth2me.essentials.Essentials;
import com.earth2me.essentials.User;
import fr.xephi.authme.ConsoleLoggerTestInitializer;
import fr.xephi.authme.ReflectionTestUtils;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Test for {@link PluginHooks}.
 */
public class PluginHooksTest {

    @BeforeClass
    public static void setLogger() {
        ConsoleLoggerTestInitializer.setupLogger();
    }

    @Test
    public void shouldHookIntoEssentials() {
        // given
        PluginManager pluginManager = mock(PluginManager.class);
        PluginHooks pluginHooks = new PluginHooks(pluginManager);
        setPluginAvailable(pluginManager, "Essentials", Essentials.class);
        assertThat(pluginHooks.isEssentialsAvailable(), equalTo(false));

        // when
        pluginHooks.tryHookToEssentials();

        // then
        assertThat(pluginHooks.isEssentialsAvailable(), equalTo(true));
    }

    // Note ljacqu 20160312: Cannot test with Multiverse or CombatTagPlus because their classes are declared final

    @Test
    public void shouldHookIntoEssentialsAtInitialization() {
        // given
        PluginManager pluginManager = mock(PluginManager.class);
        setPluginAvailable(pluginManager, "Essentials", Essentials.class);

        // when
        PluginHooks pluginHooks = new PluginHooks(pluginManager);

        // then
        assertThat(pluginHooks.isEssentialsAvailable(), equalTo(true));
    }

    @Test
    public void shouldReturnEssentialsDataFolder() {
        // given
        Essentials ess = mock(Essentials.class);
        File essDataFolder = new File("test/data-folder");
        // Need to set the data folder with reflections because getDataFolder() is declared final
        ReflectionTestUtils.setField(JavaPlugin.class, ess, "dataFolder", essDataFolder);

        PluginManager pluginManager = mock(PluginManager.class);
        setPluginAvailable(pluginManager, "Essentials", ess);
        PluginHooks pluginHooks = new PluginHooks(pluginManager);

        // when
        File dataFolder = pluginHooks.getEssentialsDataFolder();

        // then
        assertThat(dataFolder, equalTo(essDataFolder));
    }

    @Test
    public void shouldReturnNullForUnhookedEssentials() {
        // given
        PluginManager pluginManager = mock(PluginManager.class);
        PluginHooks pluginHooks = new PluginHooks(pluginManager);

        // when
        File result = pluginHooks.getEssentialsDataFolder();

        // then
        assertThat(result, nullValue());
    }

    @Test
    public void shouldSetSocialSpyStatus() {
        // given
        Player player = mock(Player.class);

        Essentials ess = mock(Essentials.class);
        User user = mock(User.class);
        given(ess.getUser(player)).willReturn(user);

        PluginManager pluginManager = mock(PluginManager.class);
        setPluginAvailable(pluginManager, "Essentials", ess);
        PluginHooks pluginHooks = new PluginHooks(pluginManager);

        // when
        pluginHooks.setEssentialsSocialSpyStatus(player, true);

        // then
        verify(ess).getUser(player);
        verify(user).setSocialSpyEnabled(true);
    }

    @Test
    public void shouldNotDoAnythingForUnhookedEssentials() {
        // given
        PluginHooks pluginHooks = new PluginHooks(mock(PluginManager.class));

        // when/then
        pluginHooks.setEssentialsSocialSpyStatus(mock(Player.class), false);
    }

    @Test
    public void shouldUnhookEssentials() {
        // given
        PluginManager pluginManager = mock(PluginManager.class);
        setPluginAvailable(pluginManager, "Essentials", Essentials.class);
        PluginHooks pluginHooks = new PluginHooks(pluginManager);

        // when
        pluginHooks.unhookEssentials();

        // then
        assertThat(pluginHooks.isEssentialsAvailable(), equalTo(false));
    }

    @Test
    public void shouldHandlePluginRetrievalError() {
        // given
        PluginManager pluginManager = mock(PluginManager.class);
        given(pluginManager.isPluginEnabled(anyString())).willReturn(true);
        doThrow(IllegalStateException.class).when(pluginManager).getPlugin(anyString());

        // when
        PluginHooks pluginHooks = new PluginHooks(pluginManager);

        // then
        assertThat(pluginHooks.isEssentialsAvailable(), equalTo(false));
        assertThat(pluginHooks.isMultiverseAvailable(), equalTo(false));
        assertThat(pluginHooks.isCombatTagPlusAvailable(), equalTo(false));
    }

    private static void setPluginAvailable(PluginManager managerMock, String pluginName,
                                           Class<? extends Plugin> pluginClass) {
        setPluginAvailable(managerMock, pluginName, mock(pluginClass));
    }

    private static <T extends Plugin> void setPluginAvailable(PluginManager managerMock, String pluginName, T plugin) {
        given(managerMock.isPluginEnabled(pluginName)).willReturn(true);
        given(managerMock.getPlugin(pluginName)).willReturn(plugin);
    }

}
