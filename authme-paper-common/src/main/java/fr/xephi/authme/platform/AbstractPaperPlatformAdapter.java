package fr.xephi.authme.platform;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.command.CommandDescription;
import fr.xephi.authme.command.CommandHandler;
import fr.xephi.authme.process.register.RegisterSecondaryArgument;
import fr.xephi.authme.process.register.RegistrationType;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Shared platform adapter behavior for Paper-derived servers.
 */
public abstract class AbstractPaperPlatformAdapter extends AbstractSpigotPlatformAdapter {

    private static final boolean DIALOG_AVAILABLE = hasDialogApi();

    private final Map<UUID, CompletableFuture<Boolean>> pendingTeleports = new ConcurrentHashMap<>();

    @Override
    public void teleportPlayer(Player player, Location location) {
        UUID playerId = player.getUniqueId();
        CompletableFuture<Boolean> previous = pendingTeleports.get(playerId);
        CompletableFuture<Boolean> teleport;
        if (previous == null) {
            teleport = player.teleportAsync(location);
        } else {
            teleport = new CompletableFuture<>();
            previous.whenComplete((ignoredResult, ignoredError) -> {
                ScheduledTask task = player.getScheduler().run(JavaPlugin.getPlugin(AuthMe.class),
                    ignored -> completeWith(teleport, player.teleportAsync(location)),
                    () -> teleport.complete(false));
                if (task == null) {
                    teleport.complete(false);
                }
            });
        }
        pendingTeleports.put(playerId, teleport);
        teleport.whenComplete((ignoredResult, ignoredError) -> pendingTeleports.remove(playerId, teleport));
    }

    private static void completeWith(CompletableFuture<Boolean> target, CompletableFuture<Boolean> source) {
        source.whenComplete((result, error) -> {
            if (error == null) {
                target.complete(result);
            } else {
                target.completeExceptionally(error);
            }
        });
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
    public void closeDialog(Player player) {
        if (DIALOG_AVAILABLE) {
            PaperDialogHelper.closeDialog(player);
        }
    }

    @Override
    public void registerCommands(AuthMe plugin, CommandHandler commandHandler, Collection<CommandDescription> commands) {
        new PaperBrigadierCommandRegistrar(commandHandler::processCommand).registerCommands(plugin, commands);
    }

    @Override
    public List<Class<? extends Listener>> getListeners() {
        return EventRegistrationAdapter.getCommonListeners();
    }

    @Override
    public boolean isProxyForwardingEnabled() {
        if (super.isProxyForwardingEnabled()) {
            return true;
        }
        return isPaperVelocityForwardingEnabled();
    }

    private static boolean hasDialogApi() {
        try {
            Class.forName("io.papermc.paper.dialog.Dialog", false, AbstractPaperPlatformAdapter.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException ignored) {
            return false;
        }
    }

    private static boolean isPaperVelocityForwardingEnabled() {
        try {
            // io.papermc.paper.configuration.GlobalConfiguration is Paper 1.18+
            Class<?> cfg = Class.forName(
                "io.papermc.paper.configuration.GlobalConfiguration",
                false, AbstractPaperPlatformAdapter.class.getClassLoader());
            Object instance = cfg.getMethod("get").invoke(null);
            Object proxies = cfg.getField("proxies").get(instance);
            Object velocity = proxies.getClass().getField("velocity").get(proxies);
            Object enabled = velocity.getClass().getField("enabled").get(velocity);
            return Boolean.TRUE.equals(enabled);
        } catch (Exception ignored) {
            return false;
        }
    }
}
