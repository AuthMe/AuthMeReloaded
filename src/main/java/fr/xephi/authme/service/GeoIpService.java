package fr.xephi.authme.service;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.google.common.io.Resources;
import com.ice.tar.TarEntry;
import com.ice.tar.TarInputStream;
import com.maxmind.db.GeoIp2Provider;
import com.maxmind.db.Reader;
import com.maxmind.db.Reader.FileMode;
import com.maxmind.db.cache.CHMCache;
import com.maxmind.db.model.Country;
import com.maxmind.db.model.CountryResponse;

import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.initialization.DataFolder;
import fr.xephi.authme.util.FileUtils;
import fr.xephi.authme.util.InternetProtocolUtils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.zip.GZIPInputStream;

import javax.inject.Inject;

public class GeoIpService {

    private static final String LICENSE =
            "[LICENSE] This product includes GeoLite2 data created by MaxMind, available at https://www.maxmind.com";

    private static final String DATABASE_NAME = "GeoLite2-Country";
    private static final String DATABASE_EXT = ".mmdb";
    private static final String DATABASE_FILE = DATABASE_NAME + DATABASE_EXT;

    private static final String ARCHIVE_FILE = DATABASE_NAME + ".tar.gz";

    private static final String ARCHIVE_URL = "https://geolite.maxmind.com/download/geoip/database/" + ARCHIVE_FILE;
    private static final String CHECKSUM_URL = ARCHIVE_URL + ".md5";

    private static final int UPDATE_INTERVAL_DAYS = 30;

    private final Path dataFile;
    private final BukkitService bukkitService;

    private GeoIp2Provider databaseReader;
    private volatile boolean downloading;

    @Inject
    GeoIpService(@DataFolder File dataFolder, BukkitService bukkitService) {
        this.bukkitService = bukkitService;
        this.dataFile = dataFolder.toPath().resolve(DATABASE_FILE);

        // Fires download of recent data or the initialization of the look up service
        isDataAvailable();
    }

    @VisibleForTesting
    GeoIpService(@DataFolder File dataFolder, BukkitService bukkitService, GeoIp2Provider reader) {
        this.bukkitService = bukkitService;
        this.dataFile = dataFolder.toPath().resolve(DATABASE_FILE);

        this.databaseReader = reader;
    }

    /**
     * Download (if absent or old) the GeoIpLite data file and then try to load it.
     *
     * @return True if the data is available, false otherwise.
     */
    private synchronized boolean isDataAvailable() {
        if (downloading) {
            // we are currently downloading the database
            return false;
        }

        if (databaseReader != null) {
            // everything is initialized
            return true;
        }

        if (Files.exists(dataFile)) {
            try {
                FileTime lastModifiedTime = Files.getLastModifiedTime(dataFile);
                if (Duration.between(lastModifiedTime.toInstant(), Instant.now()).toDays() <= UPDATE_INTERVAL_DAYS) {
                    databaseReader = new Reader(dataFile.toFile(), FileMode.MEMORY, new CHMCache());
                    ConsoleLogger.info(LICENSE);

                    // don't fire the update task - we are up to date
                    return true;
                } else {
                    ConsoleLogger.debug("GEO Ip database is older than " + UPDATE_INTERVAL_DAYS + " Days");
                }
            } catch (IOException ioEx) {
                ConsoleLogger.logException("Failed to load GeoLiteAPI database", ioEx);
                return false;
            }
        }

        // File is outdated or doesn't exist - let's try to download the data file!
        startDownloadTask();
        return false;
    }

    /**
     * Create a thread which will attempt to download new data from the GeoLite website.
     */
    private void startDownloadTask() {
        downloading = true;

        // use bukkit's cached threads
        bukkitService.runTaskAsynchronously(() -> {
            ConsoleLogger.info("Downloading GEO IP database, because the old database is outdated or doesn't exist");

            Path tempFile = null;
            try {
                // download database to temporarily location
                tempFile = Files.createTempFile(ARCHIVE_FILE, null);
                try (OutputStream out = Files.newOutputStream(tempFile)) {
                    Resources.copy(new URL(ARCHIVE_URL), out);
                }

                // MD5 checksum verification
                String targetChecksum = Resources.toString(new URL(CHECKSUM_URL), StandardCharsets.UTF_8);
                if (!verifyChecksum(Hashing.md5(), tempFile, targetChecksum)) {
                    return;
                }

                // tar extract database and copy to target destination
                if (!extractDatabase(tempFile, dataFile)) {
                    ConsoleLogger.warning("Cannot find database inside downloaded GEO IP file at " + tempFile);
                    return;
                }

                ConsoleLogger.info("Successfully downloaded new GEO IP database to " + dataFile);

                //only set this value to false on success otherwise errors could lead to endless download triggers
                downloading = false;
            } catch (IOException ioEx) {
                ConsoleLogger.logException("Could not download GeoLiteAPI database", ioEx);
            } finally {
                // clean up
                if (tempFile != null) {
                    FileUtils.delete(tempFile.toFile());
                }
            }
        });
    }

