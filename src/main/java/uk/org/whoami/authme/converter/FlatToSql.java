package uk.org.whoami.authme.converter;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import uk.org.whoami.authme.AuthMe;
import uk.org.whoami.authme.ConsoleLogger;
import uk.org.whoami.authme.settings.Settings;

/**
*
* @author Xephi59
*/
public class FlatToSql {

	public AuthMe instance;

	public FlatToSql (AuthMe instance) {
		this.instance = instance;
	}

	public FlatToSql getInstance() {
		return this;
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
	private static File output;

	public static void FlatToSqlConverter() throws IOException {
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

        try {
            source = new File(AuthMe.getInstance().getDataFolder() + File.separator + "auths.db");
            source.createNewFile();
            output = new File(AuthMe.getInstance().getDataFolder() + File.separator + "authme.sql");
    		BufferedReader br = null;
    		BufferedWriter sql = null;
            br = new BufferedReader(new FileReader(source));
            sql = new BufferedWriter(new FileWriter(output));
            String createDB = " CREATE TABLE IF NOT EXISTS " + tableName + " ("
                    + "id INTEGER AUTO_INCREMENT,"
                    + columnName + " VARCHAR(255) NOT NULL UNIQUE,"
                    + columnPassword + " VARCHAR(255) NOT NULL,"
                    + columnIp + " VARCHAR(40) NOT NULL,"
                    + columnLastLogin + " BIGINT,"
                    + lastlocX + " smallint(6) DEFAULT '0',"
                    + lastlocY + " smallint(6) DEFAULT '0',"
                    + lastlocZ + " smallint(6) DEFAULT '0',"
                    + lastlocWorld + " VARCHAR(255) DEFAULT 'world',"
                    + columnEmail + " VARCHAR(255) NOT NULL,"
                    + "CONSTRAINT table_const_prim PRIMARY KEY (id));";
            sql.write(createDB);
            String line;
            int i = 1;
            String newline;
            while ((line = br.readLine()) != null) {
            	sql.newLine();
                String[] args = line.split(":");
                if (args.length == 4)
                	newline = "INSERT INTO " + tableName + " VALUES (" + i + ", '" + args[0] + "', '" + args[1] + "', '" + args[2] + "', " + args[3] + ", 0, 0, 0, 'world', 'your@email.com');";
                else if (args.length == 7)
                	newline = "INSERT INTO " + tableName + " VALUES (" + i + ", '" + args[0] + "', '" + args[1] + "', '" + args[2] + "', " + args[3] + ", " + args[4] + ", " + args[5] + ", " + args[6] + ", 'world', 'your@email.com');";
                else if (args.length == 8)
                	newline = "INSERT INTO " + tableName + " VALUES (" + i + ", '" + args[0] + "', '" + args[1] + "', '" + args[2] + "', " + args[3] + ", " + args[4] + ", " + args[5] + ", " + args[6] + ", " + args[7] + ", 'your@email.com');";
                else
                	newline = "";
                if (newline != "")
                sql.write(newline);
                i = i + 1;
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
