/*
 * Copyright (C) 2015 AuthMe-Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package fr.xephi.authme.listener.protocollib;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.reflect.StructureModifier;
import fr.xephi.authme.AuthMe;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.data.auth.PlayerCache;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.output.ConsoleLoggerFactory;
import fr.xephi.authme.service.BukkitService;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.List;

class InventoryPacketAdapter extends PacketAdapter {

    private static final int PLAYER_INVENTORY = 0;
    // http://wiki.vg/Inventory#Inventory (0-4 crafting, 5-8 armor, 9-35 main inventory, 36-44 hotbar, 45 off hand)
    // +1 because an index starts with 0
    private static final int CRAFTING_SIZE = 5;
    private static final int ARMOR_SIZE = 4;
    private static final int MAIN_SIZE = 27;
    private static final int HOTBAR_SIZE = 9;

    private final ConsoleLogger logger = ConsoleLoggerFactory.get(InventoryPacketAdapter.class);
    private final PlayerCache playerCache;
    private final DataSource dataSource;

    InventoryPacketAdapter(AuthMe plugin, PlayerCache playerCache, DataSource dataSource) {
        super(plugin, PacketType.Play.Server.SET_SLOT, PacketType.Play.Server.WINDOW_ITEMS);
        this.playerCache = playerCache;
        this.dataSource = dataSource;
    }

    @Override
    public void onPacketSending(PacketEvent packetEvent) {
        Player player = packetEvent.getPlayer();
        PacketContainer packet = packetEvent.getPacket();

        int windowId = packet.getIntegers().read(0);
        if (windowId == PLAYER_INVENTORY && shouldHideInventory(player.getName())) {
            packetEvent.setCancelled(true);
        }
    }

    /**
     * Registers itself to ProtocolLib and blanks out the inventory packet to any applicable players.
     *
     * @param bukkitService the bukkit service (for retrieval of online players)
     */
    public void register(BukkitService bukkitService) {
        ProtocolLibrary.getProtocolManager().addPacketListener(this);

        bukkitService.getOnlinePlayers().stream()
            .filter(player -> shouldHideInventory(player.getName()))
            .forEach(this::sendBlankInventoryPacket);
    }

    private boolean shouldHideInventory(String playerName) {
        return !playerCache.isAuthenticated(playerName) && dataSource.isAuthAvailable(playerName);
    }

    public void unregister() {
        ProtocolLibrary.getProtocolManager().removePacketListener(this);
    }

    /**
     * Sends a blanked out packet to the given player in order to hide the inventory.
     *
     * @param player the player to send the blank inventory packet to
     */
    public void sendBlankInventoryPacket(Player player) {
        ProtocolManager protocolManager = ProtocolLibrary.getProtocolManager();
        PacketContainer inventoryPacket = protocolManager.createPacket(PacketType.Play.Server.WINDOW_ITEMS);
        inventoryPacket.getIntegers().write(0, PLAYER_INVENTORY);
        int inventorySize = CRAFTING_SIZE + ARMOR_SIZE + MAIN_SIZE + HOTBAR_SIZE;

        ItemStack[] blankInventory = new ItemStack[inventorySize];
        Arrays.fill(blankInventory, new ItemStack(Material.AIR));

        //old minecraft versions
        StructureModifier<ItemStack[]> itemArrayModifier = inventoryPacket.getItemArrayModifier();
        if (itemArrayModifier.size() > 0) {
            itemArrayModifier.write(0, blankInventory);
        } else {
            //minecraft versions above 1.11
            StructureModifier<List<ItemStack>> itemListModifier = inventoryPacket.getItemListModifier();
            itemListModifier.write(0, Arrays.asList(blankInventory));
        }

        try {
            protocolManager.sendServerPacket(player, inventoryPacket, false);
        } catch (InvocationTargetException invocationExc) {
            logger.logException("Error during sending blank inventory", invocationExc);
        }
    }
}
