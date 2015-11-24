package fr.xephi.authme.listener;

import java.lang.reflect.Method;

import org.bukkit.entity.Entity;
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

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.util.Utils;

public class AuthMeEntityListener implements Listener {

    public AuthMe instance;
    private static Method getShooter;
    private static boolean shooterIsProjectileSource;

    public AuthMeEntityListener(AuthMe instance) {
        this.instance = instance;
        try {
            Method m = Projectile.class.getDeclaredMethod("getShooter");
            shooterIsProjectileSource = m.getReturnType() != LivingEntity.class;
        } catch (Exception ignored) {
        }
    }

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
