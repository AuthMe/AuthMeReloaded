package fr.xephi.authme.platform;

import fr.xephi.authme.listener.PaperChatListener;
import fr.xephi.authme.listener.PaperDialogFlowListener;
import fr.xephi.authme.listener.PaperLoginValidationListener;
import fr.xephi.authme.listener.PaperProxyAutoLoginListener;
import fr.xephi.authme.listener.PlayerOpenSignListener;
import fr.xephi.authme.listener.PaperPlayerSpawnLocationListener;
import org.bukkit.event.Listener;

import java.util.Arrays;
import java.util.List;

/**
 * Platform adapter implementation for PaperMC 1.21.11+.
 * Uses Paper's async teleport API for non-blocking player teleportation,
 * the Adventure API for text components, and the Paper dialog API for
 * graphical login/register dialogs (available since 1.21.11).
 * On older Paper versions the dialog feature is gracefully disabled.
 */
public class PaperPlatformAdapter extends AbstractPaperPlatformAdapter {

    /**
     * Constructor.
     */
    public PaperPlatformAdapter() {
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
    public List<Class<? extends Listener>> getListeners() {
        return EventRegistrationAdapter.combineListeners(
            super.getListeners(),
            Arrays.asList(
                PaperChatListener.class,
                PaperDialogFlowListener.class,
                PaperProxyAutoLoginListener.class,
                PaperPlayerSpawnLocationListener.class,
                PaperLoginValidationListener.class,
                PlayerOpenSignListener.class));
    }
}
