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
            if (!new File(path).exists()) {
                fail("Path '" + path + "' does not exist!");
            }
        }
    }

    private static void removeTestsExclusionOrThrow(List<String> excludePaths) {
        boolean wasRemoved = excludePaths.removeIf("src/test/java/**/*Test.java"::equals);
        assertThat("Expected an exclusion for test classes",
            wasRemoved, equalTo(true));
    }
}
