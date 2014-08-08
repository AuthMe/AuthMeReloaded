package fr.xephi.authme.cache.backup;

import org.bukkit.inventory.ItemStack;

public class DataFileCache {

    private ItemStack[] inventory;
    private ItemStack[] armor;
    private String group;
    private boolean operator;
    private boolean flying;

    public DataFileCache(ItemStack[] inventory, ItemStack[] armor) {
        this.inventory = inventory;
        this.armor = armor;
    }

    public DataFileCache(ItemStack[] inventory, ItemStack[] armor,
            String group, boolean operator, boolean flying) {
        this.inventory = inventory;
        this.armor = armor;
        this.group = group;
        this.operator = operator;
        this.flying = flying;
    }

    public ItemStack[] getInventory() {
        return inventory;
    }

    public ItemStack[] getArmour() {
        return armor;
    }

    public String getGroup() {
        return group;
    }

    public boolean getOperator() {
        return operator;
    }

    public boolean isFlying() {
        return flying;
    }
}
