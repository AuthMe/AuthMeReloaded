package fr.xephi.authme.command.executable.authme;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.cache.auth.PlayerAuth;
import fr.xephi.authme.command.CommandParts;
import fr.xephi.authme.command.ExecutableCommand;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

import java.util.List;

/**
 */
public class ResetNameCommand extends ExecutableCommand {

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
        // AuthMe plugin instance
        final AuthMe plugin = AuthMe.getInstance();

        // Command logic
        Bukkit.getScheduler().runTaskAsynchronously(plugin, new Runnable() {
            @Override
            public void run() {
                List<PlayerAuth> authentications = plugin.database.getAllAuths();
                for (PlayerAuth auth : authentications) {
                    auth.setRealName("Player");
                    plugin.database.updateSession(auth);
                }
            }
        });

        return true;
    }
}
