package fr.xephi.authme.data.limbo.persistence;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import fr.xephi.authme.data.limbo.EnderPearlRestoreData;
import fr.xephi.authme.data.limbo.LimboPlayer;
import fr.xephi.authme.data.limbo.UserGroup;
import fr.xephi.authme.service.BukkitService;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.util.Vector;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

import static fr.xephi.authme.data.limbo.persistence.LimboPlayerSerializer.CAN_FLY;
import static fr.xephi.authme.data.limbo.persistence.LimboPlayerSerializer.ENDER_PEARLS;
import static fr.xephi.authme.data.limbo.persistence.LimboPlayerSerializer.FLY_SPEED;
import static fr.xephi.authme.data.limbo.persistence.LimboPlayerSerializer.GROUPS;
import static fr.xephi.authme.data.limbo.persistence.LimboPlayerSerializer.IS_OP;
import static fr.xephi.authme.data.limbo.persistence.LimboPlayerSerializer.LOCATION;
import static fr.xephi.authme.data.limbo.persistence.LimboPlayerSerializer.LOC_PITCH;
import static fr.xephi.authme.data.limbo.persistence.LimboPlayerSerializer.LOC_WORLD;
import static fr.xephi.authme.data.limbo.persistence.LimboPlayerSerializer.LOC_X;
import static fr.xephi.authme.data.limbo.persistence.LimboPlayerSerializer.LOC_Y;
import static fr.xephi.authme.data.limbo.persistence.LimboPlayerSerializer.LOC_YAW;
import static fr.xephi.authme.data.limbo.persistence.LimboPlayerSerializer.LOC_Z;
import static fr.xephi.authme.data.limbo.persistence.LimboPlayerSerializer.PEARL_UUID;
import static fr.xephi.authme.data.limbo.persistence.LimboPlayerSerializer.VELOCITY;
import static fr.xephi.authme.data.limbo.persistence.LimboPlayerSerializer.VEL_X;
import static fr.xephi.authme.data.limbo.persistence.LimboPlayerSerializer.VEL_Y;
import static fr.xephi.authme.data.limbo.persistence.LimboPlayerSerializer.VEL_Z;
import static fr.xephi.authme.data.limbo.persistence.LimboPlayerSerializer.VEHICLE_TYPE;
import static fr.xephi.authme.data.limbo.persistence.LimboPlayerSerializer.VEHICLE_UUID;
import static fr.xephi.authme.data.limbo.persistence.LimboPlayerSerializer.WALK_SPEED;
import static java.util.Optional.ofNullable;

/**
 * Converts a JsonElement to a LimboPlayer.
 */
class LimboPlayerDeserializer implements JsonDeserializer<LimboPlayer> {

    private static final String GROUP_LEGACY = "group";
    private static final String CONTEXT_MAP = "contextMap";
    private static final String GROUP_NAME = "groupName";

    private BukkitService bukkitService;

    LimboPlayerDeserializer(BukkitService bukkitService) {
        this.bukkitService = bukkitService;
    }

