package fr.xephi.authme.settings.properties;

import ch.jalu.configme.Comment;
import ch.jalu.configme.SettingsHolder;
import ch.jalu.configme.properties.Property;

import static ch.jalu.configme.properties.PropertyInitializer.newProperty;

public final class PremiumSettings implements SettingsHolder {

    @Comment({
        "Enable premium mode: players with an official Minecraft account",
        "can skip password authentication.",
        "Verification method is chosen automatically:",
        "  - online-mode=true: Bukkit already has the Mojang UUID; no PacketEvents needed.",
        "  - offline-mode + proxy: set Hooks.bungeecord=true; UUID is forwarded by proxy.",
        "  - offline-mode, no proxy: PacketEvents required for cryptographic verification.",
        "    Without PacketEvents, premium auto-login is disabled (fail closed).",
        "Players must use /premium to opt in."
    })
    public static final Property<Boolean> ENABLE_PREMIUM =
        newProperty("settings.enablePremium", false);

    private PremiumSettings() {
    }

}
