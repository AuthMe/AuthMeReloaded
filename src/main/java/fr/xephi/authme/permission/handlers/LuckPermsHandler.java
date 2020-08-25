package fr.xephi.authme.permission.handlers;

import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.output.ConsoleLoggerFactory;
import fr.xephi.authme.permission.PermissionNode;
import fr.xephi.authme.permission.PermissionsSystemType;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.cacheddata.CachedPermissionData;
import net.luckperms.api.model.data.DataMutateResult;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.NodeEqualityPredicate;
import net.luckperms.api.node.types.InheritanceNode;
import net.luckperms.api.query.QueryMode;
import net.luckperms.api.query.QueryOptions;
import org.bukkit.OfflinePlayer;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
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

    private final ConsoleLogger logger = ConsoleLoggerFactory.get(LuckPermsHandler.class);
    private LuckPerms luckPerms;

    public LuckPermsHandler() throws PermissionHandlerException {
        try {
            luckPerms = LuckPermsProvider.get();
        } catch (IllegalStateException e) {
            throw new PermissionHandlerException("Could not get api of LuckPerms", e);
        }
    }

    @Override
    public boolean addToGroup(OfflinePlayer player, String group) {
        Group newGroup = luckPerms.getGroupManager().getGroup(group);
        if (newGroup == null) {
            return false;
        }

        String playerName = player.getName();
        if (playerName == null) {
            return false;
        }
        User user = luckPerms.getUserManager().getUser(playerName);
        if (user == null) {
            return false;
        }

        InheritanceNode node = InheritanceNode.builder(group).build();
        DataMutateResult result = user.data().add(node);
        if (result == DataMutateResult.FAIL) {
            return false;
        }

        luckPerms.getUserManager().saveUser(user);
        return true;
    }

    @Override
    public boolean hasGroupSupport() {
        return true;
    }

    @Override
    public boolean hasPermissionOffline(String name, PermissionNode node) {
        User user = luckPerms.getUserManager().getUser(name);
        if (user == null) {
            logger.warning("LuckPermsHandler: tried to check permission for offline user "
                + name + " but it isn't loaded!");
            return false;
        }

        CachedPermissionData permissionData = user.getCachedData()
            .getPermissionData(QueryOptions.builder(QueryMode.NON_CONTEXTUAL).build());
        return permissionData.checkPermission(node.getNode()).asBoolean();
    }

    @Override
    public boolean isInGroup(OfflinePlayer player, String group) {
        String playerName = player.getName();
        if (playerName == null) {
            return false;
        }
        User user = luckPerms.getUserManager().getUser(playerName);
        if (user == null) {
            logger.warning("LuckPermsHandler: tried to check group for offline user "
                + player.getName() + " but it isn't loaded!");
            return false;
        }

        InheritanceNode inheritanceNode = InheritanceNode.builder(group).build();
        return user.data().contains(inheritanceNode, NodeEqualityPredicate.EXACT).asBoolean();
    }

    @Override
    public boolean removeFromGroup(OfflinePlayer player, String group) {
        String playerName = player.getName();
        if (playerName == null) {
            return false;
        }
        User user = luckPerms.getUserManager().getUser(playerName);
        if (user == null) {
            logger.warning("LuckPermsHandler: tried to remove group for offline user "
                + player.getName() + " but it isn't loaded!");
            return false;
        }

        InheritanceNode groupNode = InheritanceNode.builder(group).build();
        boolean result = user.data().remove(groupNode) != DataMutateResult.FAIL;

        luckPerms.getUserManager().saveUser(user);
        return result;
    }

    @Override
    public boolean setGroup(OfflinePlayer player, String group) {
        String playerName = player.getName();
        if (playerName == null) {
            return false;
        }
        User user = luckPerms.getUserManager().getUser(playerName);
        if (user == null) {
            logger.warning("LuckPermsHandler: tried to set group for offline user "
                + player.getName() + " but it isn't loaded!");
            return false;
        }
        InheritanceNode groupNode = InheritanceNode.builder(group).build();
        DataMutateResult result = user.data().add(groupNode);
        if (result == DataMutateResult.FAIL) {
            return false;
        }
        user.data().clear(node -> {
            if (!(node instanceof InheritanceNode)) {
                return false;
            }
            InheritanceNode inheritanceNode = (InheritanceNode) node;
            return !inheritanceNode.equals(groupNode);
        });

        luckPerms.getUserManager().saveUser(user);
        return true;
    }

    @Override
    public List<String> getGroups(OfflinePlayer player) {
        String playerName = player.getName();
        if (playerName == null) {
            return Collections.emptyList();
        }
        User user = luckPerms.getUserManager().getUser(playerName);
        if (user == null) {
            logger.warning("LuckPermsHandler: tried to get groups for offline user "
                + player.getName() + " but it isn't loaded!");
            return Collections.emptyList();
        }

        return user.getDistinctNodes().stream()
            .filter(node -> node instanceof InheritanceNode)
            .map(node -> (InheritanceNode) node)
            .map(node -> luckPerms.getGroupManager().getGroup(node.getGroupName()))
            .filter(Objects::nonNull)
            .sorted((o1, o2) -> {
                if (o1.getName().equals(user.getPrimaryGroup()) || o2.getName().equals(user.getPrimaryGroup())) {
                    return o1.getName().equals(user.getPrimaryGroup()) ? 1 : -1;
                }

                int i = Integer.compare(o2.getWeight().orElse(0), o1.getWeight().orElse(0));
                return i != 0 ? i : o1.getName().compareToIgnoreCase(o2.getName());
            })
            .map(Group::getName)
            .collect(Collectors.toList());
    }

    @Override
    public PermissionsSystemType getPermissionSystem() {
        return PermissionsSystemType.LUCK_PERMS;
    }

    @Override
    public void loadUserData(UUID uuid) throws PermissionLoadUserException {
        try {
            luckPerms.getUserManager().loadUser(uuid).get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            throw new PermissionLoadUserException("Unable to load the permission data of the user " + uuid, e);
        }
    }

}
