package fr.xephi.authme.data.limbo;

import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import fr.xephi.authme.TestHelper;
import org.bukkit.World;
import org.bukkit.Location;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;

import java.util.Collections;
import java.util.UUID;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Test for {@link LimboServiceHelper}.
 * <p>
 * Note: some methods are tested directly where they are used via {@link LimboServiceTest}.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.WARN)
public class LimboServiceHelperTest {

    @InjectMocks
    private LimboServiceHelper limboServiceHelper;

    @BeforeAll
    public static void initLogger() {
        TestHelper.setupLogger();
    }

    @Test
    public void shouldMergeLimboPlayers() {
        // given
        Location newLocation = mock(Location.class);
        LimboPlayer newLimbo = new LimboPlayer(newLocation, false, Collections.singletonList(new UserGroup("grp-new")), false, 0.0f, 0.0f);
        Location oldLocation = mock(Location.class);
        LimboPlayer oldLimbo = new LimboPlayer(oldLocation, true, Collections.singletonList(new UserGroup("grp-old")), true, 0.1f, 0.8f);

        // when
        LimboPlayer result = limboServiceHelper.merge(newLimbo, oldLimbo);

        // then
        assertThat(result.getLocation(), equalTo(oldLocation));
        assertThat(result.isOperator(), equalTo(true));
        assertThat(result.getGroups(), contains(new UserGroup("grp-old")));
        assertThat(result.isCanFly(), equalTo(true));
        // speeds come from newLimbo (the more recent reading of the player's actual speed)
        assertThat(result.getWalkSpeed(), equalTo(0.0f));
        assertThat(result.getFlySpeed(), equalTo(0.0f));
    }

    @Test
    public void shouldFallBackToNewLimboForMissingData() {
        // given
        Location newLocation = mock(Location.class);
        LimboPlayer newLimbo = new LimboPlayer(newLocation, false, Collections.singletonList(new UserGroup("grp-new")), true, 0.3f, 0.0f);
        LimboPlayer oldLimbo = new LimboPlayer(null, false, Collections.emptyList(), false, 0.1f, 0.1f);

        // when
        LimboPlayer result = limboServiceHelper.merge(newLimbo, oldLimbo);

        // then
        assertThat(result.getLocation(), equalTo(newLocation));
        assertThat(result.isOperator(), equalTo(false));
        assertThat(result.getGroups(), contains(new UserGroup("grp-new")));
        assertThat(result.isCanFly(), equalTo(true));
        assertThat(result.getWalkSpeed(), equalTo(0.3f));
        // flySpeed comes from newLimbo (most recent reading)
        assertThat(result.getFlySpeed(), equalTo(0.0f));
    }

    @Test
    public void shouldMergeEnderPearlRestoreData() {
        // given
        World world = mock(World.class);
        Location oldLocation = new Location(world, 1.0, 2.0, 3.0);
        Location newLocation = new Location(world, 4.0, 5.0, 6.0);
        UUID sharedUuid = UUID.nameUUIDFromBytes("shared-pearl".getBytes());
        UUID oldUuid = UUID.nameUUIDFromBytes("old-pearl".getBytes());
        UUID newUuid = UUID.nameUUIDFromBytes("new-pearl".getBytes());

        LimboPlayer newLimbo = new LimboPlayer(null, false, Collections.emptyList(), false, 0.0f, 0.0f);
        newLimbo.setEnderPearls(java.util.Arrays.asList(
            new EnderPearlRestoreData(sharedUuid, newLocation, null),
            new EnderPearlRestoreData(newUuid, newLocation, null)));

        LimboPlayer oldLimbo = new LimboPlayer(null, false, Collections.emptyList(), false, 0.0f, 0.0f);
        oldLimbo.setEnderPearls(java.util.Arrays.asList(
            new EnderPearlRestoreData(sharedUuid, oldLocation, null),
            new EnderPearlRestoreData(oldUuid, oldLocation, null)));

        // when
        LimboPlayer result = limboServiceHelper.merge(newLimbo, oldLimbo);

        // then
        assertThat(result.getEnderPearlUuids(), containsInAnyOrder(sharedUuid, oldUuid, newUuid));
        java.util.Map<UUID, EnderPearlRestoreData> pearlsByUuid = result.getEnderPearls().stream()
            .collect(java.util.stream.Collectors.toMap(EnderPearlRestoreData::getUuid, pearl -> pearl));
        assertThat(pearlsByUuid.get(sharedUuid).getLocation(), equalTo(newLocation));
        assertThat(pearlsByUuid.get(oldUuid).getLocation(), equalTo(oldLocation));
        assertThat(pearlsByUuid.get(newUuid).getLocation(), equalTo(newLocation));
    }

    @Test
    public void shouldHandleNullInputs() {
        // given
        LimboPlayer limbo = mock(LimboPlayer.class);

        // when
        LimboPlayer result1 = limboServiceHelper.merge(limbo, null);
        LimboPlayer result2 = limboServiceHelper.merge(null, limbo);
        LimboPlayer result3 = limboServiceHelper.merge(null, null);

        // then
        verifyNoInteractions(limbo);
        assertThat(result1, equalTo(limbo));
        assertThat(result2, equalTo(limbo));
        assertThat(result3, nullValue());
    }
}


