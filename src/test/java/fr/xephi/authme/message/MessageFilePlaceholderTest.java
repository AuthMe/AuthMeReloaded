package fr.xephi.authme.message;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import fr.xephi.authme.TestHelper;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.junit.Assert.fail;
import static tools.utils.FileIoUtils.listFilesOrThrow;

/**
 * Checks that the entries in messages_xx.yml files have the {@link MessageKey#getTags() placeholders}
 * that are defined for the given message.
 */
@RunWith(Parameterized.class)
public class MessageFilePlaceholderTest {

    /** Path in the resources folder where the message files are located. */
    private static final String MESSAGES_FOLDER = "/messages/";

    /** Pattern for detecting messages files. */
    private static final Pattern IS_MESSAGES_FILE = Pattern.compile("messages_.*?\\.yml");

    /** Defines exclusions: a (key, tag) pair in this map will not be checked in the test. */
    private static final Multimap<MessageKey, String> EXCLUSIONS = ImmutableMultimap.<MessageKey, String>builder()
        .put(MessageKey.INCORRECT_RECOVERY_CODE, "%count")
        .putAll(MessageKey.MAX_REGISTER_EXCEEDED, "%max_acc", "%reg_count", "%reg_names")
        .build();

    private File messagesFile;
    private String messagesFilename;

    // Note ljacqu 20170506: We pass the file name separately so we can reference it for the name in @Parameters
    public MessageFilePlaceholderTest(File messagesFile, String name) {
        this.messagesFile = messagesFile;
        this.messagesFilename = name;
    }

    @Test
    public void shouldHaveAllPlaceholders() {
        // given
        FileConfiguration configuration = YamlConfiguration.loadConfiguration(messagesFile);
        List<String> errors = new ArrayList<>();

        // when
        for (MessageKey key : MessageKey.values()) {
            List<String> missingTags = findMissingTags(key, configuration);
            if (!missingTags.isEmpty()) {
                errors.add("Message key '" + key + "' should have tags: " + String.join(", ", missingTags));
            }
        }

        // then
        if (!errors.isEmpty()) {
            fail("Errors while validating '" + messagesFilename + "':\n- " + String.join("\n- ", errors));
        }
    }

    private List<String> findMissingTags(MessageKey key, FileConfiguration configuration) {
        if (key.getTags().length > 0 && configuration.contains(key.getKey())) {
            String message = configuration.getString(key.getKey());
            return Arrays.stream(key.getTags())
                .filter(tag -> !EXCLUSIONS.get(key).contains(tag) && !message.contains(tag))
                .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    @Parameterized.Parameters(name = "{1}")
    public static List<Object[]> buildParams() {
        File folder = TestHelper.getJarFile(MESSAGES_FOLDER);

        List<Object[]> messageFiles = Arrays.stream(listFilesOrThrow(folder))
            .filter(file -> IS_MESSAGES_FILE.matcher(file.getName()).matches())
            .map(file -> new Object[]{file, file.getName()})
            .collect(Collectors.toList());
        if (messageFiles.isEmpty()) {
            throw new IllegalStateException("Found zero messages files (is the folder correct?)");
        }
        return messageFiles;
    }


}
