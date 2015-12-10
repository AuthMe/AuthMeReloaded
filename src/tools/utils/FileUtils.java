package utils;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Map;

/**
 * Utility class for reading from and writing to files.
 */
public final class FileUtils {

    private final static Charset CHARSET = Charset.forName("utf-8");

    private FileUtils() {
    }

    public static void generateFileFromTemplate(String templateFile, String destinationFile, Map<String, Object> tags) {
        String template = readFromFile(templateFile);
        String result = TagReplacer.applyReplacements(template, tags);

        writeToFile(destinationFile, result);
    }

    private static void writeToFile(String outputFile, String contents) {
        try {
            Files.write(Paths.get(outputFile), contents.getBytes());
        } catch (IOException e) {
            throw new RuntimeException("Failed to write to file '" + outputFile + "'", e);
        }
    }

    public static void appendToFile(String outputFile, String contents) {
        try {
            Files.write(Paths.get(outputFile), contents.getBytes(), StandardOpenOption.APPEND);
        } catch (IOException e) {
            throw new RuntimeException("Failed to append to file '" + outputFile + "'", e);
        }
    }

    public static String readFromFile(String file) {
        try {
            return new String(Files.readAllBytes(Paths.get(file)), CHARSET);
        } catch (IOException e) {
            throw new RuntimeException("Could not read from file '" + file + "'", e);
        }
    }

    public static List<String> readLinesFromFile(String file) {
        try {
            return Files.readAllLines(Paths.get(file), CHARSET);
        } catch (IOException e) {
            throw new RuntimeException("Could not read from file '" + file + "'", e);
        }
    }

    public static String readFromToolsFile(String file) {
        return readFromFile(ToolsConstants.TOOLS_SOURCE_ROOT + file);
    }

}
