package fr.xephi.authme.listener;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.reflect.FieldAccessException;
import com.comphenix.protocol.utility.MinecraftVersion;
import com.comphenix.protocol.wrappers.EnumWrappers.NativeGameMode;
import com.comphenix.protocol.wrappers.EnumWrappers.PlayerInfoAction;
import com.comphenix.protocol.wrappers.PlayerInfoData;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.comphenix.protocol.wrappers.WrappedGameProfile;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.cache.auth.PlayerCache;
import fr.xephi.authme.util.BukkitService;

import org.bukkit.entity.Player;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.logging.Level;

public class AuthMeTablistPacketAdapter extends PacketAdapter {

    private final BukkitService bukkitService;

    public AuthMeTablistPacketAdapter(AuthMe plugin, BukkitService bukkitService) {
        super(plugin, ListenerPriority.NORMAL, PacketType.Play.Server.PLAYER_INFO);
        this.bukkitService = bukkitService;
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
                ConsoleLogger.logException("Couldn't access field", e);
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
        for (Player onlinePlayer : bukkitService.getOnlinePlayers()) {
            if (onlinePlayer.equals(receiver) || !receiver.canSee(onlinePlayer)) {
                continue;
            }

            //removes the entity and display them
            receiver.hidePlayer(onlinePlayer);
            receiver.showPlayer(onlinePlayer);
        }
    }

    public void register() {
        if (MinecraftVersion.getCurrentVersion().isAtLeast(MinecraftVersion.BOUNTIFUL_UPDATE)) {
            ProtocolLibrary.getProtocolManager().addPacketListener(this);
        } else {
            ConsoleLogger.info("The hideTablist feature is not compatible with your minecraft version");
            ConsoleLogger.info("It requires 1.8+. Disabling the hideTablist feature...");
        }
    }

    public void unregister() {
        ProtocolLibrary.getProtocolManager().removePacketListener(this);
    }
}
