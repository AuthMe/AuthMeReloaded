package fr.xephi.authme.service;


import org.bukkit.GameMode;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Test for {@link CommonService}.
 */
@RunWith(MockitoJUnitRunner.class)
public class SpectateLoginServiceTest {
    private SpectateLoginService spectateLoginService;

    @Mock
    private Player mockPlayer;

    @Mock
    private ArmorStand mockArmorStand;

    @Before
    public void setUp() {
        spectateLoginService = new SpectateLoginService();
    }

    @Test
    public void testCreateStand() {
        when(mockPlayer.isDead()).thenReturn(false);

        spectateLoginService.createStand(mockPlayer);

        verify(mockPlayer).setGameMode(GameMode.SPECTATOR);

        verify(mockPlayer).setSpectatorTarget(mockArmorStand);
    }

    @Test
    public void testCreateStandPlayerDead() {
        when(mockPlayer.isDead()).thenReturn(true);

        spectateLoginService.createStand(mockPlayer);

        verify(mockPlayer, never()).setGameMode(any());
        verify(mockPlayer, never()).setSpectatorTarget(any());
    }

    @Test
    public void testUpdateTarget() {
        spectateLoginService.createStand(mockPlayer);

        spectateLoginService.updateTarget(mockPlayer);

        verify(mockPlayer).setSpectatorTarget(mockArmorStand);
    }

    @Test
    public void testUpdateTargetNoStand() {
        when(mockPlayer.isDead()).thenReturn(false); // Ensure the player is not dead

        spectateLoginService.updateTarget(mockPlayer);

        verify(mockPlayer, never()).setSpectatorTarget(any());
    }

    @Test
    public void testRemoveStand() {
        spectateLoginService.createStand(mockPlayer);

        spectateLoginService.removeStand(mockPlayer);

        verify(mockArmorStand).remove();
        verify(mockPlayer).setSpectatorTarget(null);
        verify(mockPlayer).setGameMode(GameMode.SURVIVAL);
    }

    @Test
    public void testRemoveStandNoStand() {
        when(mockPlayer.isDead()).thenReturn(false);

        spectateLoginService.removeStand(mockPlayer);

        verify(mockArmorStand, never()).remove();
        verify(mockPlayer, never()).setSpectatorTarget(any());
        verify(mockPlayer, never()).setGameMode(any());
    }

    @Test
    public void testHasStand() {
        spectateLoginService.createStand(mockPlayer);

        assertTrue(spectateLoginService.hasStand(mockPlayer));
    }

    @Test
    public void testHasStandNoStand() {
        when(mockPlayer.isDead()).thenReturn(false);

        assertFalse(spectateLoginService.hasStand(mockPlayer));
    }
}
