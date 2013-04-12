package uk.org.whoami.authme.converter;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.command.CommandSender;

import com.cypherx.xauth.xAuth;
import com.cypherx.xauth.database.Table;
import com.cypherx.xauth.utils.xAuthLog;

import uk.org.whoami.authme.AuthMe;
import uk.org.whoami.authme.cache.auth.PlayerAuth;
import uk.org.whoami.authme.datasource.DataSource;

/**
*
* @author Xephi59
*/
public class xAuthToFlat {

	public AuthMe instance;
	public DataSource database;

	public xAuthToFlat(AuthMe instance, DataSource database) {
		this.instance = instance;
		this.database = database;
	}

	public boolean convert(CommandSender sender) {
		if (instance.getServer().getPluginManager().getPlugin("xAuth") == null) {
			sender.sendMessage("[AuthMe] xAuth plugin not found");
			return false;
		}
		if (!(new File("./plugins/xAuth/xAuth.h2.db").exists())) {
			sender.sendMessage("[AuthMe] xAuth H2 database not found, checking for MySQL or SQLite data...");
		}
		List<Integer> players = getXAuthPlayers();
		if (players == null || players.isEmpty()) {
			sender.sendMessage("[AuthMe] Error while import xAuthPlayers");
			return false;
		}
		sender.sendMessage("[AuthMe] Starting import...");
		for (int id : players) {
			String pl = getIdPlayer(id);
			String psw = getPassword(id);
			if (psw != null && !psw.isEmpty() && pl != null) {
				PlayerAuth auth = new PlayerAuth(pl, psw, "198.18.0.1", 0);
				database.saveAuth(auth);
			}
		}
		sender.sendMessage("[AuthMe] Import done!");
		return true;
	}

	public String getIdPlayer(int id) {
		String realPass = "";
		Connection conn = xAuth.getPlugin().getDatabaseController().getConnection();
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            String sql = String.format("SELECT `playername` FROM `%s` WHERE `id` = ?",
                    xAuth.getPlugin().getDatabaseController().getTable(Table.ACCOUNT));
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

	public List<Integer> getXAuthPlayers() {
		List<Integer> xP = new ArrayList<Integer>();
		Connection conn = xAuth.getPlugin().getDatabaseController().getConnection();
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
        	String sql = String.format("SELECT * FROM `%s`",
                    xAuth.getPlugin().getDatabaseController().getTable(Table.ACCOUNT));
            ps = conn.prepareStatement(sql);
            rs = ps.executeQuery();
            while(rs.next()) {
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
            String sql = String.format("SELECT `password`, `pwtype` FROM `%s` WHERE `id` = ?",
                    xAuth.getPlugin().getDatabaseController().getTable(Table.ACCOUNT));
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
