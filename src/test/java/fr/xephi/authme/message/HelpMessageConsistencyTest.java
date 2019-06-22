package fr.xephi.authme.message;

import ch.jalu.configme.resource.PropertyReader;
import ch.jalu.configme.resource.YamlFileReader;
import fr.xephi.authme.TestHelper;
import fr.xephi.authme.command.help.HelpMessage;
import fr.xephi.authme.command.help.HelpSection;
import fr.xephi.authme.permission.DefaultPermission;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static fr.xephi.authme.message.MessagePathHelper.MESSAGES_FOLDER;
import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

/**
 * Tests that all help_xx.yml files contain all entries for
 * {@link HelpSection}, {@link HelpMessage} and {@link DefaultPermission}.
 */
public class HelpMessageConsistencyTest {

    private List<File> helpFiles;

    @Before
    public void findHelpMessagesFiles() {
        File folder = TestHelper.getJarFile("/" + MESSAGES_FOLDER);
        File[] files = folder.listFiles();
        if (files == null || files.length == 0) {
            throw new IllegalStateException("Could not get files from '" + MESSAGES_FOLDER + "'");
        }
        helpFiles = Arrays.stream(files)
            .filter(file -> MessagePathHelper.isHelpFile(file.getName()))
            .collect(Collectors.toList());
    }

    @Test
    public void shouldHaveRequiredEntries() {
        for (File file : helpFiles) {
            // given
            PropertyReader reader = new YamlFileReader(file);

            // when / then
            assertHasAllHelpSectionEntries(file.getName(), reader);
        }
    }

    private void assertHasAllHelpSectionEntries(String filename, PropertyReader reader) {
        for (HelpSection section : HelpSection.values()) {
            assertThat(filename + " should have entry for HelpSection '" + section + "'",
                reader.getString(section.getKey()), notEmptyString());
        }

        for (HelpMessage message : HelpMessage.values()) {
            assertThat(filename + " should have entry for HelpMessage '" + message + "'",
                reader.getString(message.getKey()), notEmptyString());
        }

        for (DefaultPermission defaultPermission : DefaultPermission.values()) {
            assertThat(filename + " should have entry for DefaultPermission '" + defaultPermission + "'",
                reader.getString(getPathForDefaultPermission(defaultPermission)), notEmptyString());
        }
    }

    private static String getPathForDefaultPermission(DefaultPermission defaultPermission) {
        String path = "common.defaultPermissions.";
        switch (defaultPermission) {
            case ALLOWED:
                return path + "allowed";
            case NOT_ALLOWED:
                return path + "notAllowed";
            case OP_ONLY:
                return path + "opOnly";
            default:
                throw new IllegalStateException("Unknown default permission '" + defaultPermission + "'");
        }
    }

    private static Matcher<String> notEmptyString() {
        return both(not(emptyString())).and(not(nullValue()));
    }
}
