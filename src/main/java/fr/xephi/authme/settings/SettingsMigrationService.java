package fr.xephi.authme.settings;

import ch.jalu.configme.configurationdata.ConfigurationData;
import ch.jalu.configme.migration.PlainMigrationService;
import ch.jalu.configme.properties.Property;
import ch.jalu.configme.properties.convertresult.PropertyValue;
import ch.jalu.configme.resource.PropertyReader;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.datasource.DataSourceType;
import fr.xephi.authme.initialization.DataFolder;
import fr.xephi.authme.output.ConsoleLoggerFactory;
import fr.xephi.authme.output.LogLevel;
import fr.xephi.authme.process.register.RegisterSecondaryArgument;
import fr.xephi.authme.process.register.RegistrationType;
import fr.xephi.authme.security.HashAlgorithm;
import fr.xephi.authme.settings.properties.DatabaseSettings;
import fr.xephi.authme.settings.properties.PluginSettings;
import fr.xephi.authme.settings.properties.RegistrationSettings;
import fr.xephi.authme.settings.properties.SecuritySettings;
import fr.xephi.authme.util.StringUtils;

import javax.inject.Inject;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static ch.jalu.configme.properties.PropertyInitializer.newListProperty;
import static ch.jalu.configme.properties.PropertyInitializer.newProperty;
import static fr.xephi.authme.settings.properties.DatabaseSettings.MYSQL_POOL_SIZE;
import static fr.xephi.authme.settings.properties.RegistrationSettings.DELAY_JOIN_MESSAGE;
import static fr.xephi.authme.settings.properties.RegistrationSettings.REMOVE_JOIN_MESSAGE;
import static fr.xephi.authme.settings.properties.RegistrationSettings.REMOVE_LEAVE_MESSAGE;
import static fr.xephi.authme.settings.properties.RestrictionSettings.ALLOWED_NICKNAME_CHARACTERS;
import static fr.xephi.authme.settings.properties.RestrictionSettings.FORCE_SPAWN_LOCATION_AFTER_LOGIN;
import static fr.xephi.authme.settings.properties.RestrictionSettings.FORCE_SPAWN_ON_WORLDS;

/**
 * Service for verifying that the configuration is up-to-date.
 */
public class SettingsMigrationService extends PlainMigrationService {
    
    private static ConsoleLogger logger = ConsoleLoggerFactory.get(SettingsMigrationService.class);
    private final File pluginFolder;

    // Stores old "other accounts command" config if present.
    // We need to store it in here for retrieval when we build the CommandConfig. Retrieving it from the config.yml is
    // not possible since this migration service may trigger the config.yml to be resaved. As the old command settings
    // don't exist in the code anymore, as soon as config.yml is resaved we lose this information.
    private String oldOtherAccountsCommand;
    private int oldOtherAccountsCommandThreshold;

    @Inject
    SettingsMigrationService(@DataFolder File pluginFolder) {
        this.pluginFolder = pluginFolder;
    }

    @Override
    @SuppressWarnings("checkstyle:BooleanExpressionComplexity")
    protected boolean performMigrations(PropertyReader reader, ConfigurationData configurationData) {
        boolean changes = false;

        if ("[a-zA-Z0-9_?]*".equals(reader.getString(ALLOWED_NICKNAME_CHARACTERS.getPath()))) {
            configurationData.setValue(ALLOWED_NICKNAME_CHARACTERS, "[a-zA-Z0-9_]*");
            changes = true;
        }

        String driverClass = reader.getString("DataSource.mySQLDriverClassName");
        if ("fr.xephi.authme.libs.org.mariadb.jdbc.Driver".equals(driverClass)) {
            configurationData.setValue(DatabaseSettings.BACKEND, DataSourceType.MARIADB);
            changes = true;
        }

        setOldOtherAccountsCommandFieldsIfSet(reader);

        // Note ljacqu 20160211: Concatenating migration methods with | instead of the usual ||
        // ensures that all migrations will be performed
        return changes
            | performMailTextToFileMigration(reader)
            | migrateJoinLeaveMessages(reader, configurationData)
            | migrateForceSpawnSettings(reader, configurationData)
            | migratePoolSizeSetting(reader, configurationData)
            | changeBooleanSettingToLogLevelProperty(reader, configurationData)
            | hasOldHelpHeaderProperty(reader)
            | hasSupportOldPasswordProperty(reader)
            | convertToRegistrationType(reader, configurationData)
            | mergeAndMovePermissionGroupSettings(reader, configurationData)
            | moveDeprecatedHashAlgorithmIntoLegacySection(reader, configurationData)
            | moveSaltColumnConfigWithOtherColumnConfigs(reader, configurationData)
            || hasDeprecatedProperties(reader);
    }

