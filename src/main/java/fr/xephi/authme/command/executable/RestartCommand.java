package fr.xephi.authme.command.executable;

import com.timvisee.dungeonmaze.Core;
import com.timvisee.dungeonmaze.DungeonMaze;
import com.timvisee.dungeonmaze.command.CommandParts;
import com.timvisee.dungeonmaze.command.ExecutableCommand;
import com.timvisee.dungeonmaze.util.Profiler;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public class RestartCommand extends ExecutableCommand {

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
        // Profile the restart process
        Profiler p = new Profiler(true);

        // Set whether the restart is forced
        boolean force = false;

        // Get whether the restart should be forced from the command arguments
        if(commandArguments.getCount() >= 1) {
            String arg = commandArguments.get(0);

            // Check whether the argument equals 'force'
            if(arg.equalsIgnoreCase("force") || arg.equalsIgnoreCase("forced"))
                force = true;

            else if(arg.equalsIgnoreCase("true") || arg.equalsIgnoreCase("t") || arg.equalsIgnoreCase("yes") || arg.equalsIgnoreCase("y"))
                force = true;

            else if(arg.equalsIgnoreCase("false") || arg.equalsIgnoreCase("f") || arg.equalsIgnoreCase("no") || arg.equalsIgnoreCase("n"))
                force = false;

            else {
                sender.sendMessage(ChatColor.DARK_RED + arg);
                sender.sendMessage(ChatColor.DARK_RED + "Invalid argument!");
                return true;
            }
        }

        // Show a restart warning
        if(force) {
            sender.sendMessage(ChatColor.YELLOW + "Force restarting Dungeon Maze...");
            Core.getLogger().info("Force restarting Dungeon Maze...");
        } else {
            sender.sendMessage(ChatColor.YELLOW + "Restarting Dungeon Maze...");
            Core.getLogger().info("Restarting Dungeon Maze...");
        }

        // Profile the Dungeon Maze Core destruction
        Profiler stopCoreProfiler = new Profiler(true);

        // Destroy the Dungeon Maze core
        if(!DungeonMaze.instance.destroyCore(force)) {
            // Failed to destroy the core, show a status message
            sender.sendMessage(ChatColor.DARK_RED + "Failed to stop the Dungeon Maze Core after " + stopCoreProfiler.getTimeFormatted() + "!");
            sender.sendMessage(ChatColor.DARK_RED + "Please use " + ChatColor.GOLD + "/reload" + ChatColor.DARK_RED + " for plugin instability reasons!");
            Core.getLogger().error("Failed to stop the core, after " + stopCoreProfiler.getTimeFormatted() + "!");

            // Return if the restart isn't force
            if(!force)
                return true;
        }

        // Show a status message
        sender.sendMessage(ChatColor.YELLOW + "Dungeon Maze Core stopped, took " + stopCoreProfiler.getTimeFormatted() + "!");

        // Profile the core starting
        Profiler startCoreProfiler = new Profiler(true);

        // Initialize the core, show the result status
        if(!DungeonMaze.instance.initCore()) {
            // Core failed to initialize, show a status message
            sender.sendMessage(ChatColor.DARK_RED + "Failed to start the Dungeon Maze Core after " + startCoreProfiler.getTimeFormatted() + "!");
            sender.sendMessage(ChatColor.DARK_RED + "Please use " + ChatColor.GOLD + "/reload" + ChatColor.DARK_RED + " for plugin instability reasons!");
            Core.getLogger().error("Failed to start the core, after " + startCoreProfiler.getTimeFormatted() + "!");

            // Return if the restart isn't forced
            if(!force)
                return true;
        }

        // Core initialized, show a status message
        sender.sendMessage(ChatColor.YELLOW + "Dungeon Maze Core started, took " + startCoreProfiler.getTimeFormatted() + "!");

        // Show a status message of the running services
        final int runningServices = Core.instance.getServiceManager().getServiceCount(true);
        final int totalServices = Core.instance.getServiceManager().getServiceCount();
        sender.sendMessage(ChatColor.YELLOW + "Started " + ChatColor.GOLD + runningServices + ChatColor.YELLOW + " out of " + ChatColor.GOLD + totalServices + ChatColor.YELLOW + " Dungeon Maze services!");

        // Dungeon Maze restarted, show a status message
        sender.sendMessage(ChatColor.GREEN + "Dungeon Maze has been restarted successfully, took " + p.getTimeFormatted() + "!");
        return true;
    }
}
