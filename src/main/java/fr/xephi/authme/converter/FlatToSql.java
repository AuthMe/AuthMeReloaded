package fr.xephi.authme.converter;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.settings.Settings;

/**
 *
 * @author Xephi59
 */
public class FlatToSql implements Converter {

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
    private static String columnLogged;
    private static String columnID;
    private static File source;
    private static File output;

    public FlatToSql() {
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
        columnLogged = Settings.getMySQLColumnLogged;
        columnID = Settings.getMySQLColumnId;
    }

    @Override
    public void run() {
        try {
            source = new File(AuthMe.getInstance().getDataFolder() + File.separator + "auths.db");
            source.createNewFile();
            output = new File(AuthMe.getInstance().getDataFolder() + File.separator + "authme.sql");
            output.createNewFile();
            BufferedReader br = new BufferedReader(new FileReader(source));
            BufferedWriter sql = new BufferedWriter(new FileWriter(output));
            String createDB = "CREATE TABLE IF NOT EXISTS " + tableName + " (" + columnID + " INTEGER AUTO_INCREMENT," + columnName + " VARCHAR(255) NOT NULL UNIQUE," + columnPassword + " VARCHAR(255) NOT NULL," + columnIp + " VARCHAR(40) NOT NULL DEFAULT '127.0.0.1'," + columnLastLogin + " BIGINT NOT NULL DEFAULT '" + System.currentTimeMillis() + "'," + lastlocX + " DOUBLE NOT NULL DEFAULT '0.0'," + lastlocY + " DOUBLE NOT NULL DEFAULT '0.0'," + lastlocZ + " DOUBLE NOT NULL DEFAULT '0.0'," + lastlocWorld + " VARCHAR(255) DEFAULT 'world'," + columnEmail + " VARCHAR(255) DEFAULT 'your@email.com'," + columnLogged + " SMALLINT NOT NULL DEFAULT '0'," + "CONSTRAINT table_const_prim PRIMARY KEY (" + columnID + "));";
            sql.write(createDB);
            String line;
            String newline;
            while ((line = br.readLine()) != null) {
                sql.newLine();
                String[] args = line.split(":");
                if (args.length == 4)
                    newline = "INSERT INTO " + tableName + "(" + columnName + "," + columnPassword + "," + columnIp + "," + columnLastLogin + "," + lastlocX + "," + lastlocY + "," + lastlocZ + "," + lastlocWorld + "," + columnEmail + "," + columnLogged + ") VALUES ('" + args[0] + "', '" + args[1] + "', '" + args[2] + "', " + args[3] + ", 0.0, 0.0, 0.0, 'world', 'your@email.com', 0);";
                else if (args.length == 7)
                    newline = "INSERT INTO " + tableName + "(" + columnName + "," + columnPassword + "," + columnIp + "," + columnLastLogin + "," + lastlocX + "," + lastlocY + "," + lastlocZ + "," + lastlocWorld + "," + columnEmail + "," + columnLogged + ") VALUES ('" + args[0] + "', '" + args[1] + "', '" + args[2] + "', " + args[3] + ", " + args[4] + ", " + args[5] + ", " + args[6] + ", 'world', 'your@email.com', 0);";
                else if (args.length == 8)
                    newline = "INSERT INTO " + tableName + "(" + columnName + "," + columnPassword + "," + columnIp + "," + columnLastLogin + "," + lastlocX + "," + lastlocY + "," + lastlocZ + "," + lastlocWorld + "," + columnEmail + "," + columnLogged + ") VALUES ('" + args[0] + "', '" + args[1] + "', '" + args[2] + "', " + args[3] + ", " + args[4] + ", " + args[5] + ", " + args[6] + ", '" + args[7] + "', 'your@email.com', 0);";
                else if (args.length == 9)
                    newline = "INSERT INTO " + tableName + "(" + columnName + "," + columnPassword + "," + columnIp + "," + columnLastLogin + "," + lastlocX + "," + lastlocY + "," + lastlocZ + "," + lastlocWorld + "," + columnEmail + "," + columnLogged + ") VALUES ('" + args[0] + "', '" + args[1] + "', '" + args[2] + "', " + args[3] + ", " + args[4] + ", " + args[5] + ", " + args[6] + ", '" + args[7] + "', '" + args[8] + "', 0);";
                else newline = "";
                if (newline != "")
                    sql.write(newline);
            }
            sql.close();
            br.close();
            ConsoleLogger.info("The FlatFile has been converted to authme.sql file");
        } catch (FileNotFoundException ex) {
            ConsoleLogger.showError(ex.getMessage());
        } catch (IOException ex) {
            ConsoleLogger.showError(ex.getMessage());
        }
    }
}
