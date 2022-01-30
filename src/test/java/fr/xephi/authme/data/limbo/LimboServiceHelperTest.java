package fr.xephi.authme.data.limbo;

import fr.xephi.authme.TestHelper;
import org.bukkit.Location;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Test for {@link LimboServiceHelper}.
 * <p>
 * Note: some methods are tested directly where they are used via {@link LimboServiceTest}.
 */
@ExtendWith(MockitoExtension.class)
class LimboServiceHelperTest {

    @InjectMocks
    private LimboServiceHelper limboServiceHelper;

    @BeforeAll
    static void initLogger() {
        TestHelper.setupLogger();
    }

    @Test
    void shouldMergeLimboPlayers() {
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
        assertThat(result.getWalkSpeed(), equalTo(0.1f));
        assertThat(result.getFlySpeed(), equalTo(0.8f));
    }

    @Test
    void shouldFallBackToNewLimboForMissingData() {
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
        assertThat(result.getFlySpeed(), equalTo(0.1f));
    }

    @Test
    void shouldHandleNullInputs() {
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
