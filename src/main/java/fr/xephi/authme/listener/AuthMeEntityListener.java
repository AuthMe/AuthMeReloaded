package fr.xephi.authme.listener;

import org.bukkit.entity.LivingEntity;
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

import fr.xephi.authme.ConsoleLogger;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static fr.xephi.authme.listener.ListenerService.shouldCancelEvent;

public class AuthMeEntityListener implements Listener {

    private Method getShooter;
    private boolean shooterIsProjectileSource;

    public AuthMeEntityListener() {
        try {
            getShooter = Projectile.class.getDeclaredMethod("getShooter");
            shooterIsProjectileSource = getShooter.getReturnType() != LivingEntity.class;
        } catch (NoSuchMethodException | SecurityException e) {
            ConsoleLogger.logException("Cannot load getShooter() method on Projectile class", e);
        }
    }

    // Note #360: npc status can be used to bypass security!!!
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

    // TODO #733: Player can't throw snowball but the item is taken.
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        if (event.getEntity() == null) {
            return;
        }

        Player player = null;
        Projectile projectile = event.getEntity();
        // In old versions of the Bukkit API getShooter() returns a Player object instead of a ProjectileSource
        if (shooterIsProjectileSource) {
            ProjectileSource shooter = projectile.getShooter();
            if (shooter == null || !(shooter instanceof Player)) {
                return;
            }
            player = (Player) shooter;
        } else {
            try {
                if (getShooter == null) {
                    getShooter = Projectile.class.getMethod("getShooter");
                }
                Object obj = getShooter.invoke(projectile);
                player = (Player) obj;
            } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
                ConsoleLogger.logException("Error getting shooter", e);
            }
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
