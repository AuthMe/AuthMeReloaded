package fr.xephi.authme.data.limbo.persistence;

import com.google.common.reflect.TypeToken;
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
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Saves all LimboPlayers in one JSON file and keeps the entries in memory.
 */
class SingleFilePersistenceHandler implements LimboPersistenceHandler {

    private final File cacheFile;
    private final Gson gson;
    private Map<String, LimboPlayer> entries;

    @Inject
    SingleFilePersistenceHandler(@DataFolder File dataFolder, BukkitService bukkitService) {
        cacheFile = new File(dataFolder, "limbo.json");
        if (!cacheFile.exists()) {
            FileUtils.create(cacheFile);
        }

        gson = new GsonBuilder()
            .registerTypeAdapter(LimboPlayer.class, new LimboPlayerSerializer())
            .registerTypeAdapter(LimboPlayer.class, new LimboPlayerDeserializer(bukkitService))
            .setPrettyPrinting()
            .create();

        Type type = new TypeToken<ConcurrentMap<String, LimboPlayer>>(){}.getType();
        try (FileReader fr = new FileReader(cacheFile)) {
            entries = gson.fromJson(fr, type);
        } catch (IOException e) {
            ConsoleLogger.logException("Failed to read from '" + cacheFile + "':", e);
        }

        if (entries == null) {
            entries = new ConcurrentHashMap<>();
        }
    }

    @Override
    public LimboPlayer getLimboPlayer(Player player) {
        return entries.get(PlayerUtils.getUUIDorName(player));
    }

    @Override
    public void saveLimboPlayer(Player player, LimboPlayer limbo) {
        entries.put(PlayerUtils.getUUIDorName(player), limbo);
        saveEntries("adding '" + player.getName() + "'");
    }

    @Override
    public void removeLimboPlayer(Player player) {
        LimboPlayer entry = entries.remove(PlayerUtils.getUUIDorName(player));
        if (entry != null) {
            saveEntries("removing '" + player.getName() + "'");
        }
    }

    /**
     * Saves the entries to the disk.
     *
     * @param action the reason for saving (for logging purposes)
     */
    private void saveEntries(String action) {
        try (FileWriter fw = new FileWriter(cacheFile)) {
            gson.toJson(entries, fw);
        } catch (IOException e) {
            ConsoleLogger.logException("Failed saving JSON limbo cache after " + action, e);
        }
    }

    @Override
    public LimboPersistenceType getType() {
        return LimboPersistenceType.SINGLE_FILE;
    }
}
