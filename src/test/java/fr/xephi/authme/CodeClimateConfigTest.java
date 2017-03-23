package fr.xephi.authme;

import fr.xephi.authme.util.Utils;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.Test;

import java.io.File;
import java.util.List;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

/**
 * Consistency test for the CodeClimate configuration file.
 */
public class CodeClimateConfigTest {

    private static final String CONFIG_FILE = ".codeclimate.yml";

    @Test
    public void shouldHaveExistingClassesInExclusions() {
        // given
        FileConfiguration configuration = YamlConfiguration.loadConfiguration(new File(CONFIG_FILE));
        List<String> excludePaths = configuration.getStringList("exclude_paths");

        // when / then
        assertThat(excludePaths, not(empty()));
        for (String path : excludePaths) {
            String className = convertPathToQualifiedClassName(path);
            assertThat("No class corresponds to excluded path '" + path + "'",
                Utils.isClassLoaded(className), equalTo(true));
        }
    }

    private static String convertPathToQualifiedClassName(String path) {
        // Note ljacqu 20170323: In the future, we could have legitimate exclusions that don't fulfill these checks,
        // in which case this test needs to be adapted accordingly.
        if (!path.startsWith(TestHelper.SOURCES_FOLDER)) {
            throw new IllegalArgumentException("Unexpected path '" + path + "': expected to start with sources folder");
        } else if (!path.endsWith(".java")) {
            throw new IllegalArgumentException("Expected path '" + path + "' to end with '.java'");
        }

        return path.substring(0, path.length() - ".java".length()) // strip ending .java
            .substring(TestHelper.SOURCES_FOLDER.length())         // strip starting src/main/java
            .replace('/', '.');                                    // replace '/' to '.'
    }
}
