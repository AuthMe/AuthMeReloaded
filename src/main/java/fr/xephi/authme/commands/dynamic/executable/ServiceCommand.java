package fr.xephi.authme.commands.dynamic.executable;

import com.timvisee.dungeonmaze.Core;
import com.timvisee.dungeonmaze.DungeonMaze;
import com.timvisee.dungeonmaze.command.CommandParts;
import com.timvisee.dungeonmaze.command.ExecutableCommand;
import com.timvisee.dungeonmaze.service.Service;
import com.timvisee.dungeonmaze.service.ServiceManager;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.List;

public class ServiceCommand extends ExecutableCommand {

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
        // Print the status info header
        sender.sendMessage(ChatColor.GOLD + "==========[ " + DungeonMaze.PLUGIN_NAME.toUpperCase() + " SERVICES ]==========");

        // Get the service manager and make sure it's valid
        ServiceManager serviceManager = Core.instance.getServiceManager();
        if(serviceManager == null) {
            sender.sendMessage(ChatColor.DARK_RED + "Error, failed to retrieve the services information!");
            return true;
        }

        // Print the service count and the list of services
        sender.sendMessage(ChatColor.GOLD + "Running Services: " + ChatColor.WHITE + serviceManager.getServiceCount(true) + ChatColor.GRAY + " / " + Core.instance.getServiceManager().getServiceCount());
        printServices(sender);

        // Return the result
        return true;
    }

    /**
     * Print all services.
     *
     * @param sender The command sender to print the services to.
     */
    public void printServices(CommandSender sender) {
        // Get the service manager and make sure it's valid
        ServiceManager serviceManager = Core.instance.getServiceManager();
        if(serviceManager == null) {
            sender.sendMessage(ChatColor.DARK_RED + "Error, failed to retrieve the services information!");
            return;
        }

        // Get all the services
        List<Service> services = serviceManager.getServices();

        // Print the header
        sender.sendMessage(ChatColor.GOLD + "Services:");

        // Print all the services
        for(Service service : services) {
            // Check whether the service is initialized
            if(service.isInit())
                sender.sendMessage(ChatColor.WHITE + " " + service.getName() + " service " + ChatColor.GREEN + ChatColor.ITALIC + "(Loaded)");
            else
                sender.sendMessage(ChatColor.WHITE + " " + service.getName() + " service " + ChatColor.DARK_RED + ChatColor.ITALIC + "(Not loaded)");
        }
    }
}
