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

import javax.inject.Inject;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.HttpURLConnection;
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
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.Optional;
import java.util.zip.GZIPInputStream;

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

    // The server for MaxMind doesn't seem to understand RFC1123,
    // but every HTTP implementation have to support  RFC 1023
    private static final String TIME_RFC_1023 = "EEE, dd-MMM-yy HH:mm:ss zzz";

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
                    startReading();

                    // don't fire the update task - we are up to date
                    return true;
                } else {
                    ConsoleLogger.debug("GEO IP database is older than " + UPDATE_INTERVAL_DAYS + " Days");
                }
            } catch (IOException ioEx) {
                ConsoleLogger.logException("Failed to load GeoLiteAPI database", ioEx);
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
        ConsoleLogger.info("Downloading GEO IP database, because the old database is older than "
                + UPDATE_INTERVAL_DAYS + " days or doesn't exist");

        Path tempFile = null;
        try {
            // download database to temporarily location
            tempFile = Files.createTempFile(ARCHIVE_FILE, null);
            if (!downloadDatabaseArchive(tempFile)) {
                ConsoleLogger.info("There is no newer GEO IP database uploaded to MaxMind. Using the old one for now.");
                startReading();
                return;
            }

            // MD5 checksum verification
            String expectedChecksum = Resources.toString(new URL(CHECKSUM_URL), StandardCharsets.UTF_8);
            verifyChecksum(Hashing.md5(), tempFile, expectedChecksum);

            // tar extract database and copy to target destination
            extractDatabase(tempFile, dataFile);

            //only set this value to false on success otherwise errors could lead to endless download triggers
            ConsoleLogger.info("Successfully downloaded new GEO IP database to " + dataFile);
            startReading();
        } catch (IOException ioEx) {
            ConsoleLogger.logException("Could not download GeoLiteAPI database", ioEx);
        } finally {
            // clean up
            if (tempFile != null) {
                FileUtils.delete(tempFile.toFile());
            }
        }
    }

    private void startReading() throws IOException {
        databaseReader = new Reader(dataFile.toFile(), FileMode.MEMORY, new CHMCache());
        ConsoleLogger.info(LICENSE);

        // clear downloading flag, because we now have working reader instance
        downloading = false;
    }

    /**
     * Downloads the archive to the destination file if it's newer than the locally version.
     *
     * @param lastModified modification timestamp of the already present file
     * @param destination save file
     * @return false if we already have the newest version, true if successful
     * @throws IOException if failed during downloading and writing to destination file
     */
    private boolean downloadDatabaseArchive(Instant lastModified, Path destination) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(ARCHIVE_URL).openConnection();
        if (lastModified != null) {
            // Only download if we actually need a newer version - this field is specified in GMT zone
            ZonedDateTime zonedTime = lastModified.atZone(ZoneId.of("GMT"));
            String timeFormat = DateTimeFormatter.ofPattern(TIME_RFC_1023).format(zonedTime);
            connection.addRequestProperty("If-Modified-Since", timeFormat);
        }

        if (connection.getResponseCode() == HttpURLConnection.HTTP_NOT_MODIFIED) {
            //we already have the newest version
            connection.getInputStream().close();
            return false;
        }

        Files.copy(connection.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);
        return true;
    }

    /**
     * Downloads the archive to the destination file if it's newer than the locally version.
     *
     * @param destination save file
     * @return false if we already have the newest version, true if successful
     * @throws IOException if failed during downloading and writing to destination file
     */
    private boolean downloadDatabaseArchive(Path destination) throws IOException {
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
            throw new IOException("GEO IP Checksum verification failed. " +
                    "Expected: " + expectedChecksum + "Actual:" + actualHash);
        }
    }

    /**
     * Extract the database from the tar archive. Existing outputFile will be replaced if it already exists.
     *
     * @param tarInputFile gzipped tar input file where the database is
     * @param outputFile destination file for the database
     * @throws IOException on I/O error reading the tar archive, or writing the output
     * @throws FileNotFoundException if the database cannot be found inside the archive
     */
    private void extractDatabase(Path tarInputFile, Path outputFile) throws FileNotFoundException, IOException {
        // .gz -> gzipped file
        try (BufferedInputStream in = new BufferedInputStream(Files.newInputStream(tarInputFile));
             TarInputStream tarIn = new TarInputStream(new GZIPInputStream(in))) {
            for (TarEntry entry = tarIn.getNextEntry(); entry != null; entry = tarIn.getNextEntry()) {
                // filename including folders (absolute path inside the archive)
                String filename = entry.getName();
                if (entry.isDirectory() || !filename.endsWith(DATABASE_EXT)) {
                    continue;
                }

                // found the database file and copy file
                Files.copy(tarIn, outputFile, StandardCopyOption.REPLACE_EXISTING);

                // update the last modification date to be same as in the archive
                Files.setLastModifiedTime(outputFile, FileTime.from(entry.getModTime().toInstant()));
                return;
            }
        }

        throw new FileNotFoundException("Cannot find database inside downloaded GEO IP file at " + tarInputFile);
    }

    /**
     * Get the country code of the given IP address.
     *
     * @param ip textual IP address to lookup.
     * @return two-character ISO 3166-1 alpha code for the country or "--" if it cannot be fetched.
     */
    public String getCountryCode(String ip) {
        return getCountry(ip).map(Country::getIsoCode).orElse("--");
    }

    /**
     * Get the country name of the given IP address.
     *
     * @param ip textual IP address to lookup.
     * @return The name of the country or "N/A" if it cannot be fetched.
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

            // Reader.getCountry() can be null for unknown addresses
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
