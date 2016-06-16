package fr.xephi.authme.task;

import fr.xephi.authme.ConsoleLogger;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class PurgeTask extends BukkitRunnable {

    private PurgeService purgeService;

    //how many players we should check for each tick
    private static final int INTERVALL_CHECK = 5;

    private final UUID sender;
    private final Set<String> toPurge;

    private final OfflinePlayer[] offlinePlayers;
    private final int totalPurgeCount;

    private int currentPage = 0;

    public PurgeTask(PurgeService service, CommandSender sender, Set<String> toPurge, OfflinePlayer[] offlinePlayers) {
        this.purgeService = service;
        if (sender instanceof Player) {
            this.sender = ((Player) sender).getUniqueId();
        } else {
            this.sender = null;
        }

        this.toPurge = toPurge;
        this.totalPurgeCount = toPurge.size();
        this.offlinePlayers = offlinePlayers;
    }

    @Override
    public void run() {
        if (toPurge.isEmpty()) {
            //everything was removed
            finish();
            return;
        }

        Set<OfflinePlayer> playerPortion = new HashSet<OfflinePlayer>(INTERVALL_CHECK);
        Set<String> namePortion = new HashSet<String>(INTERVALL_CHECK);
        for (int i = 0; i < INTERVALL_CHECK; i++) {
            int nextPosition = (currentPage * INTERVALL_CHECK) + i;
            if (offlinePlayers.length >= nextPosition) {
                //no more offline players on this page
                break;
            }

            OfflinePlayer offlinePlayer = offlinePlayers[nextPosition];
            String offlineName = offlinePlayer.getName();
            //remove to speed up later lookups
            if (toPurge.remove(offlineName.toLowerCase())) {
                playerPortion.add(offlinePlayer);
                namePortion.add(offlinePlayer.getName());
            }
        }

        if (!toPurge.isEmpty() && playerPortion.isEmpty()) {
            ConsoleLogger.info("Finished lookup up offlinePlayers. Begin looking purging player names only");

            //we went through all offlineplayers but there are still names remaining
            namePortion.addAll(toPurge);
            toPurge.clear();
        }

        currentPage++;
        purgeData(playerPortion, namePortion);
        if (currentPage % 20 == 0) {
            int completed = totalPurgeCount - toPurge.size();
            sendMessage("[AuthMe] Purge progress " + completed + '/' + totalPurgeCount);
        }
    }

    private void purgeData(Set<OfflinePlayer> playerPortion, Set<String> namePortion) {
        // Purge other data
        purgeService.purgeEssentials(playerPortion);
        purgeService.purgeDat(playerPortion);
        purgeService.purgeLimitedCreative(namePortion);
        purgeService.purgeAntiXray(namePortion);
        purgeService.purgePermissions(playerPortion);
    }

    private void finish() {
        cancel();

        // Show a status message
        sendMessage(ChatColor.GREEN + "[AuthMe] Database has been purged correctly");

        ConsoleLogger.info("Purge Finished!");
        purgeService.setPurging(false);
    }

    private void sendMessage(String message) {
        if (sender == null) {
            Bukkit.getConsoleSender().sendMessage(message);
        } else {
            Player player = Bukkit.getPlayer(sender);
            if (player != null) {
                player.sendMessage(message);
            }
        }
    }
}
