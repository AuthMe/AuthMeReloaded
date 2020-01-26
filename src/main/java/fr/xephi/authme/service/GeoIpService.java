package fr.xephi.authme.service;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.maxmind.db.GeoIp2Provider;
import com.maxmind.db.Reader;
import com.maxmind.db.Reader.FileMode;
import com.maxmind.db.cache.CHMCache;
import com.maxmind.db.model.Country;
import com.maxmind.db.model.CountryResponse;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.initialization.DataFolder;
import fr.xephi.authme.output.ConsoleLoggerFactory;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.ProtectionSettings;
import fr.xephi.authme.util.FileUtils;
import fr.xephi.authme.util.InternetProtocolUtils;

import javax.inject.Inject;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Objects;
import java.util.Optional;
import java.util.zip.GZIPInputStream;

public class GeoIpService {

    private static final String LICENSE =
            "[LICENSE] This product includes GeoLite2 data created by MaxMind, available at https://www.maxmind.com";

    private static final String DATABASE_NAME = "GeoLite2-Country";
    private static final String DATABASE_FILE = DATABASE_NAME + ".mmdb";
    private static final String DATABASE_TMP_FILE = DATABASE_NAME + ".mmdb.tmp";

    private static final String ARCHIVE_FILE = DATABASE_NAME + ".mmdb.gz";

    private static final String ARCHIVE_URL =
        "https://updates.maxmind.com/geoip/databases/" + DATABASE_NAME + "/update";

    private static final int UPDATE_INTERVAL_DAYS = 30;

    private final ConsoleLogger logger = ConsoleLoggerFactory.get(GeoIpService.class);
    private final Path dataFile;
    private final BukkitService bukkitService;
    private final Settings settings;

    private GeoIp2Provider databaseReader;
    private volatile boolean downloading;

    @Inject
    GeoIpService(@DataFolder File dataFolder, BukkitService bukkitService, Settings settings) {
        this.bukkitService = bukkitService;
        this.dataFile = dataFolder.toPath().resolve(DATABASE_FILE);
        this.settings = settings;

        // Fires download of recent data or the initialization of the look up service
        isDataAvailable();
    }

