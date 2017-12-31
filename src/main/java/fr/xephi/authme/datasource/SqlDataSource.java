package fr.xephi.authme.datasource;

import com.zaxxer.hikari.HikariDataSource;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.data.auth.PlayerAuth;
import fr.xephi.authme.datasource.sqlextensions.SqlExtension;
import fr.xephi.authme.security.crypts.HashedPassword;
import fr.xephi.authme.util.StringUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static fr.xephi.authme.datasource.SqlDataSourceUtils.getNullableLong;
import static fr.xephi.authme.datasource.SqlDataSourceUtils.logSqlException;

public abstract class SqlDataSource implements DataSource {

    boolean useSsl;
    String host;
    String port;
    String username;
    String password;
    String database;
    String tableName;
    int poolSize;
    int maxLifetime;
    List<String> columnOthers;
    Columns col;
    SqlExtension sqlExtension;
    HikariDataSource ds;

    abstract void setConnectionArguments();
    abstract void initHikariDataSource();

    @Override
    public void reload() {
        if (ds != null) {
            ds.close();
        }
        setConnectionArguments();
        ConsoleLogger.info("Hikari ConnectionPool arguments reloaded!");
    }

    Connection getConnection() throws SQLException {
        return ds.getConnection();
    }

    @Override
    public boolean isAuthAvailable(String user) {
        String sql = "SELECT " + col.NAME + " FROM " + tableName + " WHERE " + col.NAME + "=?;";
        try (Connection con = getConnection(); PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, user.toLowerCase());
            try (ResultSet rs = pst.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException ex) {
            logSqlException(ex);
        }
        return false;
    }

