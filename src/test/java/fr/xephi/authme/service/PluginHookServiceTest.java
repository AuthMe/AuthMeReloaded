package fr.xephi.authme.service;

import com.earth2me.essentials.Essentials;
import com.earth2me.essentials.User;
import com.onarandombox.MultiverseCore.MultiverseCore;
import com.onarandombox.MultiverseCore.api.MVWorldManager;
import com.onarandombox.MultiverseCore.api.MultiverseWorld;
import fr.xephi.authme.ReflectionTestUtils;
import fr.xephi.authme.TestHelper;
import org.bukkit.Location;
import org.bukkit.World;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Test for {@link PluginHookService}.
 */
public class PluginHookServiceTest {

    /** The plugin name of Essentials. */
    private static final String ESSENTIALS = "Essentials";
    /** The plugin name of CMI. */
    private static final String CMI = "CMI";
    /** The plugin name of Multiverse-Core. */
    private static final String MULTIVERSE = "Multiverse-Core";

    @BeforeClass
    public static void setLogger() {
        TestHelper.setupLogger();
    }

    @Test
    public void shouldHookIntoEssentials() {
        // given
        PluginManager pluginManager = mock(PluginManager.class);
        PluginHookService pluginHookService = new PluginHookService(pluginManager);
        setPluginAvailable(pluginManager, ESSENTIALS, Essentials.class);
        assertThat(pluginHookService.isEssentialsAvailable(), equalTo(false));

        // when
        pluginHookService.tryHookToEssentials();

        // then
        assertThat(pluginHookService.isEssentialsAvailable(), equalTo(true));
    }

    @Test
    public void shouldHookIntoEssentialsAtInitialization() {
        // given
        PluginManager pluginManager = mock(PluginManager.class);
        setPluginAvailable(pluginManager, ESSENTIALS, Essentials.class);

        // when
        PluginHookService pluginHookService = new PluginHookService(pluginManager);

        // then
        assertThat(pluginHookService.isEssentialsAvailable(), equalTo(true));
    }

    @Test
    public void shouldHookIntoCmiAtInitialization() {
        // given
        PluginManager pluginManager = mock(PluginManager.class);
        setPluginAvailable(pluginManager, CMI, Plugin.class);

        // when
        PluginHookService pluginHookService = new PluginHookService(pluginManager);

        // then
        assertThat(pluginHookService.isCmiAvailable(), equalTo(true));
    }

    @Test
    public void shouldHookIntoMultiverseAtInitialization() {
        // given
        PluginManager pluginManager = mock(PluginManager.class);
        setPluginAvailable(pluginManager, MULTIVERSE, MultiverseCore.class);

        // when
        PluginHookService pluginHookService = new PluginHookService(pluginManager);

        // then
        assertThat(pluginHookService.isMultiverseAvailable(), equalTo(true));
    }

    @Test
    public void shouldReturnEssentialsDataFolder() {
        // given
        Essentials ess = mock(Essentials.class);
        File essDataFolder = new File("test/data-folder");
        // Need to set the data folder with reflections because getDataFolder() is declared final
        ReflectionTestUtils.setField(JavaPlugin.class, ess, "dataFolder", essDataFolder);

        PluginManager pluginManager = mock(PluginManager.class);
        setPluginAvailable(pluginManager, ESSENTIALS, ess);
        PluginHookService pluginHookService = new PluginHookService(pluginManager);

        // when
        File dataFolder = pluginHookService.getEssentialsDataFolder();

        // then
        assertThat(dataFolder, equalTo(essDataFolder));
    }

    @Test
    public void shouldReturnNullForUnhookedEssentials() {
        // given
        PluginManager pluginManager = mock(PluginManager.class);
        PluginHookService pluginHookService = new PluginHookService(pluginManager);

        // when
        File result = pluginHookService.getEssentialsDataFolder();

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
        setPluginAvailable(pluginManager, ESSENTIALS, ess);
        PluginHookService pluginHookService = new PluginHookService(pluginManager);

        // when
        pluginHookService.setEssentialsSocialSpyStatus(player, true);

        // then
        verify(ess).getUser(player);
        verify(user).setSocialSpyEnabled(true);
    }

