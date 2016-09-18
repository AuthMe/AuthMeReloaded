package fr.xephi.authme.util;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.ReflectionTestUtils;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.PluginSettings;
import org.bukkit.entity.Player;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Collection;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Test for {@link BukkitService}.
 */
@RunWith(MockitoJUnitRunner.class)
public class BukkitServiceTest {

    @Mock
    private AuthMe authMe;
    @Mock
    private Settings settings;

    /**
     * Checks that {@link BukkitService#getOnlinePlayersIsCollection} is initialized to {@code true} on startup;
     * the test scope is configured with a Bukkit implementation that returns a Collection and not an array.
     */
    @Test
    public void shouldHavePlayerListAsCollectionMethod() {
        // given
        given(settings.getProperty(PluginSettings.USE_ASYNC_TASKS)).willReturn(true);
        BukkitService bukkitService = new BukkitService(authMe, settings);

        // when
        boolean doesMethodReturnCollection = ReflectionTestUtils
            .getFieldValue(BukkitService.class, bukkitService, "getOnlinePlayersIsCollection");

        // then
        assertThat(doesMethodReturnCollection, equalTo(true));
    }

    @Test
    public void shouldRetrieveListOfOnlinePlayersFromReflectedMethod() {
        // given
        given(settings.getProperty(PluginSettings.USE_ASYNC_TASKS)).willReturn(true);
        BukkitService bukkitService = new BukkitService(authMe, settings);
        ReflectionTestUtils.setField(BukkitService.class, bukkitService, "getOnlinePlayersIsCollection", false);
        ReflectionTestUtils.setField(BukkitService.class, bukkitService, "getOnlinePlayers",
            ReflectionTestUtils.getMethod(BukkitServiceTest.class, "onlinePlayersImpl"));

        // when
        Collection<? extends Player> players = bukkitService.getOnlinePlayers();

        // then
        assertThat(players, hasSize(2));
    }

    // Note: This method is used through reflections
    public static Player[] onlinePlayersImpl() {
        return new Player[]{
            mock(Player.class), mock(Player.class)
        };
    }

}
