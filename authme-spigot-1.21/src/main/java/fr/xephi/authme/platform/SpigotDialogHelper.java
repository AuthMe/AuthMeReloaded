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

    static void showLoginDialog(Player player) {
        List<DialogInput> inputs = new ArrayList<>();
        inputs.add(new TextInput("password", new TextComponent("Password")).maxLength(100));

        DialogBase base = new DialogBase(new TextComponent("Login"))
            .inputs(inputs)
            .afterAction(DialogBase.AfterAction.CLOSE);

        player.showDialog(new MultiActionDialog(base,
            new ActionButton(new TextComponent("Login"), new RunCommandAction("login $(password)"))));
    }

    static void showRegisterDialog(Player player, RegistrationType type, RegisterSecondaryArgument secondArg) {
        List<DialogInput> inputs = new ArrayList<>();
        String template;

        if (type == RegistrationType.EMAIL) {
            inputs.add(new TextInput("email", new TextComponent("Email")).maxLength(100));
            if (secondArg == RegisterSecondaryArgument.CONFIRMATION) {
                inputs.add(new TextInput("confirm", new TextComponent("Confirm Email")).maxLength(100));
                template = "register $(email) $(confirm)";
            } else {
                template = "register $(email)";
            }
        } else {
            inputs.add(new TextInput("password", new TextComponent("Password")).maxLength(100));
            if (secondArg == RegisterSecondaryArgument.CONFIRMATION) {
                inputs.add(new TextInput("confirm", new TextComponent("Confirm Password")).maxLength(100));
                template = "register $(password) $(confirm)";
            } else if (secondArg == RegisterSecondaryArgument.EMAIL_MANDATORY) {
                inputs.add(new TextInput("email", new TextComponent("Email")).maxLength(100));
                template = "register $(password) $(email)";
            } else {
                template = "register $(password)";
            }
        }

        DialogBase base = new DialogBase(new TextComponent("Register"))
            .inputs(inputs)
            .afterAction(DialogBase.AfterAction.CLOSE);

        player.showDialog(new MultiActionDialog(base,
            new ActionButton(new TextComponent("Register"), new RunCommandAction(template))));
    }
}