    @VisibleForTesting
    GeoIpService(@DataFolder File dataFolder, BukkitService bukkitService, Settings settings, GeoIp2Provider reader) {
        this.bukkitService = bukkitService;
        this.settings = settings;
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
                    startReading();

                    // don't fire the update task - we are up to date
                    return true;
                } else {
                    logger.debug("GEO IP database is older than " + UPDATE_INTERVAL_DAYS + " Days");
                }
            } catch (IOException ioEx) {
                logger.logException("Failed to load GeoLiteAPI database", ioEx);
                return false;
            }
        }

        //set the downloading flag in order to fix race conditions outside
        downloading = true;

        // File is outdated or doesn't exist - let's try to download the data file!
        // use bukkit's cached threads
        bukkitService.runTaskAsynchronously(this::updateDatabase);
        return false;
    }

    /**
     * Tries to update the database by downloading a new version from the website.
     */
    private void updateDatabase() {
        logger.info("Downloading GEO IP database, because the old database is older than "
                + UPDATE_INTERVAL_DAYS + " days or doesn't exist");

        Path downloadFile = null;
        Path tempFile = null;
        try {
            // download database to temporarily location
            downloadFile = Files.createTempFile(ARCHIVE_FILE, null);
            tempFile = Files.createTempFile(DATABASE_TMP_FILE, null);
            String expectedChecksum = downloadDatabaseArchive(downloadFile);
            if (expectedChecksum == null) {
                logger.info("There is no newer GEO IP database uploaded to MaxMind. Using the old one for now.");
                startReading();
                return;
            }

            // tar extract database and copy to target destination
            extractDatabase(downloadFile, tempFile);

            // MD5 checksum verification
            verifyChecksum(Hashing.md5(), tempFile, expectedChecksum);

            Files.copy(tempFile, dataFile, StandardCopyOption.REPLACE_EXISTING);

            //only set this value to false on success otherwise errors could lead to endless download triggers
            logger.info("Successfully downloaded new GEO IP database to " + dataFile);
            startReading();
        } catch (IOException ioEx) {
            logger.logException("Could not download GeoLiteAPI database", ioEx);
        } finally {
            // clean up
            if (downloadFile != null) {
                FileUtils.delete(downloadFile.toFile());
            }
            if (tempFile != null) {
                FileUtils.delete(tempFile.toFile());
            }
        }
    }

    private void startReading() throws IOException {
        databaseReader = new Reader(dataFile.toFile(), FileMode.MEMORY, new CHMCache());
        logger.info(LICENSE);

        // clear downloading flag, because we now have working reader instance
        downloading = false;
    }

    /**
     * Downloads the archive to the destination file if it's newer than the locally version.
     *
     * @param lastModified modification timestamp of the already present file
     * @param destination save file
     * @return null if no updates were found, the MD5 hash of the downloaded archive if successful
     * @throws IOException if failed during downloading and writing to destination file
     */
    private String downloadDatabaseArchive(Instant lastModified, Path destination) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(ARCHIVE_URL).openConnection();

        String clientId = settings.getProperty(ProtectionSettings.MAXMIND_API_CLIENT_ID);
        String licenseKey = settings.getProperty(ProtectionSettings.MAXMIND_API_LICENSE_KEY);
        if (clientId.isEmpty() || licenseKey.isEmpty()) {
            logger.warning("No MaxMind credentials found in the configuration file!"
                + " GeoIp protections will be disabled.");
            return null;
        }
        String basicAuth = "Basic " + new String(Base64.getEncoder().encode((clientId + ":" + licenseKey).getBytes()));
        connection.setRequestProperty("Authorization", basicAuth);

        if (lastModified != null) {
            // Only download if we actually need a newer version - this field is specified in GMT zone
            ZonedDateTime zonedTime = lastModified.atZone(ZoneId.of("GMT"));
            String timeFormat = DateTimeFormatter.RFC_1123_DATE_TIME.format(zonedTime);
            connection.addRequestProperty("If-Modified-Since", timeFormat);
        }

        if (connection.getResponseCode() == HttpURLConnection.HTTP_NOT_MODIFIED) {
            //we already have the newest version
            connection.getInputStream().close();
            return null;
        }

        String hash = connection.getHeaderField("X-Database-MD5");
        String rawModifiedDate = connection.getHeaderField("Last-Modified");
        Instant modifiedDate = Instant.from(DateTimeFormatter.RFC_1123_DATE_TIME.parse(rawModifiedDate));
        Files.copy(connection.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);
        Files.setLastModifiedTime(destination, FileTime.from(modifiedDate));
        return hash;
    }

    /**
     * Downloads the archive to the destination file if it's newer than the locally version.
     *
     * @param destination save file
     * @return null if no updates were found, the MD5 hash of the downloaded archive if successful
     * @throws IOException if failed during downloading and writing to destination file
     */
    private String downloadDatabaseArchive(Path destination) throws IOException {
        Instant lastModified = null;
        if (Files.exists(dataFile)) {
            lastModified = Files.getLastModifiedTime(dataFile).toInstant();
        }

        return downloadDatabaseArchive(lastModified, destination);
    }

    /**
     * Verify if the expected checksum is equal to the checksum of the given file.
     *
     * @param function the checksum function like MD5, SHA256 used to generate the checksum from the file
     * @param file the file we want to calculate the checksum from
     * @param expectedChecksum the expected checksum
     * @throws IOException on I/O error reading the file or the checksum verification failed
     */
    private void verifyChecksum(HashFunction function, Path file, String expectedChecksum) throws IOException {
        HashCode actualHash = function.hashBytes(Files.readAllBytes(file));
        HashCode expectedHash = HashCode.fromString(expectedChecksum);
        if (!Objects.equals(actualHash, expectedHash)) {
            throw new IOException("GEO IP Checksum verification failed. "
                + "Expected: " + expectedChecksum + "Actual:" + actualHash);
        }
    }

    /**
     * Extract the database from gzipped data. Existing outputFile will be replaced if it already exists.
     *
     * @param inputFile gzipped database input file
     * @param outputFile destination file for the database
     * @throws IOException on I/O error reading the archive, or writing the output
     */
    private void extractDatabase(Path inputFile, Path outputFile) throws IOException {
        // .gz -> gzipped file
        try (BufferedInputStream in = new BufferedInputStream(Files.newInputStream(inputFile));
             GZIPInputStream gzipIn = new GZIPInputStream(in)) {

            // found the database file and copy file
            Files.copy(gzipIn, outputFile, StandardCopyOption.REPLACE_EXISTING);

            // update the last modification date to be same as in the archive
            Files.setLastModifiedTime(outputFile, Files.getLastModifiedTime(inputFile));
        }
    }

    /**
     * Get the country code of the given IP address.
     *
     * @param ip textual IP address to lookup.
     * @return two-character ISO 3166-1 alpha code for the country, "LOCALHOST" for local addresses
     *         or "--" if it cannot be fetched.
     */
    public String getCountryCode(String ip) {
        if (InternetProtocolUtils.isLocalAddress(ip)) {
            return "LOCALHOST";
        }
        return getCountry(ip).map(Country::getIsoCode).orElse("--");
    }

    /**
     * Get the country name of the given IP address.
     *
     * @param ip textual IP address to lookup.
     * @return The name of the country, "LocalHost" for local addresses, or "N/A" if it cannot be fetched.
     */
    public String getCountryName(String ip) {
        if (InternetProtocolUtils.isLocalAddress(ip)) {
            return "LocalHost";
        }
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
        if (ip == null || ip.isEmpty() || !isDataAvailable()) {
            return Optional.empty();
        }

        try {
            InetAddress address = InetAddress.getByName(ip);

            // Reader.getCountry() can be null for unknown addresses
            return Optional.ofNullable(databaseReader.getCountry(address)).map(CountryResponse::getCountry);
        } catch (UnknownHostException e) {
            // Ignore invalid ip addresses
            // Legacy GEO IP Database returned a unknown country object with Country-Code: '--' and Country-Name: 'N/A'
        } catch (IOException ioEx) {
            logger.logException("Cannot lookup country for " + ip + " at GEO IP database", ioEx);
        }

        return Optional.empty();
    }
}
