package utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Date;

/**
 * Utility class for writing a generated file with a timestamp.
 */
public final class GeneratedFileWriter {

    private GeneratedFileWriter() {
    }

    public static void createGeneratedFile(File file, String contents, CommentType commentFormat) {
        validateFile(file);

        try (OutputStreamWriter osw = new OutputStreamWriter(new FileOutputStream(file))) {
            osw.write(generateComment(commentFormat));
            osw.write(contents);
        } catch (IOException e) {
            throw new RuntimeException("Could not write to file '" + file.getName() + "'", e);
        }
    }

    public static void createGeneratedFile(String fileName, String contents, CommentType commentFormat) {
        createGeneratedFile(new File(fileName), contents, commentFormat);
    }

    private static String generateComment(CommentType commentFormat) {
        String comment = "Auto-generated file, generated on " + new Date() + "\n\n";
        switch (commentFormat) {
            case JAVA:
                return "// " + comment;
            case YML:
                return "# " + comment;
            default:
                throw new RuntimeException("Unknown comment format '" + commentFormat + "'");
        }
    }

    private static void validateFile(File file) {
        if (!file.exists()) {
            System.out.println("File '" + file.getName() + "' doesn't exist; attempting to create it");
            try {
                boolean success = file.createNewFile();
                if (!success) {
                    throw new RuntimeException("Failed to create file '" + file.getName() + "'");
                }
            } catch (IOException e) {
                throw new RuntimeException("Could not create file '" + file.getName() + "'", e);
            }
        }
        if (!file.canWrite()) {
            throw new RuntimeException("File '" + file.getName() + "' is not writable");
        }
    }

}
