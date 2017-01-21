package fr.xephi.authme.settings;

import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.data.auth.PlayerCache;
import fr.xephi.authme.initialization.DataFolder;
import fr.xephi.authme.initialization.Reloadable;
import fr.xephi.authme.service.BukkitService;
import fr.xephi.authme.service.GeoIpService;
import fr.xephi.authme.util.PlayerUtils;
import fr.xephi.authme.util.lazytags.Tag;
import org.bukkit.Server;
import org.bukkit.entity.Player;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import static fr.xephi.authme.util.FileUtils.copyFileFromResource;

/**
 * Configuration for the welcome message (welcome.txt).
 */
public class WelcomeMessageConfiguration implements Reloadable {

    @DataFolder
    @Inject
    private File pluginFolder;

    @Inject
    private Server server;

    @Inject
    private GeoIpService geoIpService;

    @Inject
    private BukkitService bukkitService;

    @Inject
    private PlayerCache playerCache;

    /** List of all supported tags for the welcome message. */
    private final List<Tag> availableTags = Arrays.asList(
        new Tag("&",            () -> "\u00a7"),
        new Tag("{PLAYER}",     pl -> pl.getName()),
        new Tag("{ONLINE}",     () -> Integer.toString(bukkitService.getOnlinePlayers().size())),
        new Tag("{MAXPLAYERS}", () -> Integer.toString(server.getMaxPlayers())),
        new Tag("{IP}",         pl -> PlayerUtils.getPlayerIp(pl)),
        new Tag("{LOGINS}",     () -> Integer.toString(playerCache.getLogged())),
        new Tag("{WORLD}",      pl -> pl.getWorld().getName()),
        new Tag("{SERVER}",     () -> server.getServerName()),
        new Tag("{VERSION}",    () -> server.getBukkitVersion()),
        new Tag("{COUNTRY}",    pl -> geoIpService.getCountryName(PlayerUtils.getPlayerIp(pl))));

    /** Welcome message, by lines. */
    private List<String> welcomeMessage;
    /** Tags used in the welcome message. */
    private List<Tag> usedTags;

    @PostConstruct
    @Override
    public void reload() {
        welcomeMessage = readWelcomeFile();
        usedTags = determineUsedTags(welcomeMessage);
    }

    /**
     * Returns the welcome message for the given player.
     *
     * @param player the player for whom the welcome message should be prepared
     * @return the welcome message
     */
    public List<String> getWelcomeMessage(Player player) {
        // Note ljacqu 20170121: Using a Map might seem more natural here but we avoid doing so for performance
        // Although the performance gain here is probably minimal...
        List<TagValue> tagValues = new LinkedList<>();
        for (Tag tag : usedTags) {
            tagValues.add(new TagValue(tag.getName(), tag.getValue(player)));
        }

        List<String> adaptedMessages = new LinkedList<>();
        for (String line : welcomeMessage) {
            String adaptedLine = line;
            for (TagValue tagValue : tagValues) {
                adaptedLine = adaptedLine.replace(tagValue.tag, tagValue.value);
            }
            adaptedMessages.add(adaptedLine);
        }
        return adaptedMessages;
    }

    /**
     * @return the lines of the welcome message file
     */
    private List<String> readWelcomeFile() {
        File welcomeFile = new File(pluginFolder, "welcome.txt");
        if (copyFileFromResource(welcomeFile, "welcome.txt")) {
            try {
                return Files.readAllLines(welcomeFile.toPath(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                ConsoleLogger.logException("Failed to read welcome.txt file:", e);
            }
        } else {
            ConsoleLogger.warning("Failed to copy welcome.txt from JAR");
        }
        return Collections.emptyList();
    }

    /**
     * Determines which tags are used in the message.
     *
     * @param welcomeMessage the lines of the welcome message
     * @return the tags
     */
    private List<Tag> determineUsedTags(List<String> welcomeMessage) {
        return availableTags.stream()
            .filter(tag -> welcomeMessage.stream().anyMatch(msg -> msg.contains(tag.getName())))
            .collect(Collectors.toList());
    }

    private static final class TagValue {

        private final String tag;
        private final String value;

        TagValue(String tag, String value) {
            this.tag = tag;
            this.value = value;
        }

        @Override
        public String toString() {
            return "TagValue[tag='" + tag + "', value='" + value + "']";
        }
    }
}
