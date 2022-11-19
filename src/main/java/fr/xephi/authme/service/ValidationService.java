package fr.xephi.authme.service;

import ch.jalu.configme.properties.Property;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.initialization.Reloadable;
import fr.xephi.authme.message.MessageKey;
import fr.xephi.authme.output.ConsoleLoggerFactory;
import fr.xephi.authme.permission.PermissionsManager;
import fr.xephi.authme.permission.PlayerStatePermission;
import fr.xephi.authme.security.HashUtils;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.EmailSettings;
import fr.xephi.authme.settings.properties.ProtectionSettings;
import fr.xephi.authme.settings.properties.RestrictionSettings;
import fr.xephi.authme.settings.properties.SecuritySettings;
import fr.xephi.authme.util.PlayerUtils;
import fr.xephi.authme.util.StringUtils;
import fr.xephi.authme.util.Utils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.io.DataInputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

import static fr.xephi.authme.util.StringUtils.isInsideString;

/**
 * Validation service.
 */
public class ValidationService implements Reloadable {

    private final ConsoleLogger logger = ConsoleLoggerFactory.get(ValidationService.class);

    @Inject
    private Settings settings;
    @Inject
    private DataSource dataSource;
    @Inject
    private PermissionsManager permissionsManager;
    @Inject
    private GeoIpService geoIpService;

    private Pattern passwordRegex;
    private Multimap<String, String> restrictedNames;

    ValidationService() {
    }

    @PostConstruct
    @Override
    public void reload() {
        passwordRegex = Utils.safePatternCompile(settings.getProperty(RestrictionSettings.ALLOWED_PASSWORD_REGEX));
        restrictedNames = settings.getProperty(RestrictionSettings.ENABLE_RESTRICTED_USERS)
            ? loadNameRestrictions(settings.getProperty(RestrictionSettings.RESTRICTED_USERS))
            : HashMultimap.create();
    }

