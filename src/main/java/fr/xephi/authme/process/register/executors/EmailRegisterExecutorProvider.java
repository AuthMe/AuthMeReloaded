package fr.xephi.authme.process.register.executors;

import fr.xephi.authme.data.auth.PlayerAuth;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.mail.SendMailSSL;
import fr.xephi.authme.message.MessageKey;
import fr.xephi.authme.permission.PermissionsManager;
import fr.xephi.authme.process.SyncProcessManager;
import fr.xephi.authme.security.PasswordSecurity;
import fr.xephi.authme.security.crypts.HashedPassword;
import fr.xephi.authme.service.CommonService;
import fr.xephi.authme.settings.properties.EmailSettings;
import fr.xephi.authme.util.RandomStringUtils;
import org.bukkit.entity.Player;

import javax.inject.Inject;

import static fr.xephi.authme.process.register.executors.PlayerAuthBuilderHelper.createPlayerAuth;
import static fr.xephi.authme.permission.PlayerStatePermission.ALLOW_MULTIPLE_ACCOUNTS;
import static fr.xephi.authme.settings.properties.EmailSettings.RECOVERY_PASSWORD_LENGTH;

/**
 * Provides a registration executor for email registration.
 */
class EmailRegisterExecutorProvider {

    @Inject
    private PermissionsManager permissionsManager;

    @Inject
    private DataSource dataSource;

    @Inject
    private CommonService commonService;

    @Inject
    private SendMailSSL sendMailSsl;

    @Inject
    private SyncProcessManager syncProcessManager;

    @Inject
    private PasswordSecurity passwordSecurity;

    EmailRegisterExecutorProvider() {
    }

    /** Registration executor implementation for email registration. */
    class EmailRegisterExecutor implements RegistrationExecutor {

        private final Player player;
        private final String email;
        private String password;

        EmailRegisterExecutor(Player player, String email) {
            this.player = player;
            this.email = email;
        }

        @Override
        public boolean isRegistrationAdmitted() {
            final int maxRegPerEmail = commonService.getProperty(EmailSettings.MAX_REG_PER_EMAIL);
            if (maxRegPerEmail > 0 && !permissionsManager.hasPermission(player, ALLOW_MULTIPLE_ACCOUNTS)) {
                int otherAccounts = dataSource.countAuthsByEmail(email);
                if (otherAccounts >= maxRegPerEmail) {
                    commonService.send(player, MessageKey.MAX_REGISTER_EXCEEDED, Integer.toString(maxRegPerEmail),
                        Integer.toString(otherAccounts), "@");
                    return false;
                }
            }
            return true;
        }

        @Override
        public PlayerAuth buildPlayerAuth() {
            password = RandomStringUtils.generate(commonService.getProperty(RECOVERY_PASSWORD_LENGTH));
            HashedPassword hashedPassword = passwordSecurity.computeHash(password, player.getName());
            return createPlayerAuth(player, hashedPassword, email);
        }

        @Override
        public void executePostPersistAction() {
            boolean couldSendMail = sendMailSsl.sendPasswordMail(player.getName(), email, password);
            if (couldSendMail) {
                syncProcessManager.processSyncEmailRegister(player);
            } else {
                commonService.send(player, MessageKey.EMAIL_SEND_FAILURE);
            }
        }
    }
}