    private static boolean hasDeprecatedProperties(PropertyReader reader) {
        String[] deprecatedProperties = {
            "Converter.Rakamak.newPasswordHash", "Hooks.chestshop", "Hooks.legacyChestshop", "Hooks.notifications",
            "Passpartu", "Performances", "settings.restrictions.enablePasswordVerifier", "Xenoforo.predefinedSalt",
            "VeryGames", "settings.restrictions.allowAllCommandsIfRegistrationIsOptional", "DataSource.mySQLWebsite",
            "Hooks.customAttributes", "Security.stop.kickPlayersBeforeStopping",
            "settings.restrictions.keepCollisionsDisabled", "settings.forceCommands", "settings.forceCommandsAsConsole",
            "settings.forceRegisterCommands", "settings.forceRegisterCommandsAsConsole",
            "settings.sessions.sessionExpireOnIpChange", "settings.restrictions.otherAccountsCmd",
            "settings.restrictions.otherAccountsCmdThreshold, DataSource.mySQLDriverClassName"};
        for (String deprecatedPath : deprecatedProperties) {
            if (reader.contains(deprecatedPath)) {
                return true;
            }
        }
        return false;
    }

    // --------
    // Old other accounts
    // --------
    public boolean hasOldOtherAccountsCommand() {
        return !StringUtils.isBlank(oldOtherAccountsCommand);
    }

    public String getOldOtherAccountsCommand() {
        return oldOtherAccountsCommand;
    }

    public int getOldOtherAccountsCommandThreshold() {
        return oldOtherAccountsCommandThreshold;
    }

    // --------
    // Specific migrations
    // --------

    /**
     * Check if {@code Email.mailText} is present and move it to the Email.html file if it doesn't exist yet.
     *
     * @param reader The property reader
     * @return True if a migration has been completed, false otherwise
     */
    private boolean performMailTextToFileMigration(PropertyReader reader) {
        final String oldSettingPath = "Email.mailText";
        final String oldMailText = reader.getString(oldSettingPath);
        if (oldMailText == null) {
            return false;
        }

        final File emailFile = new File(pluginFolder, "email.html");
        final String mailText = oldMailText
            .replace("<playername>", "<playername />").replace("%playername%", "<playername />")
            .replace("<servername>", "<servername />").replace("%servername%", "<servername />")
            .replace("<generatedpass>", "<generatedpass />").replace("%generatedpass%", "<generatedpass />")
            .replace("<image>", "<image />").replace("%image%", "<image />");
        if (!emailFile.exists()) {
            try (FileWriter fw = new FileWriter(emailFile)) {
                fw.write(mailText);
            } catch (IOException e) {
                logger.logException("Could not create email.html configuration file:", e);
            }
        }
        return true;
    }

    /**
     * Detect deprecated {@code settings.delayJoinLeaveMessages} and inform user of new "remove join messages"
     * and "remove leave messages" settings.
     *
     * @param reader The property reader
     * @param configData Configuration data
     * @return True if the configuration has changed, false otherwise
     */
    private static boolean migrateJoinLeaveMessages(PropertyReader reader, ConfigurationData configData) {
        Property<Boolean> oldDelayJoinProperty = newProperty("settings.delayJoinLeaveMessages", false);
        boolean hasMigrated = moveProperty(oldDelayJoinProperty, DELAY_JOIN_MESSAGE, reader, configData);

        if (hasMigrated) {
            logger.info(String.format("Note that we now also have the settings %s and %s",
                REMOVE_JOIN_MESSAGE.getPath(), REMOVE_LEAVE_MESSAGE.getPath()));
        }
        return hasMigrated;
    }

    /**
     * Detects old "force spawn loc on join" and "force spawn on these worlds" settings and moves them
     * to the new paths.
     *
     * @param reader The property reader
     * @param configData Configuration data
     * @return True if the configuration has changed, false otherwise
     */
    private static boolean migrateForceSpawnSettings(PropertyReader reader, ConfigurationData configData) {
        Property<Boolean> oldForceLocEnabled = newProperty(
            "settings.restrictions.ForceSpawnLocOnJoinEnabled", false);
        Property<List<String>> oldForceWorlds = newListProperty(
            "settings.restrictions.ForceSpawnOnTheseWorlds", "world", "world_nether", "world_the_ed");

        return moveProperty(oldForceLocEnabled, FORCE_SPAWN_LOCATION_AFTER_LOGIN, reader, configData)
            | moveProperty(oldForceWorlds, FORCE_SPAWN_ON_WORLDS, reader, configData);
    }

