package fr.xephi.authme.settings;

import com.google.common.io.Files;
import fr.xephi.authme.TestHelper;
import fr.xephi.authme.service.PluginHookService;
import fr.xephi.authme.settings.properties.RestrictionSettings;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Test for {@link SpawnLoader}.
 */
@ExtendWith(MockitoExtension.class)
class SpawnLoaderTest {

    private SpawnLoader spawnLoader;

    @Mock
    private Settings settings;

    @Mock
    private PluginHookService pluginHookService;

    @TempDir
    File testFolder;

    @BeforeEach
    void setup() throws IOException {
        // Copy test config into a new temporary folder
        File source = TestHelper.getJarFile(TestHelper.PROJECT_ROOT + "settings/spawn-firstspawn.yml");
        File destination = new File(testFolder, "spawn.yml");
        Files.copy(source, destination);

        // Create a settings mock with default values
        given(settings.getProperty(RestrictionSettings.SPAWN_PRIORITY))
            .willReturn("authme, essentials, multiverse, default");
        spawnLoader = new SpawnLoader(testFolder, settings, pluginHookService);
    }

    @Test
    void shouldSetSpawn() {
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
}
