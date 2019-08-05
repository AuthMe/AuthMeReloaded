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
import com.comphenix.protocol.wrappers.WrappedAttribute;
import fr.xephi.authme.AuthMe;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.data.auth.PlayerCache;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.service.BukkitService;
import org.bukkit.entity.Player;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

class FreezePacketAdapter extends PacketAdapter {

    private final PlayerCache playerCache;
    private final DataSource dataSource;

    FreezePacketAdapter(AuthMe plugin, PlayerCache playerCache, DataSource dataSource) {
        super(plugin, PacketType.Play.Server.UPDATE_ATTRIBUTES);
        this.playerCache = playerCache;
        this.dataSource = dataSource;
    }

    @Override
    public void onPacketSending(PacketEvent packetEvent) {
        Player player = packetEvent.getPlayer();
        PacketContainer packet = packetEvent.getPacket();

        int entityId = packet.getIntegers().read(0);
        if (entityId != player.getEntityId()) {
            return;
        }

        if (!shouldFreeze(player.getName())) {
            return;
        }

        List<WrappedAttribute> newAttributes = new ArrayList<>();
        for (WrappedAttribute attribute : packet.getAttributeCollectionModifier().read(0)) {
            if ("generic.movementSpeed".equals(attribute.getAttributeKey()) ||
                "generic.flyingSpeed".equals(attribute.getAttributeKey())) {
                newAttributes.add(WrappedAttribute.newBuilder(attribute)
                    .baseValue(0.0f).modifiers(Collections.emptyList()).build());
            } else {
                newAttributes.add(attribute);
            }
        }
        packet.getAttributeCollectionModifier().write(0, newAttributes);
    }

    public void register(BukkitService bukkitService) {
        ProtocolLibrary.getProtocolManager().addPacketListener(this);

        bukkitService.getOnlinePlayers().stream()
            .filter(player -> shouldFreeze(player.getName()))
            .forEach(this::sendFreezePacket);
    }

    private boolean shouldFreeze(String playerName) {
        return !playerCache.isAuthenticated(playerName) && dataSource.isAuthAvailable(playerName);
    }

    public void unregister() {
        ProtocolLibrary.getProtocolManager().removePacketListener(this);
    }

    public void sendFreezePacket(Player player) {
        ProtocolManager protocolManager = ProtocolLibrary.getProtocolManager();
        PacketContainer attributesPacket = protocolManager.createPacket(PacketType.Play.Server.UPDATE_ATTRIBUTES);

        attributesPacket.getIntegers().write(0, player.getEntityId());
        attributesPacket.getAttributeCollectionModifier().write(0, Arrays.asList(
            WrappedAttribute.newBuilder()
                .packet(attributesPacket)
                .attributeKey("generic.movementSpeed")
                .baseValue(0.0f)
                .build(),
            WrappedAttribute.newBuilder()
                .packet(attributesPacket)
                .attributeKey("generic.flyingSpeed")
                .baseValue(0.0f)
                .build()
        ));

        try {
            protocolManager.sendServerPacket(player, attributesPacket, false);
        } catch (InvocationTargetException invocationExc) {
            ConsoleLogger.logException("Error during sending freeze packet", invocationExc);
        }
    }

    public void sendUnFreezePacket(Player player) {
        player.setWalkSpeed(player.getWalkSpeed());
        player.setFlySpeed(player.getFlySpeed());
    }
}
