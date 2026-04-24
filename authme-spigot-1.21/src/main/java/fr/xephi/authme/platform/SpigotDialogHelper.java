package fr.xephi.authme.platform;

import fr.xephi.authme.process.register.RegisterSecondaryArgument;
import fr.xephi.authme.process.register.RegistrationType;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.dialog.DialogBase;
import net.md_5.bungee.api.dialog.MultiActionDialog;
import net.md_5.bungee.api.dialog.action.ActionButton;
import net.md_5.bungee.api.dialog.action.RunCommandAction;
import net.md_5.bungee.api.dialog.input.DialogInput;
import net.md_5.bungee.api.dialog.input.TextInput;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Encapsulates the BungeeCord dialog API calls for Spigot 1.21.6+.
 *
 * <p>This class is intentionally isolated so that it is only loaded by the JVM when dialog
 * functionality is actually invoked. On servers that do not ship the BungeeCord dialog API
 * (Spigot &lt; 1.21.6), this class is never loaded, preventing {@link NoClassDefFoundError}.</p>
 */
final class SpigotDialogHelper {

    private SpigotDialogHelper() {
    }

    static void showLoginDialog(Player player, DialogWindowSpec dialog) {
        DialogBase base = new DialogBase(toTextComponent(dialog.title()))
            .inputs(createInputs(dialog))
            .afterAction(DialogBase.AfterAction.CLOSE);

        player.showDialog(new MultiActionDialog(base,
            new ActionButton(toTextComponent(dialog.primaryButtonLabel()), new RunCommandAction("login $(password)"))));
    }

    static void showTotpDialog(Player player, DialogWindowSpec dialog) {
        DialogBase base = new DialogBase(toTextComponent(dialog.title()))
            .inputs(createInputs(dialog))
            .afterAction(DialogBase.AfterAction.CLOSE);

        player.showDialog(new MultiActionDialog(base,
            new ActionButton(toTextComponent(dialog.primaryButtonLabel()), new RunCommandAction("2fa code $(code)"))));
    }

    static void showRegisterDialog(Player player, RegistrationType type, RegisterSecondaryArgument secondArg,
                                   DialogWindowSpec dialog) {
        DialogBase base = new DialogBase(toTextComponent(dialog.title()))
            .inputs(createInputs(dialog))
            .afterAction(DialogBase.AfterAction.CLOSE);

        player.showDialog(new MultiActionDialog(base,
            new ActionButton(toTextComponent(dialog.primaryButtonLabel()),
                new RunCommandAction(createRegisterTemplate(type, secondArg)))));
    }

    private static List<DialogInput> createInputs(DialogWindowSpec dialog) {
        List<DialogInput> inputs = new ArrayList<>();
        for (DialogInputSpec input : dialog.inputs()) {
            inputs.add(new TextInput(input.id(), toTextComponent(input.label())).maxLength(input.maxLength()));
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

    private static TextComponent toTextComponent(String text) {
        return new TextComponent(TextComponent.fromLegacyText(text));
    }
}
