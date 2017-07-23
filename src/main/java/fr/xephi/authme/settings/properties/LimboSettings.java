package fr.xephi.authme.settings.properties;

import ch.jalu.configme.Comment;
import ch.jalu.configme.SectionComments;
import ch.jalu.configme.SettingsHolder;
import ch.jalu.configme.properties.Property;
import com.google.common.collect.ImmutableMap;
import fr.xephi.authme.data.limbo.AllowFlightRestoreType;
import fr.xephi.authme.data.limbo.WalkFlySpeedRestoreType;
import fr.xephi.authme.data.limbo.persistence.LimboPersistenceType;
import fr.xephi.authme.data.limbo.persistence.SegmentSize;

import java.util.Map;

import static ch.jalu.configme.properties.PropertyInitializer.newProperty;

/**
 * Settings for the LimboPlayer feature.
 */
public final class LimboSettings implements SettingsHolder {

    @Comment({
        "Besides storing the data in memory, you can define if/how the data should be persisted",
        "on disk. This is useful in case of a server crash, so next time the server starts we can",
        "properly restore things like OP status, ability to fly, and walk/fly speed.",
        "DISABLED: no disk storage,",
        "INDIVIDUAL_FILES: each player data in its own file,",
        "DISTRIBUTED_FILES: distributes players into different files based on their UUID, see below"
    })
    public static final Property<LimboPersistenceType> LIMBO_PERSISTENCE_TYPE =
        newProperty(LimboPersistenceType.class, "limbo.persistence.type", LimboPersistenceType.INDIVIDUAL_FILES);

    @Comment({
        "This setting only affects DISTRIBUTED_FILES persistence. The distributed file",
        "persistence attempts to reduce the number of files by distributing players into various",
        "buckets based on their UUID. This setting defines into how many files the players should",
        "be distributed. Possible values: ONE, FOUR, EIGHT, SIXTEEN, THIRTY_TWO, SIXTY_FOUR,",
        "ONE_TWENTY for 128, TWO_FIFTY for 256.",
        "For example, if you expect 100 non-logged in players, setting to SIXTEEN will average",
        "6.25 players per file (100 / 16).",
        "Note: if you change this setting all data will be migrated. If you have a lot of data,",
        "change this setting only on server restart, not with /authme reload."
    })
    public static final Property<SegmentSize> DISTRIBUTION_SIZE =
        newProperty(SegmentSize.class, "limbo.persistence.distributionSize", SegmentSize.SIXTEEN);

    @Comment({
        "Whether the player is allowed to fly: RESTORE, ENABLE, DISABLE, NOTHING.",
        "RESTORE sets back the old property from the player. NOTHING will prevent AuthMe",
        "from modifying the 'allow flight' property on the player."
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
        newProperty(WalkFlySpeedRestoreType.class, "limbo.restoreFlySpeed", WalkFlySpeedRestoreType.RESTORE_NO_ZERO);

    @Comment({
        "Restore walk speed: RESTORE, DEFAULT, MAX_RESTORE, RESTORE_NO_ZERO.",
        "See above for a description of the values."
    })
    public static final Property<WalkFlySpeedRestoreType> RESTORE_WALK_SPEED =
        newProperty(WalkFlySpeedRestoreType.class, "limbo.restoreWalkSpeed", WalkFlySpeedRestoreType.RESTORE_NO_ZERO);

    private LimboSettings() {
    }

    @SectionComments
    public static Map<String, String[]> createSectionComments() {
        String[] limboExplanation = {
            "Before a user logs in, various properties are temporarily removed from the player,",
            "such as OP status, ability to fly, and walk/fly speed.",
            "Once the user is logged in, we add back the properties we previously saved.",
            "In this section, you may define how these properties should be handled.",
            "Read more at https://github.com/AuthMe/AuthMeReloaded/wiki/Limbo-players"
        };
        return ImmutableMap.of("limbo", limboExplanation);
    }
}
