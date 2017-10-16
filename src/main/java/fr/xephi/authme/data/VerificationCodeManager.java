package fr.xephi.authme.data;

import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.datasource.DataSourceResult;
import fr.xephi.authme.initialization.HasCleanup;
import fr.xephi.authme.initialization.SettingsDependent;
import fr.xephi.authme.mail.EmailService;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.SecuritySettings;
import fr.xephi.authme.util.RandomStringUtils;
import fr.xephi.authme.util.Utils;
import fr.xephi.authme.util.expiring.ExpiringMap;

import javax.inject.Inject;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class VerificationCodeManager implements SettingsDependent, HasCleanup {

    private EmailService emailService;
    private DataSource dataSource;

    private final ExpiringMap<String, String> verificationCodes;
    private final ExpiringMap<String, String> tempEmails;   // Store the email for 1 min?
    private final Set<String> verifiedPlayers;

    private boolean isEnabled;

    @Inject
    VerificationCodeManager(Settings settings, DataSource dataSource, EmailService emailService) {
        this.emailService = emailService;
        this.dataSource = dataSource;
        isEnabled = emailService.hasAllInformation() && settings.getProperty(SecuritySettings.USE_VERIFICATION_CODES);
        long countTimeout = settings.getProperty(SecuritySettings.VERIFICATION_CODE_EXPIRATION_MINUTES);
        verificationCodes = new ExpiringMap<>(countTimeout, TimeUnit.MINUTES);
        tempEmails = new ExpiringMap<>(1, TimeUnit.MINUTES);
        verifiedPlayers = new HashSet<>();
    }

    /**
     * Returns whether the given player is able to verify his identity
     *
     * @param name the name of the player to verify
     * @return true if the player has not been verified yet, false otherwise
     */
    public boolean isVerificationRequired(String name) {
        boolean result = false;
        if(isEnabled && !verifiedPlayers.contains(name.toLowerCase())) {
            DataSourceResult<String> emailResult = dataSource.getEmail(name);
            if (emailResult.playerExists()) {
                final String email = emailResult.getValue();
                if(!Utils.isEmailEmpty(email)) {
                    tempEmails.put(name.toLowerCase(), email);
                    result = true;
                }
            }
        }
        return result;
    }

    /**
     * Returns whether the given player is required to verify his identity trough a command
     *
     * @param name the name of the player to verify
     * @return true if the player has an existing code and has not been verified yet, false otherwise
     */
    public boolean isCodeRequired(String name) {
        return isEnabled && (verificationCodes.get(name.toLowerCase()) != null) && !verifiedPlayers.contains(name.toLowerCase());
    }

    /**
     * Check if a code exist for the player or generates and saves a new one.
     *
     * @param name the player's name
     */
    public void codeExistOrGenerateNew(String name) {
        String code = verificationCodes.get(name.toLowerCase());
        if(code == null){
            generateCode(name);
        }
    }

    /**
     * Generates a code for the player and returns it.
     *
     * @param name the name of the player to generate a code for
     */
    private void generateCode(String name) {
        String code = RandomStringUtils.generateNum(6); // 6 digits code
        verificationCodes.put(name.toLowerCase(), code);
        if(tempEmails.get(name.toLowerCase()) != null) {
            final String email = tempEmails.get(name.toLowerCase());
            emailService.sendVerificationMail(name, email, code);
        } else {
            DataSourceResult<String> emailResult = dataSource.getEmail(name);
            if (emailResult.playerExists()) {
                final String email = emailResult.getValue();
                if(!Utils.isEmailEmpty(email)) {
                    emailService.sendVerificationMail(name, email, code);
                }
            }
        }
    }

    /**
     * Checks the given code against the existing one.
     *
     * @param name the name of the player to check
     * @param code the supplied code
     * @return true if the code matches, false otherwise
     */
    public boolean checkCode(String name, String code) {
        boolean correct = false;
        if(verificationCodes.get(name.toLowerCase()).equals(code)) {
            verify(name);
            correct = true;
        }
        return correct;
    }

    /**
     * Add the user to the set of verified users
     *
     * @param name the name of the player to generate a code for
     */
    public void verify(String name){
        verifiedPlayers.add(name.toLowerCase());
    }

    /**
     * Remove the user from the set of verified users
     *
     * @param name the name of the player to generate a code for
     */
    public void unverify(String name){
        verifiedPlayers.remove(name.toLowerCase());
    }

    @Override
    public void reload(Settings settings) {
        isEnabled = settings.getProperty(SecuritySettings.USE_VERIFICATION_CODES);
        long countTimeout = settings.getProperty(SecuritySettings.VERIFICATION_CODE_EXPIRATION_MINUTES);
        verificationCodes.setExpiration(countTimeout, TimeUnit.MINUTES);
    }

    @Override
    public void performCleanup(){
        verificationCodes.removeExpiredEntries();
        tempEmails.removeExpiredEntries();
    }
}
