package fr.xephi.authme.converter;

import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.settings.Settings;
import org.bukkit.command.CommandSender;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.*;

/**
 */
public class FlatToSqlite implements Converter {

    public final CommandSender sender;
    private String tableName;
    private String columnName;
    private String columnPassword;
    private String columnIp;
    private String columnLastLogin;
    private String lastlocX;
    private String lastlocY;
    private String lastlocZ;
    private String lastlocWorld;
    private String columnEmail;
    private String database;
    private String columnID;
    private Connection con;

    /**
     * Constructor for FlatToSqlite.
     *
     * @param sender CommandSender
     */
    public FlatToSqlite(CommandSender sender) {
        this.sender = sender;
    }

    /**
     * Method close.
     *
     * @param o AutoCloseable
     */
    private static void close(AutoCloseable o) {
        if (o != null) {
            try {
                o.close();
            } catch (Exception ex) {
                ConsoleLogger.showError(ex.getMessage());
            }
        }
    }

    /**
     * Method run.
     *
     * @see java.lang.Runnable#run()
     */
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

        File source = new File(Settings.PLUGIN_FOLDER, "auths.db");
        if (!source.exists()) {
            sender.sendMessage("Source file for FlatFile database not found... Aborting");
            return;
        }

        try {
            connect();
            setup();
        } catch (Exception e) {
            sender.sendMessage("Some error appeared while trying to setup and connect to sqlite database... Aborting");
            return;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(source))) {
            String line;
            int i = 1;
            String newline;
            while ((line = reader.readLine()) != null) {
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
                if (!newline.equals(""))
                    saveAuth(newline);
                i = i + 1;
            }
            String resp = "The FlatFile has been converted to " + database + ".db file";
            ConsoleLogger.info(resp);
            sender.sendMessage(resp);
        } catch (IOException ex) {
            ConsoleLogger.showError(ex.getMessage());
            sender.sendMessage("Can't open the flat database file!");
        } finally {
            close(con);
        }
    }

    /**
     * Method connect.
     *
     * @throws ClassNotFoundException * @throws SQLException
     */
    private synchronized void connect() throws ClassNotFoundException, SQLException {
        Class.forName("org.sqlite.JDBC");
        con = DriverManager.getConnection("jdbc:sqlite:plugins/AuthMe/" + database + ".db");
    }

    /**
     * Method setup.
     *
     * @throws SQLException
     */
    private synchronized void setup() throws SQLException {
        Statement st = null;
        ResultSet rs = null;
        try {
            st = con.createStatement();
            st.executeUpdate("CREATE TABLE IF NOT EXISTS " + tableName + " (" + columnID + " INTEGER AUTO_INCREMENT," + columnName + " VARCHAR(255) NOT NULL UNIQUE," + columnPassword + " VARCHAR(255) NOT NULL," + columnIp + " VARCHAR(40) NOT NULL," + columnLastLogin + " BIGINT," + lastlocX + " DOUBLE NOT NULL DEFAULT '0.0'," + lastlocY + " DOUBLE NOT NULL DEFAULT '0.0'," + lastlocZ + " DOUBLE NOT NULL DEFAULT '0.0'," + lastlocWorld + " VARCHAR(255) NOT NULL DEFAULT '" + Settings.defaultWorld + "'," + columnEmail + " VARCHAR(255) DEFAULT 'your@email.com'," + "CONSTRAINT table_const_prim PRIMARY KEY (" + columnID + "));");
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
                st.executeUpdate("ALTER TABLE " + tableName + " ADD COLUMN " + lastlocX + " DOUBLE NOT NULL DEFAULT '0.0';");
                st.executeUpdate("ALTER TABLE " + tableName + " ADD COLUMN " + lastlocY + " DOUBLE NOT NULL DEFAULT '0.0';");
                st.executeUpdate("ALTER TABLE " + tableName + " ADD COLUMN " + lastlocZ + " DOUBLE NOT NULL DEFAULT '0.0';");
            }
            rs.close();
            rs = con.getMetaData().getColumns(null, null, tableName, lastlocWorld);
            if (!rs.next()) {
                st.executeUpdate("ALTER TABLE " + tableName + " ADD COLUMN " + lastlocWorld + " VARCHAR(255) NOT NULL DEFAULT 'world';");
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
    }

    /**
     * Method saveAuth.
     *
     * @param s String
     *
     * @return boolean
     */
    private synchronized boolean saveAuth(String s) {
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
}
