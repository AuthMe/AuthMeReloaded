package fr.xephi.authme.platform;

import fr.xephi.authme.process.register.RegisterSecondaryArgument;
import fr.xephi.authme.process.register.RegistrationType;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Encapsulates the Paper-derived dialog API calls used by Paper and Folia.
 */
public final class PaperDialogHelper {

    private PaperDialogHelper() {
    }

    static void showLoginDialog(Player player) {
        player.showDialog(createInGameLoginDialog());
    }

    static void showRegisterDialog(Player player, RegistrationType type, RegisterSecondaryArgument secondArg) {
        player.showDialog(createInGameRegisterDialog(type, secondArg));
    }

    public static Dialog createPreJoinLoginDialog() {
        DialogBase base = DialogBase.builder(Component.text("Login"))
            .inputs(List.of(createPasswordInput()))
            .canCloseWithEscape(false)
            .afterAction(DialogBase.DialogAfterAction.WAIT_FOR_RESPONSE)
            .build();

        ActionButton loginButton = ActionButton.builder(Component.text("Login"))
            .action(DialogAction.customClick(PaperDialogActionKeys.PRE_JOIN_LOGIN_SUBMIT, null))
            .build();
        ActionButton cancelButton = ActionButton.builder(Component.text("Cancel"))
            .action(DialogAction.customClick(PaperDialogActionKeys.PRE_JOIN_LOGIN_CANCEL, null))
            .build();

        return Dialog.create(factory -> factory.empty()
            .base(base)
            .type(DialogType.multiAction(List.of(loginButton, cancelButton)).build()));
    }

    public static Dialog createPreJoinRegisterDialog(RegistrationType type, RegisterSecondaryArgument secondArg) {
        DialogBase base = DialogBase.builder(Component.text("Register"))
            .inputs(createRegisterInputs(type, secondArg))
            .canCloseWithEscape(false)
            .afterAction(DialogBase.DialogAfterAction.WAIT_FOR_RESPONSE)
            .build();

        ActionButton registerButton = ActionButton.builder(Component.text("Register"))
            .action(DialogAction.customClick(PaperDialogActionKeys.PRE_JOIN_REGISTER_SUBMIT, null))
            .build();
        ActionButton cancelButton = ActionButton.builder(Component.text("Cancel"))
            .action(DialogAction.customClick(PaperDialogActionKeys.PRE_JOIN_REGISTER_CANCEL, null))
            .build();

        return Dialog.create(factory -> factory.empty()
            .base(base)
            .type(DialogType.multiAction(List.of(registerButton, cancelButton)).build()));
    }

    private static Dialog createInGameLoginDialog() {
        DialogBase base = DialogBase.builder(Component.text("Login"))
            .inputs(List.of(createPasswordInput()))
            .afterAction(DialogBase.DialogAfterAction.CLOSE)
            .build();

        ActionButton loginButton = ActionButton.builder(Component.text("Login"))
            .action(DialogAction.commandTemplate("login $(password)"))
            .build();

        return Dialog.create(factory -> factory.empty()
            .base(base)
            .type(DialogType.multiAction(List.of(loginButton)).build()));
    }

    private static Dialog createInGameRegisterDialog(RegistrationType type, RegisterSecondaryArgument secondArg) {
        String template = createRegisterTemplate(type, secondArg);
        DialogBase base = DialogBase.builder(Component.text("Register"))
            .inputs(createRegisterInputs(type, secondArg))
            .afterAction(DialogBase.DialogAfterAction.CLOSE)
            .build();

        ActionButton registerButton = ActionButton.builder(Component.text("Register"))
            .action(DialogAction.commandTemplate(template))
            .build();

        return Dialog.create(factory -> factory.empty()
            .base(base)
            .type(DialogType.multiAction(List.of(registerButton)).build()));
    }

    private static DialogInput createPasswordInput() {
        return DialogInput.text("password", Component.text("Password"))
            .maxLength(100)
            .build();
    }

    private static List<DialogInput> createRegisterInputs(RegistrationType type, RegisterSecondaryArgument secondArg) {
        List<DialogInput> inputs = new ArrayList<>();
        if (type == RegistrationType.EMAIL) {
            inputs.add(DialogInput.text("email", Component.text("Email")).maxLength(100).build());
            if (secondArg == RegisterSecondaryArgument.CONFIRMATION) {
                inputs.add(DialogInput.text("confirm", Component.text("Confirm Email")).maxLength(100).build());
            }
        } else {
            inputs.add(createPasswordInput());
            if (secondArg == RegisterSecondaryArgument.CONFIRMATION) {
                inputs.add(DialogInput.text("confirm", Component.text("Confirm Password")).maxLength(100).build());
            } else if (secondArg == RegisterSecondaryArgument.EMAIL_MANDATORY
                || secondArg == RegisterSecondaryArgument.EMAIL_OPTIONAL) {
                inputs.add(DialogInput.text("email", Component.text("Email")).maxLength(100).build());
            }
        }
        return inputs;
    }

    private static String createRegisterTemplate(RegistrationType type, RegisterSecondaryArgument secondArg) {
        if (type == RegistrationType.EMAIL) {
            if (secondArg == RegisterSecondaryArgument.CONFIRMATION) {
                return "register $(email) $(confirm)";
            }
            return "register $(email)";
        }

        if (secondArg == RegisterSecondaryArgument.CONFIRMATION) {
            return "register $(password) $(confirm)";
        }
        if (secondArg == RegisterSecondaryArgument.EMAIL_MANDATORY
            || secondArg == RegisterSecondaryArgument.EMAIL_OPTIONAL) {
            return "register $(password) $(email)";
        }
        return "register $(password)";
    }
}
