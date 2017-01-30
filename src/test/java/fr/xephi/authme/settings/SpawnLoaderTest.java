package fr.xephi.authme.settings;

import ch.jalu.injector.testing.BeforeInjecting;
import ch.jalu.injector.testing.DelayedInjectionRunner;
import ch.jalu.injector.testing.InjectDelayed;
import com.google.common.io.Files;
import fr.xephi.authme.TestHelper;
import fr.xephi.authme.initialization.DataFolder;
import fr.xephi.authme.service.PluginHookService;
import fr.xephi.authme.settings.properties.RestrictionSettings;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import java.io.File;
import java.io.IOException;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Test for {@link SpawnLoader}.
 */
@RunWith(DelayedInjectionRunner.class)
public class SpawnLoaderTest {

    @InjectDelayed
    private SpawnLoader spawnLoader;

    @Mock
    private Settings settings;

    @Mock
    private PluginHookService pluginHookService;

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @DataFolder
    private File testFolder;

    @BeforeInjecting
    public void setup() throws IOException {
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

}
