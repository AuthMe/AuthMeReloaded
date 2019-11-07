package fr.xephi.authme.data.limbo;

import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Test for {@link AllowFlightRestoreType}.
 */
class AllowFlightRestoreTypeTest {

    @Test
    void shouldRestoreValue() {
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
    void shouldEnableFlight() {
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
    void shouldDisableFlight() {
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

    @Test
    void shouldNotInteractWithPlayer() {
        // given
        LimboPlayer limboWithFly = newLimboWithAllowFlight(true);
        LimboPlayer limboWithoutFly = newLimboWithAllowFlight(false);
        Player player1 = mock(Player.class);
        Player player2 = mock(Player.class);

        // when
        AllowFlightRestoreType.NOTHING.restoreAllowFlight(player1, limboWithFly);
        AllowFlightRestoreType.NOTHING.restoreAllowFlight(player2, limboWithoutFly);

        // then
        verifyNoInteractions(player1, player2);
    }

    @Test
    void shouldRemoveFlightExceptForNothingType() {
        // given
        AllowFlightRestoreType noInteractionType = AllowFlightRestoreType.NOTHING;

        for (AllowFlightRestoreType type : AllowFlightRestoreType.values()) {
            Player player = mock(Player.class);

            // when
            type.processPlayer(player);

            // then
            if (type == noInteractionType) {
                verifyNoInteractions(player);
            } else {
                verify(player).setAllowFlight(false);
            }
        }
    }

    private static LimboPlayer newLimboWithAllowFlight(boolean allowFlight) {
        LimboPlayer limbo = mock(LimboPlayer.class);
        given(limbo.isCanFly()).willReturn(allowFlight);
        return limbo;
    }
}
