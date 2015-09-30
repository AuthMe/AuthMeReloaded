package fr.xephi.authme.process.quit;

import org.bukkit.GameMode;
import org.bukkit.entity.Player;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.events.RestoreInventoryEvent;
import fr.xephi.authme.settings.Settings;
import org.bukkit.Bukkit;

public class ProcessSyncronousPlayerQuit implements Runnable {

    protected AuthMe plugin;
    protected Player player;
    protected boolean isOp;
    protected boolean isFlying;
    protected boolean needToChange;

    public ProcessSyncronousPlayerQuit(AuthMe plugin, Player player
            , boolean isOp, boolean isFlying
            , boolean needToChange) {
        this.plugin = plugin;
        this.player = player;
        this.isOp = isOp;
        this.isFlying = isFlying;
        this.needToChange = needToChange;
    }

    @Override
    public void run() {
        RestoreInventoryEvent ev = new RestoreInventoryEvent(player);
        Bukkit.getPluginManager().callEvent(ev);
        if (!ev.isCancelled()) {
            plugin.api.setPlayerInventory(player, ev.getInventory(), ev.getArmor());
        }

        if (needToChange) {
            player.setOp(isOp);
            if (player.getGameMode() != GameMode.CREATIVE && !Settings.isMovementAllowed) {
                player.setAllowFlight(isFlying);
                player.setFlying(isFlying);
            }
        }
        try {
            player.getVehicle().eject();
        } catch (Exception e) {
        }
    }
}
