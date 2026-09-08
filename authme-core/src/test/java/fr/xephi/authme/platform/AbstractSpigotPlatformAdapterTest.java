package fr.xephi.authme.platform;

import fr.xephi.authme.data.auth.PlayerCache;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.listener.BlockListener;
import fr.xephi.authme.listener.EntityAirChangeListener;
import fr.xephi.authme.listener.EntityListener;
import fr.xephi.authme.listener.EntityPickupItemListener;
import fr.xephi.authme.listener.LegacyPlayerLoginListener;
import fr.xephi.authme.listener.LegacyPlayerPickupItemListener;
import fr.xephi.authme.listener.LegacyPlayerSpawnLocationListener;
import fr.xephi.authme.listener.PlayerListener;
import fr.xephi.authme.listener.PlayerSwapHandItemsListener;
import fr.xephi.authme.listener.ServerListener;
import fr.xephi.authme.service.BukkitService;
import fr.xephi.authme.service.PendingPremiumCache;
import fr.xephi.authme.service.PremiumLoginVerifier;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
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

    // --- Listener selection matrix tests ---

    @Test
    void shouldReturnLegacyOnlyListenersFor18Capabilities() {
        // 1.8: no optional API classes available
        AbstractSpigotPlatformAdapter adapter = new CapabilityControlledAdapter(Collections.emptySet());
        List<Class<? extends Listener>> listeners = adapter.getListeners();

        assertThat(listeners, containsInAnyOrder(
            PlayerListener.class,
            BlockListener.class,
            EntityListener.class,
            ServerListener.class,
            LegacyPlayerLoginListener.class,
            LegacyPlayerPickupItemListener.class));

        // verify no modern/optional listeners present
        assertThat(listeners, not(hasItem(LegacyPlayerSpawnLocationListener.class)));
        assertThat(listeners, not(hasItem(PlayerSwapHandItemsListener.class)));
        assertThat(listeners, not(hasItem(EntityAirChangeListener.class)));
        assertThat(listeners, not(hasItem(EntityPickupItemListener.class)));
    }

    @Test
    void shouldIncludeSpawnLocationAndSwapHandFor19Capabilities() {
        // 1.9-1.10: spawn-location + swap-hand available, but not air-change or modern pickup
        Set<String> caps = new HashSet<>();
        caps.add("org.spigotmc.event.player.PlayerSpawnLocationEvent");
        caps.add("org.bukkit.event.player.PlayerSwapHandItemsEvent");
        AbstractSpigotPlatformAdapter adapter = new CapabilityControlledAdapter(caps);
        List<Class<? extends Listener>> listeners = adapter.getListeners();

        assertThat(listeners, containsInAnyOrder(
            PlayerListener.class,
            BlockListener.class,
            EntityListener.class,
            ServerListener.class,
            LegacyPlayerLoginListener.class,
            LegacyPlayerPickupItemListener.class,
            LegacyPlayerSpawnLocationListener.class,
            PlayerSwapHandItemsListener.class));

        assertThat(listeners, not(hasItem(EntityAirChangeListener.class)));
        assertThat(listeners, not(hasItem(EntityPickupItemListener.class)));
    }

    @Test
    void shouldIncludeAirChangeFor111Capabilities() {
        // 1.11: adds air-change on top of 1.9 set
        Set<String> caps = new HashSet<>();
        caps.add("org.spigotmc.event.player.PlayerSpawnLocationEvent");
        caps.add("org.bukkit.event.player.PlayerSwapHandItemsEvent");
        caps.add("org.bukkit.event.entity.EntityAirChangeEvent");
        AbstractSpigotPlatformAdapter adapter = new CapabilityControlledAdapter(caps);
        List<Class<? extends Listener>> listeners = adapter.getListeners();

        assertThat(listeners, containsInAnyOrder(
            PlayerListener.class,
            BlockListener.class,
            EntityListener.class,
            ServerListener.class,
            LegacyPlayerLoginListener.class,
            LegacyPlayerPickupItemListener.class,
            LegacyPlayerSpawnLocationListener.class,
            PlayerSwapHandItemsListener.class,
            EntityAirChangeListener.class));

        assertThat(listeners, not(hasItem(EntityPickupItemListener.class)));
    }

    @Test
    void shouldUseModernPickupInsteadOfLegacyFor112Plus() {
        // 1.12+: all optional APIs available -> modern pickup replaces legacy
        Set<String> caps = new HashSet<>();
        caps.add("org.spigotmc.event.player.PlayerSpawnLocationEvent");
        caps.add("org.bukkit.event.player.PlayerSwapHandItemsEvent");
        caps.add("org.bukkit.event.entity.EntityAirChangeEvent");
        caps.add("org.bukkit.event.entity.EntityPickupItemEvent");
        AbstractSpigotPlatformAdapter adapter = new CapabilityControlledAdapter(caps);
        List<Class<? extends Listener>> listeners = adapter.getListeners();

        assertThat(listeners, containsInAnyOrder(
            PlayerListener.class,
            BlockListener.class,
            EntityListener.class,
            ServerListener.class,
            LegacyPlayerLoginListener.class,
            LegacyPlayerSpawnLocationListener.class,
            PlayerSwapHandItemsListener.class,
            EntityAirChangeListener.class,
            EntityPickupItemListener.class));

        assertThat(listeners, not(hasItem(LegacyPlayerPickupItemListener.class)));
    }

    @Test
    void shouldNotIncludeLegacyPickupWhenModernPickupAvailable() {
        // Edge: only modern pickup available, no other optional APIs
        Set<String> caps = new HashSet<>();
        caps.add("org.bukkit.event.entity.EntityPickupItemEvent");
        AbstractSpigotPlatformAdapter adapter = new CapabilityControlledAdapter(caps);
        List<Class<? extends Listener>> listeners = adapter.getListeners();

        assertThat(listeners, hasItem(EntityPickupItemListener.class));
        assertThat(listeners, not(hasItem(LegacyPlayerPickupItemListener.class)));
    }

    private static final class CapabilityControlledAdapter extends AbstractSpigotPlatformAdapter {
        private final Set<String> available;

        CapabilityControlledAdapter(Set<String> available) {
            this.available = new HashSet<>(available);
        }

        @Override
        public String getPlatformName() {
            return "test-capability";
        }

        @Override
        protected boolean isClassAvailable(String className) {
            return available.contains(className);
        }
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
                                                PendingPremiumCache pendingPremiumCache, BukkitService bukkitService) {
        }

        @Override
        public void unregisterPremiumVerification() {
        }
    }
}
