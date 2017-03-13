package fr.xephi.authme.data.limbo.persistence;

import com.google.common.io.Files;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.data.limbo.LimboPlayer;
import fr.xephi.authme.initialization.DataFolder;
import fr.xephi.authme.service.BukkitService;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.LimboSettings;
import fr.xephi.authme.util.FileUtils;
import fr.xephi.authme.util.PlayerUtils;
import org.bukkit.entity.Player;

import javax.inject.Inject;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Persistence handler for LimboPlayer objects by distributing the objects to store
 * in various segments (buckets) based on the start of the player's UUID.
 */
class SegmentFilesPersistenceHolder implements LimboPersistenceHandler {

    private static final Type LIMBO_MAP_TYPE = new TypeToken<Map<String, LimboPlayer>>(){}.getType();

    private final File cacheFolder;
    private final Gson gson;
    private final SegmentNameBuilder segmentNameBuilder;

    @Inject
    SegmentFilesPersistenceHolder(@DataFolder File dataFolder, BukkitService bukkitService, Settings settings) {
        cacheFolder = new File(dataFolder, "playerdata");
        if (!cacheFolder.exists()) {
            // TODO ljacqu 20170313: Create FileUtils#mkdirs
            cacheFolder.mkdirs();
        }

        gson = new GsonBuilder()
            .registerTypeAdapter(LimboPlayer.class, new LimboPlayerSerializer())
            .registerTypeAdapter(LimboPlayer.class, new LimboPlayerDeserializer(bukkitService))
            .setPrettyPrinting()
            .create();

        segmentNameBuilder = new SegmentNameBuilder(settings.getProperty(LimboSettings.SEGMENT_DISTRIBUTION));

        // TODO #1125: Check for other segment files and attempt to convert?
    }

    @Override
    public LimboPlayer getLimboPlayer(Player player) {
        String uuid = PlayerUtils.getUUIDorName(player);
        File file = getPlayerSegmentFile(uuid);
        Map<String, LimboPlayer> entries = readLimboPlayers(file);
        return entries == null ? null : entries.get(uuid);
    }

    @Override
    public void saveLimboPlayer(Player player, LimboPlayer limbo) {
        String uuid = PlayerUtils.getUUIDorName(player);
        File file = getPlayerSegmentFile(uuid);

        Map<String, LimboPlayer> entries = null;
        if (file.exists()) {
            entries = readLimboPlayers(file);
        } else {
            FileUtils.create(file);
        }
        /* intentionally separate if */
        if (entries == null) {
            entries = new HashMap<>();
        }

        entries.put(PlayerUtils.getUUIDorName(player), limbo);
        saveEntries(entries, file);
    }

    @Override
    public void removeLimboPlayer(Player player) {
        String uuid = PlayerUtils.getUUIDorName(player);
        File file = getPlayerSegmentFile(uuid);
        if (file.exists()) {
            Map<String, LimboPlayer> entries = readLimboPlayers(file);
            if (entries != null && entries.remove(PlayerUtils.getUUIDorName(player)) != null) {
                saveEntries(entries, file);
            }
        }
    }

    private void saveEntries(Map<String, LimboPlayer> entries, File file) {
        if (entries.isEmpty()) {
            // TODO #1125: Probably should do a sweep of empty files on startup / shutdown, but not all the time
            FileUtils.delete(file);
        } else {
            try (FileWriter fw = new FileWriter(file)) {
                gson.toJson(entries, fw);
            } catch (IOException e) {
                ConsoleLogger.logException("Could not write to '" + file + "':", e);
            }
        }
    }

    private Map<String, LimboPlayer> readLimboPlayers(File file) {
        if (!file.exists()) {
            return null;
        }

        try {
            return gson.fromJson(Files.toString(file, StandardCharsets.UTF_8), LIMBO_MAP_TYPE);
        } catch (IOException e) {
            ConsoleLogger.logException("Failed reading '" + file + "':", e);
        }
        return null;
    }

    private File getPlayerSegmentFile(String uuid) {
        String segment = segmentNameBuilder.createSegmentName(uuid);
        return new File(cacheFolder, segment + "-limbo.json");
    }

    @Override
    public LimboPersistenceType getType() {
        return LimboPersistenceType.SINGLE_FILE;
    }
}
