package fr.xephi.authme;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.Test;

import java.io.File;
import java.util.List;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * Consistency test for the CodeClimate configuration file.
 */
public class CodeClimateConfigTest {

    private static final String CONFIG_FILE = ".codeclimate.yml";

    @Test
    public void shouldHaveExistingClassesInExclusions() {
        // given / when
        FileConfiguration configuration = YamlConfiguration.loadConfiguration(new File(CONFIG_FILE));
        List<String> excludePaths = configuration.getStringList("exclude_patterns");

        // then
        assertThat(excludePaths, not(empty()));
        removeTestsExclusionOrThrow(excludePaths);
        for (String path : excludePaths) {
            verifySourceFileExists(path);
        }
    }

    private static void verifySourceFileExists(String path) {
        // Note ljacqu 20170323: In the future, we could have legitimate exclusions that don't fulfill these checks,
        // in which case this test needs to be adapted accordingly.
        if (!path.startsWith(TestHelper.SOURCES_FOLDER)) {
            fail("Unexpected path '" + path + "': expected to start with sources folder");
        } else if (!path.endsWith(".java")) {
            fail("Expected path '" + path + "' to end with '.java'");
        }

        if (!new File(path).exists()) {
            fail("Path '" + path + "' does not exist!");
        }
    }

    private static void removeTestsExclusionOrThrow(List<String> excludePaths) {
        boolean wasRemoved = excludePaths.removeIf("src/test/java/**/*Test.java"::equals);
        assertThat("Expected an exclusion for test classes",
            wasRemoved, equalTo(true));
    }
}
