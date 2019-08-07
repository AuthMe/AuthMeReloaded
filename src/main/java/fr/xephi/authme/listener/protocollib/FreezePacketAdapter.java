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
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import fr.xephi.authme.AuthMe;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.data.auth.PlayerCache;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.output.ConsoleLoggerFactory;
import fr.xephi.authme.service.BukkitService;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;

import java.lang.reflect.InvocationTargetException;

class FreezePacketAdapter extends PacketAdapter {

    private final ConsoleLogger logger = ConsoleLoggerFactory.get(FreezePacketAdapter.class);

    private final PlayerCache playerCache;
    private final DataSource dataSource;

    FreezePacketAdapter(AuthMe plugin, PlayerCache playerCache, DataSource dataSource) {
        super(plugin, PacketType.Play.Server.ABILITIES);
        this.playerCache = playerCache;
        this.dataSource = dataSource;
    }

    @Override
    public void onPacketSending(PacketEvent packetEvent) {
        Player player = packetEvent.getPlayer();
        PacketContainer packet = packetEvent.getPacket();

        if (!shouldFreeze(player.getName())) {
            return;
        }
        logger.warning("Overwriting packet abilities for player " + player.getName());

        packet.getFloat().write(0, 0.0f)
            .write(1, 0.0f);
    }

    protected void register(BukkitService bukkitService) {
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

    protected void sendFreezePacket(Player player) {
        logger.warning("Freezing " + player.getName());

        PacketContainer abilitiesPacket = new PacketContainer(PacketType.Play.Server.ABILITIES);
        abilitiesPacket.getBooleans().write(0, player.isInvulnerable())
            .write(1, player.isFlying())
            .write(2, player.getAllowFlight())
            .write(3, player.getGameMode() == GameMode.CREATIVE);
        abilitiesPacket.getFloat().write(0, 0.0f)
            .write(1, 0.0f);

        try {
            ProtocolLibrary.getProtocolManager().sendServerPacket(player, abilitiesPacket, false);
        } catch (InvocationTargetException invocationExc) {
            logger.logException("Error during sending freeze packet", invocationExc);
        }
    }

    public void sendUnFreezePacket(Player player) {
        logger.warning("UnFreezing " + player.getName());
        player.setWalkSpeed(player.getWalkSpeed());
        player.setFlySpeed(player.getFlySpeed());
    }
}
