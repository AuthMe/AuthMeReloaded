package fr.xephi.authme.data.limbo;

import fr.xephi.authme.TestHelper;
import org.bukkit.Location;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collections;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Test for {@link LimboServiceHelper}.
 * <p>
 * Note: some methods are tested directly where they are used via {@link LimboServiceTest}.
 */
@RunWith(MockitoJUnitRunner.class)
public class LimboServiceHelperTest {

    @InjectMocks
    private LimboServiceHelper limboServiceHelper;

    @BeforeClass
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
        assertThat(result.getWalkSpeed(), equalTo(0.1f));
        assertThat(result.getFlySpeed(), equalTo(0.8f));
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
        assertThat(result.getFlySpeed(), equalTo(0.1f));
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
