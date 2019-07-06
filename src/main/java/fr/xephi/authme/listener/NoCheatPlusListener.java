package fr.xephi.authme.listener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import fr.neatmonster.nocheatplus.checks.CheckType;
import fr.neatmonster.nocheatplus.hooks.NCPExemptionManager;
import fr.xephi.authme.events.LoginEvent;

public class NoCheatPlusListener implements Listener {

    private HashMap<Player, List<CheckType>> exempted;

    public NoCheatPlusListener() {
        exempted = new HashMap<Player, List<CheckType>>();
    }

    @EventHandler(ignoreCancelled = true)
    public void onLogin(LoginEvent event) {
        Player player = event.getPlayer();
        for (CheckType type : exempted.get(player)) {
            if (NCPExemptionManager.isExempted(player, type)) {
                NCPExemptionManager.unexempt(player, type);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onJoin(PlayerJoinEvent event) {
        if (NCPExemptionManager.isExempted(event.getPlayer(), CheckType.ALL)) {
            return;
        }

        List<CheckType> exemptions = new ArrayList<CheckType>();

        for (CheckType type : CheckType.values()) {
            if (NCPExemptionManager.isExempted(event.getPlayer(), type)) {
                exemptions.add(type);
            }
        }

        NCPExemptionManager.exemptPermanently(event.getPlayer());
        exempted.put(event.getPlayer(), exemptions);
    }
}
