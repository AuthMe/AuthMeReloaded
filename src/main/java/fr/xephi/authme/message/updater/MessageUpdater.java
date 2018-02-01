package fr.xephi.authme.message.updater;

import ch.jalu.configme.SettingsManager;
import ch.jalu.configme.beanmapper.leafproperties.LeafPropertiesGenerator;
import ch.jalu.configme.configurationdata.ConfigurationData;
import ch.jalu.configme.configurationdata.PropertyListBuilder;
import ch.jalu.configme.properties.Property;
import ch.jalu.configme.properties.StringProperty;
import ch.jalu.configme.resource.YamlFileResource;
import com.google.common.collect.ImmutableMap;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.message.MessageKey;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Migrates the used messages file to a complete, up-to-date version when necessary.
 */
public class MessageUpdater {

    private static final ConfigurationData CONFIGURATION_DATA = buildConfigurationData();

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
        YamlFileResource userResource = new MigraterYamlFileResource(userFile);
        SettingsManager settingsManager = new SettingsManager(userResource, null, CONFIGURATION_DATA);

        // Step 1: Migrate any old keys in the file to the new paths
        boolean movedOldKeys = migrateOldKeys(userResource);
        // Step 2: Take any missing messages from the message files shipped in the AuthMe JAR
        boolean addedMissingKeys = addMissingKeys(jarMessageSource, userResource, settingsManager);

        if (movedOldKeys || addedMissingKeys) {
            settingsManager.save();
            ConsoleLogger.debug("Successfully saved {0}", userFile);
            return true;
        }
        return false;
    }

    private boolean migrateOldKeys(YamlFileResource userResource) {
        boolean hasChange = OldMessageKeysMigrater.migrateOldPaths(userResource);
        if (hasChange) {
            ConsoleLogger.info("Old keys have been moved to the new ones in your messages_xx.yml file");
        }
        return hasChange;
    }

    private boolean addMissingKeys(JarMessageSource jarMessageSource, YamlFileResource userResource,
                                   SettingsManager settingsManager) {
        int addedKeys = 0;
        for (Property<?> property : CONFIGURATION_DATA.getProperties()) {
            if (!property.isPresent(userResource)) {
                settingsManager.setProperty((Property) property, jarMessageSource.getMessageFromJar(property));
                ++addedKeys;
            }
        }
        if (addedKeys > 0) {
            ConsoleLogger.info("Added " + addedKeys + " missing keys to your messages_xx.yml file");
            return true;
        }
        return false;
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

    /**
     * Extension of {@link YamlFileResource} to fine-tune the export style.
     */
    private static final class MigraterYamlFileResource extends YamlFileResource {

        private Yaml singleQuoteYaml;

        MigraterYamlFileResource(File file) {
            super(file, new MessageMigraterPropertyReader(file), new LeafPropertiesGenerator());
        }

        @Override
        protected Yaml getSingleQuoteYaml() {
            if (singleQuoteYaml == null) {
                DumperOptions options = new DumperOptions();
                options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
                options.setAllowUnicode(true);
                options.setDefaultScalarStyle(DumperOptions.ScalarStyle.SINGLE_QUOTED);
                // Overridden setting: don't split lines
                options.setSplitLines(false);
                singleQuoteYaml = new Yaml(options);
            }
            return singleQuoteYaml;
        }
    }
}
