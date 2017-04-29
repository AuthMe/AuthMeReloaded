package fr.xephi.authme.command.executable.authme.debug;

import fr.xephi.authme.permission.PermissionNode;
import org.bukkit.command.CommandSender;

import java.util.List;

/**
 * A debug section: "child" command of the debug command.
 */
interface DebugSection {

    /**
     * @return the name to get to this child command
     */
    String getName();

    /**
     * @return short description of the child command
     */
    String getDescription();

    /**
     * Executes the debug child command.
     *
     * @param sender the sender executing the command
     * @param arguments the arguments, without the label of the child command
     */
    void execute(CommandSender sender, List<String> arguments);

    /**
     * @return permission required to run this section
     */
    PermissionNode getRequiredPermission();

}
