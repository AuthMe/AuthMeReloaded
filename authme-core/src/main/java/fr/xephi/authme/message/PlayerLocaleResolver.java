package fr.xephi.authme.message;

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
