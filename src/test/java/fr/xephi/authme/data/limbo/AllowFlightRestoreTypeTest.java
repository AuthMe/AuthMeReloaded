package fr.xephi.authme.data.limbo;

import org.bukkit.entity.Player;
import org.junit.Test;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Test for {@link AllowFlightRestoreType}.
 */
public class AllowFlightRestoreTypeTest {

    @Test
    public void shouldRestoreValue() {
        // given
        LimboPlayer limboWithFly = newLimboWithAllowFlight(true);
        LimboPlayer limboWithoutFly = newLimboWithAllowFlight(false);
        Player player1 = mock(Player.class);
        Player player2 = mock(Player.class);

        // when
        AllowFlightRestoreType.RESTORE.restoreAllowFlight(player1, limboWithFly);
        AllowFlightRestoreType.RESTORE.restoreAllowFlight(player2, limboWithoutFly);

        // then
        verify(player1).setAllowFlight(true);
        verify(player2).setAllowFlight(false);
    }

    @Test
    public void shouldEnableFlight() {
        // given
        LimboPlayer limboWithFly = newLimboWithAllowFlight(true);
        LimboPlayer limboWithoutFly = newLimboWithAllowFlight(false);
        Player player1 = mock(Player.class);
        Player player2 = mock(Player.class);

        // when
        AllowFlightRestoreType.ENABLE.restoreAllowFlight(player1, limboWithFly);
        AllowFlightRestoreType.ENABLE.restoreAllowFlight(player2, limboWithoutFly);

        // then
        verify(player1).setAllowFlight(true);
        verify(player2).setAllowFlight(true);
    }


    @Test
    public void shouldDisableFlight() {
        // given
        LimboPlayer limboWithFly = newLimboWithAllowFlight(true);
        LimboPlayer limboWithoutFly = newLimboWithAllowFlight(false);
        Player player1 = mock(Player.class);
        Player player2 = mock(Player.class);

        // when
        AllowFlightRestoreType.DISABLE.restoreAllowFlight(player1, limboWithFly);
        AllowFlightRestoreType.DISABLE.restoreAllowFlight(player2, limboWithoutFly);

        // then
        verify(player1).setAllowFlight(false);
        verify(player2).setAllowFlight(false);
    }

    private static LimboPlayer newLimboWithAllowFlight(boolean allowFlight) {
        LimboPlayer limbo = mock(LimboPlayer.class);
        given(limbo.isCanFly()).willReturn(allowFlight);
        return limbo;
    }
}
