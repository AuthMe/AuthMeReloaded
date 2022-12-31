package fr.xephi.authme;

import ch.jalu.configme.properties.Property;
import fr.xephi.authme.settings.Settings;
import org.bukkit.entity.Player;
import org.mockito.Mockito;

import java.io.File;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Logger;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

/**
 * AuthMe test utilities.
 */
public final class TestHelper {

    public static final String SOURCES_FOLDER = "src/main/java/";
    public static final String TEST_SOURCES_FOLDER = "src/test/java/";
    public static final String TEST_RESOURCES_FOLDER = "src/test/resources/";
    public static final String PROJECT_ROOT = "/fr/xephi/authme/";

    private TestHelper() {
    }

    /**
     * Return a {@link File} to a file in the JAR's resources (main or test).
     *
     * @param path The absolute path to the file
     * @return The project file
     */
    public static File getJarFile(String path) {
        URI uri = getUriOrThrow(path);
        return new File(uri.getPath());
    }

    /**
     * Return a {@link Path} to a file in the JAR's resources (main or test).
     *
     * @param path The absolute path to the file
     * @return The Path object to the file
     */
    public static Path getJarPath(String path) {
        String sqlFilePath = getUriOrThrow(path).getPath();
        // Windows prepends the path with a '/' or '\', which Paths cannot handle
        String appropriatePath = System.getProperty("os.name").contains("indow")
            ? sqlFilePath.substring(1)
            : sqlFilePath;
        return Paths.get(appropriatePath);
    }

    private static URI getUriOrThrow(String path) {
        URL url = TestHelper.class.getResource(path);
        if (url == null) {
            throw new IllegalStateException("File '" + path + "' could not be loaded");
        }
        try {
            return new URI(url.toString());
        } catch (URISyntaxException e) {
            throw new IllegalStateException("File '" + path + "' cannot be converted to a URI");
        }
    }

    /**
     * Assign the necessary fields on ConsoleLogger with mocks.
     *
     * @return The logger mock used
     */
    public static Logger setupLogger() {
        Logger logger = Mockito.mock(Logger.class);
        ConsoleLogger.initialize(logger, null);
        return logger;
    }

    /**
     * Set ConsoleLogger to use a new real logger.
     *
     * @return The real logger used by ConsoleLogger
     */
    public static Logger setRealLogger() {
        Logger logger = Logger.getAnonymousLogger();
        ConsoleLogger.initialize(logger, null);
        return logger;
    }

    /**
     * Configures the player mock to return the given IP address.
     *
     * @param player the player mock
     * @param ip the ip address it should return
     */
    public static void mockIpAddressToPlayer(Player player, String ip) {
        try {
            InetAddress inetAddress = InetAddress.getByName(ip);
            InetSocketAddress inetSocketAddress = new InetSocketAddress(inetAddress, 8093);
            given(player.getAddress()).willReturn(inetSocketAddress);
        } catch (UnknownHostException e) {
            throw new IllegalStateException("Invalid IP address: " + ip, e);
        }
    }

    /**
     * Configures the Settings mock to return the property's default value for any given property.
     *
     * @param settings the settings mock
     */
    @SuppressWarnings("unchecked")
    public static void returnDefaultsForAllProperties(Settings settings) {
        given(settings.getProperty(any(Property.class)))
            .willAnswer(invocation -> ((Property<?>) invocation.getArgument(0)).getDefaultValue());
    }
}