    /**
     * Verify if the expected checksum is equal to the checksum of the given file.
     *
     * @param function the checksum function like MD5, SHA256 used to generate the checksum from the file
     * @param file the file we want to calculate the checksum from
     * @param expectedChecksum the expected checksum
     * @return true if equal, false otherwise
     * @throws IOException on I/O error reading the file
     */
    private boolean verifyChecksum(HashFunction function, Path file, String expectedChecksum) throws IOException {
        HashCode actualHash = function.hashBytes(Files.readAllBytes(file));
        HashCode expectedHash = HashCode.fromString(expectedChecksum);
        if (Objects.equals(actualHash, expectedHash)) {
            return true;
        }

        ConsoleLogger.warning("GEO IP checksum verification failed");
        ConsoleLogger.warning("Expected: " + expectedHash + " Actual: " + actualHash);
        return false;
    }

    /**
     * Extract the database from the tar archive. Existing outputFile will be replaced if it already exists.
     *
     * @param tarInputFile gzipped tar input file where the database is
     * @param outputFile destination file for the database
     * @return true if the database was found, false otherwise
     * @throws IOException on I/O error reading the tar archive or writing the output
     */
    private boolean extractDatabase(Path tarInputFile, Path outputFile) throws IOException {
        // .gz -> gzipped file
        try (BufferedInputStream in = new BufferedInputStream(Files.newInputStream(tarInputFile));
             TarInputStream tarIn = new TarInputStream(new GZIPInputStream(in))) {
            TarEntry entry;
            while ((entry = tarIn.getNextEntry()) != null) {
                if (!entry.isDirectory()) {
                    // filename including folders (absolute path inside the archive)
                    String filename = entry.getName();
                    if (filename.endsWith(DATABASE_EXT)) {
                        // found the database file
                        Files.copy(tarIn, outputFile, StandardCopyOption.REPLACE_EXISTING);

                        // update the last modification date to be same as in the archive
                        Files.setLastModifiedTime(outputFile, FileTime.from(entry.getModTime().toInstant()));
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /**
     * Get the country code of the given IP address.
     *
     * @param ip textual IP address to lookup.
     * @return two-character ISO 3166-1 alpha code for the country.
     */
    public String getCountryCode(String ip) {
        return getCountry(ip).map(Country::getIsoCode).orElse("--");
    }

    /**
     * Get the country name of the given IP address.
     *
     * @param ip textual IP address to lookup.
     * @return The name of the country.
     */
    public String getCountryName(String ip) {
        return getCountry(ip).map(Country::getName).orElse("N/A");
    }

    /**
     * Get the country of the given IP address
     *
     * @param ip textual IP address to lookup
     * @return the wrapped Country model or {@link Optional#empty()} if
     * <ul>
     *     <li>Database reader isn't initialized</li>
     *     <li>MaxMind has no record about this IP address</li>
     *     <li>IP address is local</li>
     *     <li>Textual representation is not a valid IP address</li>
     * </ul>
     */
    private Optional<Country> getCountry(String ip) {
        if (ip == null || ip.isEmpty() || InternetProtocolUtils.isLocalAddress(ip) || !isDataAvailable()) {
            return Optional.empty();
        }

        try {
            InetAddress address = InetAddress.getByName(ip);

            //Reader.getCountry() can be null for unknown addresses
            return Optional.ofNullable(databaseReader.getCountry(address)).map(CountryResponse::getCountry);
        } catch (UnknownHostException e) {
            // Ignore invalid ip addresses
            // Legacy GEO IP Database returned a unknown country object with Country-Code: '--' and Country-Name: 'N/A'
        } catch (IOException ioEx) {
            ConsoleLogger.logException("Cannot lookup country for " + ip + " at GEO IP database", ioEx);
        }

        return Optional.empty();
    }
}
