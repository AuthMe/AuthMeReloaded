/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package uk.org.whoami.authme;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 *
 * @author stefano
 */
public class LogActions {
    
        private boolean logData;
        private File dataLog;
        private boolean logMessages;
        private File messageLog;
        private String dataFolder;
        private String dLog,mLog;
        
   public LogActions() {
           try {
                if (logData)
                    this.dataLog = new File(dataFolder, dLog);
                if (logMessages)
                    this.messageLog = new File(dataFolder, mLog);
            } catch (Exception e){
               
                    ConsoleLogger.showError("[AuthMe] Error opening logfiles; bad filename?");
                if (logData)
                    this.dataLog = new File(dataFolder, "death_data.log");
                if (logMessages)
                    this.messageLog = new File(dataFolder, "death_messages.log");
            }
   }
          
        
   @SuppressWarnings("unused")
   private void initFiles() {
	  if (logData && !this.dataLog.exists()) {
		  try {
			    this.dataLog.createNewFile();
				BufferedWriter writer = new BufferedWriter(new FileWriter(this.dataLog,true));
				writer.write("#AuthMe Registration Log - This file stores serialized data on player regitration, changepassword and unregistration:");
				writer.newLine();
				writer.write("#Date | Time | Player | ip | Action [Register|ChangePassword|Unregister]");
				writer.newLine();
				writer.write("#Dont remove this file if you want to store all Registration Action");
				writer.newLine();
				writer.write("#If This file will become too big, rename it and let plugin create new one");
				writer.newLine();
				writer.close();
			} catch (IOException e) {
				ConsoleLogger.showError("[AuthMe] Error writing data log: ");
				e.printStackTrace();
			}
	  }
	  if (logMessages && !this.messageLog.exists()) {
		  try {
			    this.messageLog.createNewFile();
				BufferedWriter writer = new BufferedWriter(new FileWriter(this.messageLog,true));
				writer.write("#AuthMe Multiple Access Log - This file stores player Login, Wrong Password and Error");
				writer.newLine();
				writer.close();
			} catch (IOException e) {
				ConsoleLogger.showError("[AuthMe] Error writing message log: ");
				e.printStackTrace();
			}
	  }
  }
}
