package fr.xephi.authme.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.converter.Converter;
import fr.xephi.authme.converter.CrazyLoginConverter;
import fr.xephi.authme.converter.FlatToSql;
import fr.xephi.authme.converter.FlatToSqlite;
import fr.xephi.authme.converter.RakamakConverter;
import fr.xephi.authme.converter.RoyalAuthConverter;
import fr.xephi.authme.converter.SqlToFlat;
import fr.xephi.authme.converter.vAuthConverter;
import fr.xephi.authme.converter.xAuthConverter;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.settings.Messages;

public class ConverterCommand implements CommandExecutor {

    private AuthMe plugin;
    private Messages m = Messages.getInstance();
    private DataSource database;

    public ConverterCommand(AuthMe plugin, DataSource database) {
        this.plugin = plugin;
        this.database = database;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmnd, String label,
            final String[] args) {
        if (!(sender instanceof Player)) {
            return true;
        }

        if (!plugin.authmePermissible(sender, "authme.admin.converter")) {
            m._(sender, "no_perm");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage("Usage : /converter flattosql | flattosqlite | xauth | crazylogin | rakamak | royalauth | vauth | sqltoflat");
            return true;
        }

        ConvertType type = ConvertType.fromName(args[0]);
        if (type == null) {
            m._(sender, "error");
            return true;
        }
        Converter converter = null;
        switch (type) {
            case ftsql:
                converter = new FlatToSql();
                break;
            case ftsqlite:
                converter = new FlatToSqlite(sender);
                break;
            case xauth:
                converter = new xAuthConverter(plugin, database, sender);
                break;
            case crazylogin:
                converter = new CrazyLoginConverter(plugin, database, sender);
                break;
            case rakamak:
                converter = new RakamakConverter(plugin, database, sender);
                break;
            case royalauth:
                converter = new RoyalAuthConverter(plugin);
                break;
            case vauth:
                converter = new vAuthConverter(plugin, database, sender);
                break;
            case sqltoflat:
                converter = new SqlToFlat(plugin, database, sender);
                break;
            default:
                break;
        }
        if (converter == null) {
            m._(sender, "error");
            return true;
        }
        Bukkit.getScheduler().runTaskAsynchronously(plugin, converter);
        sender.sendMessage("[AuthMe] Successfully converted from " + args[0]);
        return true;
    }

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

        ConvertType(String name) {
            this.name = name;
        }

        String getName() {
            return this.name;
        }

        public static ConvertType fromName(String name) {
            for (ConvertType type : ConvertType.values()) {
                if (type.getName().equalsIgnoreCase(name))
                    return type;
            }
            return null;
        }
    }
}
