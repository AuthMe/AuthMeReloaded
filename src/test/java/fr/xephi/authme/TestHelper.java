package fr.xephi.authme;

import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * AuthMe test utilities.
 */
public final class TestHelper {

    private TestHelper() {
    }

    /**
     * Return a {@link File} to a file in the JAR's resources (main or test).
     *
     * @param path The absolute path to the file
     * @return The project file
     */
    public static File getJarFile(String path) {
        URL url = getUrlOrThrow(path);
        return new File(url.getFile());
    }

    /**
     * Return a {@link Path} to a file in the JAR's resources (main or test).
     *
     * @param path The absolute path to the file
     * @return The Path object to the file
     */
    public static Path getJarPath(String path) {
        String sqlFilePath = getUrlOrThrow(path).getPath();
        // Windows preprends the path with a '/' or '\', which Paths cannot handle
        String appropriatePath = System.getProperty("os.name").contains("indow")
            ? sqlFilePath.substring(1)
            : sqlFilePath;
        return Paths.get(appropriatePath);
    }

    private static URL getUrlOrThrow(String path) {
        URL url = TestHelper.class.getResource(path);
        if (url == null) {
            throw new IllegalStateException("File '" + path + "' could not be loaded");
        }
        return url;
    }

}
