package fr.xephi.authme.command.executable.authme.debug;

import com.google.common.collect.ImmutableList;
import fr.xephi.authme.permission.AdminPermission;
import fr.xephi.authme.permission.DebugSectionPermissions;
import fr.xephi.authme.permission.DefaultPermission;
import fr.xephi.authme.permission.PermissionNode;
import fr.xephi.authme.permission.PermissionsManager;
import fr.xephi.authme.permission.PlayerPermission;
import fr.xephi.authme.permission.PlayerStatePermission;
import fr.xephi.authme.service.BukkitService;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;

/**
 * Checks if a player has a given permission, as checked by AuthMe.
 */
class HasPermissionChecker implements DebugSection {

    static final List<Class<? extends PermissionNode>> PERMISSION_NODE_CLASSES = ImmutableList.of(
        AdminPermission.class, PlayerPermission.class, PlayerStatePermission.class, DebugSectionPermissions.class);

    @Inject
    private PermissionsManager permissionsManager;

    @Inject
    private BukkitService bukkitService;

    @Override
    public String getName() {
        return "perm";
    }

    @Override
    public String getDescription() {
        return "Checks if a player has a given permission";
    }

    @Override
    public void execute(CommandSender sender, List<String> arguments) {
        sender.sendMessage(ChatColor.BLUE + "AuthMe permission check");
        if (arguments.size() < 2) {
            sender.sendMessage("Check if a player has permission:");
            sender.sendMessage("Example: /authme debug perm bobby my.perm.node");
            sender.sendMessage("Permission system type used: " + permissionsManager.getPermissionSystem());
            return;
        }

        final String playerName = arguments.get(0);
        final String permissionNode = arguments.get(1);

        Player player = bukkitService.getPlayerExact(playerName);
        if (player == null) {
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerName);
            if (offlinePlayer == null) {
                sender.sendMessage(ChatColor.DARK_RED + "Player '" + playerName + "' does not exist");
            } else {
                sender.sendMessage("Player '" + playerName + "' not online; checking with offline player");
                performPermissionCheck(offlinePlayer, permissionNode, permissionsManager::hasPermissionOffline, sender);
            }
        } else {
            performPermissionCheck(player, permissionNode, permissionsManager::hasPermission, sender);
        }
    }

    @Override
    public PermissionNode getRequiredPermission() {
        return DebugSectionPermissions.HAS_PERMISSION_CHECK;
    }

    /**
     * Performs a permission check and informs the given sender of the result. {@code permissionChecker} is the
     * permission check to perform with the given {@code node} and the {@code player}.
     *
     * @param player the player to check a permission for
     * @param node the node of the permission to check
     * @param permissionChecker permission checking function
     * @param sender the sender to inform of the result
     * @param <P> the player type
     */
    private static <P extends OfflinePlayer> void performPermissionCheck(
        P player, String node, BiFunction<P, PermissionNode, Boolean> permissionChecker, CommandSender sender) {

        PermissionNode permNode = getPermissionNode(sender, node);
        if (permissionChecker.apply(player, permNode)) {
            sender.sendMessage(ChatColor.DARK_GREEN + "Success: player '" + player.getName()
                + "' has permission '" + node + "'");
        } else {
            sender.sendMessage(ChatColor.DARK_RED + "Check failed: player '" + player.getName()
                + "' does NOT have permission '" + node + "'");
        }
    }

    /**
     * Based on the given permission node (String), tries to find the according AuthMe {@link PermissionNode}
     * instance, or creates a new one if not available.
     *
     * @param sender the sender (used to inform him if no AuthMe PermissionNode can be matched)
     * @param node the node to search for
     * @return the node as {@link PermissionNode} object
     */
    private static PermissionNode getPermissionNode(CommandSender sender, String node) {
        Optional<? extends PermissionNode> permNode = PERMISSION_NODE_CLASSES.stream()
            .map(Class::getEnumConstants)
            .flatMap(Arrays::stream)
            .filter(perm -> perm.getNode().equals(node))
            .findFirst();
        if (permNode.isPresent()) {
            return permNode.get();
        } else {
            sender.sendMessage("Did not detect AuthMe permission; using default permission = DENIED");
            return createPermNode(node);
        }
    }

    private static PermissionNode createPermNode(String node) {
        return new PermissionNode() {
            @Override
            public String getNode() {
                return node;
            }

            @Override
            public DefaultPermission getDefaultPermission() {
                return DefaultPermission.NOT_ALLOWED;
            }
        };
    }
}
