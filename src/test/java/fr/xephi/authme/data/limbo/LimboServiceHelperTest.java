package fr.xephi.authme.data.limbo;

import org.bukkit.Location;
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
import static org.mockito.Mockito.verifyZeroInteractions;

/**
 * Test for {@link LimboServiceHelper}.
 * <p>
 * Note: some methods are tested directly where they are used via {@link LimboServiceTest}.
 */
@RunWith(MockitoJUnitRunner.class)
public class LimboServiceHelperTest {

    @InjectMocks
    private LimboServiceHelper limboServiceHelper;

    @Test
    public void shouldMergeLimboPlayers() {
        // given
        Location newLocation = mock(Location.class);
        LimboPlayer newLimbo = new LimboPlayer(newLocation, false, Collections.singletonList("grp-new"), false);
        Location oldLocation = mock(Location.class);
        LimboPlayer oldLimbo = new LimboPlayer(oldLocation, true, Collections.singletonList("grp-old"), true);

        // when
        LimboPlayer result = limboServiceHelper.merge(newLimbo, oldLimbo);

        // then
        assertThat(result.getLocation(), equalTo(oldLocation));
        assertThat(result.isOperator(), equalTo(true));
        assertThat(result.getGroups(), contains("grp-old"));
        assertThat(result.isCanFly(), equalTo(true));
    }

    @Test
    public void shouldFallBackToNewLimboForMissingData() {
        // given
        Location newLocation = mock(Location.class);
        LimboPlayer newLimbo = new LimboPlayer(newLocation, false, Collections.singletonList("grp-new"), true);
        LimboPlayer oldLimbo = new LimboPlayer(null, false, Collections.emptyList(), false);

        // when
        LimboPlayer result = limboServiceHelper.merge(newLimbo, oldLimbo);

        // then
        assertThat(result.getLocation(), equalTo(newLocation));
        assertThat(result.isOperator(), equalTo(false));
        assertThat(result.getGroups(), contains("grp-new"));
        assertThat(result.isCanFly(), equalTo(true));
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
        verifyZeroInteractions(limbo);
        assertThat(result1, equalTo(limbo));
        assertThat(result2, equalTo(limbo));
        assertThat(result3, nullValue());
    }
}
