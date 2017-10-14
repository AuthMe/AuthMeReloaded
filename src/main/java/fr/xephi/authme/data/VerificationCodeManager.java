package fr.xephi.authme.data;

import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.datasource.DataSourceResult;
import fr.xephi.authme.initialization.HasCleanup;
import fr.xephi.authme.initialization.SettingsDependent;
import fr.xephi.authme.mail.EmailService;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.SecuritySettings;
import fr.xephi.authme.util.RandomStringUtils;
import fr.xephi.authme.util.expiring.ExpiringMap;

import javax.inject.Inject;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class VerificationCodeManager implements SettingsDependent, HasCleanup {

    @Inject
    public EmailService emailService;

    @Inject
    private DataSource dataSource;

    private final ExpiringMap<String, String> verificationCodes;
    private final Set<String> verifiedPlayers;

    private boolean isEnabled;

    @Inject
    VerificationCodeManager(Settings settings){
        isEnabled = settings.getProperty(SecuritySettings.USE_VERIFICATION_CODES);
        long countTimeout = settings.getProperty(SecuritySettings.VERIFICATION_CODE_EXPIRATION_MINUTES);
        verificationCodes = new ExpiringMap<>(countTimeout, TimeUnit.MINUTES);
        verifiedPlayers = new HashSet<>();
    }

    /**
     * Returns whether the given player is able to verify his identity
     *
     * @param name the name of the player to verify
     * @return true if the player has not been verified yet, false otherwise
     */
    public boolean isVerificationRequired(String name){
        return isEnabled && !verifiedPlayers.contains(name.toLowerCase());
    }

    /**
     * Returns whether the given player is required to verify his identity trough a command
     *
     * @param name the name of the player to verify
     * @return true if the player has an existing code and has not been verified yet, false otherwise
     */
    public boolean isCodeRequired(String name){
        return isEnabled && (verificationCodes.get(name) != null) && !verifiedPlayers.contains(name.toLowerCase());
    }

    /**
     * Returns the stored code for the player or generates and saves a new one.
     *
     * @param name the player's name
     * @return the code the player is required to enter
     */
    public String getCodeOrGenerateNewOne(String name){
        String code = verificationCodes.get(name.toLowerCase());
        return code == null ? generateCode(name) : code;
    }

    /**
     * Generates a code for the player and returns it.
     *
     * @param name the name of the player to generate a code for
     * @return the generated code
     */
    public String generateCode(String name){
        String code = RandomStringUtils.generateNum(6); // 6 digits code
        verificationCodes.put(name.toLowerCase(), code);
        DataSourceResult<String> emailResult = dataSource.getEmail(name);
        if (!emailResult.playerExists()) {  // Is this check required?
            final String email = emailResult.getValue();
            emailService.sendVerificationMail(name, email, code);
        }
        return code;
    }

    /**
     * Checks the given code against the existing one.
     *
     * @param name the name of the player to check
     * @param code the supplied code
     * @return true if the code matches, false otherwise
     */
    public boolean checkCode(String name, String code){
        boolean correct = false;
        if(verificationCodes.get(name).equals(code)){
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
    public void reload(Settings settings){
        isEnabled = settings.getProperty(SecuritySettings.USE_VERIFICATION_CODES);
        long countTimeout = settings.getProperty(SecuritySettings.VERIFICATION_CODE_EXPIRATION_MINUTES);
        verificationCodes.setExpiration(countTimeout, TimeUnit.MINUTES);
    }

    @Override
    public void performCleanup(){
        verificationCodes.removeExpiredEntries();
    }
}
