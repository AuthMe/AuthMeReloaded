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
 * Encapsulates the Paper dialog API calls for Paper 1.21.11+.
 *
 * <p>This class is intentionally isolated so that it is only loaded by the JVM when dialog
 * functionality is actually invoked. On servers that do not ship the Paper dialog API
 * (Paper &lt; 1.21.11), this class is never loaded, preventing {@link NoClassDefFoundError}.</p>
 */
final class PaperDialogHelper {

    private PaperDialogHelper() {
    }

    static void showLoginDialog(Player player) {
        DialogInput passwordInput = DialogInput.text("password", Component.text("Password"))
            .maxLength(100)
            .build();

        DialogBase base = DialogBase.builder(Component.text("Login"))
            .inputs(List.of(passwordInput))
            .afterAction(DialogBase.DialogAfterAction.CLOSE)
            .build();

        ActionButton loginButton = ActionButton.builder(Component.text("Login"))
            .action(DialogAction.commandTemplate("login $(password)"))
            .build();

        Dialog dialog = Dialog.create(factory -> factory.empty()
            .base(base)
            .type(DialogType.multiAction(List.of(loginButton)).build()));

        player.showDialog(dialog);
    }

    static void showRegisterDialog(Player player, RegistrationType type, RegisterSecondaryArgument secondArg) {
        List<DialogInput> inputs = new ArrayList<>();
        String template;

        if (type == RegistrationType.EMAIL) {
            inputs.add(DialogInput.text("email", Component.text("Email")).maxLength(100).build());
            if (secondArg == RegisterSecondaryArgument.CONFIRMATION) {
                inputs.add(DialogInput.text("confirm", Component.text("Confirm Email")).maxLength(100).build());
                template = "register $(email) $(confirm)";
            } else {
                template = "register $(email)";
            }
        } else {
            inputs.add(DialogInput.text("password", Component.text("Password")).maxLength(100).build());
            if (secondArg == RegisterSecondaryArgument.CONFIRMATION) {
                inputs.add(DialogInput.text("confirm", Component.text("Confirm Password")).maxLength(100).build());
                template = "register $(password) $(confirm)";
            } else if (secondArg == RegisterSecondaryArgument.EMAIL_MANDATORY) {
                inputs.add(DialogInput.text("email", Component.text("Email")).maxLength(100).build());
                template = "register $(password) $(email)";
            } else {
                template = "register $(password)";
            }
        }

        DialogBase base = DialogBase.builder(Component.text("Register"))
            .inputs(inputs)
            .afterAction(DialogBase.DialogAfterAction.CLOSE)
            .build();

        ActionButton registerButton = ActionButton.builder(Component.text("Register"))
            .action(DialogAction.commandTemplate(template))
            .build();

        Dialog dialog = Dialog.create(factory -> factory.empty()
            .base(base)
            .type(DialogType.multiAction(List.of(registerButton)).build()));

        player.showDialog(dialog);
    }
}
