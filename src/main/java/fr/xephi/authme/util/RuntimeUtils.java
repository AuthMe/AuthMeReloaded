package fr.xephi.authme.util;

public class RuntimeUtils {
	public static int getCoreCount() {
		return Runtime.getRuntime().availableProcessors();
	}
}
