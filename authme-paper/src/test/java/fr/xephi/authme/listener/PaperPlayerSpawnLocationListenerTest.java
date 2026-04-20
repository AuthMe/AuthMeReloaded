package fr.xephi.authme.listener;

import com.destroystokyo.paper.profile.PlayerProfile;
import fr.xephi.authme.service.BukkitService;
import fr.xephi.authme.service.TeleportationService;
import io.papermc.paper.connection.PlayerConfigurationConnection;
import io.papermc.paper.event.player.AsyncPlayerSpawnLocationEvent;
import org.bukkit.Location;
import org.bukkit.World;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;

import java.util.function.Supplier;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.WARN)
public class PaperPlayerSpawnLocationListenerTest {

    @InjectMocks
    private PaperPlayerSpawnLocationListener listener;

    @Mock
    private BukkitService bukkitService;

    @Mock
    private TeleportationService teleportationService;

    @Test
    public void shouldApplyCustomSpawnLocationFromPaperAsyncEvent() {
        // given
        doAnswer(invocation -> {
            Supplier<?> supplier = invocation.getArgument(0);
            return supplier.get();
        }).when(bukkitService).callSyncMethodFromOptionallyAsyncTask(any());

        World world = mock(World.class);
        Location originalSpawn = new Location(world, 1.0, 64.0, 1.0);
        Location customSpawn = new Location(world, 10.0, 70.0, 10.0);
        PlayerProfile profile = mock(PlayerProfile.class);
        given(profile.getName()).willReturn("Bobby");
        PlayerConfigurationConnection connection = mock(PlayerConfigurationConnection.class);
        given(connection.getProfile()).willReturn(profile);
        given(teleportationService.prepareOnJoinSpawnLocation("Bobby", originalSpawn)).willReturn(customSpawn);

        AsyncPlayerSpawnLocationEvent event = new AsyncPlayerSpawnLocationEvent(connection, originalSpawn, false);

        // when
        listener.onPlayerSpawn(event);

        // then
        verify(teleportationService).prepareOnJoinSpawnLocation("Bobby", originalSpawn);
        assertThat(event.getSpawnLocation(), is(customSpawn));
    }
}
