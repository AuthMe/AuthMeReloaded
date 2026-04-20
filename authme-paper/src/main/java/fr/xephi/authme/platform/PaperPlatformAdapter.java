package fr.xephi.authme.platform;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.command.CommandDescription;
import fr.xephi.authme.command.CommandHandler;
import fr.xephi.authme.listener.PaperChatListener;
import fr.xephi.authme.listener.PlayerOpenSignListener;
import fr.xephi.authme.listener.PaperPlayerSpawnLocationListener;
import fr.xephi.authme.process.register.RegisterSecondaryArgument;
import fr.xephi.authme.process.register.RegistrationType;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerKickEvent;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Platform adapter implementation for PaperMC 1.21.11+.
 * Uses Paper's async teleport API for non-blocking player teleportation,
 * the Adventure API for text components, and the Paper dialog API for
 * graphical login/register dialogs (available since 1.21.11).
 * On older Paper versions the dialog feature is gracefully disabled.
 */
public class PaperPlatformAdapter extends AbstractSpigotPlatformAdapter {

    /**
     * True when the Paper dialog API is present on the running server (Paper 1.21.11+).
     * Checked once at class-load time via Class.forName so that PaperDialogHelper — which
     * directly references those classes — is never loaded on servers that don't have them.
     */
    private static final boolean DIALOG_AVAILABLE;

    static {
        boolean available = false;
        try {
            // Use initialize=false so we only check for class existence without triggering
            // the Dialog static initializer (which requires a running Paper server).
            Class.forName("io.papermc.paper.dialog.Dialog", false, PaperPlatformAdapter.class.getClassLoader());
            available = true;
        } catch (ClassNotFoundException ignored) {
            // Paper dialog API absent (Paper < 1.21.11)
        }
        DIALOG_AVAILABLE = available;
    }

    @Override
    public void teleportPlayer(Player player, Location location) {
        player.teleportAsync(location);
    }

    @Override
    public String getPlatformName() {
        return "paper-1.21";
    }

    @Override
    public String getCompatibilityError() {
        return getCompatibilityError("This AuthMe Paper build requires the Paper 1.21.11+ API.",
            "io.papermc.paper.event.player.AsyncChatEvent",
            "io.papermc.paper.event.player.AsyncPlayerSpawnLocationEvent",
            "io.papermc.paper.event.player.PlayerOpenSignEvent");
    }

    @Override
    public List<Class<? extends Listener>> getAdditionalListeners() {
        return Arrays.asList(PaperChatListener.class, PaperPlayerSpawnLocationListener.class, PlayerOpenSignListener.class);
    }

    /**
     * Paper 1.20.6+ exposes {@link Player#getRespawnLocation()}, which preserves anchor- and world-based respawn rules
     * in addition to ordinary bed spawns.
     */
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
    public void showLoginDialog(Player player) {
        PaperDialogHelper.showLoginDialog(player);
    }

    @Override
    public void showRegisterDialog(Player player, RegistrationType type, RegisterSecondaryArgument secondArg) {
        PaperDialogHelper.showRegisterDialog(player, type, secondArg);
    }

    @Override
    public void registerCommands(AuthMe plugin, CommandHandler commandHandler, Collection<CommandDescription> commands) {
        new PaperBrigadierCommandRegistrar(commandHandler::processCommand).registerCommands(plugin, commands);
    }
}
