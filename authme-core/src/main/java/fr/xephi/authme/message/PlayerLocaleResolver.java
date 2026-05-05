package fr.xephi.authme.message;

import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.PluginSettings;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;

/**
 * Maps Minecraft client locale strings (e.g. {@code "fr_fr"}) to AuthMe language codes (e.g. {@code "fr"}).
 */
public final class PlayerLocaleResolver {

    // Minecraft locales whose language code alone is not enough to pick the right AuthMe file
    private static final Map<String, String> LOCALE_OVERRIDES = Map.of(
        "pt_br", "br",
        "zh_cn", "zhcn",
        "zh_tw", "zhtw",
        "zh_hk", "zhhk"
    );

    private PlayerLocaleResolver() {
    }

    /**
     * Returns the AuthMe language code to use for the given sender, or {@code null} to use the server default.
     * <p>
     * Returns non-null only when the sender is a {@link Player} and per-player locale is enabled in settings.
     *
     * @param settings the plugin settings (may be {@code null}, treated as per-player locale disabled)
     * @param sender   the command sender
     * @return the player's language code, or {@code null} to fall back to the server-configured language
     */
    public static String resolveLanguage(Settings settings, CommandSender sender) {
        if (settings != null && sender instanceof Player player
                && settings.getProperty(PluginSettings.PER_PLAYER_LOCALE)) {
            return toLanguageCode(player.getLocale());
        }
        return null;
    }

    /**
     * Converts a Minecraft client locale string to an AuthMe language code.
     * <p>
     * Returns {@code null} for blank input; the caller is expected to fall back to the
     * server-configured language in that case.
     *
     * @param minecraftLocale the locale reported by {@link org.bukkit.entity.Player#getLocale()} (e.g. {@code "fr_fr"})
     * @return the matching AuthMe language code, or {@code null} if the input is blank
     */
    public static String toLanguageCode(String minecraftLocale) {
        if (minecraftLocale == null || minecraftLocale.isBlank()) {
            return null;
        }
        String locale = minecraftLocale.toLowerCase();
        String override = LOCALE_OVERRIDES.get(locale);
        if (override != null) {
            return override;
        }
        int underscore = locale.indexOf('_');
        return underscore > 0 ? locale.substring(0, underscore) : locale;
    }
}
