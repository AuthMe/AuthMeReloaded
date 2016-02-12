package fr.xephi.authme;

import java.io.File;
import java.net.URL;

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
        URL url = TestHelper.class.getResource(path);
        if (url == null) {
            throw new IllegalStateException("File '" + path + "' could not be loaded");
        }
        return new File(url.getFile());
    }

}
