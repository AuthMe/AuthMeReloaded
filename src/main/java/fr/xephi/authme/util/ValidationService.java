package fr.xephi.authme.util;

import com.github.authme.configme.properties.Property;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.initialization.Reloadable;
import fr.xephi.authme.output.MessageKey;
import fr.xephi.authme.permission.PermissionsManager;
import fr.xephi.authme.permission.PlayerStatePermission;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.EmailSettings;
import fr.xephi.authme.settings.properties.ProtectionSettings;
import fr.xephi.authme.settings.properties.RestrictionSettings;
import fr.xephi.authme.settings.properties.SecuritySettings;
import org.bukkit.command.CommandSender;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Validation service.
 */
public class ValidationService implements Reloadable {

    @Inject
    private Settings settings;
    @Inject
    private DataSource dataSource;
    @Inject
    private PermissionsManager permissionsManager;
    @Inject
    private GeoLiteAPI geoLiteApi;

    private Pattern passwordRegex;
    private Set<String> unrestrictedNames;

    ValidationService() { }

    @PostConstruct
    @Override
    public void reload() {
        passwordRegex = Utils.safePatternCompile(settings.getProperty(RestrictionSettings.ALLOWED_PASSWORD_REGEX));
        // Use Set for more efficient contains() lookup
        unrestrictedNames = new HashSet<>(settings.getProperty(RestrictionSettings.UNRESTRICTED_NAMES));
    }

    /**
     * Verifies whether a password is valid according to the plugin settings.
     *
     * @param password the password to verify
     * @param username the username the password is associated with
     * @return the validation result
     */
    public ValidationResult validatePassword(String password, String username) {
        String passLow = password.toLowerCase();
        if (!passwordRegex.matcher(passLow).matches()) {
            return new ValidationResult(MessageKey.PASSWORD_CHARACTERS_ERROR, passwordRegex.pattern());
        } else if (passLow.equalsIgnoreCase(username)) {
            return new ValidationResult(MessageKey.PASSWORD_IS_USERNAME_ERROR);
        } else if (password.length() < settings.getProperty(SecuritySettings.MIN_PASSWORD_LENGTH)
            || password.length() > settings.getProperty(SecuritySettings.MAX_PASSWORD_LENGTH)) {
            return new ValidationResult(MessageKey.INVALID_PASSWORD_LENGTH);
        } else if (settings.getProperty(SecuritySettings.UNSAFE_PASSWORDS).contains(passLow)) {
            return new ValidationResult(MessageKey.PASSWORD_UNSAFE_ERROR);
        }
        return new ValidationResult();
    }

    /**
     * Verifies whether the email is valid and admitted for use according to the plugin settings.
     *
     * @param email the email to verify
     * @return true if the email is valid, false otherwise
     */
    public boolean validateEmail(String email) {
        if (!email.contains("@") || "your@email.com".equalsIgnoreCase(email)) {
            return false;
        }
        final String emailDomain = email.split("@")[1];
        return validateWhitelistAndBlacklist(
            emailDomain, EmailSettings.DOMAIN_WHITELIST, EmailSettings.DOMAIN_BLACKLIST);
    }

    /**
     * Queries the database whether the email is still free for registration, i.e. whether the given
     * command sender may use the email to register a new account (as defined by settings and permissions).
     *
     * @param email the email to verify
     * @param sender the command sender
     * @return true if the email may be used, false otherwise (registration threshold has been exceeded)
     */
    public boolean isEmailFreeForRegistration(String email, CommandSender sender) {
        return permissionsManager.hasPermission(sender, PlayerStatePermission.ALLOW_MULTIPLE_ACCOUNTS)
            || dataSource.countAuthsByEmail(email) < settings.getProperty(EmailSettings.MAX_REG_PER_EMAIL);
    }

    /**
     * Checks whether the player's country is allowed to join the server, based on the given IP address
     * and the configured country whitelist or blacklist.
     *
     * @param hostAddress the IP address to verify
     * @return true if the IP address' country is allowed, false otherwise
     */
    public boolean isCountryAdmitted(String hostAddress) {
        // Check if we have restrictions on country, if not return true and avoid the country lookup
        if (settings.getProperty(ProtectionSettings.COUNTRIES_WHITELIST).isEmpty()
            && settings.getProperty(ProtectionSettings.COUNTRIES_BLACKLIST).isEmpty()) {
            return true;
        }

        String countryCode = geoLiteApi.getCountryCode(hostAddress);
        return validateWhitelistAndBlacklist(countryCode,
            ProtectionSettings.COUNTRIES_WHITELIST,
            ProtectionSettings.COUNTRIES_BLACKLIST);
    }

    /**
     * Checks if the name is unrestricted according to the configured settings.
     *
     * @param name the name to verify
     * @return true if unrestricted, false otherwise
     */
    public boolean isUnrestricted(String name) {
        return unrestrictedNames.contains(name.toLowerCase());
    }

    /**
     * Verifies whether the given value is allowed according to the given whitelist and blacklist settings.
     * Whitelist has precedence over blacklist: if a whitelist is set, the value is rejected if not present
     * in the whitelist.
     *
     * @param value the value to verify
     * @param whitelist the whitelist property
     * @param blacklist the blacklist property
     * @return true if the value is admitted by the lists, false otherwise
     */
    private boolean validateWhitelistAndBlacklist(String value, Property<List<String>> whitelist,
                                                  Property<List<String>> blacklist) {
        List<String> whitelistValue = settings.getProperty(whitelist);
        if (!CollectionUtils.isEmpty(whitelistValue)) {
            return containsIgnoreCase(whitelistValue, value);
        }
        List<String> blacklistValue = settings.getProperty(blacklist);
        return CollectionUtils.isEmpty(blacklistValue) || !containsIgnoreCase(blacklistValue, value);
    }

    private static boolean containsIgnoreCase(Collection<String> coll, String needle) {
        for (String entry : coll) {
            if (entry.equalsIgnoreCase(needle)) {
                return true;
            }
        }
        return false;
    }

    public static final class ValidationResult {
        private final MessageKey messageKey;
        private final String[] args;

        /**
         * Constructor for a successful validation.
         */
        public ValidationResult() {
            this.messageKey = null;
            this.args = null;
        }

        /**
         * Constructor for a failed validation.
         *
         * @param messageKey message key of the validation error
         * @param args arguments for the message key
         */
        public ValidationResult(MessageKey messageKey, String... args) {
            this.messageKey = messageKey;
            this.args = args;
        }

        /**
         * Returns whether an error was found during the validation, i.e. whether the validation failed.
         *
         * @return true if there is an error, false if the validation was successful
         */
        public boolean hasError() {
            return messageKey != null;
        }

        public MessageKey getMessageKey() {
            return messageKey;
        }
        public String[] getArgs() {
            return args;
        }
    }
}
