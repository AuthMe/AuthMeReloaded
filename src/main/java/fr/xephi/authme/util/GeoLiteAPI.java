package fr.xephi.authme.util;

import com.maxmind.geoip.LookupService;
import fr.xephi.authme.AuthMe;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.settings.Settings;
import org.bukkit.Bukkit;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.zip.GZIPInputStream;

public class GeoLiteAPI {

    private static final String LICENSE = "[LICENSE] This product uses data from the GeoLite API created by MaxMind, " +
        "available at http://www.maxmind.com";
    private static final String GEOIP_URL = "http://geolite.maxmind.com/download/geoip/database/GeoLiteCountry" +
        "/GeoIP.dat.gz";
    private static final AuthMe plugin = AuthMe.getInstance();
    private static LookupService lookupService;

    /**
     * Download (if absent) the GeoIpLite data file and then try to load it.
     *
     * @return True if the data is available, false otherwise.
     */
    public static boolean isDataAvailable() {
        if (lookupService != null) {
            return true;
        }
        final File data = new File(Settings.PLUGIN_FOLDER, "GeoIP.dat");
        if (data.exists()) {
            try {
                lookupService = new LookupService(data);
                plugin.getLogger().info(LICENSE);
                return true;
            } catch (IOException e) {
            	ConsoleLogger.writeStackTrace("Could not find/download GeoLiteAPI", e);
                return false;
            }
        }
        // Ok, let's try to download the data file!
        Bukkit.getScheduler().runTaskAsynchronously(plugin, new Runnable() {
            @Override
            public void run() {
                try {
                    URL downloadUrl = new URL(GEOIP_URL);
                    URLConnection conn = downloadUrl.openConnection();
                    conn.setConnectTimeout(10000);
                    conn.connect();
                    InputStream input = conn.getInputStream();
                    if (conn.getURL().toString().endsWith(".gz")) {
                        input = new GZIPInputStream(input);
                    }
                    OutputStream output = new FileOutputStream(data);
                    byte[] buffer = new byte[2048];
                    int length = input.read(buffer);
                    while (length >= 0) {
                        output.write(buffer, 0, length);
                        length = input.read(buffer);
                    }
                    output.close();
                    input.close();
                } catch (IOException e) {
                    ConsoleLogger.writeStackTrace("Could not download GeoLiteAPI", e);
                }
            }
        });
        return false;
    }

    /**
     * Get the country code of the given IP address.
     *
     * @param ip Ip address
     *
     * @return String
     */
    public static String getCountryCode(String ip) {
        if (isDataAvailable()) {
            return lookupService.getCountry(ip).getCode();
        }
        return "--";
    }

    /**
     * Get the country name of the given IP address.
     *
     * @param ip Ip address
     *
     * @return String
     */
    public static String getCountryName(String ip) {
        if (isDataAvailable()) {
            return lookupService.getCountry(ip).getName();
        }
        return "N/A";
    }

}
