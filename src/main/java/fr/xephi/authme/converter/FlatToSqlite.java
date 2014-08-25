package fr.xephi.authme.converter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.bukkit.command.CommandSender;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.settings.Settings;

public class FlatToSqlite implements Converter {

    public CommandSender sender;

    public FlatToSqlite(CommandSender sender) {
        this.sender = sender;
    }

    private static String tableName;
    private static String columnName;
    private static String columnPassword;
    private static String columnIp;
    private static String columnLastLogin;
    private static String lastlocX;
    private static String lastlocY;
    private static String lastlocZ;
    private static String lastlocWorld;
    private static String columnEmail;
    private static File source;
    private static String database;
    private static String columnID;
    private static Connection con;

    @Override
    public void run() {
        database = Settings.getMySQLDatabase;
        tableName = Settings.getMySQLTablename;
        columnName = Settings.getMySQLColumnName;
        columnPassword = Settings.getMySQLColumnPassword;
        columnIp = Settings.getMySQLColumnIp;
        columnLastLogin = Settings.getMySQLColumnLastLogin;
        lastlocX = Settings.getMySQLlastlocX;
        lastlocY = Settings.getMySQLlastlocY;
        lastlocZ = Settings.getMySQLlastlocZ;
        lastlocWorld = Settings.getMySQLlastlocWorld;
        columnEmail = Settings.getMySQLColumnEmail;
        columnID = Settings.getMySQLColumnId;
        ConsoleLogger.info("Converting FlatFile to SQLite ...");
        if (new File(AuthMe.getInstance().getDataFolder() + File.separator + database + ".db").exists()) {
            sender.sendMessage("The Database " + database + ".db can't be created cause the file already exist");
            return;
        }
        try {
            connect();
            setup();
        } catch (Exception e) {
            ConsoleLogger.showError("Problem while trying to convert to sqlite !");
            sender.sendMessage("Problem while trying to convert to sqlite !");
            return;
        }
        try {
            source = new File(AuthMe.getInstance().getDataFolder() + File.separator + "auths.db");
            source.createNewFile();
            BufferedReader br = new BufferedReader(new FileReader(source));
            String line;
            int i = 1;
            String newline;
            while ((line = br.readLine()) != null) {
                String[] args = line.split(":");
                if (args.length == 4)
                    newline = "INSERT INTO " + tableName + " VALUES (" + i + ", '" + args[0] + "', '" + args[1] + "', '" + args[2] + "', " + args[3] + ", 0, 0, 0, 'world', 'your@email.com');";
                else if (args.length == 7)
                    newline = "INSERT INTO " + tableName + " VALUES (" + i + ", '" + args[0] + "', '" + args[1] + "', '" + args[2] + "', " + args[3] + ", " + args[4] + ", " + args[5] + ", " + args[6] + ", 'world', 'your@email.com');";
                else if (args.length == 8)
                    newline = "INSERT INTO " + tableName + " VALUES (" + i + ", '" + args[0] + "', '" + args[1] + "', '" + args[2] + "', " + args[3] + ", " + args[4] + ", " + args[5] + ", " + args[6] + ", '" + args[7] + "', 'your@email.com');";
                else if (args.length == 9)
                    newline = "INSERT INTO " + tableName + " VALUES (" + i + ", '" + args[0] + "', '" + args[1] + "', '" + args[2] + "', " + args[3] + ", " + args[4] + ", " + args[5] + ", " + args[6] + ", '" + args[7] + "', '" + args[8] + "');";
                else newline = "";
                if (newline != "")
                    saveAuth(newline);
                i = i + 1;
            }
            br.close();
            ConsoleLogger.info("The FlatFile has been converted to " + database + ".db file");
            close();
            sender.sendMessage("The FlatFile has been converted to " + database + ".db file");
            return;
        } catch (FileNotFoundException ex) {
            ConsoleLogger.showError(ex.getMessage());
        } catch (IOException ex) {
            ConsoleLogger.showError(ex.getMessage());
        }
        sender.sendMessage("Errors appears while trying to convert to SQLite");
        return;
    }

