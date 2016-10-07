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
public final class FileUtils {

    // Utility class
    private FileUtils() {
    }

    /**
     * Copy a resource file (from the JAR) to the given file if it doesn't exist.
     *
     * @param destinationFile The file to check and copy to (outside of JAR)
     * @param resourcePath    Absolute path to the resource file (path to file within JAR)
     *
     * @return False if the file does not exist and could not be copied, true otherwise
     */
    public static boolean copyFileFromResource(File destinationFile, String resourcePath) {
        if (destinationFile.exists()) {
            return true;
        } else if (!destinationFile.getParentFile().exists() && !destinationFile.getParentFile().mkdirs()) {
            ConsoleLogger.warning("Cannot create parent directories for '" + destinationFile + "'");
            return false;
        }

        // ClassLoader#getResourceAsStream does not deal with the '\' path separator: replace to '/'
        final String normalizedPath = resourcePath.replace("\\", "/");
        try (InputStream is = AuthMe.class.getClassLoader().getResourceAsStream(normalizedPath)) {
            if (is == null) {
                ConsoleLogger.warning(format("Cannot copy resource '%s' to file '%s': cannot load resource",
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

    /**
     * Delete a given directory and all its content.
     *
     * @param directory The directory to remove
     */
    public static void purgeDirectory(File directory) {
        if (!directory.isDirectory()) {
            return;
        }
        File[] files = directory.listFiles();
        if (files == null) {
            return;
        }
        for (File target : files) {
            if (target.isDirectory()) {
                purgeDirectory(target);
            }
            delete(target);
        }
    }

    /**
     * Delete the given file or directory and log a message if it was unsuccessful.
     * Method is null safe and does nothing when null is passed.
     *
     * @param file the file to delete
     */
    public static void delete(File file) {
        if (file != null) {
            boolean result = file.delete();
            if (!result) {
                ConsoleLogger.warning("Could not delete file '" + file + "'");
            }
        }
    }

    /**
     * Construct a file path from the given elements, i.e. separate the given elements by the file separator.
     *
     * @param elements The elements to create a path with
     *
     * @return The created path
     */
    public static String makePath(String... elements) {
        return String.join(File.separator, elements);
    }
}
