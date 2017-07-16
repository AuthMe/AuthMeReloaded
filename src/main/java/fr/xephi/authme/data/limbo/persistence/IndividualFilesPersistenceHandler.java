package fr.xephi.authme.data.limbo.persistence;

import com.google.common.io.Files;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.data.limbo.LimboPlayer;
import fr.xephi.authme.initialization.DataFolder;
import fr.xephi.authme.service.BukkitService;
import fr.xephi.authme.util.FileUtils;
import fr.xephi.authme.util.PlayerUtils;
import org.bukkit.entity.Player;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Saves LimboPlayer objects as JSON into individual files.
 */
class IndividualFilesPersistenceHandler implements LimboPersistenceHandler {

    private final Gson gson;
    private final File cacheDir;

    @Inject
    IndividualFilesPersistenceHandler(@DataFolder File dataFolder, BukkitService bukkitService) {
        cacheDir = new File(dataFolder, "playerdata");
        if (!cacheDir.exists() && !cacheDir.isDirectory() && !cacheDir.mkdir()) {
            ConsoleLogger.warning("Failed to create playerdata directory '" + cacheDir + "'");
        }
        gson = new GsonBuilder()
            .registerTypeAdapter(LimboPlayer.class, new LimboPlayerSerializer())
            .registerTypeAdapter(LimboPlayer.class, new LimboPlayerDeserializer(bukkitService))
            .setPrettyPrinting()
            .create();
    }

    @Override
    public LimboPlayer getLimboPlayer(Player player) {
        String id = PlayerUtils.getUuidOrName(player);
        File file = new File(cacheDir, id + File.separator + "data.json");
        if (!file.exists()) {
            return null;
        }

        try {
            String str = Files.asCharSource(file, StandardCharsets.UTF_8).read();
            return gson.fromJson(str, LimboPlayer.class);
        } catch (IOException e) {
            ConsoleLogger.logException("Could not read player data on disk for '" + player.getName() + "'", e);
            return null;
        }
    }

    @Override
    public void saveLimboPlayer(Player player, LimboPlayer limboPlayer) {
        String id = PlayerUtils.getUuidOrName(player);
        try {
            File file = new File(cacheDir, id + File.separator + "data.json");
            Files.createParentDirs(file);
            Files.touch(file);
            Files.write(gson.toJson(limboPlayer), file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            ConsoleLogger.logException("Failed to write " + player.getName() + " data:", e);
        }
    }

    /**
     * Removes the LimboPlayer. This will delete the
     * "playerdata/&lt;uuid or name&gt;/" folder from disk.
     *
     * @param player player to remove
     */
    @Override
    public void removeLimboPlayer(Player player) {
        String id = PlayerUtils.getUuidOrName(player);
        File file = new File(cacheDir, id);
        if (file.exists()) {
            FileUtils.purgeDirectory(file);
            FileUtils.delete(file);
        }
    }

    @Override
    public LimboPersistenceType getType() {
        return LimboPersistenceType.INDIVIDUAL_FILES;
    }
}
