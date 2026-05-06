package fr.xephi.authme.service;

import fr.xephi.authme.mail.EmailService;
import fr.xephi.authme.message.MessageKey;
import fr.xephi.authme.message.Messages;
import fr.xephi.authme.platform.DialogInputSpec;
import fr.xephi.authme.platform.DialogWindowSpec;
import fr.xephi.authme.process.register.RegisterSecondaryArgument;
import fr.xephi.authme.process.register.RegistrationType;
import fr.xephi.authme.settings.properties.RegistrationSettings;
import org.bukkit.entity.Player;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Builds translated dialog specs shared by all platform-specific dialog renderers.
 */
public class DialogWindowService {

    private static final int DEFAULT_MAX_INPUT_LENGTH = 100;
    private static final int TOTP_CODE_MAX_INPUT_LENGTH = 16;

    @Inject
    private CommonService commonService;

    @Inject
    private Messages messages;

    @Inject
    private EmailService emailService;

    DialogWindowService() {
    }

    public DialogWindowSpec createLoginDialog(Player player) {
        boolean showRecovery = commonService.getProperty(RegistrationSettings.DIALOG_SHOW_FORGOT_PASSWORD_BUTTON)
            && emailService.hasAllInformation();
        boolean showBody = commonService.getProperty(RegistrationSettings.DIALOG_SHOW_BODY);
        return createLoginDialogSpec(
            key -> getPostJoinMessage(player, key),
            showRecovery,
            false,
            false,
            showBody);
    }

    public DialogWindowSpec createPreJoinLoginDialog(String playerName) {
        boolean showRecovery = commonService.getProperty(RegistrationSettings.DIALOG_SHOW_FORGOT_PASSWORD_BUTTON)
            && emailService.hasAllInformation();
        boolean showBody = commonService.getProperty(RegistrationSettings.DIALOG_SHOW_BODY);
        Function<MessageKey, String> text = key -> messages.retrieveSingle(playerName, key);

        // When recovery is shown it occupies the secondary button slot, so cancel button is suppressed.
        // Escape is also disabled: the player must either submit, use recovery, or disconnect.
        boolean showCancelButton = !showRecovery && commonService.getProperty(RegistrationSettings.PRE_JOIN_DIALOG_SHOW_CANCEL_BUTTON);
        boolean canCloseWithEscape = !showRecovery && commonService.getProperty(RegistrationSettings.PRE_JOIN_DIALOG_ALLOW_CLOSE_WITH_ESCAPE);

        // Password field only — email is collected on a separate recovery page if needed
        return new DialogWindowSpec(
            text.apply(MessageKey.DIALOG_LOGIN_TITLE),
            List.of(new DialogInputSpec("password", text.apply(MessageKey.DIALOG_LOGIN_PASSWORD), DEFAULT_MAX_INPUT_LENGTH)),
            text.apply(MessageKey.DIALOG_LOGIN_BUTTON),
            showRecovery ? text.apply(MessageKey.DIALOG_LOGIN_RECOVERY_BUTTON) : text.apply(MessageKey.DIALOG_CANCEL_BUTTON),
            showRecovery || showCancelButton,
            canCloseWithEscape,
            showRecovery ? "forgot_password" : null,
            showBody ? text.apply(MessageKey.DIALOG_LOGIN_BODY) : null);
    }

    public DialogWindowSpec createPreJoinRecoveryDialog(String playerName) {
        boolean showBody = commonService.getProperty(RegistrationSettings.DIALOG_SHOW_BODY);
        Function<MessageKey, String> text = key -> messages.retrieveSingle(playerName, key);

        return new DialogWindowSpec(
            text.apply(MessageKey.DIALOG_RECOVERY_TITLE),
            List.of(new DialogInputSpec("email", text.apply(MessageKey.DIALOG_RECOVERY_EMAIL), DEFAULT_MAX_INPUT_LENGTH)),
            text.apply(MessageKey.DIALOG_RECOVERY_BUTTON),
            text.apply(MessageKey.DIALOG_CANCEL_BUTTON),
            true,
            false,
            null,
            showBody ? text.apply(MessageKey.DIALOG_RECOVERY_BODY) : null);
    }

    public DialogWindowSpec createTotpDialog(Player player) {
        boolean showBody = commonService.getProperty(RegistrationSettings.DIALOG_SHOW_BODY);
        Function<MessageKey, String> text = key -> getPostJoinMessage(player, key);

        return new DialogWindowSpec(
            text.apply(MessageKey.DIALOG_TWO_FACTOR_TITLE),
            List.of(new DialogInputSpec("code",
                text.apply(MessageKey.DIALOG_TWO_FACTOR_CODE),
                TOTP_CODE_MAX_INPUT_LENGTH)),
            text.apply(MessageKey.DIALOG_TWO_FACTOR_BUTTON),
            text.apply(MessageKey.DIALOG_CANCEL_BUTTON),
            false,
            false,
            null,
            showBody ? text.apply(MessageKey.DIALOG_TWO_FACTOR_BODY) : null);
    }

