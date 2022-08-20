package fr.xephi.authme.service;

import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.initialization.HasCleanup;
import fr.xephi.authme.initialization.Reloadable;
import fr.xephi.authme.output.ConsoleLoggerFactory;
import fr.xephi.authme.mail.EmailService;
import fr.xephi.authme.message.MessageKey;
import fr.xephi.authme.message.Messages;
import fr.xephi.authme.security.PasswordSecurity;
import fr.xephi.authme.security.crypts.HashedPassword;
import fr.xephi.authme.settings.properties.SecuritySettings;
import fr.xephi.authme.util.PlayerUtils;
import fr.xephi.authme.util.RandomStringUtils;
import fr.xephi.authme.util.expiring.Duration;
import fr.xephi.authme.util.expiring.ExpiringMap;
import fr.xephi.authme.util.expiring.ExpiringSet;
import org.bukkit.entity.Player;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import static fr.xephi.authme.settings.properties.EmailSettings.RECOVERY_PASSWORD_LENGTH;

/**
 * Manager for password recovery.
 */
public class PasswordRecoveryService implements Reloadable, HasCleanup {
    
    private final ConsoleLogger logger = ConsoleLoggerFactory.get(PasswordRecoveryService.class);

    @Inject
    private CommonService commonService;

    @Inject
    private DataSource dataSource;

    @Inject
    private EmailService emailService;

    @Inject
    private PasswordSecurity passwordSecurity;

    @Inject
    private RecoveryCodeService recoveryCodeService;

    @Inject
    private Messages messages;

    private ExpiringSet<String> emailCooldown;
    private ExpiringMap<String, String> successfulRecovers;

    @PostConstruct
    private void initEmailCooldownSet() {
        emailCooldown = new ExpiringSet<>(
            commonService.getProperty(SecuritySettings.EMAIL_RECOVERY_COOLDOWN_SECONDS), TimeUnit.SECONDS);
        successfulRecovers = new ExpiringMap<>(
            commonService.getProperty(SecuritySettings.PASSWORD_CHANGE_TIMEOUT), TimeUnit.MINUTES);
    }

    /**
     * Create a new recovery code and send it to the player
     * via email.
     *
     * @param player The player getting the code.
     * @param email The email to send the code to.
     */
    public void createAndSendRecoveryCode(Player player, String email) {
        if (!checkEmailCooldown(player)) {
            return;
        }

        String recoveryCode = recoveryCodeService.generateCode(player.getName());
        boolean couldSendMail = emailService.sendRecoveryCode(player.getName(), email, recoveryCode);
        if (couldSendMail) {
            commonService.send(player, MessageKey.RECOVERY_CODE_SENT);
            emailCooldown.add(player.getName().toLowerCase(Locale.ROOT));
        } else {
            commonService.send(player, MessageKey.EMAIL_SEND_FAILURE);
        }
    }

    /**
     * Generate a new password and send it to the player via
     * email. This will update the database with the new password.
     *
     * @param player The player recovering their password.
     * @param email The email to send the password to.
     */
    public void generateAndSendNewPassword(Player player, String email) {
        if (!checkEmailCooldown(player)) {
            return;
        }

        String name = player.getName();
        String thePass = RandomStringUtils.generate(commonService.getProperty(RECOVERY_PASSWORD_LENGTH));
        HashedPassword hashNew = passwordSecurity.computeHash(thePass, name);

        logger.info("Generating new password for '" + name + "'");

        dataSource.updatePassword(name, hashNew);
        boolean couldSendMail = emailService.sendPasswordMail(name, email, thePass);
        if (couldSendMail) {
            commonService.send(player, MessageKey.RECOVERY_EMAIL_SENT_MESSAGE);
            emailCooldown.add(player.getName().toLowerCase(Locale.ROOT));
        } else {
            commonService.send(player, MessageKey.EMAIL_SEND_FAILURE);
        }
    }

    /**
     * Allows a player to change their password after
     * correctly entering a recovery code.
     *
     * @param player The player recovering their password.
     */
    public void addSuccessfulRecovery(Player player) {
        String name = player.getName();
        String address = PlayerUtils.getPlayerIp(player);

        successfulRecovers.put(name, address);
        commonService.send(player, MessageKey.RECOVERY_CHANGE_PASSWORD);
    }

    /**
     * Removes a player from the list of successful recovers so that he can
     * no longer use the /email setpassword command.
     *
     * @param player The player to remove.
     */
    public void removeFromSuccessfulRecovery(Player player) {
        successfulRecovers.remove(player.getName());
    }

    /**
     * Check if a player is able to have emails sent.
     *
     * @param player The player to check.
     * @return True if the player is not on cooldown.
     */
    private boolean checkEmailCooldown(Player player) {
        Duration waitDuration = emailCooldown.getExpiration(player.getName().toLowerCase(Locale.ROOT));
        if (waitDuration.getDuration() > 0) {
            String durationText = messages.formatDuration(waitDuration);
            messages.send(player, MessageKey.EMAIL_COOLDOWN_ERROR, durationText);
            return false;
        }
        return true;
    }

    /**
     * Checks if a player can change their password after recovery
     * using the /email setpassword command.
     *
     * @param player The player to check.
     * @return True if the player can change their password.
     */
    public boolean canChangePassword(Player player) {
        String name = player.getName();
        String playerAddress = PlayerUtils.getPlayerIp(player);
        String storedAddress = successfulRecovers.get(name);

        return storedAddress != null && playerAddress.equals(storedAddress);
    }

    @Override
    public void reload() {
        emailCooldown.setExpiration(
            commonService.getProperty(SecuritySettings.EMAIL_RECOVERY_COOLDOWN_SECONDS), TimeUnit.SECONDS);
        successfulRecovers.setExpiration(
            commonService.getProperty(SecuritySettings.PASSWORD_CHANGE_TIMEOUT), TimeUnit.MINUTES);
    }

    @Override
    public void performCleanup() {
        emailCooldown.removeExpiredEntries();
        successfulRecovers.removeExpiredEntries();
    }
}
