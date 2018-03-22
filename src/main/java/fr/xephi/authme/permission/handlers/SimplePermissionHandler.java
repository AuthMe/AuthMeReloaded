package fr.xephi.authme.permission.handlers;

import fr.xephi.authme.listener.OfflinePlayerInfo;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Optional;

public interface SimplePermissionHandler extends PermissionHandler {

    @Override
    default List<String> getGroups(Player player) {
        return getGroupsOffline(OfflinePlayerInfo.fromPlayer(player)).join();
    }

    @Override
    default Optional<String> getPrimaryGroup(Player player) {
        return getPrimaryGroupOffline(OfflinePlayerInfo.fromPlayer(player)).join();
    }

    @Override
    default boolean isInGroup(Player player, String groupName) {
        return isInGroupOffline(OfflinePlayerInfo.fromPlayer(player), groupName).join();
    }

    @Override
    default boolean addToGroup(Player player, String groupName) {
        return addToGroupOffline(OfflinePlayerInfo.fromPlayer(player), groupName).join();
    }

    @Override
    default boolean removeFromGroup(Player player, String groupName) {
        return removeFromGroupOffline(OfflinePlayerInfo.fromPlayer(player), groupName).join();
    }

    @Override
    default boolean setGroup(Player player, String groupName) {
        return setGroupOffline(OfflinePlayerInfo.fromPlayer(player), groupName).join();
    }

}
