package fr.xephi.authme.command.executable.converter;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.command.CommandParts;
import fr.xephi.authme.command.ExecutableCommand;
import fr.xephi.authme.converter.*;
import fr.xephi.authme.settings.Messages;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

/**
 */
public class ConverterCommand extends ExecutableCommand {

    /**
     * Execute the command.
     *
     * @param sender           The command sender.
     * @param commandReference The command reference.
     * @param commandArguments The command arguments.
     * @return True if the command was executed successfully, false otherwise.
     */
    @Override
    public boolean executeCommand(CommandSender sender, CommandParts commandReference, CommandParts commandArguments) {
        // AuthMe plugin instance
        final AuthMe plugin = AuthMe.getInstance();

        // Messages instance
        final Messages m = Messages.getInstance();

        // Get the conversion job
        String job = commandArguments.get(0);

        // Determine the job type
        ConvertType jobType = ConvertType.fromName(job);
        if (jobType == null) {
            m.send(sender, "error");
            return true;
        }

        // Get the proper converter instance
        Converter converter = null;
        switch (jobType) {
            case ftsql:
                converter = new FlatToSql();
                break;
            case ftsqlite:
                converter = new FlatToSqlite(sender);
                break;
            case xauth:
                converter = new xAuthConverter(plugin, sender);
                break;
            case crazylogin:
                converter = new CrazyLoginConverter(plugin, sender);
                break;
            case rakamak:
                converter = new RakamakConverter(plugin, sender);
                break;
            case royalauth:
                converter = new RoyalAuthConverter(plugin);
                break;
            case vauth:
                converter = new vAuthConverter(plugin, sender);
                break;
            case sqltoflat:
                converter = new SqlToFlat(plugin, sender);
                break;
            default:
                break;
        }

        // Run the convert job
        Bukkit.getScheduler().runTaskAsynchronously(plugin, converter);

        // Show a status message
        sender.sendMessage("[AuthMe] Successfully converted from " + jobType.getName());
        return true;
    }

    /**
     */
    public enum ConvertType {
        ftsql("flattosql"),
        ftsqlite("flattosqlite"),
        xauth("xauth"),
        crazylogin("crazylogin"),
        rakamak("rakamak"),
        royalauth("royalauth"),
        vauth("vauth"),
        sqltoflat("sqltoflat");

        String name;

        /**
         * Constructor for ConvertType.
         *
         * @param name String
         */
        ConvertType(String name) {
            this.name = name;
        }

        /**
         * Method fromName.
         *
         * @param name String
         * @return ConvertType
         */
        public static ConvertType fromName(String name) {
            for (ConvertType type : ConvertType.values()) {
                if (type.getName().equalsIgnoreCase(name))
                    return type;
            }
            return null;
        }

        /**
         * Method getName.
         *
         * @return String
         */
        String getName() {
            return this.name;
        }
    }
}
