package fr.xephi.authme.data.limbo;

import org.bukkit.Location;
import org.bukkit.World;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;

/**
 * Contains matchers for LimboPlayer.
 */
// Justification: Javadoc would be huge because of the many parameters
@SuppressWarnings("checkstyle:MissingJavadocMethod")
public final class LimboPlayerMatchers {

    private LimboPlayerMatchers() {
    }

    public static Matcher<LimboPlayer> isLimbo(LimboPlayer limbo) {
        UserGroup[] groups = limbo.getGroups().toArray(new UserGroup[limbo.getGroups().size()]);
        return isLimbo(limbo.isOperator(), limbo.isCanFly(), limbo.getWalkSpeed(), limbo.getFlySpeed(), groups);
    }

    public static Matcher<LimboPlayer> isLimbo(boolean isOp, boolean canFly, float walkSpeed, float flySpeed,
                                               UserGroup... groups) {
        return new TypeSafeMatcher<LimboPlayer>() {
            @Override
            protected boolean matchesSafely(LimboPlayer item) {
                return item.isOperator() == isOp
                    && collectionContains(item.getGroups(), groups)
                    && item.isCanFly() == canFly
                    && walkSpeed == item.getWalkSpeed()
                    && flySpeed == item.getFlySpeed();
            }

            @Override
            public void describeTo(Description description) {
                List<String> groupNames = Arrays.stream(groups).map(UserGroup::getGroupName).collect(toList());
                description.appendText(format("Limbo with isOp=%s, groups={%s}, canFly=%s, walkSpeed=%f, flySpeed=%f",
                    isOp, String.join(" ,", groupNames), canFly, walkSpeed, flySpeed));
            }

            @Override
            public void describeMismatchSafely(LimboPlayer item, Description description) {
                List<String> groupNames = item.getGroups().stream()
                    .map(UserGroup::getGroupName)
                    .collect(toList());

                description.appendText(format("Limbo with isOp=%s, groups={%s}, canFly=%s, walkSpeed=%f, flySpeed=%f",
                    item.isOperator(), String.join(" ,", groupNames), item.isCanFly(),
                    item.getWalkSpeed(), item.getFlySpeed()));
            }
        };
    }

    public static Matcher<LimboPlayer> hasLocation(String world, double x, double y, double z) {
        return new TypeSafeMatcher<LimboPlayer>() {
            @Override
            protected boolean matchesSafely(LimboPlayer item) {
                Location location = item.getLocation();
                return location.getWorld().getName().equals(world)
                    && location.getX() == x && location.getY() == y && location.getZ() == z;
            }

            @Override
            public void describeTo(Description description) {
                description.appendText(format("Limbo with location: world=%s, x=%f, y=%f, z=%f",
                    world, x, y, z));
            }

            @Override
            public void describeMismatchSafely(LimboPlayer item, Description description) {
                Location location = item.getLocation();
                if (location == null) {
                    description.appendText("Limbo with location = null");
                } else {
                    description.appendText(format("Limbo with location: world=%s, x=%f, y=%f, z=%f",
                        location.getWorld().getName(), location.getX(), location.getY(), location.getZ()));
                }
            }
        };
    }

    public static Matcher<LimboPlayer> hasLocation(World world, double x, double y, double z) {
        return hasLocation(world.getName(), x, y, z);
    }

    public static Matcher<LimboPlayer> hasLocation(String world, double x, double y, double z, float yaw, float pitch) {
        return new TypeSafeMatcher<LimboPlayer>() {
            @Override
            protected boolean matchesSafely(LimboPlayer item) {
                Location location = item.getLocation();
                return hasLocation(location.getWorld(), location.getX(), location.getY(), location.getZ()).matches(item)
                    && location.getYaw() == yaw && location.getPitch() == pitch;
            }

            @Override
            public void describeTo(Description description) {
                description.appendText(format("Limbo with location: world=%s, x=%f, y=%f, z=%f, yaw=%f, pitch=%f",
                    world, x, y, z, yaw, pitch));
            }

            @Override
            public void describeMismatchSafely(LimboPlayer item, Description description) {
                Location location = item.getLocation();
                if (location == null) {
                    description.appendText("Limbo with location = null");
                } else {
                    description.appendText(format("Limbo with location: world=%s, x=%f, y=%f, z=%f, yaw=%f, pitch=%f",
                        location.getWorld().getName(), location.getX(), location.getY(), location.getZ(),
                        location.getYaw(), location.getPitch()));
                }
            }
        };
    }

    public static Matcher<LimboPlayer> hasLocation(Location location) {
        return hasLocation(location.getWorld().getName(), location.getX(), location.getY(), location.getZ(),
            location.getYaw(), location.getPitch());
    }

    // Hamcrest's contains() doesn't like it when there are no items, so we need to check for the empty case explicitly
    private static boolean collectionContains(Collection<UserGroup> givenItems, UserGroup... expectedItems) {
        if (expectedItems.length == 0) {
            return givenItems.isEmpty();
        }
        return contains(expectedItems).matches(givenItems);
    }
}
