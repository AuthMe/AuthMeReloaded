package fr.xephi.authme.listener;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.reflect.FieldAccessException;
import com.comphenix.protocol.wrappers.EnumWrappers.NativeGameMode;
import com.comphenix.protocol.wrappers.EnumWrappers.PlayerInfoAction;
import com.comphenix.protocol.wrappers.PlayerInfoData;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.comphenix.protocol.wrappers.WrappedGameProfile;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.cache.auth.PlayerCache;
import fr.xephi.authme.util.Utils;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.logging.Level;

import org.bukkit.entity.Player;

public class AuthMeTablistPacketAdapter extends PacketAdapter {

    public AuthMeTablistPacketAdapter(AuthMe plugin) {
        super(plugin, ListenerPriority.NORMAL, PacketType.Play.Server.PLAYER_INFO);
    }

    @Override
    public void onPacketSending(PacketEvent event) {
        if (event.getPacketType() == PacketType.Play.Server.PLAYER_INFO) {
            //this hides the tablist for the new joining players. Already playing users will see the new player
            try {
                if (!PlayerCache.getInstance().isAuthenticated(event.getPlayer().getName().toLowerCase())) {
                    event.setCancelled(true);
                }
            } catch (FieldAccessException e) {
                ConsoleLogger.showError("Couldn't access field.");
            }
        }
    }

    public void sendTablist(Player receiver) {
        WrappedGameProfile gameProfile = WrappedGameProfile.fromPlayer(receiver);

        ProtocolManager protocolManager = ProtocolLibrary.getProtocolManager();
        NativeGameMode gamemode = NativeGameMode.fromBukkit(receiver.getGameMode());

        WrappedChatComponent displayName = WrappedChatComponent.fromText(receiver.getDisplayName());
        PlayerInfoData playerInfoData = new PlayerInfoData(gameProfile, 0, gamemode, displayName);

        //add info containing the skin data
        PacketContainer addInfo = protocolManager.createPacket(PacketType.Play.Server.PLAYER_INFO);
        addInfo.getPlayerInfoAction().write(0, PlayerInfoAction.ADD_PLAYER);
        addInfo.getPlayerInfoDataLists().write(0, Arrays.asList(playerInfoData));

        try {
            //adds the skin
            protocolManager.sendServerPacket(receiver, addInfo);
        } catch (InvocationTargetException ex) {
            plugin.getLogger().log(Level.SEVERE, "Exception sending instant skin change packet", ex);
        }

        //triggers an update for others player to see them
        for (Player onlinePlayer : Utils.getOnlinePlayers()) {
            if (onlinePlayer.equals(receiver)) {
                continue;
            }

            //removes the entity and display them
            receiver.hidePlayer(onlinePlayer);
            receiver.showPlayer(onlinePlayer);
        }
    }

    public void register() {
        ProtocolLibrary.getProtocolManager().addPacketListener(this);
    }

    public void unregister() {
        ProtocolLibrary.getProtocolManager().removePacketListener(this);
    }
}