    /**
     * Detects the old auto poolSize value and replaces it with the default value.
     *
     * @param reader The property reader
     * @param configData Configuration data
     * @return True if the configuration has changed, false otherwise
     */
    private static boolean migratePoolSizeSetting(PropertyReader reader, ConfigurationData configData) {
        Integer oldValue = reader.getInt(MYSQL_POOL_SIZE.getPath());
        if (oldValue == null || oldValue > 0) {
            return false;
        }
        configData.setValue(MYSQL_POOL_SIZE, 10);
        return true;
    }

    /**
     * Changes the old boolean property "hide spam from console" to the new property specifying
     * the log level.
     *
     * @param reader The property reader
     * @param configData Configuration data
     * @return True if the configuration has changed, false otherwise
     */
    private static boolean changeBooleanSettingToLogLevelProperty(PropertyReader reader,
                                                                  ConfigurationData configData) {
        final String oldPath = "Security.console.noConsoleSpam";
        final Property<LogLevel> newProperty = PluginSettings.LOG_LEVEL;
        if (!newProperty.isValidInResource(reader) && reader.contains(oldPath)) {
            logger.info("Moving '" + oldPath + "' to '" + newProperty.getPath() + "'");
            boolean oldValue = Optional.ofNullable(reader.getBoolean(oldPath)).orElse(false);
            LogLevel level = oldValue ? LogLevel.INFO : LogLevel.FINE;
            configData.setValue(newProperty, level);
            return true;
        }
        return false;
    }

    private static boolean hasOldHelpHeaderProperty(PropertyReader reader) {
        if (reader.contains("settings.helpHeader")) {
            logger.warning("Help header setting is now in messages/help_xx.yml, "
                + "please check the file to set it again");
            return true;
        }
        return false;
    }

    private static boolean hasSupportOldPasswordProperty(PropertyReader reader) {
        String path = "settings.security.supportOldPasswordHash";
        if (reader.contains(path)) {
            logger.warning("Property '" + path + "' is no longer supported. "
                + "Use '" + SecuritySettings.LEGACY_HASHES.getPath() + "' instead.");
            return true;
        }
        return false;
    }

    /**
     * Converts old boolean configurations for registration to the new enum properties, if applicable.
     *
     * @param reader The property reader
     * @param configData Configuration data
     * @return True if the configuration has changed, false otherwise
     */
    private static boolean convertToRegistrationType(PropertyReader reader, ConfigurationData configData) {
        String oldEmailRegisterPath = "settings.registration.enableEmailRegistrationSystem";
        if (RegistrationSettings.REGISTRATION_TYPE.isValidInResource(reader)
            || !reader.contains(oldEmailRegisterPath)) {
            return false;
        }

        boolean useEmail = newProperty(oldEmailRegisterPath, false).determineValue(reader).getValue();
        RegistrationType registrationType = useEmail ? RegistrationType.EMAIL : RegistrationType.PASSWORD;

        String useConfirmationPath = useEmail
            ? "settings.registration.doubleEmailCheck"
            : "settings.restrictions.enablePasswordConfirmation";
        boolean hasConfirmation = newProperty(useConfirmationPath, false).determineValue(reader).getValue();
        RegisterSecondaryArgument secondaryArgument = hasConfirmation
            ? RegisterSecondaryArgument.CONFIRMATION
            : RegisterSecondaryArgument.NONE;

        logger.warning("Merging old registration settings into '"
            + RegistrationSettings.REGISTRATION_TYPE.getPath() + "'");
        configData.setValue(RegistrationSettings.REGISTRATION_TYPE, registrationType);
        configData.setValue(RegistrationSettings.REGISTER_SECOND_ARGUMENT, secondaryArgument);
        return true;
    }

    /**
     * Migrates old permission group settings to the new configurations.
     *
     * @param reader The property reader
     * @param configData Configuration data
     * @return True if the configuration has changed, false otherwise
     */
    private static boolean mergeAndMovePermissionGroupSettings(PropertyReader reader, ConfigurationData configData) {
        boolean performedChanges;

        // We have two old settings replaced by only one: move the first non-empty one
        Property<String> oldUnloggedInGroup = newProperty("settings.security.unLoggedinGroup", "");
        Property<String> oldRegisteredGroup = newProperty("GroupOptions.RegisteredPlayerGroup", "");
        if (!oldUnloggedInGroup.determineValue(reader).getValue().isEmpty()) {
            performedChanges = moveProperty(oldUnloggedInGroup, PluginSettings.REGISTERED_GROUP, reader, configData);
        } else {
            performedChanges = moveProperty(oldRegisteredGroup, PluginSettings.REGISTERED_GROUP, reader, configData);
        }

        // Move paths of other old options
        performedChanges |= moveProperty(newProperty("GroupOptions.UnregisteredPlayerGroup", ""),
            PluginSettings.UNREGISTERED_GROUP, reader, configData);
        performedChanges |= moveProperty(newProperty("permission.EnablePermissionCheck", false),
            PluginSettings.ENABLE_PERMISSION_CHECK, reader, configData);
        return performedChanges;
    }

