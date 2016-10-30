package tools.utils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;

/**
 * Utility class for I/O operations on files.
 */
public final class FileIoUtils {

    private FileIoUtils() {
    }

    public static void generateFileFromTemplate(String templateFile, String destinationFile, TagValueHolder tags) {
        String template = readFromFile(templateFile);
        String result = TagReplacer.applyReplacements(template, tags);
        writeToFile(destinationFile, result);
    }

    public static void writeToFile(String outputFile, String contents) {
        writeToFile(Paths.get(outputFile), contents);
    }

    public static void writeToFile(Path path, String contents) {
        try {
            Files.write(path, contents.getBytes());
        } catch (IOException e) {
            throw new UnsupportedOperationException("Failed to write to file '" + path + "'", e);
        }
    }

    public static void appendToFile(String outputFile, String contents) {
        try {
            Files.write(Paths.get(outputFile), contents.getBytes(), StandardOpenOption.APPEND);
        } catch (IOException e) {
            throw new UnsupportedOperationException("Failed to append to file '" + outputFile + "'", e);
        }
    }

    public static String readFromFile(String file) {
        return readFromFile(Paths.get(file));
    }

    public static String readFromFile(Path file) {
        try {
            return new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UnsupportedOperationException("Could not read from file '" + file + "'", e);
        }
    }

    public static List<String> readLinesFromFile(Path path) {
        try {
            return Files.readAllLines(path, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UnsupportedOperationException("Could not read from file '" + path + "'", e);
        }
    }

}
