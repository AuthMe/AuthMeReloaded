package fr.xephi.authme;

import com.google.common.base.Throwables;
import fr.xephi.authme.settings.Settings;
import org.bukkit.Bukkit;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.logging.Logger;

public class ConsoleLogger {

    private static final Logger log = Logger.getLogger("AuthMe");

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
            log.warning("[AuthMe] " + message);
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
            BufferedWriter w = new BufferedWriter(fw);
            w.write(string);
            w.newLine();
            w.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void writeStackTrace(Exception ex){
        writeLog(Throwables.getStackTraceAsString(ex));
    }
}
