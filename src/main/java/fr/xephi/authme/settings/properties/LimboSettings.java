package fr.xephi.authme.settings.properties;

import ch.jalu.configme.Comment;
import ch.jalu.configme.SectionComments;
import ch.jalu.configme.SettingsHolder;
import ch.jalu.configme.properties.Property;
import com.google.common.collect.ImmutableMap;
import fr.xephi.authme.data.limbo.AllowFlightRestoreType;
import fr.xephi.authme.data.limbo.WalkFlySpeedRestoreType;

import java.util.Map;

import static ch.jalu.configme.properties.PropertyInitializer.newProperty;

/**
 * Settings for the LimboPlayer feature.
 */
public final class LimboSettings implements SettingsHolder {

    @Comment({
        "Whether the player is allowed to fly: RESTORE, ENABLE, DISABLE.",
        "RESTORE sets back the old property from the player."
    })
    public static final Property<AllowFlightRestoreType> RESTORE_ALLOW_FLIGHT =
        newProperty(AllowFlightRestoreType.class, "limbo.restoreAllowFlight", AllowFlightRestoreType.RESTORE);

    @Comment({
        "Restore fly speed: RESTORE, DEFAULT, MAX_RESTORE, RESTORE_NO_ZERO.",
        "RESTORE: restore the speed the player had;",
        "DEFAULT: always set to default speed;",
        "MAX_RESTORE: take the maximum of the player's current speed and the previous one",
        "RESTORE_NO_ZERO: Like 'restore' but sets speed to default if the player's speed was 0"
    })
    public static final Property<WalkFlySpeedRestoreType> RESTORE_FLY_SPEED =
        newProperty(WalkFlySpeedRestoreType.class, "limbo.restoreFlySpeed", WalkFlySpeedRestoreType.MAX_RESTORE);

    @Comment({
        "Restore walk speed: RESTORE, DEFAULT, MAX_RESTORE, RESTORE_NO_ZERO.",
        "See above for a description of the values."
    })
    public static final Property<WalkFlySpeedRestoreType> RESTORE_WALK_SPEED =
        newProperty(WalkFlySpeedRestoreType.class, "limbo.restoreWalkSpeed", WalkFlySpeedRestoreType.MAX_RESTORE);

    private LimboSettings() {
    }

    @SectionComments
    public static Map<String, String[]> createSectionComments() {
        String[] limboExplanation = {
            "Before a user logs in, various properties are temporarily removed from the player,",
            "such as OP status, ability to fly, and walk/fly speed.",
            "Once the user is logged in, we add back the properties we previously saved.",
            "In this section, you may define how the properties should be restored."
        };
        return ImmutableMap.of("limbo", limboExplanation);
    }
}
