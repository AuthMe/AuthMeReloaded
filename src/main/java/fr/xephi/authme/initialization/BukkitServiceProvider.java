package fr.xephi.authme.initialization;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.service.BukkitService;
import fr.xephi.authme.service.FoliaBukkitService;
import fr.xephi.authme.service.SpigotBukkitService;
import fr.xephi.authme.settings.Settings;

import javax.inject.Inject;
import javax.inject.Provider;

/**
 * Creates the AuthMe bukkit service provider.
 */
public class BukkitServiceProvider implements Provider<BukkitService> {

    @Inject
    private AuthMe authMe;
    @Inject
    private Settings settings;

    BukkitServiceProvider() {
    }

    @Override
    public BukkitService get() {
        try {
            Class.forName("io.papermc.paper.threadedregions.scheduler.AsyncScheduler");
            return new FoliaBukkitService(authMe, settings);
        } catch (ClassNotFoundException e) {
            return new SpigotBukkitService(authMe, settings);
        }
    }
}
