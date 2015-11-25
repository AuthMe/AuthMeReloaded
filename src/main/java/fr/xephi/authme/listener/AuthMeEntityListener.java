package fr.xephi.authme.listener;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.util.Utils;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;
import org.bukkit.projectiles.ProjectileSource;

import java.lang.reflect.Method;

/**
 */
public class AuthMeEntityListener implements Listener {

    private static Method getShooter;
    private static boolean shooterIsProjectileSource;
    public final AuthMe instance;

    /**
     * Constructor for AuthMeEntityListener.
     *
     * @param instance AuthMe
     */
    public AuthMeEntityListener(AuthMe instance) {
        this.instance = instance;
        try {
            Method m = Projectile.class.getDeclaredMethod("getShooter");
            shooterIsProjectileSource = m.getReturnType() != LivingEntity.class;
        } catch (Exception ignored) {
        }
    }

    /**
     * Method onEntityDamage.
     *
     * @param event EntityDamageEvent
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onEntityDamage(EntityDamageEvent event) {
        Entity entity = event.getEntity();
        if (entity == null || !(entity instanceof Player)) {
            return;
        }

        Player player = (Player) entity;
        if (Utils.checkAuth(player)) {
            return;
        }
        player.setFireTicks(0);
        event.setDamage(0);
        event.setCancelled(true);
    }

    /**
     * Method onEntityTarget.
     *
     * @param event EntityTargetEvent
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onEntityTarget(EntityTargetEvent event) {
        Entity entity = event.getTarget();
        if (entity == null || !(entity instanceof Player)) {
            return;
        }

        if (Utils.checkAuth((Player) entity)) {
            return;
        }

        event.setTarget(null);
        event.setCancelled(true);
    }

    /**
     * Method onDmg.
     *
     * @param event EntityDamageByEntityEvent
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onDmg(EntityDamageByEntityEvent event) {
        Entity entity = event.getDamager();
        if (entity == null || !(entity instanceof Player)) {
            return;
        }

        Player player = (Player) entity;
        if (Utils.checkAuth(player)) {
            return;
        }

        event.setCancelled(true);
    }

    /**
     * Method onFoodLevelChange.
     *
     * @param event FoodLevelChangeEvent
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        Entity entity = event.getEntity();
        if (entity == null || !(entity instanceof Player)) {
            return;
        }

        if (Utils.checkAuth((Player) entity)) {
            return;
        }

        event.setCancelled(true);
    }

    /**
     * Method entityRegainHealthEvent.
     *
     * @param event EntityRegainHealthEvent
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void entityRegainHealthEvent(EntityRegainHealthEvent event) {
        Entity entity = event.getEntity();
        if (entity == null || !(entity instanceof Player)) {
            return;
        }

        if (Utils.checkAuth((Player) entity)) {
            return;
        }

        event.setAmount(0);
        event.setCancelled(true);
    }

    /**
     * Method onEntityInteract.
     *
     * @param event EntityInteractEvent
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onEntityInteract(EntityInteractEvent event) {
        Entity entity = event.getEntity();
        if (entity == null || !(entity instanceof Player)) {
            return;
        }

        if (Utils.checkAuth((Player) entity)) {
            return;
        }

        event.setCancelled(true);
    }

    /**
     * Method onLowestEntityInteract.
     *
     * @param event EntityInteractEvent
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onLowestEntityInteract(EntityInteractEvent event) {
        Entity entity = event.getEntity();
        if (entity == null || !(entity instanceof Player)) {
            return;
        }

        if (Utils.checkAuth((Player) entity)) {
            return;
        }

        event.setCancelled(true);
    }

    // TODO: Need to check this, player can't throw snowball but the item is taken.

    /**
     * Method onProjectileLaunch.
     *
     * @param event ProjectileLaunchEvent
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        Projectile projectile = event.getEntity();
        Player player = null;
        if (projectile == null) {
            return;
        }

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
                Object obj = getShooter.invoke(null);
                player = (Player) obj;
            } catch (Exception ignored) {
            }
        }

        if (Utils.checkAuth(player)) {
            return;
        }

        event.setCancelled(true);
    }

    /**
     * Method onShoot.
     *
     * @param event EntityShootBowEvent
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.NORMAL)
    public void onShoot(EntityShootBowEvent event) {
        Entity entity = event.getEntity();
        if (entity == null || !(entity instanceof Player)) {
            return;
        }

        Player player = (Player) entity;
        if (Utils.checkAuth(player)) {
            return;
        }

        event.setCancelled(true);
    }

}
