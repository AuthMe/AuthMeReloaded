package fr.xephi.authme.listener;

import fr.xephi.authme.ConsoleLogger;

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

import javax.inject.Inject;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class EntityListener implements Listener {

    private final ListenerService listenerService;
    private Method getShooter;
    private boolean shooterIsLivingEntity;

    @Inject
    EntityListener(ListenerService listenerService) {
        this.listenerService = listenerService;
        try {
            getShooter = Projectile.class.getDeclaredMethod("getShooter");
            shooterIsLivingEntity = getShooter.getReturnType() == LivingEntity.class;
        } catch (NoSuchMethodException | SecurityException e) {
            ConsoleLogger.logException("Cannot load getShooter() method on Projectile class", e);
        }
    }

    // Note #360: npc status can be used to bypass security!!!
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onDamage(EntityDamageEvent event) {
        if (listenerService.shouldCancelEvent(event)) {
            event.getEntity().setFireTicks(0);
            event.setDamage(0);
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onAttack(EntityDamageByEntityEvent event) {
        if (listenerService.shouldCancelEvent(event.getDamager())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onEntityTarget(EntityTargetEvent event) {
        if (listenerService.shouldCancelEvent(event)) {
            event.setTarget(null);
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        if (listenerService.shouldCancelEvent(event)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void entityRegainHealthEvent(EntityRegainHealthEvent event) {
        if (listenerService.shouldCancelEvent(event)) {
            event.setAmount(0);
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onEntityInteract(EntityInteractEvent event) {
        if (listenerService.shouldCancelEvent(event)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onLowestEntityInteract(EntityInteractEvent event) {
        if (listenerService.shouldCancelEvent(event)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        if (event.getEntity() == null) {
            return;
        }

        Projectile projectile = event.getEntity();
        // In the Bukkit API prior to 1.7, getShooter() returns a LivingEntity instead of a ProjectileSource
        Object shooterRaw = null;
        if (shooterIsLivingEntity) {
            try {
                if (getShooter == null) {
                    getShooter = Projectile.class.getMethod("getShooter");
                }
                shooterRaw = getShooter.invoke(projectile);
            } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
                ConsoleLogger.logException("Error getting shooter", e);
            }
        } else {
            shooterRaw = projectile.getShooter();
        }
        if (shooterRaw instanceof Player && listenerService.shouldCancelEvent((Player) shooterRaw)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.NORMAL)
    public void onShoot(EntityShootBowEvent event) {
        if (listenerService.shouldCancelEvent(event)) {
            event.setCancelled(true);
        }
    }

}
