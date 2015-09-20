package fr.xephi.authme.listener;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.Utils;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;
import org.bukkit.inventory.ItemStack;

public class AuthMeEntityListener implements Listener {

    public AuthMe instance;

    public AuthMeEntityListener(AuthMe instance) {
        this.instance = instance;
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
        for (ItemStack item : player.getInventory().getArmorContents()) {
            if (item != null) {
                Utils.fixDurability(item);
            }
        }
        player.setFireTicks(0);
        event.setDamage(0.0);
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

        Utils.fixDurability(player.getItemInHand());
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

        event.setAmount(0.0);
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

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        Entity entity = (Entity) event.getEntity().getShooter();
        if (entity == null || !(entity instanceof Player)) {
            return;
        }

        if (Utils.checkAuth((Player) entity)) {
            return;
        }

        event.setCancelled(true);
    }

    @EventHandler
    public void onShoot(EntityShootBowEvent event) {
        Entity entity = event.getEntity();
        if (entity == null || !(entity instanceof Player)) {
            return;
        }

        Player player = (Player) entity;
        if (Utils.checkAuth(player)) {
            return;
        }

        Utils.fixDurability(player.getItemInHand());
        event.setCancelled(true);
    }

}
