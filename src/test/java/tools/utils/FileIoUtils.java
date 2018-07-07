package tools.utils;

import java.io.File;
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

    /**
     * Writes the given contents to the file, overriding any existing content.
     *
     * @param path the file to write to
     * @param contents the contents to write
     */
    public static void writeToFile(Path path, String contents) {
        try {
            Files.write(path, contents.getBytes());
        } catch (IOException e) {
            throw new UnsupportedOperationException("Failed to write to file '" + path + "'", e);
        }
    }

    /**
     * Adds the given contents to the file while keeping any existing content.
     *
     * @param outputFile the file to write to
     * @param contents the contents to append
     */
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

    /**
     * Returns the given file's contents as string.
     *
     * @param file the file to read
     * @return the file's contents
     */
    public static String readFromFile(Path file) {
        try {
            return new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UnsupportedOperationException("Could not read from file '" + file + "'", e);
        }
    }

    /**
     * Returns the lines of the given file.
     *
     * @param path the path of the file to read
     * @return the lines of the file
     */
    public static List<String> readLinesFromFile(Path path) {
        try {
            return Files.readAllLines(path, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UnsupportedOperationException("Could not read from file '" + path + "'", e);
        }
    }

    /**
     * Returns a folder's files or throws an exception if the folder could not be read or if it is empty.
     *
     * @param folder the folder to read
     * @return the files in the folder
     */
    public static File[] listFilesOrThrow(File folder) {
        File[] files = folder.listFiles();
        if (files == null) {
            throw new IllegalStateException("Could not read folder '" + folder + "'");
        } else if (files.length == 0) {
            throw new IllegalStateException("Folder '" + folder + "' is empty");
        }
        return files;
    }
}
