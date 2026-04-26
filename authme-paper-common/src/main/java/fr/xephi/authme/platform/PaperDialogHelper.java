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
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Encapsulates the Paper-derived dialog API calls used by Paper and Folia.
 */
public final class PaperDialogHelper {

    private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.legacySection();

    private PaperDialogHelper() {
    }

    static void closeDialog(Player player) {
        player.closeDialog();
    }

    static void showLoginDialog(Player player, DialogWindowSpec dialog) {
        player.showDialog(createInGameCommandDialog(dialog, "login $(password)"));
    }

    static void showTotpDialog(Player player, DialogWindowSpec dialog) {
        player.showDialog(createInGameCommandDialog(dialog, "2fa code $(code)"));
    }

    static void showRegisterDialog(Player player, RegistrationType type, RegisterSecondaryArgument secondArg,
                                   DialogWindowSpec dialog) {
        player.showDialog(createInGameCommandDialog(dialog, createRegisterTemplate(type, secondArg)));
    }

    public static Dialog createPreJoinLoginDialog(DialogWindowSpec dialog) {
        DialogBase base = DialogBase.builder(legacyComponent(dialog.title()))
            .inputs(createInputs(dialog))
            .canCloseWithEscape(dialog.canCloseWithEscape())
            .afterAction(DialogBase.DialogAfterAction.WAIT_FOR_RESPONSE)
            .build();

        ActionButton loginButton = ActionButton.builder(legacyComponent(dialog.primaryButtonLabel()))
            .action(DialogAction.customClick(PaperDialogActionKeys.PRE_JOIN_LOGIN_SUBMIT, null))
            .build();

        return Dialog.create(factory -> factory.empty()
            .base(base)
            .type(DialogType.multiAction(createPreJoinButtons(dialog,
                loginButton,
                PaperDialogActionKeys.PRE_JOIN_LOGIN_CANCEL)).build()));
    }

    public static Dialog createPreJoinRegisterDialog(DialogWindowSpec dialog) {
        DialogBase base = DialogBase.builder(legacyComponent(dialog.title()))
            .inputs(createInputs(dialog))
            .canCloseWithEscape(dialog.canCloseWithEscape())
            .afterAction(DialogBase.DialogAfterAction.WAIT_FOR_RESPONSE)
            .build();

        ActionButton registerButton = ActionButton.builder(legacyComponent(dialog.primaryButtonLabel()))
            .action(DialogAction.customClick(PaperDialogActionKeys.PRE_JOIN_REGISTER_SUBMIT, null))
            .build();

        return Dialog.create(factory -> factory.empty()
            .base(base)
            .type(DialogType.multiAction(createPreJoinButtons(dialog,
                registerButton,
                PaperDialogActionKeys.PRE_JOIN_REGISTER_CANCEL)).build()));
    }

    private static Dialog createInGameCommandDialog(DialogWindowSpec dialog, String commandTemplate) {
        DialogBase base = DialogBase.builder(legacyComponent(dialog.title()))
            .inputs(createInputs(dialog))
            .afterAction(DialogBase.DialogAfterAction.CLOSE)
            .build();

        ActionButton primaryButton = ActionButton.builder(legacyComponent(dialog.primaryButtonLabel()))
            .action(DialogAction.commandTemplate(commandTemplate))
            .build();

        return Dialog.create(factory -> factory.empty()
            .base(base)
            .type(DialogType.multiAction(List.of(primaryButton)).build()));
    }

    private static List<ActionButton> createPreJoinButtons(DialogWindowSpec dialog,
                                                           ActionButton primaryButton,
                                                           net.kyori.adventure.key.Key cancelActionKey) {
        List<ActionButton> buttons = new ArrayList<>();
        buttons.add(primaryButton);
        if (dialog.showSecondaryButton()) {
            buttons.add(ActionButton.builder(legacyComponent(dialog.secondaryButtonLabel()))
                .action(DialogAction.customClick(cancelActionKey, null))
                .build());
        }
        return buttons;
    }

    private static List<DialogInput> createInputs(DialogWindowSpec dialog) {
        List<DialogInput> inputs = new ArrayList<>();
        for (DialogInputSpec input : dialog.inputs()) {
            inputs.add(DialogInput.text(input.id(), legacyComponent(input.label())).maxLength(input.maxLength()).build());
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

    private static Component legacyComponent(String text) {
        return LEGACY_SERIALIZER.deserialize(text);
    }
}
