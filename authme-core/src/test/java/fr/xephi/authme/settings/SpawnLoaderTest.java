package fr.xephi.authme.settings;

import org.junit.jupiter.api.extension.ExtendWith;
import fr.xephi.authme.DelayedInjectionExtension;
import ch.jalu.injector.testing.BeforeInjecting;
import ch.jalu.injector.testing.InjectDelayed;
import com.google.common.io.Files;
import fr.xephi.authme.TestHelper;
import fr.xephi.authme.initialization.DataFolder;
import fr.xephi.authme.platform.TeleportAdapter;
import fr.xephi.authme.service.PluginHookService;
import fr.xephi.authme.settings.properties.RestrictionSettings;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import fr.xephi.authme.TempFolder;
import org.mockito.Mock;
import org.mockito.MockedStatic;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

/**
 * Test for {@link SpawnLoader}.
 */
@ExtendWith(DelayedInjectionExtension.class)
public class SpawnLoaderTest {

    @InjectDelayed
    private SpawnLoader spawnLoader;

    @Mock
    private Settings settings;

    @Mock
    private PluginHookService pluginHookService;

    @Mock
    private TeleportAdapter teleportAdapter;
    public TempFolder temporaryFolder = new TempFolder();

    @DataFolder
    private File testFolder;

    @BeforeInjecting
    public void setup() throws IOException {
        TestHelper.setupLogger();

        // Copy test config into a new temporary folder
        testFolder = temporaryFolder.newFolder();
        File source = TestHelper.getJarFile(TestHelper.PROJECT_ROOT + "settings/spawn-firstspawn.yml");
        File destination = new File(testFolder, "spawn.yml");
        Files.copy(source, destination);

        // Create a settings mock with default values
        given(settings.getProperty(RestrictionSettings.SPAWN_PRIORITY))
            .willReturn("authme, essentials, multiverse, default");
    }

    @Test
    public void shouldSetSpawn() {
        // given
        World world = mock(World.class);
        given(world.getName()).willReturn("new_world");
        Location newSpawn = new Location(world, 123, 45.0, -67.89);

        // when
        boolean result = spawnLoader.setSpawn(newSpawn);

        // then
        assertThat(result, equalTo(true));
        YamlConfiguration configuration = YamlConfiguration.loadConfiguration(new File(testFolder, "spawn.yml"));
        assertThat(configuration.getDouble("spawn.x"), equalTo(123.0));
        assertThat(configuration.getDouble("spawn.y"), equalTo(45.0));
        assertThat(configuration.getDouble("spawn.z"), equalTo(-67.89));
        assertThat(configuration.getString("spawn.world"), equalTo("new_world"));
    }

    @Test
    public void shouldReturnBedSpawnLocationForDeadPlayer() {
        // given
        Player player = mock(Player.class);
        given(player.getHealth()).willReturn(0.0);
        World world = mock(World.class);
        Location bedSpawnLocation = new Location(world, 10.0, 70.0, -3.0);
        given(teleportAdapter.getPlayerRespawnLocation(player)).willReturn(bedSpawnLocation);

        // when
        Location result = spawnLoader.getPlayerLocationOrSpawn(player);

        // then
        assertThat(result, equalTo(bedSpawnLocation));
    }

    @Test
    public void shouldFallbackToConfiguredSpawnIfDeadPlayerHasNoRespawnLocation() {
        // given
        given(settings.getProperty(RestrictionSettings.SPAWN_PRIORITY)).willReturn("default");
        spawnLoader.reload();

        Player player = mock(Player.class);
        given(player.getHealth()).willReturn(0.0);
        given(teleportAdapter.getPlayerRespawnLocation(player)).willReturn(null);
        World world = mock(World.class);
        Location worldSpawn = new Location(world, 5.0, 65.0, 5.0);
        given(player.getWorld()).willReturn(world);
        given(world.getSpawnLocation()).willReturn(worldSpawn);

        // when
        Location result = spawnLoader.getPlayerLocationOrSpawn(player);

        // then
        assertThat(result, equalTo(worldSpawn));
    }

    @Test
    public void shouldIgnoreNullCandidateWorldSpawnLocations() {
        // given
        given(settings.getProperty(RestrictionSettings.SPAWN_PRIORITY)).willReturn("default");
        spawnLoader.reload();

        World world = mock(World.class);
        Location worldSpawn = new Location(world, 0.0, 0.0, 0.0);
        given(world.getSpawnLocation()).willReturn(worldSpawn);

        World candidateWorld = mock(World.class);
        given(candidateWorld.getSpawnLocation()).willReturn(null);

        // when
        Location result;
        try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
            bukkitMock.when(Bukkit::getWorlds).thenReturn(List.of(candidateWorld));
            result = spawnLoader.getSpawnLocation(world);
        }

        // then
        assertThat(result, equalTo(worldSpawn));
    }

}


