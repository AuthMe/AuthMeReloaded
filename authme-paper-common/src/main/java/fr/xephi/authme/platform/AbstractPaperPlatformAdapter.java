package fr.xephi.authme.platform;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.command.CommandDescription;
import fr.xephi.authme.command.CommandHandler;
import fr.xephi.authme.process.register.RegisterSecondaryArgument;
import fr.xephi.authme.process.register.RegistrationType;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerKickEvent;

import java.util.Collection;
import java.util.List;

/**
 * Shared platform adapter behavior for Paper-derived servers.
 */
public abstract class AbstractPaperPlatformAdapter extends AbstractSpigotPlatformAdapter {

    private static final boolean DIALOG_AVAILABLE = hasDialogApi();

    @Override
    public void teleportPlayer(Player player, Location location) {
        player.teleportAsync(location);
    }

    @Override
    public Location getPlayerRespawnLocation(Player player) {
        return player.getRespawnLocation();
    }

    @Override
    public String getKickReason(PlayerKickEvent event) {
        return PlainTextComponentSerializer.plainText().serialize(event.reason());
    }

    @Override
    public boolean isDialogSupported() {
        return DIALOG_AVAILABLE;
    }

    @Override
    public void showLoginDialog(Player player, DialogWindowSpec dialog) {
        PaperDialogHelper.showLoginDialog(player, dialog);
    }

    @Override
    public void showTotpDialog(Player player, DialogWindowSpec dialog) {
        PaperDialogHelper.showTotpDialog(player, dialog);
    }

    @Override
    public void showRegisterDialog(Player player, RegistrationType type,
                                   RegisterSecondaryArgument secondArg, DialogWindowSpec dialog) {
        PaperDialogHelper.showRegisterDialog(player, type, secondArg, dialog);
    }

    @Override
    public void registerCommands(AuthMe plugin, CommandHandler commandHandler, Collection<CommandDescription> commands) {
        new PaperBrigadierCommandRegistrar(commandHandler::processCommand).registerCommands(plugin, commands);
    }

    @Override
    public List<Class<? extends Listener>> getListeners() {
        return EventRegistrationAdapter.getCommonListeners();
    }

    private static boolean hasDialogApi() {
        try {
            Class.forName("io.papermc.paper.dialog.Dialog", false, AbstractPaperPlatformAdapter.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException ignored) {
            return false;
        }
    }
}
