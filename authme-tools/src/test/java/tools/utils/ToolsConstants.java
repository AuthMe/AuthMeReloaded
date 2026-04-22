package tools.utils;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Constants for the src/tools folder.
 */
public final class ToolsConstants {

    private static final Path CURRENT_DIRECTORY = Paths.get("").toAbsolutePath().normalize();

    private static final Path REPOSITORY_ROOT =
        Files.isDirectory(CURRENT_DIRECTORY.resolve("authme-core")) ? CURRENT_DIRECTORY : CURRENT_DIRECTORY.getParent();

    private static final Path TOOLS_MODULE_ROOT =
        Files.isDirectory(CURRENT_DIRECTORY.resolve("src").resolve("test").resolve("java").resolve("tools"))
            ? CURRENT_DIRECTORY
            : REPOSITORY_ROOT.resolve("authme-tools");

    public static final String MAIN_SOURCE_ROOT = asDirectory(REPOSITORY_ROOT.resolve("authme-core")
        .resolve("src").resolve("main").resolve("java"));

    public static final String REPOSITORY_ROOT_PATH = asDirectory(REPOSITORY_ROOT);

    public static final String MAIN_RESOURCES_ROOT = asDirectory(REPOSITORY_ROOT.resolve("authme-core")
        .resolve("src").resolve("main").resolve("resources"));

    public static final String CORE_TEST_SOURCE_ROOT = asDirectory(REPOSITORY_ROOT.resolve("authme-core")
        .resolve("src").resolve("test").resolve("java"));

    public static final String CORE_TEST_RESOURCES_ROOT = asDirectory(REPOSITORY_ROOT.resolve("authme-core")
        .resolve("src").resolve("test").resolve("resources"));

    public static final String TOOLS_TEST_SOURCE_ROOT = asDirectory(TOOLS_MODULE_ROOT.resolve("src")
        .resolve("test").resolve("java"));

    public static final String TOOLS_SOURCE_ROOT = asDirectory(TOOLS_MODULE_ROOT.resolve("src")
        .resolve("test").resolve("java").resolve("tools"));

    // Docs are published at the repository root, while the tools run from the authme-tools module.
    public static final String DOCS_FOLDER = asDirectory(REPOSITORY_ROOT.resolve("docs"));

    public static final String DOCS_FOLDER_URL = "https://github.com/AuthMe/AuthMeReloaded/tree/master/docs/";

    private ToolsConstants() {
    }

    private static String asDirectory(Path path) {
        return path.toString() + File.separator;
    }
}
