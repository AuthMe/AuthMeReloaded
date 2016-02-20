package fr.xephi.authme.converter;

import de.luricos.bukkit.xAuth.database.DatabaseTables;
import de.luricos.bukkit.xAuth.utils.xAuthLog;
import de.luricos.bukkit.xAuth.xAuth;
import fr.xephi.authme.AuthMe;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.cache.auth.PlayerAuth;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.util.CollectionUtils;
import org.bukkit.command.CommandSender;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

class xAuthToFlat {

    private final AuthMe instance;
    private final DataSource database;
    private final CommandSender sender;

    public xAuthToFlat(AuthMe instance, CommandSender sender) {
        this.instance = instance;
        this.database = instance.getDataSource();
        this.sender = sender;
    }

    public boolean convert() {
        if (instance.getServer().getPluginManager().getPlugin("xAuth") == null) {
            sender.sendMessage("[AuthMe] xAuth plugin not found");
            return false;
        }
        File xAuthDb = new File(instance.getDataFolder().getParent(), "xAuth" + File.separator + "xAuth.h2.db");
        if (!xAuthDb.exists()) {
            sender.sendMessage("[AuthMe] xAuth H2 database not found, checking for MySQL or SQLite data...");
        }
        List<Integer> players = getXAuthPlayers();
        if (CollectionUtils.isEmpty(players)) {
            sender.sendMessage("[AuthMe] Error while importing xAuthPlayers: did not find any players");
            return false;
        }
        sender.sendMessage("[AuthMe] Starting import...");
        try {
            for (int id : players) {
                String pl = getIdPlayer(id);
                String psw = getPassword(id);
                if (psw != null && !psw.isEmpty() && pl != null) {
                    PlayerAuth auth = new PlayerAuth(pl, psw, "192.168.0.1", 0, "your@email.com", pl);
                    database.saveAuth(auth);
                }
            }
            sender.sendMessage("[AuthMe] Successfully converted from xAuth database");
        } catch (Exception e) {
            sender.sendMessage("[AuthMe] An error has occurred while importing the xAuth database."
                + " The import may have succeeded partially.");
            ConsoleLogger.logException("Error during xAuth database import", e);
        }
        return true;
    }

    private String getIdPlayer(int id) {
        String realPass = "";
        Connection conn = xAuth.getPlugin().getDatabaseController().getConnection();
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            String sql = String.format("SELECT `playername` FROM `%s` WHERE `id` = ?", xAuth.getPlugin().getDatabaseController().getTable(DatabaseTables.ACCOUNT));
            ps = conn.prepareStatement(sql);
            ps.setInt(1, id);
            rs = ps.executeQuery();
            if (!rs.next())
                return null;
            realPass = rs.getString("playername").toLowerCase();
        } catch (SQLException e) {
            xAuthLog.severe("Failed to retrieve name for account: " + id, e);
            return null;
        } finally {
            xAuth.getPlugin().getDatabaseController().close(conn, ps, rs);
        }
        return realPass;
    }

    private List<Integer> getXAuthPlayers() {
        List<Integer> xP = new ArrayList<>();
        Connection conn = xAuth.getPlugin().getDatabaseController().getConnection();
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            String sql = String.format("SELECT * FROM `%s`", xAuth.getPlugin().getDatabaseController().getTable(DatabaseTables.ACCOUNT));
            ps = conn.prepareStatement(sql);
            rs = ps.executeQuery();
            while (rs.next()) {
                xP.add(rs.getInt("id"));
            }
        } catch (SQLException e) {
            xAuthLog.severe("Cannot import xAuthPlayers", e);
            return new ArrayList<>();
        } finally {
            xAuth.getPlugin().getDatabaseController().close(conn, ps, rs);
        }
        return xP;
    }

    private String getPassword(int accountId) {
        String realPass = "";
        Connection conn = xAuth.getPlugin().getDatabaseController().getConnection();
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            String sql = String.format("SELECT `password`, `pwtype` FROM `%s` WHERE `id` = ?", xAuth.getPlugin().getDatabaseController().getTable(DatabaseTables.ACCOUNT));
            ps = conn.prepareStatement(sql);
            ps.setInt(1, accountId);
            rs = ps.executeQuery();
            if (!rs.next())
                return null;
            realPass = rs.getString("password");
        } catch (SQLException e) {
            xAuthLog.severe("Failed to retrieve password hash for account: " + accountId, e);
            return null;
        } finally {
            xAuth.getPlugin().getDatabaseController().close(conn, ps, rs);
        }
        return realPass;
    }
}