    private synchronized static void connect() throws ClassNotFoundException,
            SQLException {
        Class.forName("org.sqlite.JDBC");
        ConsoleLogger.info("SQLite driver loaded");
        con = DriverManager.getConnection("jdbc:sqlite:plugins/AuthMe/" + database + ".db");
    }

    private synchronized static void setup() throws SQLException {
        Statement st = null;
        ResultSet rs = null;
        try {
            st = con.createStatement();
            st.executeUpdate("CREATE TABLE IF NOT EXISTS " + tableName + " (" + columnID + " INTEGER AUTO_INCREMENT," + columnName + " VARCHAR(255) NOT NULL UNIQUE," + columnPassword + " VARCHAR(255) NOT NULL," + columnIp + " VARCHAR(40) NOT NULL," + columnLastLogin + " BIGINT," + lastlocX + " smallint(6) DEFAULT '0'," + lastlocY + " smallint(6) DEFAULT '0'," + lastlocZ + " smallint(6) DEFAULT '0'," + lastlocWorld + " VARCHAR(255) DEFAULT 'world'," + columnEmail + " VARCHAR(255) DEFAULT 'your@email.com'," + "CONSTRAINT table_const_prim PRIMARY KEY (" + columnID + "));");
            rs = con.getMetaData().getColumns(null, null, tableName, columnPassword);
            if (!rs.next()) {
                st.executeUpdate("ALTER TABLE " + tableName + " ADD COLUMN " + columnPassword + " VARCHAR(255) NOT NULL;");
            }
            rs.close();
            rs = con.getMetaData().getColumns(null, null, tableName, columnIp);
            if (!rs.next()) {
                st.executeUpdate("ALTER TABLE " + tableName + " ADD COLUMN " + columnIp + " VARCHAR(40) NOT NULL;");
            }
            rs.close();
            rs = con.getMetaData().getColumns(null, null, tableName, columnLastLogin);
            if (!rs.next()) {
                st.executeUpdate("ALTER TABLE " + tableName + " ADD COLUMN " + columnLastLogin + " BIGINT;");
            }
            rs.close();
            rs = con.getMetaData().getColumns(null, null, tableName, lastlocX);
            if (!rs.next()) {
                st.executeUpdate("ALTER TABLE " + tableName + " ADD COLUMN " + lastlocX + " smallint(6) NOT NULL DEFAULT '0'; " + "ALTER TABLE " + tableName + " ADD COLUMN " + lastlocY + " smallint(6) NOT NULL DEFAULT '0'; " + "ALTER TABLE " + tableName + " ADD COLUMN " + lastlocZ + " smallint(6) NOT NULL DEFAULT '0';");
            }
            rs.close();
            rs = con.getMetaData().getColumns(null, null, tableName, lastlocWorld);
            if (!rs.next()) {
                st.executeUpdate("ALTER TABLE " + tableName + " ADD COLUMN " + lastlocWorld + " VARCHAR(255) NOT NULL DEFAULT 'world' AFTER " + lastlocZ + ";");
            }
            rs.close();
            rs = con.getMetaData().getColumns(null, null, tableName, columnEmail);
            if (!rs.next()) {
                st.executeUpdate("ALTER TABLE " + tableName + " ADD COLUMN " + columnEmail + "  VARCHAR(255) DEFAULT 'your@email.com';");
            }
        } finally {
            close(rs);
            close(st);
        }
        ConsoleLogger.info("SQLite Setup finished");
    }

    private static synchronized boolean saveAuth(String s) {
        PreparedStatement pst = null;
        try {
            pst = con.prepareStatement(s);
            pst.executeUpdate();
        } catch (SQLException e) {
            ConsoleLogger.showError(e.getMessage());
            return false;
        } finally {
            close(pst);
        }
        return true;
    }

    private static void close(Statement st) {
        if (st != null) {
            try {
                st.close();
            } catch (SQLException ex) {
                ConsoleLogger.showError(ex.getMessage());
            }
        }
    }

    private static void close(ResultSet rs) {
        if (rs != null) {
            try {
                rs.close();
            } catch (SQLException ex) {
                ConsoleLogger.showError(ex.getMessage());
            }
        }
    }

    public synchronized static void close() {
        try {
            con.close();
        } catch (SQLException ex) {
            ConsoleLogger.showError(ex.getMessage());
        }
    }
}
