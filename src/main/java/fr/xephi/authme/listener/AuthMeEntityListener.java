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

import java.lang.reflect.Method;

import static fr.xephi.authme.listener.ListenerService.shouldCancelEvent;

public class AuthMeEntityListener implements Listener {

    private static Method getShooter;
    private static boolean shooterIsProjectileSource;

    public AuthMeEntityListener() {
        try {
            Method m = Projectile.class.getDeclaredMethod("getShooter");
            shooterIsProjectileSource = m.getReturnType() != LivingEntity.class;
        } catch (Exception ignored) {
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

    // TODO #568: Need to check this, player can't throw snowball but the item is taken.
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        if (event.getEntity() == null) {
            return;
        }

        Player player = null;
        Projectile projectile = event.getEntity();
        if (shooterIsProjectileSource) {
            ProjectileSource shooter = projectile.getShooter();
            if (shooter == null || !(shooter instanceof Player)) {
                return;
            }
            player = (Player) shooter;
        } else {
            // TODO #568 20151220: Invoking getShooter() with null but method isn't static
            try {
                if (getShooter == null) {
                    getShooter = Projectile.class.getMethod("getShooter");
                }
                Object obj = getShooter.invoke(null);
                player = (Player) obj;
            } catch (Exception ignored) {
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
