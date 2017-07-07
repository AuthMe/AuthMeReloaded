package fr.xephi.authme.service;

import com.google.common.annotations.VisibleForTesting;
import com.maxmind.geoip.LookupService;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.initialization.DataFolder;
import fr.xephi.authme.util.FileUtils;
import fr.xephi.authme.util.InternetProtocolUtils;

import javax.inject.Inject;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;

import static com.maxmind.geoip.LookupService.GEOIP_MEMORY_CACHE;

public class GeoIpService {
    private static final String LICENSE =
        "[LICENSE] This product uses data from the GeoLite API created by MaxMind, available at http://www.maxmind.com";
    private static final String GEOIP_URL =
        "http://geolite.maxmind.com/download/geoip/database/GeoLiteCountry/GeoIP.dat.gz";
    private LookupService lookupService;
    private Thread downloadTask;

    private final File dataFile;

    @Inject
    GeoIpService(@DataFolder File dataFolder) {
        this.dataFile = new File(dataFolder, "GeoIP.dat");
        // Fires download of recent data or the initialization of the look up service
        isDataAvailable();
    }

    @VisibleForTesting
    GeoIpService(@DataFolder File dataFolder, LookupService lookupService) {
        this.dataFile = dataFolder;
        this.lookupService = lookupService;
    }

    /**
     * Download (if absent or old) the GeoIpLite data file and then try to load it.
     *
     * @return True if the data is available, false otherwise.
     */
    private synchronized boolean isDataAvailable() {
        if (downloadTask != null && downloadTask.isAlive()) {
            return false;
        }
        if (lookupService != null) {
            return true;
        }

        if (dataFile.exists()) {
            boolean dataIsOld = (System.currentTimeMillis() - dataFile.lastModified()) > TimeUnit.DAYS.toMillis(30);
            if (!dataIsOld) {
                try {
                    lookupService = new LookupService(dataFile, GEOIP_MEMORY_CACHE);
                    ConsoleLogger.info(LICENSE);
                    return true;
                } catch (IOException e) {
                    ConsoleLogger.logException("Failed to load GeoLiteAPI database", e);
                    return false;
                }
            } else {
                FileUtils.delete(dataFile);
            }
        }
        // Ok, let's try to download the data file!
        downloadTask = createDownloadTask();
        downloadTask.start();
        return false;
    }

    /**
     * Create a thread which will attempt to download new data from the GeoLite website.
     *
     * @return the generated download thread
     */
    private Thread createDownloadTask() {
        return new Thread(new Runnable() {
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
                    OutputStream output = new FileOutputStream(dataFile);
                    byte[] buffer = new byte[2048];
                    int length = input.read(buffer);
                    while (length >= 0) {
                        output.write(buffer, 0, length);
                        length = input.read(buffer);
                    }
                    output.close();
                    input.close();
                } catch (IOException e) {
                    ConsoleLogger.logException("Could not download GeoLiteAPI database", e);
                }
            }
        });
    }

    /**
     * Get the country code of the given IP address.
     *
     * @param ip textual IP address to lookup.
     *
     * @return two-character ISO 3166-1 alpha code for the country.
     */
    public String getCountryCode(String ip) {
        if (!InternetProtocolUtils.isLocalAddress(ip) && isDataAvailable()) {
            return lookupService.getCountry(ip).getCode();
        }
        return "--";
    }

    /**
     * Get the country name of the given IP address.
     *
     * @param ip textual IP address to lookup.
     *
     * @return The name of the country.
     */
    public String getCountryName(String ip) {
        if (!InternetProtocolUtils.isLocalAddress(ip) && isDataAvailable()) {
            return lookupService.getCountry(ip).getName();
        }
        return "N/A";
    }

}
