package fr.xephi.authme.util;

/**
 * Runtime utilities.
 */
public class RuntimeUtils {

    // Utility class
    private RuntimeUtils() {
    }

    /**
     * Return the available core count of the JVM.
     *
     * @return the core count
     */
	public static int getCoreCount() {
		return Runtime.getRuntime().availableProcessors();
	}
}
