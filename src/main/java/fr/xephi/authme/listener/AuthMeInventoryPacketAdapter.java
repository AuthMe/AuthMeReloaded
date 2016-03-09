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
package fr.xephi.authme.listener;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.reflect.MethodUtils;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.cache.auth.PlayerCache;
import fr.xephi.authme.settings.Settings;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Collections;
import java.util.logging.Level;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

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

    public void sendInventoryPacket(Player player) {
        ProtocolManager protocolManager = ProtocolLibrary.getProtocolManager();
        PacketContainer inventoryPacket = protocolManager.createPacket(PacketType.Play.Server.WINDOW_ITEMS);

        // we are sending our own inventory
        inventoryPacket.getIntegers().write(0, PLAYER_INVENTORY);

        ItemStack[] playerCrafting = new ItemStack[CRAFTING_SIZE];
        Arrays.fill(playerCrafting, new ItemStack(Material.AIR));
        ItemStack[] armorContents = player.getInventory().getArmorContents();
        ItemStack[] mainInventory = player.getInventory().getContents();

        // bukkit saves the armor in reversed order
        Collections.reverse(Arrays.asList(armorContents));

        // same main inventory. The hotbar is at the beginning but it should be at the end of the array
        ItemStack[] hotbar = Arrays.copyOfRange(mainInventory, 0, HOTBAR_SIZE);
        ItemStack[] storedInventory = Arrays.copyOfRange(mainInventory, HOTBAR_SIZE, mainInventory.length);

        // concat all parts of the inventory together
        int inventorySize = CRAFTING_SIZE + ARMOR_SIZE + MAIN_SIZE + HOTBAR_SIZE;
        if (offHandSupported) {
            inventorySize++;
        }

        ItemStack[] completeInventory = new ItemStack[inventorySize];

        System.arraycopy(playerCrafting, 0, completeInventory, 0, playerCrafting.length);
        System.arraycopy(armorContents, 0, completeInventory, CRAFTING_SIZE, armorContents.length);

        // storedInventory and hotbar
        System.arraycopy(storedInventory, 0, completeInventory, CRAFTING_SIZE + ARMOR_SIZE, storedInventory.length);
        System.arraycopy(hotbar, 0, completeInventory, CRAFTING_SIZE + ARMOR_SIZE + MAIN_SIZE, hotbar.length);

        if (offHandSupported) {
            completeInventory[OFF_HAND_POSITION] = player.getInventory().getItemInOffHand();
        }

        inventoryPacket.getItemArrayModifier().write(0, completeInventory);
        try {
            protocolManager.sendServerPacket(player, inventoryPacket, false);
        } catch (InvocationTargetException invocationExc) {
            plugin.getLogger().log(Level.WARNING, "Error during inventory recovery", invocationExc);
        }
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