    /**
     * If a deprecated hash is used, it is added to the legacy hashes option and the active hash
     * is changed to SHA256.
     *
     * @param reader The property reader
     * @param configData Configuration data
     * @return True if the configuration has changed, false otherwise
     */
    private static boolean moveDeprecatedHashAlgorithmIntoLegacySection(PropertyReader reader,
                                                                        ConfigurationData configData) {
        HashAlgorithm currentHash = SecuritySettings.PASSWORD_HASH.determineValue(reader).getValue();
        // Skip CUSTOM (has no class) and PLAINTEXT (is force-migrated later on in the startup process)
        if (currentHash != HashAlgorithm.CUSTOM && currentHash != HashAlgorithm.PLAINTEXT) {
            Class<?> encryptionClass = currentHash.getClazz();
            if (encryptionClass.isAnnotationPresent(Deprecated.class)) {
                configData.setValue(SecuritySettings.PASSWORD_HASH, HashAlgorithm.SHA256);
                Set<HashAlgorithm> legacyHashes = SecuritySettings.LEGACY_HASHES.determineValue(reader).getValue();
                legacyHashes.add(currentHash);
                configData.setValue(SecuritySettings.LEGACY_HASHES, legacyHashes);
                logger.warning("The hash algorithm '" + currentHash
                    + "' is no longer supported for active use. New hashes will be in SHA256.");
                return true;
            }
        }
        return false;
    }

    /**
     * Moves the property for the password salt column name to the same path as all other column name properties.
     *
     * @param reader The property reader
     * @param configData Configuration data
     * @return True if the configuration has changed, false otherwise
     */
    private static boolean moveSaltColumnConfigWithOtherColumnConfigs(PropertyReader reader,
                                                                      ConfigurationData configData) {
        Property<String> oldProperty = newProperty("ExternalBoardOptions.mySQLColumnSalt",
            DatabaseSettings.MYSQL_COL_SALT.getDefaultValue());
        return moveProperty(oldProperty, DatabaseSettings.MYSQL_COL_SALT, reader, configData);
    }

    /**
     * Retrieves the old config to run a command when alt accounts are detected and sets them to this instance
     * for further processing.
     *
     * @param reader The property reader
     */
    private void setOldOtherAccountsCommandFieldsIfSet(PropertyReader reader) {
        Property<String> commandProperty = newProperty("settings.restrictions.otherAccountsCmd", "");
        Property<Integer> commandThresholdProperty = newProperty("settings.restrictions.otherAccountsCmdThreshold", 0);

        PropertyValue<String> commandPropValue = commandProperty.determineValue(reader);
        int commandThreshold = commandThresholdProperty.determineValue(reader).getValue();
        if (commandPropValue.isValidInResource() && commandThreshold >= 2) {
            oldOtherAccountsCommand = commandPropValue.getValue();
            oldOtherAccountsCommandThreshold = commandThreshold;
        }
    }

    /**
     * Checks for an old property path and moves it to a new path if it is present and the new path is not yet set.
     *
     * @param oldProperty The old property (create a temporary {@link Property} object with the path)
     * @param newProperty The new property to move the value to
     * @param reader The property reader
     * @param configData Configuration data
     * @param <T> The type of the property
     * @return True if a migration has been done, false otherwise
     */
    protected static <T> boolean moveProperty(Property<T> oldProperty,
                                              Property<T> newProperty,
                                              PropertyReader reader,
                                              ConfigurationData configData) {
        PropertyValue<T> oldPropertyValue = oldProperty.determineValue(reader);
        if (oldPropertyValue.isValidInResource()) {
            if (reader.contains(newProperty.getPath())) {
                logger.info("Detected deprecated property " + oldProperty.getPath());
            } else {
                logger.info("Renaming " + oldProperty.getPath() + " to " + newProperty.getPath());
                configData.setValue(newProperty, oldPropertyValue.getValue());
            }
            return true;
        }
        return false;
    }

}
