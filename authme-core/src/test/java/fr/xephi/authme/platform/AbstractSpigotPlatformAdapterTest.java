package fr.xephi.authme.platform;

import fr.xephi.authme.data.auth.PlayerCache;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.service.PendingPremiumCache;
import fr.xephi.authme.service.PremiumLoginVerifier;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

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

    @Test
    void shouldCreatePacketInterceptionAdapterLazilyAndReuseIt() {
        TrackingPacketInterceptionAdapter packetInterceptionAdapter = new TrackingPacketInterceptionAdapter();
        TestSpigotPlatformAdapter adapter = new TestSpigotPlatformAdapter(packetInterceptionAdapter);
        PlayerCache playerCache = mock(PlayerCache.class);
        DataSource dataSource = mock(DataSource.class);
        Player player = mock(Player.class);

        adapter.sendBlankInventoryPacket(player);
        adapter.unregisterInventoryProtection();
        adapter.unregisterTabCompleteBlock();

        assertThat(adapter.createPacketInterceptionAdapterCalls, is(0));
        verifyNoInteractions(playerCache, dataSource);

        adapter.registerInventoryProtection(playerCache, dataSource);
        adapter.sendBlankInventoryPacket(player);
        adapter.registerTabCompleteBlock(playerCache);
        adapter.unregisterInventoryProtection();
        adapter.unregisterTabCompleteBlock();

        assertThat(adapter.createPacketInterceptionAdapterCalls, is(1));
        assertThat(packetInterceptionAdapter.inventoryProtectionRegistrations, is(1));
        assertThat(packetInterceptionAdapter.blankInventoryPacketsSent, is(1));
        assertThat(packetInterceptionAdapter.tabCompleteRegistrations, is(1));
        assertThat(packetInterceptionAdapter.inventoryProtectionUnregistrations, is(1));
        assertThat(packetInterceptionAdapter.tabCompleteUnregistrations, is(1));
    }

    private static final class TestSpigotPlatformAdapter extends AbstractSpigotPlatformAdapter {

        private final PacketInterceptionAdapter packetInterceptionAdapter;
        private int createPacketInterceptionAdapterCalls;

        private TestSpigotPlatformAdapter(PacketInterceptionAdapter packetInterceptionAdapter) {
            this.packetInterceptionAdapter = packetInterceptionAdapter;
        }

        @Override
        public String getPlatformName() {
            return "test";
        }

        @Override
        protected PacketInterceptionAdapter createPacketInterceptionAdapter() {
            createPacketInterceptionAdapterCalls++;
            return packetInterceptionAdapter;
        }
    }

    private static final class TrackingPacketInterceptionAdapter implements PacketInterceptionAdapter {

        private int inventoryProtectionRegistrations;
        private int inventoryProtectionUnregistrations;
        private int blankInventoryPacketsSent;
        private int tabCompleteRegistrations;
        private int tabCompleteUnregistrations;

        @Override
        public void registerInventoryProtection(PlayerCache playerCache, DataSource dataSource) {
            inventoryProtectionRegistrations++;
        }

        @Override
        public void unregisterInventoryProtection() {
            inventoryProtectionUnregistrations++;
        }

        @Override
        public void sendBlankInventoryPacket(Player player) {
            blankInventoryPacketsSent++;
        }

        @Override
        public void registerTabCompleteBlock(PlayerCache playerCache) {
            tabCompleteRegistrations++;
        }

        @Override
        public void unregisterTabCompleteBlock() {
            tabCompleteUnregistrations++;
        }

        @Override
        public void registerPremiumVerification(DataSource dataSource, PremiumLoginVerifier verifier,
                                                PendingPremiumCache pendingPremiumCache) {
        }

        @Override
        public void unregisterPremiumVerification() {
        }
    }
}
