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
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.mockito.ArgumentMatchers;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import fr.xephi.authme.TempFolder;
import org.mockito.Mock;
import org.mockito.MockedStatic;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.bukkit.GameRule;

import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
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
    public void shouldReturnExactWorldSpawnForServerPriorityWithZeroRadius() {
        // given
        given(settings.getProperty(RestrictionSettings.SPAWN_PRIORITY)).willReturn("server");
        spawnLoader.reload();

        World world = mock(World.class);
        Location worldSpawn = new Location(world, 10.0, 64.0, -5.0);
        given(world.getSpawnLocation()).willReturn(worldSpawn);
        given(world.getGameRuleValue(GameRule.SPAWN_RADIUS)).willReturn(0);

        // when
        Location result = spawnLoader.getSpawnLocation(world);

        // then
        assertThat(result, equalTo(worldSpawn));
    }

    @Test
    public void shouldReturnExactYAndFaceTowardCenterWhenBaseYIsAlreadySafe() {
        // given
        given(settings.getProperty(RestrictionSettings.SPAWN_PRIORITY)).willReturn("server");
        spawnLoader.reload();

        World world = mock(World.class);
        // worldSpawn at (100, 64, 200); place the result due east (+X) so the expected yaw is -90°
        Location worldSpawn = new Location(world, 100.0, 64.0, 200.0);
        given(world.getSpawnLocation()).willReturn(worldSpawn);
        // radius=0 forces dx=dz=0 → exact worldSpawn → returned as-is (no yaw recalculation)
        given(world.getGameRuleValue(GameRule.SPAWN_RADIUS)).willReturn(10);

        Block passable = mock(Block.class);
        given(passable.isPassable()).willReturn(true);
        given(world.getBlockAt(ArgumentMatchers.anyInt(), ArgumentMatchers.eq(64), ArgumentMatchers.anyInt()))
            .willReturn(passable);
        given(world.getBlockAt(ArgumentMatchers.anyInt(), ArgumentMatchers.eq(65), ArgumentMatchers.anyInt()))
            .willReturn(passable);

        // when
        Location result = spawnLoader.getSpawnLocation(world);

        // then – position within radius, Y unchanged, pitch = 0
        assertThat(result.getX(), both(greaterThanOrEqualTo(90.5)).and(lessThanOrEqualTo(110.5)));
        assertThat(result.getZ(), both(greaterThanOrEqualTo(190.5)).and(lessThanOrEqualTo(210.5)));
        assertThat(result.getY(), equalTo(64.0));
        assertThat((double) result.getPitch(), closeTo(0.0, 0.001));
        // yaw must point from the result position toward worldSpawn (100, 200)
        double expectedYaw = Math.toDegrees(Math.atan2(-(100.0 - result.getX()), 200.0 - result.getZ()));
        assertThat((double) result.getYaw(), closeTo(expectedYaw, 0.001));
    }

    @Test
    public void shouldSearchDownwardWhenBaseYIsInAVoid() {
        // given – foot at baseY is passable but head (baseY+1) is solid (1-block-high gap, e.g. near Nether ceiling)
        given(settings.getProperty(RestrictionSettings.SPAWN_PRIORITY)).willReturn("server");
        spawnLoader.reload();

        World world = mock(World.class);
        Location worldSpawn = new Location(world, 0.0, 70.0, 0.0);
        given(world.getSpawnLocation()).willReturn(worldSpawn);
        given(world.getGameRuleValue(GameRule.SPAWN_RADIUS)).willReturn(5);

        Block passable = mock(Block.class);
        given(passable.isPassable()).willReturn(true);
        Block solid = mock(Block.class);
        given(solid.isPassable()).willReturn(false);

        // y=70 passable, y=71 solid → initial check fails; foot is passable → search downward
        given(world.getBlockAt(ArgumentMatchers.anyInt(), ArgumentMatchers.eq(70), ArgumentMatchers.anyInt()))
            .willReturn(passable);
        given(world.getBlockAt(ArgumentMatchers.anyInt(), ArgumentMatchers.eq(71), ArgumentMatchers.anyInt()))
            .willReturn(solid);
        // y=69 passable → isPassable(69) && isPassable(70) = true → safe at y=69
        given(world.getBlockAt(ArgumentMatchers.anyInt(), ArgumentMatchers.eq(69), ArgumentMatchers.anyInt()))
            .willReturn(passable);

        // when
        Location result = spawnLoader.getSpawnLocation(world);

        // then – first clear 2-block gap found going downward is at y=69
        assertThat(result.getY(), equalTo(69.0));
    }

    @Test
    public void shouldSearchUpwardWhenBaseYIsInsideASolidBlock() {
        // given – baseY is inside a solid block (e.g. underground)
        given(settings.getProperty(RestrictionSettings.SPAWN_PRIORITY)).willReturn("server");
        spawnLoader.reload();

        World world = mock(World.class);
        Location worldSpawn = new Location(world, 0.0, 60.0, 0.0);
        given(world.getSpawnLocation()).willReturn(worldSpawn);
        given(world.getGameRuleValue(GameRule.SPAWN_RADIUS)).willReturn(5);

        Block passable = mock(Block.class);
        given(passable.isPassable()).willReturn(true);
        Block solid = mock(Block.class);
        given(solid.isPassable()).willReturn(false);

        // y=60 is solid → search upward
        given(world.getBlockAt(ArgumentMatchers.anyInt(), ArgumentMatchers.eq(60), ArgumentMatchers.anyInt()))
            .willReturn(solid);
        // y=61 solid, y=62 solid, y=63 passable, y=64 passable → safe at y=63
        given(world.getBlockAt(ArgumentMatchers.anyInt(), ArgumentMatchers.eq(61), ArgumentMatchers.anyInt()))
            .willReturn(solid);
        given(world.getBlockAt(ArgumentMatchers.anyInt(), ArgumentMatchers.eq(62), ArgumentMatchers.anyInt()))
            .willReturn(solid);
        given(world.getBlockAt(ArgumentMatchers.anyInt(), ArgumentMatchers.eq(63), ArgumentMatchers.anyInt()))
            .willReturn(passable);
        given(world.getBlockAt(ArgumentMatchers.anyInt(), ArgumentMatchers.eq(64), ArgumentMatchers.anyInt()))
            .willReturn(passable);

        // when
        Location result = spawnLoader.getSpawnLocation(world);

        // then
        assertThat(result.getY(), equalTo(63.0));
    }

    @Test
    public void shouldFallbackToExactWorldSpawnWhenNoSafeSpotFoundWithinMargin() {
        // given
        given(settings.getProperty(RestrictionSettings.SPAWN_PRIORITY)).willReturn("server");
        spawnLoader.reload();

        World world = mock(World.class);
        Location worldSpawn = new Location(world, 12.0, 64.0, -34.0);
        given(world.getSpawnLocation()).willReturn(worldSpawn);
        given(world.getGameRuleValue(GameRule.SPAWN_RADIUS)).willReturn(5);

        Block solid = mock(Block.class);
        given(solid.isPassable()).willReturn(false);
        given(world.getBlockAt(ArgumentMatchers.anyInt(), ArgumentMatchers.anyInt(), ArgumentMatchers.anyInt()))
            .willReturn(solid);

        // when
        Location result = spawnLoader.getSpawnLocation(world);

        // then – falls back to exact worldSpawn (X/Y/Z unchanged)
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


