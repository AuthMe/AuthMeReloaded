package fr.xephi.authme.settings;

import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.initialization.DataFolder;
import fr.xephi.authme.initialization.Reloadable;
import fr.xephi.authme.output.ConsoleLoggerFactory;
import fr.xephi.authme.settings.properties.HooksSettings;

import javax.inject.Inject;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AllowedProxyUuidLoader implements Reloadable {

    private List<UUID> allowed_proxies;
    private final File pluginFolder;
    private final Settings settings;
    private ConsoleLogger logger;

    @Inject
    AllowedProxyUuidLoader(@DataFolder File pluginFolder, Settings settings){
        this.pluginFolder = pluginFolder;
        this.settings = settings;
        this.logger = ConsoleLoggerFactory.get(AllowedProxyUuidLoader.class);
        reload();
    }

    @Override
    public void reload() {
        allowed_proxies = new ArrayList<>();
        if (settings.getProperty(HooksSettings.VELOCITY)) {
            File dir = new File(String.valueOf(pluginFolder), "allowed-proxy");

            if (!dir.exists()){
                dir.mkdir();
            }
            String[] files = dir.list();

            for (String pathname : files) {
                try {
                    allowed_proxies.add(UUID.fromString(pathname.replaceFirst("\\.txt", "")));
                } catch (IllegalArgumentException e) {
                    logger.warning("Invalid UUID file in allowed-uuid folder: " + pathname.replaceFirst("\\.txt", "") + ", please delete it.");
                }
            }
        }
    }

    public boolean isAllowedProxy(UUID uuid) {
        return allowed_proxies.contains(uuid);
    }
}
