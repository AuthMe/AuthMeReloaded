/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 * 
 * http://forums.bukkit.org/threads/cardboard-serializable-itemstack-with-enchantments.75768/
 */
package uk.org.whoami.authme.debug;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;

public class CardboardBox implements Serializable {
    private static final long serialVersionUID = 729890133797629668L;
    private final int type, amount;
    private final short damage;
    private final HashMap<CardboardEnchantment, Integer> enchants;

    public CardboardBox(ItemStack item) {
        this.type = item.getTypeId();
        this.amount = item.getAmount();
        this.damage = item.getDurability();

        HashMap<CardboardEnchantment, Integer> map = new HashMap<CardboardEnchantment, Integer>();

        Map<Enchantment, Integer> enchantments = item.getEnchantments();

        for(Enchantment enchantment : enchantments.keySet()) {
            map.put(new CardboardEnchantment(enchantment), enchantments.get(enchantment));
        }

        this.enchants = map;
    }

    public ItemStack unbox() {
        ItemStack item = new ItemStack(type, amount, damage);

        HashMap<Enchantment, Integer> map = new HashMap<Enchantment, Integer>();

        for(CardboardEnchantment cEnchantment : enchants.keySet()) {
            map.put(cEnchantment.unbox(), enchants.get(cEnchantment));
        }

        item.addUnsafeEnchantments(map);

        return item;
    }
}