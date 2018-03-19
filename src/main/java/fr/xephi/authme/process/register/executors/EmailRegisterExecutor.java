package fr.xephi.authme.process.register.executors;

import fr.xephi.authme.data.auth.PlayerAuth;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.mail.EmailService;
import fr.xephi.authme.message.MessageKey;
import fr.xephi.authme.process.SyncProcessManager;
import fr.xephi.authme.security.PasswordSecurity;
import fr.xephi.authme.security.crypts.HashedPassword;
import fr.xephi.authme.service.CommonService;
import fr.xephi.authme.settings.properties.EmailSettings;
import fr.xephi.authme.util.RandomStringUtils;
import org.bukkit.entity.Player;

import javax.inject.Inject;

import static fr.xephi.authme.permission.PlayerStatePermission.ALLOW_MULTIPLE_ACCOUNTS;
import static fr.xephi.authme.process.register.executors.PlayerAuthBuilderHelper.createPlayerAuth;
import static fr.xephi.authme.settings.properties.EmailSettings.RECOVERY_PASSWORD_LENGTH;

/**
 * Executor for email registration: the player only provides his email address,
 * to which a generated password is sent.
 */
class EmailRegisterExecutor implements RegistrationExecutor<EmailRegisterParams> {

    @Inject
    private DataSource dataSource;

    @Inject
    private CommonService commonService;

    @Inject
    private EmailService emailService;

    @Inject
    private SyncProcessManager syncProcessManager;

    @Inject
    private PasswordSecurity passwordSecurity;

    @Override
    public boolean isRegistrationAdmitted(EmailRegisterParams params) {
        final int maxRegPerEmail = commonService.getProperty(EmailSettings.MAX_REG_PER_EMAIL);
        if (maxRegPerEmail > 0 && !commonService.hasPermission(params.getPlayer(), ALLOW_MULTIPLE_ACCOUNTS)) {
            int otherAccounts = dataSource.countAuthsByEmail(params.getEmail());
            if (otherAccounts >= maxRegPerEmail) {
                commonService.send(params.getPlayer(), MessageKey.MAX_REGISTER_EXCEEDED,
                    Integer.toString(maxRegPerEmail), Integer.toString(otherAccounts), "@");
                return false;
            }
        }
        return true;
    }

    @Override
    public PlayerAuth buildPlayerAuth(EmailRegisterParams params) {
        String password = RandomStringUtils.generate(commonService.getProperty(RECOVERY_PASSWORD_LENGTH));
        HashedPassword hashedPassword = passwordSecurity.computeHash(password, params.getPlayer().getName());
        params.setPassword(password);
        return createPlayerAuth(params.getPlayer(), hashedPassword, params.getEmail());
    }

    @Override
    public void executePostPersistAction(EmailRegisterParams params) {
        Player player = params.getPlayer();
        boolean couldSendMail = emailService.sendPasswordMail(
            player.getName(), params.getEmail(), params.getPassword());
        if (couldSendMail) {
            syncProcessManager.processSyncEmailRegister(player);
        } else {
            commonService.send(player, MessageKey.EMAIL_SEND_FAILURE);
        }
    }

}
