package fr.xephi.authme.data.limbo.persistence;

import com.google.common.io.Files;
import fr.xephi.authme.TestHelper;
import fr.xephi.authme.data.limbo.LimboPlayer;
import fr.xephi.authme.data.limbo.UserGroup;
import fr.xephi.authme.service.BukkitService;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.LimboSettings;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.UUID;

import static fr.xephi.authme.TestHelper.PROJECT_ROOT;
import static fr.xephi.authme.TestHelper.TEST_RESOURCES_FOLDER;
import static fr.xephi.authme.data.limbo.LimboPlayerMatchers.hasLocation;
import static fr.xephi.authme.data.limbo.LimboPlayerMatchers.isLimbo;
import static java.util.UUID.fromString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayContainingInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

/**
 * Test for {@link DistributedFilesPersistenceHandler}.
 */
@ExtendWith(MockitoExtension.class)
class DistributedFilesPersistenceHandlerTest {

    /** Player is in seg32-10110 and should be migrated into seg16-f. */
    private static final UUID MIGRATED_UUID = fromString("f6a97c88-7c8f-c12e-4931-6206d4ca067d");
    private static final Matcher<LimboPlayer> MIGRATED_LIMBO_MATCHER =
        isLimbo(false, true, 0.2f, 0.1f, new UserGroup("noob"));

    /** Existing player in seg16-f. */
    private static final UUID UUID_FAB69 = fromString("fab69c88-2cd0-1fed-f00d-dead14ca067d");
    private static final Matcher<LimboPlayer> FAB69_MATCHER =
        isLimbo(false, false, 0.2f, 0.1f, new UserGroup(""));

    /** Player in seg16-8. */
    private static final UUID UUID_STAFF = fromString("88897c88-7c8f-c12e-4931-6206d4ca067d");
    private static final Matcher<LimboPlayer> STAFF_MATCHER =
        isLimbo(true, false, 0.3f, 0.1f, new UserGroup("staff"), new UserGroup("mod"));

    /** Player in seg16-8. */
    private static final UUID UUID_8C679 = fromString("8c679491-1234-abcd-9102-1fa6e0cc3f81");
    private static final Matcher<LimboPlayer> SC679_MATCHER =
        isLimbo(false, true, 0.1f, 0.0f, new UserGroup("primary"));

    /** UUID for which no data is stored (belongs to a segment file that does not exist, seg16-4). */
    private static final UUID UNKNOWN_UUID = fromString("42d1cc0b-8f12-d04a-e7ba-a067d05cdc39");

    /** UUID for which no data is stored (belongs to an existing segment file: seg16-8). */
    private static final UUID UNKNOWN_UUID2 = fromString("84d1cc0b-8f12-d04a-e7ba-a067d05cdc39");


    private DistributedFilesPersistenceHandler persistenceHandler;

    @Mock
    private Settings settings;
    @Mock
    private BukkitService bukkitService;
    @TempDir
    File dataFolder;
    private File playerDataFolder;

    @BeforeAll
    static void initLogger() {
        TestHelper.setupLogger();
    }

    @BeforeEach
    void setUpClasses() throws IOException {
        given(settings.getProperty(LimboSettings.DISTRIBUTION_SIZE)).willReturn(SegmentSize.SIXTEEN);
        playerDataFolder = new File(dataFolder, "playerdata");
        playerDataFolder.mkdir();

        File limboFilesFolder = new File(TEST_RESOURCES_FOLDER + PROJECT_ROOT + "data/limbo");
        for (File file : limboFilesFolder.listFiles()) {
            File from = new File(playerDataFolder, file.getName());
            Files.copy(file, from);
        }

        given(bukkitService.getWorld(anyString()))
            .willAnswer(invocation -> {
                World world = mock(World.class);
                lenient().when(world.getName()).thenReturn(invocation.getArgument(0));
                return world;
            });

        persistenceHandler = new DistributedFilesPersistenceHandler(dataFolder, bukkitService, settings);
    }

    // Note ljacqu 20170314: These tests are a little slow to set up; therefore we sometimes
    // test things in one test that would traditionally belong into two separate tests

