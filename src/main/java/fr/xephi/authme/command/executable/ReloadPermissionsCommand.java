package fr.xephi.authme.command.executable;

import com.timvisee.dungeonmaze.Core;
import com.timvisee.dungeonmaze.command.CommandParts;
import com.timvisee.dungeonmaze.command.ExecutableCommand;
import com.timvisee.dungeonmaze.permission.PermissionsManager;
import com.timvisee.dungeonmaze.util.Profiler;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public class ReloadPermissionsCommand extends ExecutableCommand {

    /**
     * Execute the command.
     *
     * @param sender           The command sender.
     * @param commandReference The command reference.
     * @param commandArguments The command arguments.
     *
     * @return True if the command was executed successfully, false otherwise.
     */
    @Override
    public boolean executeCommand(CommandSender sender, CommandParts commandReference, CommandParts commandArguments) {
        // Profile the permissions reload process
        Profiler p = new Profiler(true);

        // Show a status message
        sender.sendMessage(ChatColor.YELLOW + "Reloading permissions...");
        Core.getLogger().info("Reloading permissions...");

        // Get the permissions manager and make sure it's valid
        PermissionsManager permissionsManager = Core.getPermissionsManager();
        if(permissionsManager == null) {
            Core.getLogger().info("Failed to access the permissions manager after " + p.getTimeFormatted() + "!");
            sender.sendMessage(ChatColor.DARK_RED + "Failed to access the permissions manager after " + p.getTimeFormatted() + "!");
            return true;
        }

        // Reload the permissions service, show an error on failure
        if(!permissionsManager.reload()) {
            Core.getLogger().info("Failed to reload permissions after " + p.getTimeFormatted() + "!");
            sender.sendMessage(ChatColor.DARK_RED + "Failed to reload permissions after " + p.getTimeFormatted() + "!");
            return true;
        }

        // Show a success message
        Core.getLogger().info("Permissions reloaded successfully, took " + p.getTimeFormatted() + "!");
        sender.sendMessage(ChatColor.GREEN + "Permissions reloaded successfully, took " + p.getTimeFormatted() + "!");

        // Get and show the permissions system being used
        String permissionsSystem = ChatColor.GOLD + permissionsManager.getUsedPermissionsSystemType().getName();
        Core.getLogger().info("Used permissions system: " + permissionsSystem);
        sender.sendMessage(ChatColor.GREEN + "Used permissions system: " + permissionsSystem);
        return true;
    }
}
