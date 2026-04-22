package fr.xephi.authme.message;

import ch.jalu.configme.resource.PropertyReader;
import ch.jalu.configme.resource.YamlFileReader;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import fr.xephi.authme.TestHelper;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static fr.xephi.authme.message.MessagePathHelper.MESSAGES_FOLDER;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Checks that the entries in messages_xx.yml files have the {@link MessageKey#getTags() placeholders}
 * that are defined for the given message.
 */
public class MessageFilePlaceholderTest {

    /** Defines exclusions: a (key, tag) pair in this map will not be checked in the test. */
    private static final Multimap<MessageKey, String> EXCLUSIONS = ImmutableMultimap.<MessageKey, String>builder()
        .put(MessageKey.INCORRECT_RECOVERY_CODE, "%count")
        .putAll(MessageKey.MAX_REGISTER_EXCEEDED, "%max_acc", "%reg_count", "%reg_names")
        .build();

    @ParameterizedTest(name = "{1}")
    @MethodSource("buildParams")
    public void shouldHaveAllPlaceholders(File messagesFile, String messagesFilename) {
        // given
        PropertyReader reader = new YamlFileReader(messagesFile);
        List<String> errors = new ArrayList<>(0);

        // when
        for (MessageKey key : MessageKey.values()) {
            List<String> missingTags = findMissingTags(key, reader);
            if (!missingTags.isEmpty()) {
                errors.add("Message key '" + key + "' should have tags: " + String.join(", ", missingTags));
            }
        }

        // then
        if (!errors.isEmpty()) {
            fail("Errors while validating '" + messagesFilename + "':\n- " + String.join("\n- ", errors));
        }
    }

    private List<String> findMissingTags(MessageKey key, PropertyReader reader) {
        if (key.getTags().length > 0 && reader.contains(key.getKey())) {
            String message = reader.getString(key.getKey());
            return Arrays.stream(key.getTags())
                .filter(tag -> !EXCLUSIONS.get(key).contains(tag) && !message.contains(tag))
                .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    public static Stream<Arguments> buildParams() {
        File folder = TestHelper.getJarFile("/" + MESSAGES_FOLDER);

        File[] files = folder.listFiles();
        if (files == null) {
            throw new IllegalStateException("Could not read folder '" + folder + "'");
        }

        List<Arguments> messageFiles = Arrays.stream(files)
            .filter(file -> MessagePathHelper.isMessagesFile(file.getName()))
            .map(file -> Arguments.of(file, file.getName()))
            .collect(Collectors.toList());
        if (messageFiles.isEmpty()) {
            throw new IllegalStateException("Found zero messages files (is the folder correct?)");
        }
        return messageFiles.stream();
    }
}