    @Override
    public LimboPlayer deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext context) {
        JsonObject jsonObject = jsonElement.getAsJsonObject();
        if (jsonObject == null) {
            return null;
        }

        Location loc = deserializeLocation(jsonObject);
        boolean operator = getBoolean(jsonObject, IS_OP);

        Collection<UserGroup> groups = getLimboGroups(jsonObject);
        boolean canFly = getBoolean(jsonObject, CAN_FLY);
        float walkSpeed = getFloat(jsonObject, WALK_SPEED, LimboPlayer.DEFAULT_WALK_SPEED);
        float flySpeed = getFloat(jsonObject, FLY_SPEED, LimboPlayer.DEFAULT_FLY_SPEED);

        LimboPlayer limboPlayer = new LimboPlayer(loc, operator, groups, canFly, walkSpeed, flySpeed);

        JsonElement pearlsElement = jsonObject.get(ENDER_PEARLS);
        if (pearlsElement != null && pearlsElement.isJsonArray()) {
            Set<EnderPearlRestoreData> pearls = new HashSet<>();
            for (JsonElement pearlElement : pearlsElement.getAsJsonArray()) {
                EnderPearlRestoreData pearl = deserializeEnderPearl(pearlElement);
                if (pearl != null) {
                    pearls.add(pearl);
                }
            }
            limboPlayer.setEnderPearls(pearls);
        }

        JsonElement vehicleUuidElement = jsonObject.get(VEHICLE_UUID);
        JsonElement vehicleTypeElement = jsonObject.get(VEHICLE_TYPE);
        if (vehicleUuidElement != null && vehicleTypeElement != null) {
            try {
                UUID vehicleUuid = UUID.fromString(vehicleUuidElement.getAsString());
                EntityType vehicleType = EntityType.valueOf(vehicleTypeElement.getAsString());
                limboPlayer.setVehicle(vehicleUuid, vehicleType);
            } catch (IllegalArgumentException ignored) {
            }
        }

        return limboPlayer;
    }

    private Location deserializeLocation(JsonObject jsonObject) {
        JsonObject locationObject = jsonObject.getAsJsonObject(LOCATION);
        if (locationObject != null) {
            return deserializeLocationObject(locationObject);
        }
        return null;
    }

    private EnderPearlRestoreData deserializeEnderPearl(JsonElement pearlElement) {
        try {
            if (!pearlElement.isJsonObject()) {
                return new EnderPearlRestoreData(UUID.fromString(pearlElement.getAsString()), null, null);
            }

            JsonObject pearlObject = pearlElement.getAsJsonObject();
            UUID pearlUuid = UUID.fromString(getString(pearlObject, PEARL_UUID));
            Location location = deserializeLocationObject(pearlObject.getAsJsonObject(LOCATION));
            Vector velocity = deserializeVelocity(pearlObject.getAsJsonObject(VELOCITY));
            return new EnderPearlRestoreData(pearlUuid, location, velocity);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private Location deserializeLocationObject(JsonObject locationObject) {
        if (locationObject == null) {
            return null;
        }

        World world = bukkitService.getWorld(getString(locationObject, LOC_WORLD));
        if (world == null) {
            return null;
        }

        double x = getDouble(locationObject, LOC_X);
        double y = getDouble(locationObject, LOC_Y);
        double z = getDouble(locationObject, LOC_Z);
        float yaw = getFloat(locationObject, LOC_YAW);
        float pitch = getFloat(locationObject, LOC_PITCH);
        return new Location(world, x, y, z, yaw, pitch);
    }

    private Vector deserializeVelocity(JsonObject velocityObject) {
        if (velocityObject == null) {
            return null;
        }
        return new Vector(
            getDouble(velocityObject, VEL_X),
            getDouble(velocityObject, VEL_Y),
            getDouble(velocityObject, VEL_Z));
    }

    private static String getString(JsonObject jsonObject, String memberName) {
        JsonElement element = jsonObject.get(memberName);
        return element != null ? element.getAsString() : "";
    }

    /**
     * @param jsonObject LimboPlayer represented as JSON
     * @return The list of UserGroups create from JSON
     */
    private static List<UserGroup> getLimboGroups(JsonObject jsonObject) {
        JsonElement element = jsonObject.get(GROUPS);
        if (element == null) {
            String legacyGroup = ofNullable(jsonObject.get(GROUP_LEGACY)).map(JsonElement::getAsString).orElse(null);
            return legacyGroup == null ? Collections.emptyList() :
                Collections.singletonList(new UserGroup(legacyGroup, null));
        }
        List<UserGroup> result = new ArrayList<>();
        JsonArray jsonArray = element.getAsJsonArray();
        for (JsonElement arrayElement : jsonArray) {
            if (!arrayElement.isJsonObject()) {
                result.add(new UserGroup(arrayElement.getAsString(), null));
            } else {
                JsonObject jsonGroup = arrayElement.getAsJsonObject();
                Map<String, String> contextMap = null;
                if (jsonGroup.has(CONTEXT_MAP)) {
                    JsonElement contextMapJson = jsonGroup.get("contextMap");
                    Type type = new TypeToken<Map<String, String>>() {
                    }.getType();
                    contextMap = new Gson().fromJson(contextMapJson.getAsString(), type);
                }

                String groupName = jsonGroup.get(GROUP_NAME).getAsString();
                result.add(new UserGroup(groupName, contextMap));
            }
        }
        return result;
    }

    private static boolean getBoolean(JsonObject jsonObject, String memberName) {
        JsonElement element = jsonObject.get(memberName);
        return element != null && element.getAsBoolean();
    }

    private static float getFloat(JsonObject jsonObject, String memberName) {
        return getNumberFromElement(jsonObject.get(memberName), JsonElement::getAsFloat, 0.0f);
    }

    private static float getFloat(JsonObject jsonObject, String memberName, float defaultValue) {
        return getNumberFromElement(jsonObject.get(memberName), JsonElement::getAsFloat, defaultValue);
    }

    private static double getDouble(JsonObject jsonObject, String memberName) {
        return getNumberFromElement(jsonObject.get(memberName), JsonElement::getAsDouble, 0.0);
    }

    /**
     * Gets a number from the given JsonElement safely.
     *
     * @param jsonElement    the element to retrieve the number from
     * @param numberFunction the function to get the number from the element
     * @param defaultValue   the value to return if the element is null or the number cannot be retrieved
     * @param <N>            the number type
     * @return the number from the given JSON element, or the default value
     */
    private static <N extends Number> N getNumberFromElement(JsonElement jsonElement,
                                                             Function<JsonElement, N> numberFunction,
                                                             N defaultValue) {
        if (jsonElement != null) {
            try {
                return numberFunction.apply(jsonElement);
            } catch (NumberFormatException ignore) {
            }
        }
        return defaultValue;
    }
}
