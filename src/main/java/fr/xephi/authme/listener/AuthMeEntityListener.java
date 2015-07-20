package fr.xephi.authme.listener;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityInteractEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.Utils;
import fr.xephi.authme.cache.auth.PlayerCache;
import fr.xephi.authme.plugin.manager.CombatTagComunicator;
import fr.xephi.authme.settings.Settings;

public class AuthMeEntityListener implements Listener {

    public AuthMe instance;

    public AuthMeEntityListener(AuthMe instance) {
        this.instance = instance;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onEntityDamage(EntityDamageEvent event) {
        Entity entity = event.getEntity();

        if (!(entity instanceof Player)) {
            return;
        }

        if (Utils.getInstance().isUnrestricted((Player) entity)) {
            return;
        }

        if (instance.citizens.isNPC(entity))
            return;

        Player player = (Player) entity;
        String name = player.getName().toLowerCase();

        if (CombatTagComunicator.isNPC(player))
            return;

        if (PlayerCache.getInstance().isAuthenticated(name)) {
            return;
        }

        if (!instance.database.isAuthAvailable(name)) {
            if (!Settings.isForcedRegistrationEnabled) {
                return;
            }
        }
        player.setFireTicks(0);
        event.setDamage(0.0);
        event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onEntityTarget(EntityTargetEvent event) {
        if (event.getTarget() == null)
            return;
        Entity entity = event.getTarget();
        if (!(entity instanceof Player)) {
            return;
        }

        if (instance.citizens.isNPC(entity))
            return;

        Player player = (Player) entity;
        String name = player.getName().toLowerCase();

        if (PlayerCache.getInstance().isAuthenticated(name)) {
            return;
        }

        if (!instance.database.isAuthAvailable(name)) {
            if (!Settings.isForcedRegistrationEnabled) {
                return;
            }
        }
        event.setTarget(null);
        event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onDmg(EntityDamageByEntityEvent event) {
        Entity entity = event.getDamager();

        if (entity == null || !(entity instanceof Player)) {
            return;
        }

        Player player = (Player) entity;
        String name = player.getName().toLowerCase();

        if (PlayerCache.getInstance().isAuthenticated(name)) {
            return;
        }

        if (!instance.database.isAuthAvailable(name)) {
            if (!Settings.isForcedRegistrationEnabled) {
                return;
            }
        }

        event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof Player)) {
            return;
        }

        if (instance.citizens.isNPC(entity))
            return;

        Player player = (Player) entity;
        String name = player.getName().toLowerCase();

        if (PlayerCache.getInstance().isAuthenticated(name)) {
            return;
        }

        if (!instance.database.isAuthAvailable(name)) {
            if (!Settings.isForcedRegistrationEnabled) {
                return;
            }
        }

        event.setCancelled(true);

    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void EntityRegainHealthEvent(EntityRegainHealthEvent event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof Player)) {
            return;
        }

        if (instance.citizens.isNPC(entity))
            return;

        Player player = (Player) entity;
        String name = player.getName().toLowerCase();

        if (PlayerCache.getInstance().isAuthenticated(name)) {
            return;
        }

        if (!instance.database.isAuthAvailable(name)) {
            if (!Settings.isForcedRegistrationEnabled) {
                return;
            }
        }

        event.setAmount(0.0);
        event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onEntityInteract(EntityInteractEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getEntity();
        String name = player.getName().toLowerCase();

        if (Utils.getInstance().isUnrestricted(player) || CombatTagComunicator.isNPC(player)) {
            return;
        }

        if (instance.citizens.isNPC(player))
            return;

        if (PlayerCache.getInstance().isAuthenticated(player.getName())) {
            return;
        }

        if (!instance.database.isAuthAvailable(name)) {
            if (!Settings.isForcedRegistrationEnabled) {
                return;
            }
        }
        event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onLowestEntityInteract(EntityInteractEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getEntity();
        String name = player.getName().toLowerCase();

        if (Utils.getInstance().isUnrestricted(player) || CombatTagComunicator.isNPC(player)) {
            return;
        }

        if (instance.citizens.isNPC(player))
            return;

        if (PlayerCache.getInstance().isAuthenticated(player.getName())) {
            return;
        }

        if (!instance.database.isAuthAvailable(name)) {
            if (!Settings.isForcedRegistrationEnabled) {
                return;
            }
        }
        event.setCancelled(true);
    }
}
