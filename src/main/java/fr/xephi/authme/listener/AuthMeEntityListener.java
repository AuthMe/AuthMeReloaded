package fr.xephi.authme.listener;

import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityInteractEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.projectiles.ProjectileSource;

import static fr.xephi.authme.listener.ListenerService.shouldCancelEvent;

public class AuthMeEntityListener implements Listener {

    public AuthMeEntityListener() {}

    // TODO #360: npc status can be used to bypass security!!!
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onEntityDamage(EntityDamageEvent event) {
        if (shouldCancelEvent(event)) {
            event.getEntity().setFireTicks(0);
            event.setDamage(0);
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onEntityTarget(EntityTargetEvent event) {
        if (shouldCancelEvent(event)) {
            event.setTarget(null);
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (shouldCancelEvent(event)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        if (shouldCancelEvent(event)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void entityRegainHealthEvent(EntityRegainHealthEvent event) {
        if (shouldCancelEvent(event)) {
            event.setAmount(0);
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onEntityInteract(EntityInteractEvent event) {
        if (shouldCancelEvent(event)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onLowestEntityInteract(EntityInteractEvent event) {
        if (shouldCancelEvent(event)) {
            event.setCancelled(true);
        }
    }

    // TODO: Need to check this, player can't throw snowball but the item is taken.
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        if (event.getEntity() == null) {
            return;
        }

        Player player = null;
        Projectile projectile = event.getEntity();
        if (projectile != null && (projectile.getShooter() instanceof ProjectileSource)) {
            ProjectileSource shooter = projectile.getShooter();
            if (shooter == null || !(shooter instanceof Player)) {
                return;
            }
            player = (Player) shooter;
        }

        if (ListenerService.shouldCancelEvent(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.NORMAL)
    public void onShoot(EntityShootBowEvent event) {
        if (shouldCancelEvent(event)) {
            event.setCancelled(true);
        }
    }

}
