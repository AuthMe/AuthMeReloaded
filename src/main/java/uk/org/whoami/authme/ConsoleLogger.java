/*
 * Copyright 2011 Sebastian Köhler <sebkoehler@whoami.org.uk>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.org.whoami.authme;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.logging.Logger;

import org.bukkit.Bukkit;

import uk.org.whoami.authme.settings.Settings;

public class ConsoleLogger {

    private static final Logger log = Logger.getLogger("Minecraft");

    public static void info(String message) {
    	if (AuthMe.getInstance().isEnabled()) {
    		log.info("[AuthMe] " + message);
    		if (Settings.useLogging) {
    			Calendar date = Calendar.getInstance();
    			final String actually = "[" + DateFormat.getDateInstance().format(date.getTime()) + ", " + date.get(Calendar.HOUR_OF_DAY) + ":" + date.get(Calendar.MINUTE) + ":" + date.get(Calendar.SECOND) + "] " + message;
    			Bukkit.getScheduler().runTaskAsynchronously(AuthMe.getInstance(), new Runnable() {
    				@Override
    				public void run() {
    					writeLog(actually);
    				}
    			});
    		}
    	}
    }

    public static void showError(String message) {
    	if (AuthMe.getInstance().isEnabled()) {
            log.severe("[AuthMe] ERROR: " + message);
            if (Settings.useLogging) {
                Calendar date = Calendar.getInstance();
                final String actually = "[" + DateFormat.getDateInstance().format(date.getTime()) + ", " + date.get(Calendar.HOUR_OF_DAY) + ":" + date.get(Calendar.MINUTE) + ":" + date.get(Calendar.SECOND) + "] ERROR : " + message;
                Bukkit.getScheduler().runTaskAsynchronously(AuthMe.getInstance(), new Runnable() {
        			@Override
        			public void run() {
        				writeLog(actually);
        			}
                });
            }
    	}
    }

	public static void writeLog(String string) {
        try {
        	FileWriter fw = new FileWriter(AuthMe.getInstance().getDataFolder() + File.separator + "authme.log", true);
    		BufferedWriter w = null;
			w = new BufferedWriter(fw);
			w.write(string);
			w.newLine();
			w.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
