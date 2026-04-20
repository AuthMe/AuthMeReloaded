package fr.xephi.authme;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * Minimal test helper replacing JUnit 4's TemporaryFolder rule.
 */
public class TempFolder {

    private final File root;

    public TempFolder() {
        try {
            root = Files.createTempDirectory("authme-tests-").toFile();
            root.deleteOnExit();
        } catch (IOException e) {
            throw new IllegalStateException("Could not create temporary folder", e);
        }
    }

    public File newFolder() throws IOException {
        File folder = Files.createTempDirectory(root.toPath(), "folder-").toFile();
        folder.deleteOnExit();
        return folder;
    }

    public File newFile() throws IOException {
        File file = Files.createTempFile(root.toPath(), "file-", ".tmp").toFile();
        file.deleteOnExit();
        return file;
    }

    public File getRoot() {
        return root;
    }
}
