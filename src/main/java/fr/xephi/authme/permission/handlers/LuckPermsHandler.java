package fr.xephi.authme.permission.handlers;

import fr.xephi.authme.permission.PermissionNode;
import fr.xephi.authme.permission.PermissionsSystemType;
import me.lucko.luckperms.LuckPerms;
import me.lucko.luckperms.api.DataMutateResult;
import me.lucko.luckperms.api.Group;
import me.lucko.luckperms.api.LuckPermsApi;
import me.lucko.luckperms.api.Node;
import me.lucko.luckperms.api.User;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Handler for LuckPerms.
 *
 * @see <a href="https://www.spigotmc.org/resources/luckperms-an-advanced-permissions-system.28140/">LuckPerms SpigotMC page</a>
 * @see <a href="https://github.com/lucko/LuckPerms">LuckPerms on Github</a>
 */
public class LuckPermsHandler implements PermissionHandler {

    private LuckPermsApi luckPermsApi;

    public LuckPermsHandler() throws PermissionHandlerException {
        try {
            luckPermsApi = LuckPerms.getApi();
        } catch (IllegalStateException e) {
            e.printStackTrace();
            throw new PermissionHandlerException("Could not get api of LuckPerms");
        }
    }

    private User getUser(String playerName) {
        Player player = Bukkit.getPlayerExact(playerName);
        if (player != null) {
            return getUser(player);
        }

        UUID uuid = luckPermsApi.getStorage().getUUID(playerName).join();
        if (uuid == null) {
            return null;
        }

        return getUser(uuid);
    }

    private User getUser(OfflinePlayer player) {
        return getUser(player.getUniqueId());
    }

    private User getUser(UUID playerUuid) {
        User user = luckPermsApi.getUser(playerUuid);
        if (user == null) {
            // user not loaded, we need to load them from the storage.
            // this is a blocking call.
            luckPermsApi.getStorage().loadUser(playerUuid).join();

            // then grab a new instance
            user = luckPermsApi.getUser(playerUuid);
        }
        return user;
    }

    private void saveUser(User user) {
        luckPermsApi.getStorage().saveUser(user)
            .thenAcceptAsync(wasSuccessful -> {
                if (!wasSuccessful) {
                    return;
                }
                user.refreshPermissions();
            }, luckPermsApi.getStorage().getAsyncExecutor());
    }

    private void cleanupUser(User user) {
        if (Bukkit.getPlayer(user.getUuid()) != null) {
            luckPermsApi.cleanupUser(user);
        }
    }

    @Override
    public boolean addToGroup(OfflinePlayer player, String group) {
        Group newGroup = luckPermsApi.getGroup(group);
        if (newGroup == null) {
            return false;
        }

        User user = getUser(player);
        if (user == null) {
            return false;
        }

        DataMutateResult result = user.setPermissionUnchecked(luckPermsApi.getNodeFactory().makeGroupNode(newGroup).build());
        if (result == DataMutateResult.FAIL) {
            return false;
        }

        saveUser(user);
        cleanupUser(user);

        return true;
    }

    @Override
    public boolean hasGroupSupport() {
        return true;
    }

    @Override
    public boolean hasPermissionOffline(String name, PermissionNode node) {
        User user = getUser(name);
        if (user == null) {
            return false;
        }

        Node permissionNode = luckPermsApi.getNodeFactory().newBuilder(node.getNode()).build();
        boolean result = user.hasPermission(permissionNode).asBoolean();

        cleanupUser(user);
        return result;
    }

    @Override
    public boolean isInGroup(OfflinePlayer player, String group) {
        User user = getUser(player);
        if (user == null) {
            return false;
        }

        Group permissionGroup = luckPermsApi.getGroup(group);
        boolean result = permissionGroup != null && user.isInGroup(permissionGroup);

        cleanupUser(user);
        return result;
    }

    @Override
    public boolean removeFromGroup(OfflinePlayer player, String group) {
        User user = getUser(player);
        if (user == null) {
            return false;
        }

        Group permissionGroup = luckPermsApi.getGroup(group);
        if (permissionGroup == null) {
            return false;
        }

        Node groupNode = luckPermsApi.getNodeFactory().makeGroupNode(permissionGroup).build();
        boolean result = user.unsetPermissionUnchecked(groupNode) != DataMutateResult.FAIL;

        cleanupUser(user);
        return result;
    }

    @Override
    public boolean setGroup(OfflinePlayer player, String group) {
        User user = getUser(player);
        if (user == null) {
            return false;
        }
        Group permissionGroup = luckPermsApi.getGroup(group);
        if (permissionGroup == null) {
            return false;
        }
        Node groupNode = luckPermsApi.getNodeFactory().makeGroupNode(permissionGroup).build();
        DataMutateResult result = user.setPermissionUnchecked(groupNode);
        if (result == DataMutateResult.FAIL) {
            return false;
        }
        user.clearMatching(node -> node.isGroupNode() && !node.getGroupName().equals(permissionGroup.getName()));

        saveUser(user);
        cleanupUser(user);
        return true;
    }

    @Override
    public List<String> getGroups(OfflinePlayer player) {
        User user = getUser(player);
        if (user == null) {
            return Collections.emptyList();
        }

        List<String> result = user.getOwnNodes().stream()
            .filter(Node::isGroupNode)
            .map(n -> luckPermsApi.getGroupSafe(n.getGroupName()))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .distinct()
            .sorted((o1, o2) -> {
                if (o1.getName().equals(user.getPrimaryGroup()) || o2.getName().equals(user.getPrimaryGroup())) {
                    return o1.getName().equals(user.getPrimaryGroup()) ? 1 : -1;
                }

                int i = Integer.compare(o2.getWeight().orElse(0), o1.getWeight().orElse(0));
                return i != 0 ? i : o1.getName().compareToIgnoreCase(o2.getName());
            })
            .map(Group::getName)
            .collect(Collectors.toList());

        cleanupUser(user);
        return result;
    }

    @Override
    public PermissionsSystemType getPermissionSystem() {
        return PermissionsSystemType.LUCK_PERMS;
    }
}
