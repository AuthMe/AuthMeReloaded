package fr.xephi.authme.permission.handlers;

import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.permission.PermissionNode;
import fr.xephi.authme.permission.PermissionsSystemType;
import me.lucko.luckperms.LuckPerms;
import me.lucko.luckperms.api.Contexts;
import me.lucko.luckperms.api.DataMutateResult;
import me.lucko.luckperms.api.Group;
import me.lucko.luckperms.api.LuckPermsApi;
import me.lucko.luckperms.api.Node;
import me.lucko.luckperms.api.User;
import me.lucko.luckperms.api.caching.PermissionData;
import me.lucko.luckperms.api.caching.UserData;
import org.bukkit.OfflinePlayer;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
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
            throw new PermissionHandlerException("Could not get api of LuckPerms", e);
        }
    }

    private void saveUser(User user) {
        luckPermsApi.getUserManager().saveUser(user)
            .thenAcceptAsync(wasSuccessful -> user.refreshCachedData());
    }

    @Override
    public boolean addToGroup(OfflinePlayer player, String group) {
        Group newGroup = luckPermsApi.getGroup(group);
        if (newGroup == null) {
            return false;
        }

        User user = luckPermsApi.getUser(player.getName());
        if (user == null) {
            return false;
        }

        DataMutateResult result = user.setPermission(
            luckPermsApi.getNodeFactory().makeGroupNode(newGroup).build());
        if (result == DataMutateResult.FAIL) {
            return false;
        }

        saveUser(user);
        luckPermsApi.cleanupUser(user);

        return true;
    }

    @Override
    public boolean hasGroupSupport() {
        return true;
    }

    @Override
    public boolean hasPermissionOffline(String name, PermissionNode node) {
        User user = luckPermsApi.getUser(name);
        if (user == null) {
            ConsoleLogger.warning("LuckPermsHandler: tried to check permission for offline user "
                + name + " but it isn't loaded!");
            return false;
        }

        UserData userData = user.getCachedData();
        PermissionData permissionData = userData.getPermissionData(Contexts.allowAll());
        boolean result = permissionData.getPermissionValue(node.getNode()).asBoolean();

        luckPermsApi.cleanupUser(user);
        return result;
    }

    @Override
    public boolean isInGroup(OfflinePlayer player, String group) {
        User user = luckPermsApi.getUser(player.getName());
        if (user == null) {
            ConsoleLogger.warning("LuckPermsHandler: tried to check group for offline user "
                + player.getName() + " but it isn't loaded!");
            return false;
        }

        Group permissionGroup = luckPermsApi.getGroup(group);
        boolean result = permissionGroup != null && user.inheritsGroup(permissionGroup);

        luckPermsApi.cleanupUser(user);
        return result;
    }

    @Override
    public boolean removeFromGroup(OfflinePlayer player, String group) {
        User user = luckPermsApi.getUser(player.getName());
        if (user == null) {
            ConsoleLogger.warning("LuckPermsHandler: tried to remove group for offline user "
                + player.getName() + " but it isn't loaded!");
            return false;
        }

        Group permissionGroup = luckPermsApi.getGroup(group);
        if (permissionGroup == null) {
            return false;
        }

        Node groupNode = luckPermsApi.getNodeFactory().makeGroupNode(permissionGroup).build();
        boolean result = user.unsetPermission(groupNode) != DataMutateResult.FAIL;

        luckPermsApi.cleanupUser(user);
        return result;
    }

    @Override
    public boolean setGroup(OfflinePlayer player, String group) {
        User user = luckPermsApi.getUser(player.getName());
        if (user == null) {
            ConsoleLogger.warning("LuckPermsHandler: tried to set group for offline user "
                + player.getName() + " but it isn't loaded!");
            return false;
        }
        Group permissionGroup = luckPermsApi.getGroup(group);
        if (permissionGroup == null) {
            return false;
        }
        Node groupNode = luckPermsApi.getNodeFactory().makeGroupNode(permissionGroup).build();
        DataMutateResult result = user.setPermission(groupNode);
        if (result == DataMutateResult.FAIL) {
            return false;
        }
        user.clearMatching(node -> node.isGroupNode() && !node.getGroupName().equals(permissionGroup.getName()));

        saveUser(user);
        luckPermsApi.cleanupUser(user);
        return true;
    }

    @Override
    public List<String> getGroups(OfflinePlayer player) {
        User user = luckPermsApi.getUser(player.getName());
        if (user == null) {
            ConsoleLogger.warning("LuckPermsHandler: tried to get groups for offline user "
                + player.getName() + " but it isn't loaded!");
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

        luckPermsApi.cleanupUser(user);
        return result;
    }

    @Override
    public PermissionsSystemType getPermissionSystem() {
        return PermissionsSystemType.LUCK_PERMS;
    }

    @Override
    public void loadUserData(UUID uuid) throws PermissionLoadUserException {
        try {
            luckPermsApi.getUserManager().loadUser(uuid).get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            throw new PermissionLoadUserException("Unable to load the permission data of the user " + uuid, e);
        }
    }

    @Override
    public void loadUserData(String name) throws PermissionLoadUserException {
        try {
            UUID uuid = luckPermsApi.getStorage().getUUID(name).get(5, TimeUnit.SECONDS);
            loadUserData(uuid);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            throw new PermissionLoadUserException("Unable to load the permission data of the user " + name, e);
        }
    }
}
