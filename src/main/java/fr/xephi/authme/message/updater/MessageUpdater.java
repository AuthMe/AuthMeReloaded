package fr.xephi.authme.message.updater;

import ch.jalu.configme.SettingsManager;
import ch.jalu.configme.configurationdata.ConfigurationData;
import ch.jalu.configme.configurationdata.PropertyListBuilder;
import ch.jalu.configme.properties.Property;
import ch.jalu.configme.properties.StringProperty;
import ch.jalu.configme.resource.PropertyResource;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Files;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.message.MessageKey;
import fr.xephi.authme.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Migrates the used messages file to a complete, up-to-date version when necessary.
 */
public class MessageUpdater {

    /**
     * Configuration data object for all message keys incl. comments associated to sections.
     */
    private static final ConfigurationData CONFIGURATION_DATA = buildConfigurationData();

    public static ConfigurationData getConfigurationData() {
        return CONFIGURATION_DATA;
    }

    /**
     * Applies any necessary migrations to the user's messages file and saves it if it has been modified.
     *
     * @param userFile the user's messages file (yml file in the plugin's folder)
     * @param localJarPath path to the messages file in the JAR for the same language (may not exist)
     * @param defaultJarPath path to the messages file in the JAR for the default language
     * @return true if the file has been migrated and saved, false if it is up-to-date
     */
    public boolean migrateAndSave(File userFile, String localJarPath, String defaultJarPath) {
        JarMessageSource jarMessageSource = new JarMessageSource(localJarPath, defaultJarPath);
        return migrateAndSave(userFile, jarMessageSource);
    }

    /**
     * Performs the migration.
     *
     * @param userFile the file to verify and migrate
     * @param jarMessageSource jar message source to get texts from if missing
     * @return true if the file has been migrated and saved, false if it is up-to-date
     */
    private boolean migrateAndSave(File userFile, JarMessageSource jarMessageSource) {
        // YamlConfiguration escapes all special characters when saving, making the file hard to use, so use ConfigMe
        PropertyResource userResource = new MigraterYamlFileResource(userFile);

        // Step 1: Migrate any old keys in the file to the new paths
        boolean movedOldKeys = migrateOldKeys(userResource);
        // Step 2: Perform newer migrations
        boolean movedNewerKeys = migrateKeys(userResource);
        // Step 3: Take any missing messages from the message files shipped in the AuthMe JAR
        boolean addedMissingKeys = addMissingKeys(jarMessageSource, userResource);

        if (movedOldKeys || movedNewerKeys || addedMissingKeys) {
            backupMessagesFile(userFile);

            SettingsManager settingsManager = new SettingsManager(userResource, null, CONFIGURATION_DATA);
            settingsManager.save();
            ConsoleLogger.debug("Successfully saved {0}", userFile);
            return true;
        }
        return false;
    }

    private boolean migrateKeys(PropertyResource userResource) {
        return moveIfApplicable(userResource, "misc.two_factor_create", MessageKey.TWO_FACTOR_CREATE.getKey());
    }

    private static boolean moveIfApplicable(PropertyResource resource, String oldPath, String newPath) {
        if (resource.getString(newPath) == null && resource.getString(oldPath) != null) {
            resource.setValue(newPath, resource.getString(oldPath));
            return true;
        }
        return false;
    }

    private boolean migrateOldKeys(PropertyResource userResource) {
        boolean hasChange = OldMessageKeysMigrater.migrateOldPaths(userResource);
        if (hasChange) {
            ConsoleLogger.info("Old keys have been moved to the new ones in your messages_xx.yml file");
        }
        return hasChange;
    }

    private boolean addMissingKeys(JarMessageSource jarMessageSource, PropertyResource userResource) {
        List<String> addedKeys = new ArrayList<>();
        for (Property<?> property : CONFIGURATION_DATA.getProperties()) {
            final String key = property.getPath();
            if (userResource.getString(key) == null) {
                userResource.setValue(key, jarMessageSource.getMessageFromJar(property));
                addedKeys.add(key);
            }
        }
        if (!addedKeys.isEmpty()) {
            ConsoleLogger.info(
                "Added " + addedKeys.size() + " missing keys to your messages_xx.yml file: " + addedKeys);
            return true;
        }
        return false;
    }

    private static void backupMessagesFile(File messagesFile) {
        String backupName = FileUtils.createBackupFilePath(messagesFile);
        File backupFile = new File(backupName);
        try {
            Files.copy(messagesFile, backupFile);
        } catch (IOException e) {
            throw new IllegalStateException("Could not back up '" + messagesFile + "' to '" + backupFile + "'", e);
        }
    }

    /**
     * Constructs the {@link ConfigurationData} for exporting a messages file in its entirety.
     *
     * @return the configuration data to export with
     */
    private static ConfigurationData buildConfigurationData() {
        Map<String, String[]> comments = ImmutableMap.<String, String[]>builder()
            .put("registration", new String[]{"Registration"})
            .put("password", new String[]{"Password errors on registration"})
            .put("login", new String[]{"Login"})
            .put("error", new String[]{"Errors"})
            .put("antibot", new String[]{"AntiBot"})
            .put("unregister", new String[]{"Unregister"})
            .put("misc", new String[]{"Other messages"})
            .put("session", new String[]{"Session messages"})
            .put("on_join_validation", new String[]{"Error messages when joining"})
            .put("email", new String[]{"Email"})
            .put("recovery", new String[]{"Password recovery by email"})
            .put("captcha", new String[]{"Captcha"})
            .put("verification", new String[]{"Verification code"})
            .put("time", new String[]{"Time units"})
            .put("two_factor", new String[]{"Two-factor authentication"})
            .build();

        Set<String> addedKeys = new HashSet<>();
        PropertyListBuilder builder = new PropertyListBuilder();
        // Add one key per section based on the comments map above so that the order is clear
        for (String path : comments.keySet()) {
            MessageKey key = Arrays.stream(MessageKey.values()).filter(p -> p.getKey().startsWith(path + "."))
                .findFirst().orElseThrow(() -> new IllegalStateException(path));
            builder.add(new StringProperty(key.getKey(), ""));
            addedKeys.add(key.getKey());
        }
        // Add all remaining keys to the property list builder
        Arrays.stream(MessageKey.values())
            .filter(key -> !addedKeys.contains(key.getKey()))
            .forEach(key -> builder.add(new StringProperty(key.getKey(), "")));

        return new ConfigurationData(builder.create(), comments);
    }

}
