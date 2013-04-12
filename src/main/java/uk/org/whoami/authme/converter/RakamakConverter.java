package uk.org.whoami.authme.converter;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map.Entry;

import uk.org.whoami.authme.AuthMe;
import uk.org.whoami.authme.ConsoleLogger;
import uk.org.whoami.authme.security.PasswordSecurity;
import uk.org.whoami.authme.security.PasswordSecurity.HashAlgorithm;
import uk.org.whoami.authme.settings.Settings;

/**
*
* @author Xephi59
*/
public class RakamakConverter {

	public AuthMe instance;

	public RakamakConverter (AuthMe instance) {
		this.instance = instance;
	}

	public RakamakConverter getInstance() {
		return this;
	}

	private static HashAlgorithm hash;
	private static Boolean useIP;
	private static String fileName;
	private static String ipFileName;
	private static File source;
	private static File output;
	private static File ipfiles;
	private static boolean alreadyExist = false;

	public static void RakamakConvert() throws IOException {
		hash = Settings.rakamakHash;
		useIP = Settings.rakamakUseIp;
		fileName = Settings.rakamakUsers;
		ipFileName = Settings.rakamakUsersIp;
		HashMap<String, String> playerIP = new HashMap<String, String>();
		HashMap<String, String> playerPSW = new HashMap<String, String>();
        try {
            source = new File(AuthMe.getInstance().getDataFolder() + File.separator + fileName);
            ipfiles = new File(AuthMe.getInstance().getDataFolder() + File.separator + ipFileName);
            output = new File(AuthMe.getInstance().getDataFolder() + File.separator + "auths.db");
            source.createNewFile();
            ipfiles.createNewFile();
            if (new File(AuthMe.getInstance().getDataFolder() + File.separator + "auths.db").exists()) {
            	alreadyExist  = true;
            }
            output.createNewFile();
    		BufferedReader users = null;
    		BufferedWriter outputDB = null;
    		BufferedReader ipFile = null;
            ipFile = new BufferedReader(new FileReader(ipfiles));
			String line;
            String newLine = null;
            if (useIP) {
            	String tempLine;
            	while ((tempLine = ipFile.readLine()) != null) {
            		if (tempLine.contains("=")) {
                		String[] args = tempLine.split("=");
                		playerIP.put(args[0], args[1]);
            		}
            	}
            }
            ipFile.close();
            users = new BufferedReader(new FileReader(source));
            while ((line = users.readLine()) != null) {
            	if (line.contains("=")) {
                	String[] arguments = line.split("=");
            		try {
            			playerPSW.put(arguments[0],PasswordSecurity.getHash(hash, arguments[1], arguments[0].toLowerCase()));
					} catch (NoSuchAlgorithmException e) {
						ConsoleLogger.showError(e.getMessage());
					}
            	}
            }
            users.close();
            outputDB = new BufferedWriter(new FileWriter(output));
				for (Entry<String, String> m : playerPSW.entrySet()) {
					if (useIP) {
						String player = m.getKey();
						String psw = playerPSW.get(player);
						String ip = playerIP.get(player);
						newLine = player + ":" + psw + ":" + ip + ":1325376060:0:0:0";
					} else {
						String player = m.getKey();
						String psw = playerPSW.get(player);
						String ip = "127.0.0.1";
						newLine = player + ":" + psw + ":" + ip + ":1325376060:0:0:0";
					}
					if (alreadyExist) outputDB.newLine();
					outputDB.write(newLine);
					System.out.println("Write line");
					outputDB.newLine();
				}
			outputDB.close();
			ConsoleLogger.info("Rakamak database has been converted to auths.db");
        } catch (FileNotFoundException ex) {
            ConsoleLogger.showError(ex.getMessage());
        } catch (IOException ex) {
            ConsoleLogger.showError(ex.getMessage());
        }
	}
}
