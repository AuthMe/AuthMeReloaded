package fr.xephi.authme.platform;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class AbstractSpigotPlatformAdapterTest {

    @Test
    void shouldUseBedSpawnLocationByDefault() {
        // given
        AbstractSpigotPlatformAdapter adapter = new AbstractSpigotPlatformAdapter() {
            @Override
            public String getPlatformName() {
                return "test";
            }
        };
        Player player = mock(Player.class);
        World world = mock(World.class);
        Location bedSpawn = new Location(world, 10.0, 64.0, -2.0);
        given(player.getBedSpawnLocation()).willReturn(bedSpawn);

        // when
        Location result = adapter.getPlayerRespawnLocation(player);

        // then
        assertThat(result, equalTo(bedSpawn));
    }
}
