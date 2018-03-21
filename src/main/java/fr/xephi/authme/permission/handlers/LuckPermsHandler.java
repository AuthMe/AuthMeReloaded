package fr.xephi.authme.permission.handlers;

import fr.xephi.authme.OfflinePlayerWrapper;
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

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static fr.xephi.authme.util.OptionalUtils.handleOptional;
import static java.util.concurrent.CompletableFuture.completedFuture;

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

    @Override
    public PermissionsSystemType getPermissionSystem() {
        return PermissionsSystemType.LUCK_PERMS;
    }

    @Override
    public boolean hasGroupSupport() {
        return true;
    }

    private CompletableFuture<User> getUser(OfflinePlayerWrapper player) {
        if (player.isOnline()) { //FIXME: how to get this? :/
            return completedFuture(luckPermsApi.getUser(player.getUniqueId()));
        }
        return luckPermsApi.getUserManager().loadUser(player.getUniqueId(), player.getName());
    }

    private Optional<Group> getGroup(String groupName) {
        return luckPermsApi.getGroupSafe(groupName);
    }

    private Node getGroupNode(Group group) {
        return luckPermsApi.getNodeFactory().makeGroupNode(group).build();
    }

    private void saveUser(User user) {
        luckPermsApi.getUserManager().saveUser(user); // Async, handled by LuckPerms
    }

    @Override
    public CompletableFuture<Boolean> addToGroup(OfflinePlayerWrapper player, String groupName) {
        return handleOptional(getGroup(groupName),
            group -> getUser(player).thenApply(user -> {
                if (user.setPermission(getGroupNode(group)).wasFailure()) {
                    return false;
                }
                saveUser(user);
                return true;
            }),
            () -> completedFuture(false)
        );
    }

    @Override
    public CompletableFuture<Boolean> hasPermissionOffline(OfflinePlayerWrapper player, PermissionNode node) {
        return getUser(player).thenApply(user -> {
            UserData userData = user.getCachedData();
            PermissionData permissionData = userData.getPermissionData(Contexts.allowAll());
            return permissionData.getPermissionValue(node.getNode()).asBoolean();
        });
    }

    @Override
    public CompletableFuture<Boolean> isInGroup(OfflinePlayerWrapper player, String groupName) {
        return handleOptional(luckPermsApi.getGroupSafe(groupName),
            group -> getUser(player).thenApply(user -> user.inheritsGroup(group)),
            () -> completedFuture(false)
        );
    }

    @Override
    public CompletableFuture<Boolean> removeFromGroup(OfflinePlayerWrapper player, String groupName) {
        return handleOptional(getGroup(groupName),
            group -> getUser(player).thenApply(user -> {
                if (user.unsetPermission(getGroupNode(group)).wasFailure()) {
                    return false;
                }
                saveUser(user);
                return true;
            }),
            () -> completedFuture(false)
        );
    }

    @Override
    public CompletableFuture<Boolean> setGroup(OfflinePlayerWrapper player, String groupName) {
        return handleOptional(getGroup(groupName),
            group -> getUser(player).thenApply(user -> {
                if (user.setPermission(getGroupNode(group)) == DataMutateResult.FAIL) {
                    return false;
                }
                user.clearMatching(node -> node.isGroupNode() && !node.getGroupName().equals(group.getName()));
                saveUser(user);
                return true;
            }),
            () -> completedFuture(false)
        );
    }

    @Override
    public CompletableFuture<List<String>> getGroups(OfflinePlayerWrapper player) {
        return getUser(player).thenApply(user -> user.getOwnNodes().stream()
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
            .collect(Collectors.toList()));
    }

}