    @Override
    public HashedPassword getPassword(String user) {
        boolean useSalt = !col.SALT.isEmpty();
        String sql = "SELECT " + col.PASSWORD
            + (useSalt ? ", " + col.SALT : "")
            + " FROM " + tableName + " WHERE " + col.NAME + "=?;";
        try (Connection con = getConnection(); PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, user.toLowerCase());
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    return new HashedPassword(rs.getString(col.PASSWORD),
                        useSalt ? rs.getString(col.SALT) : null);
                }
            }
        } catch (SQLException ex) {
            logSqlException(ex);
        }
        return null;
    }

    @Override
    public PlayerAuth getAuth(String user) {
        String sql = "SELECT * FROM " + tableName + " WHERE " + col.NAME + "=?;";
        PlayerAuth auth;
        try (Connection con = getConnection(); PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, user.toLowerCase());
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    int id = rs.getInt(col.ID);
                    auth = buildAuthFromResultSet(rs);
                    sqlExtension.extendAuth(auth, id, con);
                    return auth;
                }
            }
        } catch (SQLException ex) {
            logSqlException(ex);
        }
        return null;
    }

    @Override
    public boolean saveAuth(PlayerAuth auth) {
        try (Connection con = getConnection()) {
            // TODO ljacqu 20171104: Replace with generic columns util to clean this up
            boolean useSalt = !col.SALT.isEmpty() || !StringUtils.isEmpty(auth.getPassword().getSalt());
            boolean hasEmail = auth.getEmail() != null;
            String emailPlaceholder = hasEmail ? "?" : "DEFAULT";

            String sql = "INSERT INTO " + tableName + "("
                + col.NAME + "," + col.PASSWORD + "," + col.REAL_NAME
                + "," + col.EMAIL + "," + col.REGISTRATION_DATE + "," + col.REGISTRATION_IP
                + (useSalt ? "," + col.SALT : "")
                + ") VALUES (?,?,?," + emailPlaceholder + ",?,?" + (useSalt ? ",?" : "") + ");";
            try (PreparedStatement pst = con.prepareStatement(sql)) {
                int index = 1;
                pst.setString(index++, auth.getNickname());
                pst.setString(index++, auth.getPassword().getHash());
                pst.setString(index++, auth.getRealName());
                if (hasEmail) {
                    pst.setString(index++, auth.getEmail());
                }
                pst.setObject(index++, auth.getRegistrationDate());
                pst.setString(index++, auth.getRegistrationIp());
                if (useSalt) {
                    pst.setString(index++, auth.getPassword().getSalt());
                }
                pst.executeUpdate();
            }

            if (!columnOthers.isEmpty()) {
                for (String column : columnOthers) {
                    try (PreparedStatement pst = con.prepareStatement("UPDATE " + tableName + " SET " + column + "=? WHERE " + col.NAME + "=?;")) {
                        pst.setString(1, auth.getRealName());
                        pst.setString(2, auth.getNickname());
                        pst.executeUpdate();
                    }
                }
            }

            sqlExtension.saveAuth(auth, con);
            return true;
        } catch (SQLException ex) {
            logSqlException(ex);
        }
        return false;
    }

    @Override
    public boolean updatePassword(PlayerAuth auth) {
        return updatePassword(auth.getNickname(), auth.getPassword());
    }

    @Override
    public boolean updatePassword(String user, HashedPassword password) {
        user = user.toLowerCase();
        try (Connection con = getConnection()) {
            boolean useSalt = !col.SALT.isEmpty();
            if (useSalt) {
                String sql = String.format("UPDATE %s SET %s = ?, %s = ? WHERE %s = ?;",
                    tableName, col.PASSWORD, col.SALT, col.NAME);
                try (PreparedStatement pst = con.prepareStatement(sql)) {
                    pst.setString(1, password.getHash());
                    pst.setString(2, password.getSalt());
                    pst.setString(3, user);
                    pst.executeUpdate();
                }
            } else {
                String sql = String.format("UPDATE %s SET %s = ? WHERE %s = ?;",
                    tableName, col.PASSWORD, col.NAME);
                try (PreparedStatement pst = con.prepareStatement(sql)) {
                    pst.setString(1, password.getHash());
                    pst.setString(2, user);
                    pst.executeUpdate();
                }
            }
            sqlExtension.changePassword(user, password, con);
            return true;
        } catch (SQLException ex) {
            logSqlException(ex);
        }
        return false;
    }

    @Override
    public boolean updateSession(PlayerAuth auth) {
        String sql = "UPDATE " + tableName + " SET "
            + col.LAST_IP + "=?, " + col.LAST_LOGIN + "=?, " + col.REAL_NAME + "=? WHERE " + col.NAME + "=?;";
        try (Connection con = getConnection(); PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, auth.getLastIp());
            pst.setObject(2, auth.getLastLogin());
            pst.setString(3, auth.getRealName());
            pst.setString(4, auth.getNickname());
            pst.executeUpdate();
            return true;
        } catch (SQLException ex) {
            logSqlException(ex);
        }
        return false;
    }

    @Override
    public Set<String> getRecordsToPurge(long until) {
        Set<String> list = new HashSet<>();
        String select = "SELECT " + col.NAME + " FROM " + tableName + " WHERE GREATEST("
            + " COALESCE(" + col.LAST_LOGIN + ", 0),"
            + " COALESCE(" + col.REGISTRATION_DATE + ", 0)"
            + ") < ?;";
        try (Connection con = getConnection();
             PreparedStatement selectPst = con.prepareStatement(select)) {
            selectPst.setLong(1, until);
            try (ResultSet rs = selectPst.executeQuery()) {
                while (rs.next()) {
                    list.add(rs.getString(col.NAME));
                }
            }
        } catch (SQLException ex) {
            logSqlException(ex);
        }

        return list;
    }

    @Override
    public boolean removeAuth(String user) {
        user = user.toLowerCase();
        String sql = "DELETE FROM " + tableName + " WHERE " + col.NAME + "=?;";
        try (Connection con = getConnection(); PreparedStatement pst = con.prepareStatement(sql)) {
            sqlExtension.removeAuth(user, con);
            pst.setString(1, user.toLowerCase());
            pst.executeUpdate();
            return true;
        } catch (SQLException ex) {
            logSqlException(ex);
        }
        return false;
    }

    @Override
    public boolean updateQuitLoc(PlayerAuth auth) {
        String sql = "UPDATE " + tableName
            + " SET " + col.LASTLOC_X + " =?, " + col.LASTLOC_Y + "=?, " + col.LASTLOC_Z + "=?, "
            + col.LASTLOC_WORLD + "=?, " + col.LASTLOC_YAW + "=?, " + col.LASTLOC_PITCH + "=?"
            + " WHERE " + col.NAME + "=?;";
        try (Connection con = getConnection(); PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setDouble(1, auth.getQuitLocX());
            pst.setDouble(2, auth.getQuitLocY());
            pst.setDouble(3, auth.getQuitLocZ());
            pst.setString(4, auth.getWorld());
            pst.setFloat(5, auth.getYaw());
            pst.setFloat(6, auth.getPitch());
            pst.setString(7, auth.getNickname());
            pst.executeUpdate();
            return true;
        } catch (SQLException ex) {
            logSqlException(ex);
        }
        return false;
    }

    @Override
    public boolean updateEmail(PlayerAuth auth) {
        String sql = "UPDATE " + tableName + " SET " + col.EMAIL + " =? WHERE " + col.NAME + "=?;";
        try (Connection con = getConnection(); PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, auth.getEmail());
            pst.setString(2, auth.getNickname());
            pst.executeUpdate();
            return true;
        } catch (SQLException ex) {
            logSqlException(ex);
        }
        return false;
    }

    @Override
    public void closeConnection() {
        if (ds != null && !ds.isClosed()) {
            ds.close();
        }
    }

    @Override
    public List<String> getAllAuthsByIp(String ip) {
        List<String> result = new ArrayList<>();
        String sql = "SELECT " + col.NAME + " FROM " + tableName + " WHERE " + col.LAST_IP + "=?;";
        try (Connection con = getConnection(); PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, ip);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    result.add(rs.getString(col.NAME));
                }
            }
        } catch (SQLException ex) {
            logSqlException(ex);
        }
        return result;
    }

    @Override
    public int countAuthsByEmail(String email) {
        String sql = "SELECT COUNT(1) FROM " + tableName + " WHERE UPPER(" + col.EMAIL + ") = UPPER(?)";
        try (Connection con = getConnection(); PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, email);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException ex) {
            logSqlException(ex);
        }
        return 0;
    }

    @Override
    public void purgeRecords(Collection<String> toPurge) {
        String sql = "DELETE FROM " + tableName + " WHERE " + col.NAME + "=?;";
        try (Connection con = getConnection(); PreparedStatement pst = con.prepareStatement(sql)) {
            for (String name : toPurge) {
                pst.setString(1, name.toLowerCase());
                pst.executeUpdate();
            }
        } catch (SQLException ex) {
            logSqlException(ex);
        }
    }

    @Override
    public boolean isLogged(String user) {
        String sql = "SELECT " + col.IS_LOGGED + " FROM " + tableName + " WHERE " + col.NAME + "=?;";
        try (Connection con = getConnection(); PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, user);
            try (ResultSet rs = pst.executeQuery()) {
                return rs.next() && (rs.getInt(col.IS_LOGGED) == 1);
            }
        } catch (SQLException ex) {
            logSqlException(ex);
        }
        return false;
    }

    @Override
    public void setLogged(String user) {
        String sql = "UPDATE " + tableName + " SET " + col.IS_LOGGED + "=? WHERE " + col.NAME + "=?;";
        try (Connection con = getConnection(); PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setInt(1, 1);
            pst.setString(2, user.toLowerCase());
            pst.executeUpdate();
        } catch (SQLException ex) {
            logSqlException(ex);
        }
    }

    @Override
    public void setUnlogged(String user) {
        String sql = "UPDATE " + tableName + " SET " + col.IS_LOGGED + "=? WHERE " + col.NAME + "=?;";
        try (Connection con = getConnection(); PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setInt(1, 0);
            pst.setString(2, user.toLowerCase());
            pst.executeUpdate();
        } catch (SQLException ex) {
            logSqlException(ex);
        }
    }

    @Override
    public boolean hasSession(String user) {
        String sql = "SELECT " + col.HAS_SESSION + " FROM " + tableName + " WHERE " + col.NAME + "=?;";
        try (Connection con = getConnection(); PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, user.toLowerCase());
            try (ResultSet rs = pst.executeQuery()) {
                return rs.next() && (rs.getInt(col.HAS_SESSION) == 1);
            }
        } catch (SQLException ex) {
            logSqlException(ex);
        }
        return false;
    }

    @Override
    public void grantSession(String user) {
        String sql = "UPDATE " + tableName + " SET " + col.HAS_SESSION + "=? WHERE " + col.NAME + "=?;";
        try (Connection con = getConnection(); PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setInt(1, 1);
            pst.setString(2, user.toLowerCase());
            pst.executeUpdate();
        } catch (SQLException ex) {
            logSqlException(ex);
        }
    }

    @Override
    public void revokeSession(String user) {
        String sql = "UPDATE " + tableName + " SET " + col.HAS_SESSION + "=? WHERE " + col.NAME + "=?;";
        try (Connection con = getConnection(); PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setInt(1, 0);
            pst.setString(2, user.toLowerCase());
            pst.executeUpdate();
        } catch (SQLException ex) {
            logSqlException(ex);
        }
    }

    @Override
    public void purgeLogged() {
        String sql = "UPDATE " + tableName + " SET " + col.IS_LOGGED + "=? WHERE " + col.IS_LOGGED + "=?;";
        try (Connection con = getConnection(); PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setInt(1, 0);
            pst.setInt(2, 1);
            pst.executeUpdate();
        } catch (SQLException ex) {
            logSqlException(ex);
        }
    }

    @Override
    public int getAccountsRegistered() {
        int result = 0;
        String sql = "SELECT COUNT(*) FROM " + tableName;
        try (Connection con = getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) {
                result = rs.getInt(1);
            }
        } catch (SQLException ex) {
            logSqlException(ex);
        }
        return result;
    }

    @Override
    public boolean updateRealName(String user, String realName) {
        String sql = "UPDATE " + tableName + " SET " + col.REAL_NAME + "=? WHERE " + col.NAME + "=?;";
        try (Connection con = getConnection(); PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, realName);
            pst.setString(2, user);
            pst.executeUpdate();
            return true;
        } catch (SQLException ex) {
            logSqlException(ex);
        }
        return false;
    }

    @Override
    public DataSourceResult<String> getEmail(String user) {
        String sql = "SELECT " + col.EMAIL + " FROM " + tableName + " WHERE " + col.NAME + "=?;";
        try (Connection con = getConnection(); PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, user);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    return DataSourceResult.of(rs.getString(1));
                }
            }
        } catch (SQLException ex) {
            logSqlException(ex);
        }
        return DataSourceResult.unknownPlayer();
    }

    @Override
    public List<PlayerAuth> getAllAuths() {
        List<PlayerAuth> auths = new ArrayList<>();
        try (Connection con = getConnection(); Statement st = con.createStatement()) {
            try (ResultSet rs = st.executeQuery("SELECT * FROM " + tableName)) {
                while (rs.next()) {
                    PlayerAuth auth = buildAuthFromResultSet(rs);
                    sqlExtension.extendAuth(auth, rs.getInt(col.ID), con);
                    auths.add(auth);
                }
            }
        } catch (SQLException ex) {
            logSqlException(ex);
        }
        return auths;
    }

    @Override
    public List<String> getLoggedPlayersWithEmptyMail() {
        List<String> players = new ArrayList<>();
        String sql = "SELECT " + col.REAL_NAME + " FROM " + tableName + " WHERE " + col.IS_LOGGED + " = 1"
            + " AND (" + col.EMAIL + " = 'your@email.com' OR " + col.EMAIL + " IS NULL);";
        try (Connection con = getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                players.add(rs.getString(1));
            }
        } catch (SQLException ex) {
            logSqlException(ex);
        }
        return players;
    }

    @Override
    public List<PlayerAuth> getRecentlyLoggedInPlayers() {
        List<PlayerAuth> players = new ArrayList<>();
        String sql = "SELECT * FROM " + tableName + " ORDER BY " + col.LAST_LOGIN + " DESC LIMIT 10;";
        try (Connection con = getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                players.add(buildAuthFromResultSet(rs));
            }
        } catch (SQLException e) {
            logSqlException(e);
        }
        return players;
    }

    private PlayerAuth buildAuthFromResultSet(ResultSet row) throws SQLException {
        String salt = col.SALT.isEmpty() ? null : row.getString(col.SALT);
        int group = col.GROUP.isEmpty() ? -1 : row.getInt(col.GROUP);
        return PlayerAuth.builder()
            .name(row.getString(col.NAME))
            .realName(row.getString(col.REAL_NAME))
            .password(row.getString(col.PASSWORD), salt)
            .lastLogin(getNullableLong(row, col.LAST_LOGIN))
            .lastIp(row.getString(col.LAST_IP))
            .email(row.getString(col.EMAIL))
            .registrationDate(row.getLong(col.REGISTRATION_DATE))
            .registrationIp(row.getString(col.REGISTRATION_IP))
            .groupId(group)
            .locWorld(row.getString(col.LASTLOC_WORLD))
            .locX(row.getDouble(col.LASTLOC_X))
            .locY(row.getDouble(col.LASTLOC_Y))
            .locZ(row.getDouble(col.LASTLOC_Z))
            .locYaw(row.getFloat(col.LASTLOC_YAW))
            .locPitch(row.getFloat(col.LASTLOC_PITCH))
            .build();
    }
}
