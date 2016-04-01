package fr.xephi.authme.util;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.ConsoleLogger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

import static java.lang.String.format;

/**
 * File utilities.
 */
public class FileUtils {

    private FileUtils() {
    }

    /**
     * Copy a resource file (from the JAR) to the given file if it doesn't exist.
     *
     * @param destinationFile The file to check and copy to (outside of JAR)
     * @param resourcePath Absolute path to the resource file (path to file within JAR)
     * @return False if the file does not exist and could not be copied, true otherwise
     */
    public static boolean copyFileFromResource(File destinationFile, String resourcePath) {
        if (destinationFile.exists()) {
            return true;
        } else if (!destinationFile.getParentFile().exists() && !destinationFile.getParentFile().mkdirs()) {
            ConsoleLogger.showError("Cannot create parent directories for '" + destinationFile + "'");
            return false;
        }

        // ClassLoader#getResourceAsStream does not deal with the '\' path separator: replace to '/'
        final String normalizedPath = resourcePath.replace("\\", "/");
        try (InputStream is = AuthMe.class.getClassLoader().getResourceAsStream(normalizedPath)) {
            if (is == null) {
                ConsoleLogger.showError(format("Cannot copy resource '%s' to file '%s': cannot load resource",
                    resourcePath, destinationFile.getPath()));
            } else {
                Files.copy(is, destinationFile.toPath());
                return true;
            }
        } catch (IOException e) {
            ConsoleLogger.logException(format("Cannot copy resource '%s' to file '%s':",
                resourcePath, destinationFile.getPath()), e);
        }
        return false;
    }
}