    @Test
    public void shouldNotDoAnythingForUnhookedEssentials() {
        // given
        PluginHookService pluginHookService = new PluginHookService(mock(PluginManager.class));

        // when/then
        pluginHookService.setEssentialsSocialSpyStatus(mock(Player.class), false);
    }

    @Test
    public void shouldUnhookEssentialsAndMultiverse() {
        // given
        PluginManager pluginManager = mock(PluginManager.class);
        setPluginAvailable(pluginManager, ESSENTIALS, Essentials.class);
        setPluginAvailable(pluginManager, MULTIVERSE, MultiverseCore.class);
        PluginHookService pluginHookService = new PluginHookService(pluginManager);

        // when
        pluginHookService.unhookEssentials();
        pluginHookService.unhookMultiverse();

        // then
        assertThat(pluginHookService.isEssentialsAvailable(), equalTo(false));
        assertThat(pluginHookService.isMultiverseAvailable(), equalTo(false));
    }

    @Test
    public void shouldHandlePluginRetrievalError() {
        // given
        PluginManager pluginManager = mock(PluginManager.class);
        given(pluginManager.isPluginEnabled(anyString())).willReturn(true);
        doThrow(IllegalStateException.class).when(pluginManager).getPlugin(anyString());

        // when
        PluginHookService pluginHookService = new PluginHookService(pluginManager);

        // then
        assertThat(pluginHookService.isEssentialsAvailable(), equalTo(false));
        assertThat(pluginHookService.isCmiAvailable(), equalTo(false));
        assertThat(pluginHookService.isMultiverseAvailable(), equalTo(false));
    }

    @Test
    public void shouldReturnNullForUnavailableMultiverse() {
        // given
        PluginManager pluginManager = mock(PluginManager.class);
        PluginHookService pluginHookService = new PluginHookService(pluginManager);
        World world = mock(World.class);

        // when
        Location result = pluginHookService.getMultiverseSpawn(world);

        // then
        assertThat(result, nullValue());
    }

    @Test
    public void shouldGetMultiverseSpawn() {
        // given
        Location location = mock(Location.class);
        MultiverseWorld multiverseWorld = mock(MultiverseWorld.class);
        given(multiverseWorld.getSpawnLocation()).willReturn(location);

        World world = mock(World.class);
        MVWorldManager mvWorldManager = mock(MVWorldManager.class);
        given(mvWorldManager.isMVWorld(world)).willReturn(true);
        given(mvWorldManager.getMVWorld(world)).willReturn(multiverseWorld);
        MultiverseCore multiverse = mock(MultiverseCore.class);
        given(multiverse.getMVWorldManager()).willReturn(mvWorldManager);

        PluginManager pluginManager = mock(PluginManager.class);
        setPluginAvailable(pluginManager, MULTIVERSE, multiverse);
        PluginHookService pluginHookService = new PluginHookService(pluginManager);

        // when
        Location spawn = pluginHookService.getMultiverseSpawn(world);

        // then
        assertThat(spawn, equalTo(location));
        verify(mvWorldManager).isMVWorld(world);
        verify(mvWorldManager).getMVWorld(world);
        verify(multiverseWorld).getSpawnLocation();
    }

    @Test
    public void shouldReturnNullForNonMvWorld() {
        // given
        World world = mock(World.class);
        MVWorldManager mvWorldManager = mock(MVWorldManager.class);
        given(mvWorldManager.isMVWorld(world)).willReturn(false);

        PluginManager pluginManager = mock(PluginManager.class);
        MultiverseCore multiverse = mock(MultiverseCore.class);
        setPluginAvailable(pluginManager, MULTIVERSE, multiverse);
        given(multiverse.getMVWorldManager()).willReturn(mvWorldManager);
        PluginHookService pluginHookService = new PluginHookService(pluginManager);

        // when
        Location spawn = pluginHookService.getMultiverseSpawn(world);

        // then
        assertThat(spawn, nullValue());
        verify(mvWorldManager).isMVWorld(world);
        verify(mvWorldManager, never()).getMVWorld(world);
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
