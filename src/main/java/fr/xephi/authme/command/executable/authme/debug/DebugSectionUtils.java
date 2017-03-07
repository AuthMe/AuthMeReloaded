package fr.xephi.authme.command.executable.authme.debug;

import org.bukkit.Location;

import java.math.RoundingMode;
import java.text.DecimalFormat;

/**
 * Utilities used within the DebugSection implementations.
 */
final class DebugSectionUtils {

    private DebugSectionUtils() {
    }

    /**
     * Formats the given location in a human readable way. Null-safe.
     *
     * @param location the location to format
     * @return the formatted location
     */
    static String formatLocation(Location location) {
        if (location == null) {
            return "null";
        }

        String worldName = location.getWorld() == null ? "null" : location.getWorld().getName();
        return formatLocation(location.getX(), location.getY(), location.getZ(), worldName);
    }

    /**
     * Formats the given location in a human readable way.
     *
     * @param x the x coordinate
     * @param y the y coordinate
     * @param z the z coordinate
     * @param world the world name
     * @return the formatted location
     */
    static String formatLocation(double x, double y, double z, String world) {
        return "(" + round(x) + ", " + round(y) + ", " + round(z) + ") in '" + world + "'";
    }

    /**
     * Rounds the given number to two decimals.
     *
     * @param number the number to round
     * @return the rounded number
     */
    private static String round(double number) {
        DecimalFormat df = new DecimalFormat("#.##");
        df.setRoundingMode(RoundingMode.HALF_UP);
        return df.format(number);
    }
}
