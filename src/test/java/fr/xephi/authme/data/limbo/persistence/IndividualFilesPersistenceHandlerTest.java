package fr.xephi.authme.data.limbo.persistence;

import ch.jalu.injector.testing.BeforeInjecting;
import ch.jalu.injector.testing.DelayedInjectionRunner;
import ch.jalu.injector.testing.InjectDelayed;
import fr.xephi.authme.TestHelper;
import fr.xephi.authme.data.limbo.LimboPlayer;
import fr.xephi.authme.data.limbo.UserGroup;
import fr.xephi.authme.initialization.DataFolder;
import fr.xephi.authme.service.BukkitService;
import fr.xephi.authme.util.FileUtils;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;
import java.util.UUID;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Test for {@link IndividualFilesPersistenceHandler}.
 */
@RunWith(DelayedInjectionRunner.class)
public class IndividualFilesPersistenceHandlerTest {

    private static final UUID SAMPLE_UUID = UUID.nameUUIDFromBytes("PersistenceTest".getBytes());
    private static final String SOURCE_FOLDER = TestHelper.PROJECT_ROOT + "data/backup/";

    @InjectDelayed
    private IndividualFilesPersistenceHandler handler;

    @Mock
    private BukkitService bukkitService;

    @DataFolder
    private File dataFolder;

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @BeforeInjecting
    public void copyTestFiles() throws IOException {
        dataFolder = temporaryFolder.newFolder();
        File playerFolder = new File(dataFolder, FileUtils.makePath("playerdata", SAMPLE_UUID.toString()));
        if (!playerFolder.mkdirs()) {
            throw new IllegalStateException("Cannot create '" + playerFolder.getAbsolutePath() + "'");
        }
        Files.copy(TestHelper.getJarPath(FileUtils.makePath(SOURCE_FOLDER, "sample-folder", "data.json")),
            new File(playerFolder, "data.json").toPath());
    }

    @Test
    public void shouldReadDataFromFile() {
        // given
        Player player = mock(Player.class);
        given(player.getUniqueId()).willReturn(SAMPLE_UUID);
        World world = mock(World.class);
        given(bukkitService.getWorld("nether")).willReturn(world);

        // when
        LimboPlayer data = handler.getLimboPlayer(player);

        // then
        assertThat(data, not(nullValue()));
        assertThat(data.isOperator(), equalTo(true));
        assertThat(data.isCanFly(), equalTo(true));
        assertThat(data.getWalkSpeed(), equalTo(0.2f));
        assertThat(data.getFlySpeed(), equalTo(0.1f));
        assertThat(data.getGroups(), contains(new UserGroup("players")));
        Location location = data.getLocation();
        assertThat(location.getX(), equalTo(-113.219));
        assertThat(location.getY(), equalTo(72.0));
        assertThat(location.getZ(), equalTo(130.637));
        assertThat(location.getWorld(), equalTo(world));
        assertThat(location.getPitch(), equalTo(24.15f));
        assertThat(location.getYaw(), equalTo(-292.484f));
    }

    @Test
    public void shouldReturnNullForUnavailablePlayer() {
        // given
        Player player = mock(Player.class);
        given(player.getUniqueId()).willReturn(UUID.nameUUIDFromBytes("other-player".getBytes()));

        // when
        LimboPlayer data = handler.getLimboPlayer(player);

        // then
        assertThat(data, nullValue());
    }

    @Test
    public void shouldSavePlayerData() {
        // given
        Player player = mock(Player.class);
        UUID uuid = UUID.nameUUIDFromBytes("New player".getBytes());
        given(player.getUniqueId()).willReturn(uuid);


        World world = mock(World.class);
        given(world.getName()).willReturn("player-world");
        Location location = new Location(world, 0.2, 102.25, -89.28, 3.02f, 90.13f);
        LimboPlayer limbo = new LimboPlayer(location, true, Collections.singletonList(new UserGroup("primary-grp")), true, 1.2f, 0.8f);

        // when
        handler.saveLimboPlayer(player, limbo);

        // then
        File playerFile = new File(dataFolder, FileUtils.makePath("playerdata", uuid.toString(), "data.json"));
        assertThat(playerFile.exists(), equalTo(true));
        // TODO ljacqu 20160711: Check contents of file
    }

}