    @Test
    void shouldMigrateOldSegmentFilesOnStartup() {
        // Ensure that only the files of the current segmenting scheme remain
        assertThat(playerDataFolder.list(), arrayContainingInAnyOrder("seg16-8-limbo.json", "seg16-f-limbo.json"));

        // Check that the expected limbo players can be read
        assertThat(persistenceHandler.getLimboPlayer(mockPlayerWithUuid(MIGRATED_UUID)), MIGRATED_LIMBO_MATCHER);
        assertThat(persistenceHandler.getLimboPlayer(mockPlayerWithUuid(UUID_FAB69)), FAB69_MATCHER);
        assertThat(persistenceHandler.getLimboPlayer(mockPlayerWithUuid(UUID_STAFF)), STAFF_MATCHER);
        assertThat(persistenceHandler.getLimboPlayer(mockPlayerWithUuid(UUID_8C679)), SC679_MATCHER);

        // Check that unknown players are null (whose segment file exists and does not exist)
        assertThat(persistenceHandler.getLimboPlayer(mockPlayerWithUuid(UNKNOWN_UUID)), nullValue());
        assertThat(persistenceHandler.getLimboPlayer(mockPlayerWithUuid(UNKNOWN_UUID2)), nullValue());
    }

    @Test
    void shouldRemovePlayer() {
        // given
        Player playerToRemove = mockPlayerWithUuid(UUID_STAFF);
        Player unknownPlayerToRemove = mockPlayerWithUuid(UNKNOWN_UUID);

        // when
        persistenceHandler.removeLimboPlayer(playerToRemove);
        persistenceHandler.removeLimboPlayer(unknownPlayerToRemove);

        // then
        assertThat(persistenceHandler.getLimboPlayer(playerToRemove), nullValue());
        assertThat(persistenceHandler.getLimboPlayer(unknownPlayerToRemove), nullValue());
        // Player in same segment should still exist...
        assertThat(persistenceHandler.getLimboPlayer(mockPlayerWithUuid(UUID_8C679)), SC679_MATCHER);

        // Check that we didn't create seg16-4 by deleting UNKNOWN_UUID.
        assertThat(playerDataFolder.list(), arrayContainingInAnyOrder("seg16-8-limbo.json", "seg16-f-limbo.json"));
    }

    @Test
    void shouldAddPlayer() {
        // given
        Player uuidToAdd1 = mockPlayerWithUuid(UNKNOWN_UUID);
        Location location1 = mockLocation("1world");
        LimboPlayer limbo1 = new LimboPlayer(location1, false, Collections.singletonList(new UserGroup("group-1")), true, 0.1f, 0.2f);
        Player uuidToAdd2 = mockPlayerWithUuid(UNKNOWN_UUID2);
        Location location2 = mockLocation("2world");
        LimboPlayer limbo2 = new LimboPlayer(location2, true, Collections.emptyList(), false, 0.0f, 0.25f);

        // when
        persistenceHandler.saveLimboPlayer(uuidToAdd1, limbo1);
        persistenceHandler.saveLimboPlayer(uuidToAdd2, limbo2);

        // then
        LimboPlayer addedPlayer1 = persistenceHandler.getLimboPlayer(uuidToAdd1);
        assertThat(addedPlayer1, isLimbo(limbo1));
        assertThat(addedPlayer1, hasLocation(location1));
        LimboPlayer addedPlayer2 = persistenceHandler.getLimboPlayer(uuidToAdd2);
        assertThat(addedPlayer2, isLimbo(limbo2));
        assertThat(addedPlayer2, hasLocation(location2));

        assertThat(persistenceHandler.getLimboPlayer(mockPlayerWithUuid(MIGRATED_UUID)), MIGRATED_LIMBO_MATCHER);
        assertThat(persistenceHandler.getLimboPlayer(mockPlayerWithUuid(UUID_FAB69)), FAB69_MATCHER);
        assertThat(persistenceHandler.getLimboPlayer(mockPlayerWithUuid(UUID_STAFF)), STAFF_MATCHER);
        assertThat(persistenceHandler.getLimboPlayer(mockPlayerWithUuid(UUID_8C679)), SC679_MATCHER);
    }

    @Test
    void shouldHandleReadErrorGracefully() throws IOException {
        // given
        // assumption
        File invalidFile = new File(playerDataFolder, "seg16-4-limbo.json");
        assertThat(invalidFile.exists(), equalTo(false));
        Files.write("not valid json".getBytes(), invalidFile);

        // when
        LimboPlayer result = persistenceHandler.getLimboPlayer(mockPlayerWithUuid(UNKNOWN_UUID));

        // then
        assertThat(result, nullValue());
    }

    private static Player mockPlayerWithUuid(UUID uuid) {
        Player player = mock(Player.class);
        given(player.getUniqueId()).willReturn(uuid);
        return player;
    }

    private static World mockWorldWithName(String name) {
        World world = mock(World.class);
        given(world.getName()).willReturn(name);
        return world;
    }

    private static Location mockLocation(String worldName) {
        World world = mockWorldWithName(worldName);
        Location location = mock(Location.class);
        given(location.getWorld()).willReturn(world);
        return location;
    }
}