    /**
     * Verifies whether a password is valid according to the plugin settings.
     *
     * @param password the password to verify
     * @param username the username the password is associated with
     * @return the validation result
     */
    public ValidationResult validatePassword(String password, String username) {
        String passLow = password.toLowerCase(Locale.ROOT);
        if (!passwordRegex.matcher(passLow).matches()) {
            return new ValidationResult(MessageKey.PASSWORD_CHARACTERS_ERROR, passwordRegex.pattern());
        } else if (passLow.equalsIgnoreCase(username)) {
            return new ValidationResult(MessageKey.PASSWORD_IS_USERNAME_ERROR);
        } else if (password.length() < settings.getProperty(SecuritySettings.MIN_PASSWORD_LENGTH)
            || password.length() > settings.getProperty(SecuritySettings.MAX_PASSWORD_LENGTH)) {
            return new ValidationResult(MessageKey.INVALID_PASSWORD_LENGTH);
        } else if (settings.getProperty(SecuritySettings.UNSAFE_PASSWORDS).contains(passLow)) {
            return new ValidationResult(MessageKey.PASSWORD_UNSAFE_ERROR);
        } else if (settings.getProperty(SecuritySettings.HAVE_I_BEEN_PWNED_CHECK)) {
            HaveIBeenPwnedResults results = validatePasswordHaveIBeenPwned(password);

            if (results != null
                && results.isPwned()
                && results.getPwnCount() > settings.getProperty(SecuritySettings.HAVE_I_BEEN_PWNED_LIMIT)) {
                return new ValidationResult(MessageKey.PASSWORD_PWNED_ERROR, String.valueOf(results.getPwnCount()));
            }
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
        if (Utils.isEmailEmpty(email) || !StringUtils.isInsideString('@', email)) {
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
     * @param email  the email to verify
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

        String countryCode = geoIpService.getCountryCode(hostAddress);
        boolean isCountryAllowed = validateWhitelistAndBlacklist(countryCode,
            ProtectionSettings.COUNTRIES_WHITELIST, ProtectionSettings.COUNTRIES_BLACKLIST);
        logger.debug("Country code `{0}` for `{1}` is allowed: {2}", countryCode, hostAddress, isCountryAllowed);
        return isCountryAllowed;
    }

    /**
     * Checks if the name is unrestricted according to the configured settings.
     *
     * @param name the name to verify
     * @return true if unrestricted, false otherwise
     */
    public boolean isUnrestricted(String name) {
        return settings.getProperty(RestrictionSettings.UNRESTRICTED_NAMES).contains(name.toLowerCase(Locale.ROOT));
    }

    /**
     * Checks that the player meets any name restriction if present (IP/domain-based).
     *
     * @param player the player to check
     * @return true if the player may join, false if the player does not satisfy the name restrictions
     */
    public boolean fulfillsNameRestrictions(Player player) {
        Collection<String> restrictions = restrictedNames.get(player.getName().toLowerCase(Locale.ROOT));
        if (Utils.isCollectionEmpty(restrictions)) {
            return true;
        }

        String ip = PlayerUtils.getPlayerIp(player);
        String domain = player.getAddress().getHostName();
        for (String restriction : restrictions) {
            if (restriction.startsWith("regex:")) {
                restriction = restriction.replace("regex:", "");
            } else {
                restriction = restriction.replace("*", "(.*)");
            }
            if (ip.matches(restriction)) {
                return true;
            }
            if (domain.matches(restriction)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Verifies whether the given value is allowed according to the given whitelist and blacklist settings.
     * Whitelist has precedence over blacklist: if a whitelist is set, the value is rejected if not present
     * in the whitelist.
     *
     * @param value     the value to verify
     * @param whitelist the whitelist property
     * @param blacklist the blacklist property
     * @return true if the value is admitted by the lists, false otherwise
     */
    private boolean validateWhitelistAndBlacklist(String value, Property<List<String>> whitelist,
                                                  Property<List<String>> blacklist) {
        List<String> whitelistValue = settings.getProperty(whitelist);
        if (!Utils.isCollectionEmpty(whitelistValue)) {
            return containsIgnoreCase(whitelistValue, value);
        }
        List<String> blacklistValue = settings.getProperty(blacklist);
        return Utils.isCollectionEmpty(blacklistValue) || !containsIgnoreCase(blacklistValue, value);
    }

    private static boolean containsIgnoreCase(Collection<String> coll, String needle) {
        for (String entry : coll) {
            if (entry.equalsIgnoreCase(needle)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Loads the configured name restrictions into a Multimap by player name (all-lowercase).
     *
     * @param configuredRestrictions the restriction rules to convert to a map
     * @return map of allowed IPs/domain names by player name
     */
    private Multimap<String, String> loadNameRestrictions(Set<String> configuredRestrictions) {
        Multimap<String, String> restrictions = HashMultimap.create();
        for (String restriction : configuredRestrictions) {
            if (isInsideString(';', restriction)) {
                String[] data = restriction.split(";");
                restrictions.put(data[0].toLowerCase(Locale.ROOT), data[1]);
            } else {
                logger.warning("Restricted user rule must have a ';' separating name from restriction,"
                    + " but found: '" + restriction + "'");
            }
        }
        return restrictions;
    }

    /**
     * Check haveibeenpwned.com for the given password.
     *
     * @param password password to check for
     * @return Results of the check
     */
    public HaveIBeenPwnedResults validatePasswordHaveIBeenPwned(String password) {
        String hash = HashUtils.sha1(password);

        String hashPrefix = hash.substring(0, 5);

        try {
            String url = String.format("https://api.pwnedpasswords.com/range/%s", hashPrefix);
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", "AuthMeReloaded");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.setDoInput(true);
            StringBuilder outStr = new StringBuilder();

            try (DataInputStream input = new DataInputStream(connection.getInputStream())) {
                for (int c = input.read(); c != -1; c = input.read())
                    outStr.append((char) c);
            }

            String[] hashes = outStr.toString().split("\n");
            for (String hashSuffix : hashes) {
                String[] hashSuffixParts = hashSuffix.trim().split(":");
                if (hashSuffixParts[0].equalsIgnoreCase(hash.substring(5))) {
                    return new HaveIBeenPwnedResults(true, Integer.parseInt(hashSuffixParts[1]));
                }
            }

            return new HaveIBeenPwnedResults(false, 0);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
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
         * @param args       arguments for the message key
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

    public static final class HaveIBeenPwnedResults {
        private final boolean isPwned;
        private final int pwnCount;

        public HaveIBeenPwnedResults(boolean isPwned, int pwnCount) {
            this.isPwned = isPwned;
            this.pwnCount = pwnCount;
        }

        public boolean isPwned() {
            return isPwned;
        }

        public int getPwnCount() {
            return pwnCount;
        }
    }
}
