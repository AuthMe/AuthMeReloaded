package fr.xephi.authme.service;

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

    DialogWindowService() {
    }

    public DialogWindowSpec createLoginDialog(Player player) {
        boolean showRecovery = commonService.getProperty(RegistrationSettings.DIALOG_SHOW_FORGOT_PASSWORD_BUTTON);
        boolean showBody = commonService.getProperty(RegistrationSettings.DIALOG_SHOW_BODY);
        Function<MessageKey, String> text = key -> getPostJoinMessage(player, key);

        List<DialogInputSpec> inputs = new ArrayList<>();
        inputs.add(new DialogInputSpec("password",
            text.apply(MessageKey.DIALOG_LOGIN_PASSWORD),
            DEFAULT_MAX_INPUT_LENGTH));
        if (showRecovery) {
            inputs.add(new DialogInputSpec("email",
                text.apply(MessageKey.DIALOG_LOGIN_RECOVERY_EMAIL),
                DEFAULT_MAX_INPUT_LENGTH));
        }

        return new DialogWindowSpec(
            text.apply(MessageKey.DIALOG_LOGIN_TITLE),
            inputs,
            text.apply(MessageKey.DIALOG_LOGIN_BUTTON),
            showRecovery ? text.apply(MessageKey.DIALOG_LOGIN_RECOVERY_BUTTON) : text.apply(MessageKey.DIALOG_CANCEL_BUTTON),
            showRecovery,
            false,
            showRecovery ? "email recover $(email)" : null,
            showBody ? text.apply(MessageKey.DIALOG_LOGIN_BODY) : null);
    }

    public DialogWindowSpec createPreJoinLoginDialog(String playerName) {
        return createLoginDialogSpec(key -> messages.retrieveSingle(playerName, key),
            commonService.getProperty(RegistrationSettings.PRE_JOIN_DIALOG_SHOW_CANCEL_BUTTON),
            commonService.getProperty(RegistrationSettings.PRE_JOIN_DIALOG_ALLOW_CLOSE_WITH_ESCAPE));
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
                                                   boolean showSecondaryButton,
                                                   boolean canCloseWithEscape) {
        boolean showBody = commonService.getProperty(RegistrationSettings.DIALOG_SHOW_BODY);
        return new DialogWindowSpec(
            textResolver.apply(MessageKey.DIALOG_LOGIN_TITLE),
            List.of(new DialogInputSpec("password",
                textResolver.apply(MessageKey.DIALOG_LOGIN_PASSWORD),
                DEFAULT_MAX_INPUT_LENGTH)),
            textResolver.apply(MessageKey.DIALOG_LOGIN_BUTTON),
            textResolver.apply(MessageKey.DIALOG_CANCEL_BUTTON),
            showSecondaryButton,
            canCloseWithEscape,
            null,
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
