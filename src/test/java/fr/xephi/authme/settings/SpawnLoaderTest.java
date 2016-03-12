package fr.xephi.authme.settings;

import com.google.common.io.Files;
import fr.xephi.authme.TestHelper;
import fr.xephi.authme.hooks.PluginHooks;
import fr.xephi.authme.settings.properties.RestrictionSettings;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Test for {@link SpawnLoader}.
 */
public class SpawnLoaderTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private File testFolder;
    private NewSetting settings;

    @Before
    public void setup() throws IOException {
        // Copy test config into a new temporary folder
        testFolder = temporaryFolder.newFolder();
        File source = TestHelper.getJarFile("/spawn/spawn-firstspawn.yml");
        File destination = new File(testFolder, "spawn.yml");
        Files.copy(source, destination);

        // Create a settings mock with default values
        settings = mock(NewSetting.class);
        given(settings.getProperty(RestrictionSettings.SPAWN_PRIORITY))
            .willReturn("authme, essentials, multiverse, default");
    }

    @Test
    public void shouldSetSpawn() {
        // given
        SpawnLoader spawnLoader = new SpawnLoader(testFolder, settings, mock(PluginHooks.class));
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
