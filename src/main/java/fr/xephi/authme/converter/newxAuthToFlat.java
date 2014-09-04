package fr.xephi.authme.converter;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.command.CommandSender;

import de.luricos.bukkit.xAuth.xAuth;
import de.luricos.bukkit.xAuth.database.Table;
import de.luricos.bukkit.xAuth.utils.xAuthLog;
import fr.xephi.authme.AuthMe;
import fr.xephi.authme.cache.auth.PlayerAuth;
import fr.xephi.authme.datasource.DataSource;

public class newxAuthToFlat {

    public AuthMe instance;
    public DataSource database;
    public CommandSender sender;

    public newxAuthToFlat(AuthMe instance, DataSource database,
            CommandSender sender) {
        this.instance = instance;
        this.database = database;
        this.sender = sender;
    }

    public boolean convert() {
        if (instance.getServer().getPluginManager().getPlugin("xAuth") == null) {
            sender.sendMessage("[AuthMe] xAuth plugin not found");
            return false;
        }
        if (!(new File(instance.getDataFolder().getParent() + File.separator + "xAuth" + File.separator + "xAuth.h2.db").exists())) {
            sender.sendMessage("[AuthMe] xAuth H2 database not found, checking for MySQL or SQLite data...");
        }
        List<Integer> players = getXAuthPlayers();
        if (players == null || players.isEmpty()) {
            sender.sendMessage("[AuthMe] Error while import xAuthPlayers");
            return false;
        }
        sender.sendMessage("[AuthMe] Starting import...");
        try {
            for (int id : players) {
                String pl = getIdPlayer(id);
                String psw = getPassword(id);
                if (psw != null && !psw.isEmpty() && pl != null) {
                    PlayerAuth auth = new PlayerAuth(pl, psw, "198.18.0.1", 0, "your@email.com");
                    database.saveAuth(auth);
                }
            }
            sender.sendMessage("[AuthMe] Successfull convert from xAuth database");
        } catch (Exception e) {
            sender.sendMessage("[AuthMe] An error has been thrown while import xAuth database, the import hadn't fail but can be not complete ");
        }
        return true;
    }

    public String getIdPlayer(int id) {
        String realPass = "";
        Connection conn = xAuth.getPlugin().getDatabaseController().getConnection();
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            String sql = String.format("SELECT `playername` FROM `%s` WHERE `id` = ?", xAuth.getPlugin().getDatabaseController().getTable(Table.ACCOUNT));
            ps = conn.prepareStatement(sql);
            ps.setInt(1, id);
            rs = ps.executeQuery();
            if (!rs.next())
                return null;
            realPass = rs.getString("playername");
        } catch (SQLException e) {
            xAuthLog.severe("Failed to retrieve name for account: " + id, e);
            return null;
        } finally {
            xAuth.getPlugin().getDatabaseController().close(conn, ps, rs);
        }
        return realPass;
    }

    public List<Integer> getXAuthPlayers() {
        List<Integer> xP = new ArrayList<Integer>();
        Connection conn = xAuth.getPlugin().getDatabaseController().getConnection();
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            String sql = String.format("SELECT * FROM `%s`", xAuth.getPlugin().getDatabaseController().getTable(Table.ACCOUNT));
            ps = conn.prepareStatement(sql);
            rs = ps.executeQuery();
            while (rs.next()) {
                xP.add(rs.getInt("id"));
            }
        } catch (SQLException e) {
            xAuthLog.severe("Cannot import xAuthPlayers", e);
            return new ArrayList<Integer>();
        } finally {
            xAuth.getPlugin().getDatabaseController().close(conn, ps, rs);
        }
        return xP;
    }

    public String getPassword(int accountId) {
        String realPass = "";
        Connection conn = xAuth.getPlugin().getDatabaseController().getConnection();
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            String sql = String.format("SELECT `password`, `pwtype` FROM `%s` WHERE `id` = ?", xAuth.getPlugin().getDatabaseController().getTable(Table.ACCOUNT));
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
