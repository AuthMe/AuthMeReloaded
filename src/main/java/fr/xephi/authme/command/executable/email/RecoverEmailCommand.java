package fr.xephi.authme.command.executable.email;

import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.command.PlayerCommand;
import fr.xephi.authme.data.auth.PlayerAuth;
import fr.xephi.authme.data.auth.PlayerCache;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.initialization.Reloadable;
import fr.xephi.authme.mail.EmailService;
import fr.xephi.authme.message.MessageKey;
import fr.xephi.authme.message.Messages;
import fr.xephi.authme.security.PasswordSecurity;
import fr.xephi.authme.security.crypts.HashedPassword;
import fr.xephi.authme.service.CommonService;
import fr.xephi.authme.service.RecoveryCodeService;
import fr.xephi.authme.settings.properties.SecuritySettings;
import fr.xephi.authme.util.RandomStringUtils;
import fr.xephi.authme.util.expiring.Duration;
import fr.xephi.authme.util.expiring.ExpiringSet;
import org.bukkit.entity.Player;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static fr.xephi.authme.settings.properties.EmailSettings.RECOVERY_PASSWORD_LENGTH;

/**
 * Command for password recovery by email.
 */
public class RecoverEmailCommand extends PlayerCommand implements Reloadable {

    @Inject
    private PasswordSecurity passwordSecurity;

    @Inject
    private CommonService commonService;

    @Inject
    private DataSource dataSource;

    @Inject
    private PlayerCache playerCache;

    @Inject
    private EmailService emailService;

    @Inject
    private RecoveryCodeService recoveryCodeService;

    @Inject
    private Messages messages;

    private ExpiringSet<String> emailCooldown;

    @PostConstruct
    private void initEmailCooldownSet() {
        emailCooldown = new ExpiringSet<>(
            commonService.getProperty(SecuritySettings.EMAIL_RECOVERY_COOLDOWN_SECONDS), TimeUnit.SECONDS);
    }

    @Override
    protected void runCommand(Player player, List<String> arguments) {
        final String playerMail = arguments.get(0);
        final String playerName = player.getName();

        if (!emailService.hasAllInformation()) {
            ConsoleLogger.warning("Mail API is not set");
            commonService.send(player, MessageKey.INCOMPLETE_EMAIL_SETTINGS);
            return;
        }
        if (playerCache.isAuthenticated(playerName)) {
            commonService.send(player, MessageKey.ALREADY_LOGGED_IN_ERROR);
            return;
        }

        PlayerAuth auth = dataSource.getAuth(playerName); // TODO: Create method to get email only
        if (auth == null) {
            commonService.send(player, MessageKey.USAGE_REGISTER);
            return;
        }

        final String email = auth.getEmail();
        if (email == null || !email.equalsIgnoreCase(playerMail) || "your@email.com".equalsIgnoreCase(email)) {
            commonService.send(player, MessageKey.INVALID_EMAIL);
            return;
        }

        if (recoveryCodeService.isRecoveryCodeNeeded()) {
            // Process /email recovery addr@example.com
            if (arguments.size() == 1) {
                createAndSendRecoveryCode(player, email);
            } else {
                // Process /email recovery addr@example.com 12394
                processRecoveryCode(player, arguments.get(1), email);
            }
        } else {
            boolean maySendMail = checkEmailCooldown(player);
            if (maySendMail) {
                generateAndSendNewPassword(player, email);
            }
        }
    }

    @Override
    public void reload() {
        emailCooldown.setExpiration(
            commonService.getProperty(SecuritySettings.EMAIL_RECOVERY_COOLDOWN_SECONDS), TimeUnit.SECONDS);
    }

    private void createAndSendRecoveryCode(Player player, String email) {
        if (!checkEmailCooldown(player)) {
            return;
        }

        String recoveryCode = recoveryCodeService.generateCode(player.getName());
        boolean couldSendMail = emailService.sendRecoveryCode(player.getName(), email, recoveryCode);
        if (couldSendMail) {
            commonService.send(player, MessageKey.RECOVERY_CODE_SENT);
            emailCooldown.add(player.getName().toLowerCase());
        } else {
            commonService.send(player, MessageKey.EMAIL_SEND_FAILURE);
        }
    }

    private void processRecoveryCode(Player player, String code, String email) {
        final String name = player.getName();
        if (recoveryCodeService.isCodeValid(name, code)) {
            generateAndSendNewPassword(player, email);
            recoveryCodeService.removeCode(name);
        } else {
            commonService.send(player, MessageKey.INCORRECT_RECOVERY_CODE);
        }
    }

    private void generateAndSendNewPassword(Player player, String email) {
        String name = player.getName();
        String thePass = RandomStringUtils.generate(commonService.getProperty(RECOVERY_PASSWORD_LENGTH));
        HashedPassword hashNew = passwordSecurity.computeHash(thePass, name);

        dataSource.updatePassword(name, hashNew);
        boolean couldSendMail = emailService.sendPasswordMail(name, email, thePass);
        if (couldSendMail) {
            commonService.send(player, MessageKey.RECOVERY_EMAIL_SENT_MESSAGE);
            emailCooldown.add(player.getName().toLowerCase());
        } else {
            commonService.send(player, MessageKey.EMAIL_SEND_FAILURE);
        }
    }

    private boolean checkEmailCooldown(Player player) {
        Duration waitDuration = emailCooldown.getExpiration(player.getName().toLowerCase());
        if (waitDuration.getDuration() > 0) {
            String durationText = messages.formatDuration(waitDuration);
            messages.send(player, MessageKey.EMAIL_COOLDOWN_ERROR, durationText);
            return false;
        }
        return true;
    }
}
