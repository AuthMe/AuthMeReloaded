package fr.xephi.authme.permission.handlers;

import fr.xephi.authme.listener.OfflinePlayerInfo;
import fr.xephi.authme.permission.PermissionNode;
import fr.xephi.authme.permission.PermissionsSystemType;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static java.util.concurrent.CompletableFuture.completedFuture;

/**
 * Handler for permissions via Vault.
 *
 * @see <a href="https://dev.bukkit.org/projects/vault">Vault Bukkit page</a>
 * @see <a href="https://github.com/milkbowl/Vault">Vault on Github</a>
 */
public class VaultHandler implements PermissionHandler {

    private Permission vaultProvider;

    public VaultHandler(Server server) throws PermissionHandlerException {
        this.vaultProvider = getVaultPermission(server);
    }

    private static Permission getVaultPermission(Server server) throws PermissionHandlerException {
        // Get the permissions provider service
        RegisteredServiceProvider<Permission> permissionProvider = server
            .getServicesManager().getRegistration(Permission.class);
        if (permissionProvider == null) {
            throw new PermissionHandlerException("Could not load permissions provider service");
        }

        // Get the Vault provider and make sure it's valid
        Permission vaultPerms = permissionProvider.getProvider();
        if (vaultPerms == null) {
            throw new PermissionHandlerException("Could not load Vault permissions provider");
        }
        return vaultPerms;
    }

    @Override
    public boolean hasGroupSupport() {
        return vaultProvider.hasGroupSupport();
    }

    @Override
    public CompletableFuture<Boolean> hasPermissionOffline(OfflinePlayerInfo offlineInfo, PermissionNode node) {
        return completedFuture(vaultProvider.playerHas((String) null, offlineInfo.getName(), node.getNode()));
    }

    @Override
    public boolean isInGroup(Player player, String groupName) {
        return vaultProvider.playerInGroup(null, player, groupName);
    }

    @Override
    public CompletableFuture<Boolean> isInGroupOffline(OfflinePlayerInfo offlineInfo, String groupName) {
        return completedFuture(vaultProvider.playerInGroup((String) null, offlineInfo.getName(), groupName));
    }

    @Override
    public boolean removeFromGroup(Player player, String groupName) {
        return vaultProvider.playerRemoveGroup(null, player, groupName);
    }

    @Override
    public CompletableFuture<Boolean> removeFromGroupOffline(OfflinePlayerInfo offlinePlayerInfo, String groupName) {
        return completedFuture(vaultProvider.playerRemoveGroup((String) null, offlinePlayerInfo.getName(), groupName));
    }

    @Override
    public boolean setGroup(Player player, String groupName) {
        getGroups(player).forEach(group -> removeFromGroup(player, group));
        return addToGroup(player, groupName);
    }

    @Override
    public CompletableFuture<Boolean> setGroupOffline(OfflinePlayerInfo offlineInfo, String groupName) {
        return getGroupsOffline(offlineInfo).thenApply(groups -> {
            groups.forEach(group -> removeFromGroupOffline(offlineInfo, group));
            return addToGroupOffline(offlineInfo, groupName).join();
        });
    }

    @Override
    public List<String> getGroups(Player player) {
        return Arrays.asList(vaultProvider.getPlayerGroups((String) null, player));
    }

    @Override
    public CompletableFuture<List<String>> getGroupsOffline(OfflinePlayerInfo offlineInfo) {
        return completedFuture(Arrays.asList(vaultProvider.getPlayerGroups((String) null, offlineInfo.getName())));
    }

    @Override
    public Optional<String> getPrimaryGroup(Player player) {
        return Optional.ofNullable(vaultProvider.getPrimaryGroup(null, player));
    }

    @Override
    public CompletableFuture<Optional<String>> getPrimaryGroupOffline(OfflinePlayerInfo offlineInfo) {
        return completedFuture(Optional.ofNullable(vaultProvider.getPrimaryGroup((String) null, offlineInfo.getName())));
    }

    @Override
    public boolean addToGroup(Player player, String groupName) {
        return vaultProvider.playerAddGroup(null, player, groupName);
    }

    @Override
    public CompletableFuture<Boolean> addToGroupOffline(OfflinePlayerInfo offlineInfo, String groupName) {
        return completedFuture(vaultProvider.playerAddGroup((String) null, offlineInfo.getName(), groupName));
    }

    @Override
    public PermissionsSystemType getPermissionSystem() {
        return PermissionsSystemType.VAULT;
    }
}
