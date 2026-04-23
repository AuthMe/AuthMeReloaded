package fr.xephi.authme.data.limbo.persistence;

import fr.xephi.authme.TestHelper;
import fr.xephi.authme.data.limbo.EnderPearlRestoreData;
import fr.xephi.authme.data.limbo.LimboPlayer;
import fr.xephi.authme.data.limbo.UserGroup;
import fr.xephi.authme.service.BukkitService;
import fr.xephi.authme.util.FileUtils;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Test for {@link IndividualFilesPersistenceHandler}.
 */
@ExtendWith(MockitoExtension.class)
class IndividualFilesPersistenceHandlerTest {

    private static final UUID SAMPLE_UUID = UUID.nameUUIDFromBytes("PersistenceTest".getBytes());
    private static final String SOURCE_FOLDER = TestHelper.PROJECT_ROOT + "data/backup/";

    private IndividualFilesPersistenceHandler handler;

    @Mock
    private BukkitService bukkitService;

    @TempDir
    File dataFolder;

    @BeforeEach
    void copyTestFilesAndInitHandler() throws IOException {
        File playerFolder = new File(dataFolder, FileUtils.makePath("playerdata", SAMPLE_UUID.toString()));
        if (!playerFolder.mkdirs()) {
            throw new IllegalStateException("Cannot create '" + playerFolder.getAbsolutePath() + "'");
        }
        Files.copy(TestHelper.getJarPath(FileUtils.makePath(SOURCE_FOLDER, "sample-folder", "data.json")),
            new File(playerFolder, "data.json").toPath());

        handler = new IndividualFilesPersistenceHandler(dataFolder, bukkitService);
    }

    @Test
    void shouldReadDataFromFile() {
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
    void shouldReturnNullForUnavailablePlayer() {
        // given
        Player player = mock(Player.class);
        given(player.getUniqueId()).willReturn(UUID.nameUUIDFromBytes("other-player".getBytes()));

        // when
        LimboPlayer data = handler.getLimboPlayer(player);

        // then
        assertThat(data, nullValue());
    }

    @Test
    void shouldSavePlayerData() {
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

    @Test
    void shouldPersistEnderPearlRestoreData() {
        // given
        Player player = mock(Player.class);
        UUID uuid = UUID.nameUUIDFromBytes("Pearl player".getBytes());
        given(player.getUniqueId()).willReturn(uuid);

        World world = mock(World.class);
        given(world.getName()).willReturn("pearl-world");
        given(bukkitService.getWorld("pearl-world")).willReturn(world);

        Location pearlLocation = new Location(world, 2.5, 64.0, -7.5, 10.0f, 20.0f);
        Vector pearlVelocity = new Vector(0.1, 0.2, -0.3);
        UUID pearlUuid = UUID.nameUUIDFromBytes("Saved pearl".getBytes());

        LimboPlayer limbo = new LimboPlayer(null, false, Collections.emptyList(), false, 0.2f, 0.1f);
        limbo.setEnderPearls(Collections.singletonList(new EnderPearlRestoreData(pearlUuid, pearlLocation, pearlVelocity)));

        // when
        handler.saveLimboPlayer(player, limbo);
        LimboPlayer loadedLimbo = handler.getLimboPlayer(player);

        // then
        assertThat(loadedLimbo.getEnderPearls(), hasSize(1));
        EnderPearlRestoreData loadedPearl = loadedLimbo.getEnderPearls().iterator().next();
        assertThat(loadedPearl.getUuid(), equalTo(pearlUuid));
        assertThat(loadedPearl.getLocation().getWorld(), equalTo(world));
        assertThat(loadedPearl.getLocation().getX(), equalTo(2.5));
        assertThat(loadedPearl.getLocation().getY(), equalTo(64.0));
        assertThat(loadedPearl.getLocation().getZ(), equalTo(-7.5));
        assertThat(loadedPearl.getVelocity(), equalTo(pearlVelocity));
    }

}