    public DialogWindowSpec createRegisterDialog(Player player,
                                                 RegistrationType type,
                                                 RegisterSecondaryArgument secondArg) {
        return createRegisterDialogSpec(type, secondArg, key -> getPostJoinMessage(player, key), false, false);
    }

    public DialogWindowSpec createPreJoinRegisterDialog(String playerName,
                                                        RegistrationType type,
                                                        RegisterSecondaryArgument secondArg) {
        return createRegisterDialogSpec(type, secondArg, key -> messages.retrieveSingle(playerName, key),
            commonService.getProperty(RegistrationSettings.PRE_JOIN_DIALOG_SHOW_CANCEL_BUTTON),
            commonService.getProperty(RegistrationSettings.PRE_JOIN_DIALOG_ALLOW_CLOSE_WITH_ESCAPE));
    }

    private DialogWindowSpec createLoginDialogSpec(Function<MessageKey, String> textResolver,
                                                   boolean showRecovery,
                                                   boolean showCancelButton,
                                                   boolean canCloseWithEscape,
                                                   boolean showBody) {
        List<DialogInputSpec> inputs = new ArrayList<>();
        inputs.add(new DialogInputSpec("password",
            textResolver.apply(MessageKey.DIALOG_LOGIN_PASSWORD),
            DEFAULT_MAX_INPUT_LENGTH));
        if (showRecovery) {
            inputs.add(new DialogInputSpec("email",
                textResolver.apply(MessageKey.DIALOG_LOGIN_RECOVERY_EMAIL),
                DEFAULT_MAX_INPUT_LENGTH));
        }

        boolean showSecondaryButton = showRecovery || showCancelButton;
        String secondaryLabel = showRecovery
            ? textResolver.apply(MessageKey.DIALOG_LOGIN_RECOVERY_BUTTON)
            : textResolver.apply(MessageKey.DIALOG_CANCEL_BUTTON);

        return new DialogWindowSpec(
            textResolver.apply(MessageKey.DIALOG_LOGIN_TITLE),
            inputs,
            textResolver.apply(MessageKey.DIALOG_LOGIN_BUTTON),
            secondaryLabel,
            showSecondaryButton,
            canCloseWithEscape,
            showRecovery ? "email recover $(email)" : null,
            showBody ? textResolver.apply(MessageKey.DIALOG_LOGIN_BODY) : null);
    }

    private DialogWindowSpec createRegisterDialogSpec(RegistrationType type,
                                                      RegisterSecondaryArgument secondArg,
                                                      Function<MessageKey, String> textResolver,
                                                      boolean showSecondaryButton,
                                                      boolean canCloseWithEscape) {
        boolean showBody = commonService.getProperty(RegistrationSettings.DIALOG_SHOW_BODY);
        List<DialogInputSpec> inputs = new ArrayList<>();

        if (type == RegistrationType.EMAIL) {
            inputs.add(new DialogInputSpec("email",
                textResolver.apply(MessageKey.DIALOG_REGISTER_EMAIL),
                DEFAULT_MAX_INPUT_LENGTH));
            if (secondArg == RegisterSecondaryArgument.CONFIRMATION) {
                inputs.add(new DialogInputSpec("confirm",
                    textResolver.apply(MessageKey.DIALOG_REGISTER_CONFIRM_EMAIL),
                    DEFAULT_MAX_INPUT_LENGTH));
            }
        } else {
            // For EMAIL_MANDATORY: email is placed before password — it is the account identifier
            // and mirrors the signup order most players expect (email → then choose a password).
            if (secondArg == RegisterSecondaryArgument.EMAIL_MANDATORY) {
                inputs.add(new DialogInputSpec("email",
                    textResolver.apply(MessageKey.DIALOG_REGISTER_EMAIL),
                    DEFAULT_MAX_INPUT_LENGTH));
            }
            inputs.add(new DialogInputSpec("password",
                textResolver.apply(MessageKey.DIALOG_REGISTER_PASSWORD),
                DEFAULT_MAX_INPUT_LENGTH));
            if (secondArg == RegisterSecondaryArgument.CONFIRMATION) {
                inputs.add(new DialogInputSpec("confirm",
                    textResolver.apply(MessageKey.DIALOG_REGISTER_CONFIRM_PASSWORD),
                    DEFAULT_MAX_INPUT_LENGTH));
            } else if (secondArg == RegisterSecondaryArgument.EMAIL_OPTIONAL) {
                inputs.add(new DialogInputSpec("email",
                    textResolver.apply(MessageKey.DIALOG_REGISTER_EMAIL),
                    DEFAULT_MAX_INPUT_LENGTH));
            }
        }

        return new DialogWindowSpec(
            textResolver.apply(MessageKey.DIALOG_REGISTER_TITLE),
            inputs,
            textResolver.apply(MessageKey.DIALOG_REGISTER_BUTTON),
            textResolver.apply(MessageKey.DIALOG_CANCEL_BUTTON),
            showSecondaryButton,
            canCloseWithEscape,
            null,
            showBody ? textResolver.apply(MessageKey.DIALOG_REGISTER_BODY) : null);
    }

    private String getPostJoinMessage(Player player, MessageKey key) {
        return commonService.retrieveSingleMessage(player, key);
    }
}
