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
import fr.xephi.authme.AuthMe;
import fr.xephi.authme.cache.auth.PlayerCache;
import fr.xephi.authme.settings.Settings;
import org.apache.commons.lang.reflect.MethodUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.logging.Level;

public class AuthMeInventoryPacketAdapter extends PacketAdapter {

    private static final int PLAYER_INVENTORY = 0;
    // http://wiki.vg/Inventory#Inventory (0-4 crafting, 5-8 armor, 9-35 main inventory, 36-44 hotbar, 45 off hand)
    // +1 because an index starts with 0
    private static final int CRAFTING_SIZE = 5;
    private static final int ARMOR_SIZE = 4;
    private static final int MAIN_SIZE = 27;
    private static final int HOTBAR_SIZE = 9;
    private static final int OFF_HAND_POSITION = 45;

    private final boolean offHandSupported = MethodUtils
            .getAccessibleMethod(PlayerInventory.class, "getItemInOffHand", new Class[]{}) != null;

    public AuthMeInventoryPacketAdapter(AuthMe plugin) {
        super(plugin, PacketType.Play.Server.SET_SLOT, PacketType.Play.Server.WINDOW_ITEMS);
    }

    @Override
    public void onPacketSending(PacketEvent packetEvent) {
        Player player = packetEvent.getPlayer();
        PacketContainer packet = packetEvent.getPacket();

        byte windowId = packet.getIntegers().read(0).byteValue();
        if (windowId == PLAYER_INVENTORY && Settings.protectInventoryBeforeLogInEnabled
            && !PlayerCache.getInstance().isAuthenticated(player.getName())) {
            packetEvent.setCancelled(true);
        }
    }

    public void register() {
        ProtocolLibrary.getProtocolManager().addPacketListener(this);
    }

    public void unregister() {
        ProtocolLibrary.getProtocolManager().removePacketListener(this);
    }

    public void sendBlankInventoryPacket(Player player) {
        ProtocolManager protocolManager = ProtocolLibrary.getProtocolManager();
        PacketContainer inventoryPacket = protocolManager.createPacket(PacketType.Play.Server.WINDOW_ITEMS);
        inventoryPacket.getIntegers().write(0, PLAYER_INVENTORY);
        int inventorySize = CRAFTING_SIZE + ARMOR_SIZE + MAIN_SIZE + HOTBAR_SIZE;

        ItemStack[] blankInventory = new ItemStack[inventorySize];
        Arrays.fill(blankInventory, new ItemStack(Material.AIR));
        inventoryPacket.getItemArrayModifier().write(0, blankInventory);

        try {
            protocolManager.sendServerPacket(player, inventoryPacket, false);
        } catch (InvocationTargetException invocationExc) {
            plugin.getLogger().log(Level.WARNING, "Error during sending blank inventory", invocationExc);
        }
    }
}
