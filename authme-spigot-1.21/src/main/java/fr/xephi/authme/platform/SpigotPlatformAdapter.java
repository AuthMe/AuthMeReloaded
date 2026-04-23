package fr.xephi.authme.platform;

import fr.xephi.authme.listener.PlayerSignOpenListener;
import fr.xephi.authme.process.register.RegisterSecondaryArgument;
import fr.xephi.authme.process.register.RegistrationType;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

import java.util.Collections;
import java.util.List;

/**
 * Platform adapter implementation for Spigot 1.20.x and 1.21.x.
 * Dialog UI is supported on Spigot 1.21.6+ (BungeeCord dialog API).
 * On older server versions in this range the dialog feature is gracefully disabled.
 */
public class SpigotPlatformAdapter extends AbstractSpigotPlatformAdapter {

    /**
     * True when the BungeeCord dialog API is present on the running server (Spigot 1.21.6+).
     * Checked once at class-load time via Class.forName so that SpigotDialogHelper — which
     * directly references those classes — is never loaded on servers that don't have them.
     */
    private static final boolean DIALOG_AVAILABLE;

    static {
        boolean available = false;
        try {
            Class.forName("net.md_5.bungee.api.dialog.MultiActionDialog");
            available = true;
        } catch (ClassNotFoundException ignored) {
            // BungeeCord dialog API absent (Spigot < 1.21.6)
        }
        DIALOG_AVAILABLE = available;
    }

    @Override
    public String getPlatformName() {
        return "spigot-1.21";
    }

    @Override
    public String getCompatibilityError() {
        return getCompatibilityError("This AuthMe Spigot 1.21 build requires the Spigot 1.20+ API.",
            "org.spigotmc.event.player.PlayerSpawnLocationEvent",
            "org.bukkit.event.player.PlayerSignOpenEvent");
    }

    @Override
    public List<Class<? extends Listener>> getListeners() {
        return EventRegistrationAdapter.combineListeners(
            super.getListeners(),
            Collections.singletonList(PlayerSignOpenListener.class));
    }

    /**
     * Spigot 1.20.6+ exposes {@link Player#getRespawnLocation()}, which preserves the server's actual respawn target
     * instead of limiting us to the legacy bed-only API.
     */
    @Override
    public Location getPlayerRespawnLocation(Player player) {
        return player.getRespawnLocation();
    }

    @Override
    public boolean isDialogSupported() {
        return DIALOG_AVAILABLE;
    }

    @Override
    public void showLoginDialog(Player player) {
        SpigotDialogHelper.showLoginDialog(player);
    }

    @Override
    public void showRegisterDialog(Player player, RegistrationType type, RegisterSecondaryArgument secondArg) {
        SpigotDialogHelper.showRegisterDialog(player, type, secondArg);
    }
}
