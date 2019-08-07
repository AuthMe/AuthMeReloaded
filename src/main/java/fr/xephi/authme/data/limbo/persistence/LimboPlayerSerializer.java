package fr.xephi.authme.data.limbo.persistence;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import fr.xephi.authme.data.limbo.LimboPlayer;
import org.bukkit.Location;

import java.lang.reflect.Type;

/**
 * Converts a LimboPlayer to a JsonElement.
 */
class LimboPlayerSerializer implements JsonSerializer<LimboPlayer> {

    static final String LOCATION = "location";
    static final String LOC_WORLD = "world";
    static final String LOC_X = "x";
    static final String LOC_Y = "y";
    static final String LOC_Z = "z";
    static final String LOC_YAW = "yaw";
    static final String LOC_PITCH = "pitch";

    static final String GROUPS = "groups";
    static final String IS_OP = "operator";
    static final String CAN_FLY = "can-fly";
    static final String WALK_SPEED = "walk-speed";
    static final String FLY_SPEED = "fly-speed";

    private static final Gson GSON = new Gson();


    @Override
    public JsonElement serialize(LimboPlayer limboPlayer, Type type, JsonSerializationContext context) {
        Location loc = limboPlayer.getLocation();
        JsonObject locationObject = new JsonObject();
        locationObject.addProperty(LOC_WORLD, loc.getWorld().getName());
        locationObject.addProperty(LOC_X, loc.getX());
        locationObject.addProperty(LOC_Y, loc.getY());
        locationObject.addProperty(LOC_Z, loc.getZ());
        locationObject.addProperty(LOC_YAW, loc.getYaw());
        locationObject.addProperty(LOC_PITCH, loc.getPitch());

        JsonObject obj = new JsonObject();
        obj.add(LOCATION, locationObject);
        obj.add(GROUPS, GSON.toJsonTree(limboPlayer.getGroups()).getAsJsonArray());

        obj.addProperty(IS_OP, limboPlayer.isOperator());
        obj.addProperty(CAN_FLY, limboPlayer.isCanFly());
        obj.addProperty(WALK_SPEED, limboPlayer.getWalkSpeed());
        obj.addProperty(FLY_SPEED, limboPlayer.getFlySpeed());
        return obj;
    }
}
