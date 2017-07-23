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
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Persistence handler for LimboPlayer objects by distributing the objects to store
 * in various segments (buckets) based on the start of the player's UUID.
 */
class DistributedFilesPersistenceHandler implements LimboPersistenceHandler {

    private static final Type LIMBO_MAP_TYPE = new TypeToken<Map<String, LimboPlayer>>(){}.getType();

    private final File cacheFolder;
    private final Gson gson;
    private final SegmentNameBuilder segmentNameBuilder;

    @Inject
    DistributedFilesPersistenceHandler(@DataFolder File dataFolder, BukkitService bukkitService, Settings settings) {
        cacheFolder = new File(dataFolder, "playerdata");
        FileUtils.createDirectory(cacheFolder);

        gson = new GsonBuilder()
            .registerTypeAdapter(LimboPlayer.class, new LimboPlayerSerializer())
            .registerTypeAdapter(LimboPlayer.class, new LimboPlayerDeserializer(bukkitService))
            .setPrettyPrinting()
            .create();

        segmentNameBuilder = new SegmentNameBuilder(settings.getProperty(LimboSettings.DISTRIBUTION_SIZE));

        convertOldDataToCurrentSegmentScheme();
        deleteEmptyFiles();
    }

    @Override
    public LimboPlayer getLimboPlayer(Player player) {
        String uuid = PlayerUtils.getUuidOrName(player);
        File file = getPlayerSegmentFile(uuid);
        Map<String, LimboPlayer> entries = readLimboPlayers(file);
        return entries == null ? null : entries.get(uuid);
    }

    @Override
    public void saveLimboPlayer(Player player, LimboPlayer limbo) {
        String uuid = PlayerUtils.getUuidOrName(player);
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

        entries.put(PlayerUtils.getUuidOrName(player), limbo);
        saveEntries(entries, file);
    }

    @Override
    public void removeLimboPlayer(Player player) {
        String uuid = PlayerUtils.getUuidOrName(player);
        File file = getPlayerSegmentFile(uuid);
        if (file.exists()) {
            Map<String, LimboPlayer> entries = readLimboPlayers(file);
            if (entries != null && entries.remove(PlayerUtils.getUuidOrName(player)) != null) {
                saveEntries(entries, file);
            }
        }
    }

    @Override
    public LimboPersistenceType getType() {
        return LimboPersistenceType.DISTRIBUTED_FILES;
    }

    private void saveEntries(Map<String, LimboPlayer> entries, File file) {
        try (FileWriter fw = new FileWriter(file)) {
            gson.toJson(entries, fw);
        } catch (Exception e) {
            ConsoleLogger.logException("Could not write to '" + file + "':", e);
        }
    }

    private Map<String, LimboPlayer> readLimboPlayers(File file) {
        if (!file.exists()) {
            return null;
        }

        try {
            return gson.fromJson(Files.asCharSource(file, StandardCharsets.UTF_8).read(), LIMBO_MAP_TYPE);
        } catch (Exception e) {
            ConsoleLogger.logException("Failed reading '" + file + "':", e);
        }
        return null;
    }

    private File getPlayerSegmentFile(String uuid) {
        String segment = segmentNameBuilder.createSegmentName(uuid);
        return getSegmentFile(segment);
    }

    private File getSegmentFile(String segmentId) {
        return new File(cacheFolder, segmentId + "-limbo.json");
    }

    /**
     * Loads segment files in the cache folder that don't correspond to the current segmenting scheme
     * and migrates the data into files of the current segments. This allows a player to change the
     * segment size without any loss of data.
     */
    private void convertOldDataToCurrentSegmentScheme() {
        String currentPrefix = segmentNameBuilder.getPrefix();
        File[] files = listFiles(cacheFolder);
        Map<String, LimboPlayer> allLimboPlayers = new HashMap<>();
        List<File> migratedFiles = new ArrayList<>();

        for (File file : files) {
            if (isLimboJsonFile(file) && !file.getName().startsWith(currentPrefix)) {
                Map<String, LimboPlayer> data = readLimboPlayers(file);
                if (data != null) {
                    allLimboPlayers.putAll(data);
                    migratedFiles.add(file);
                }
            }
        }

        if (!allLimboPlayers.isEmpty()) {
            saveToNewSegments(allLimboPlayers);
            migratedFiles.forEach(FileUtils::delete);
        }
    }

    /**
     * Saves the LimboPlayer data read from old segmenting schemes into the current segmenting scheme.
     *
     * @param limbosFromOldSegments the limbo players to store into the current segment files
     */
    private void saveToNewSegments(Map<String, LimboPlayer> limbosFromOldSegments) {
        Map<String, Map<String, LimboPlayer>> limboBySegment = groupBySegment(limbosFromOldSegments);

        ConsoleLogger.info("Saving " + limbosFromOldSegments.size() + " LimboPlayers from old segments into "
            + limboBySegment.size() + " current segments");
        for (Map.Entry<String, Map<String, LimboPlayer>> entry : limboBySegment.entrySet()) {
            File file = getSegmentFile(entry.getKey());
            Map<String, LimboPlayer> limbosToSave = Optional.ofNullable(readLimboPlayers(file))
                .orElseGet(HashMap::new);
            limbosToSave.putAll(entry.getValue());
            saveEntries(limbosToSave, file);
        }
    }

    /**
     * Converts a Map of UUID to LimboPlayers to a 2-dimensional Map of LimboPlayers by segment ID and UUID.
     * {@code Map(uuid -> LimboPlayer) to Map(segment -> Map(uuid -> LimboPlayer))}
     *
     * @param readLimboPlayers the limbo players to order by segment
     * @return limbo players ordered by segment ID and associated player UUID
     */
    private Map<String, Map<String, LimboPlayer>> groupBySegment(Map<String, LimboPlayer> readLimboPlayers) {
        Map<String, Map<String, LimboPlayer>> limboBySegment = new HashMap<>();
        for (Map.Entry<String, LimboPlayer> entry : readLimboPlayers.entrySet()) {
            String segmentId = segmentNameBuilder.createSegmentName(entry.getKey());
            limboBySegment.computeIfAbsent(segmentId, s -> new HashMap<>())
                .put(entry.getKey(), entry.getValue());
        }
        return limboBySegment;
    }

    /**
     * Deletes segment files that are empty.
     */
    private void deleteEmptyFiles() {
        File[] files = listFiles(cacheFolder);

        long deletedFiles = Arrays.stream(files)
            // typically the size is 2 because there's an empty JSON map: {}
            .filter(f -> isLimboJsonFile(f) && f.length() < 3)
            .peek(FileUtils::delete)
            .count();
        ConsoleLogger.debug("Limbo: Deleted {0} empty segment files", deletedFiles);
    }

    /**
     * @param file the file to check
     * @return true if it is a segment file storing Limbo data, false otherwise
     */
    private static boolean isLimboJsonFile(File file) {
        String name = file.getName();
        return name.startsWith("seg") && name.endsWith("-limbo.json");
    }

    private static File[] listFiles(File folder) {
        File[] files = folder.listFiles();
        if (files == null) {
            ConsoleLogger.warning("Could not get files of '" + folder + "'");
            return new File[0];
        }
        return files;
    }
}
