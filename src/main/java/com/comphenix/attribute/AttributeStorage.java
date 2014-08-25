package com.comphenix.attribute;

import java.util.UUID;

import org.bukkit.inventory.ItemStack;

import com.comphenix.attribute.Attributes.Attribute;
import com.comphenix.attribute.Attributes.AttributeType;
import com.comphenix.attribute.Attributes.Operation;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;

/**
 * Store meta-data in an ItemStack as attributes.
 * 
 * @author Kristian
 */
public class AttributeStorage {

    private ItemStack target;
    private final UUID uniqueKey;

    private AttributeStorage(ItemStack target, UUID uniqueKey) {
        this.target = Preconditions.checkNotNull(target, "target cannot be NULL");
        this.uniqueKey = Preconditions.checkNotNull(uniqueKey, "uniqueKey cannot be NULL");
    }

    /**
     * Construct a new attribute storage system.
     * <p>
     * The key must be the same in order to retrieve the same data.
     * 
     * @param target
     *            - the item stack where the data will be stored.
     * @param uniqueKey
     *            - the unique key used to retrieve the correct data.
     */
    public static AttributeStorage newTarget(ItemStack target, UUID uniqueKey) {
        return new AttributeStorage(target, uniqueKey);
    }

    /**
     * Retrieve the data stored in the item's attribute.
     * 
     * @param defaultValue
     *            - the default value to return if no data can be found.
     * @return The stored data, or defaultValue if not found.
     */
    public String getData(String defaultValue) {
        Attribute current = getAttribute(new Attributes(target), uniqueKey);
        return current != null ? current.getName() : defaultValue;
    }

    /**
     * Determine if we are storing any data.
     * 
     * @return TRUE if we are, FALSE otherwise.
     */
    public boolean hasData() {
        return getAttribute(new Attributes(target), uniqueKey) != null;
    }

    /**
     * Set the data stored in the attributes.
     * 
     * @param data
     *            - the data.
     */
    public void setData(String data) {
        Attributes attributes = new Attributes(target);
        Attribute current = getAttribute(attributes, uniqueKey);

        if (current == null) {
            attributes.add(Attribute.newBuilder().name(data).amount(getBaseDamage(target)).uuid(uniqueKey).operation(Operation.ADD_NUMBER).type(AttributeType.GENERIC_ATTACK_DAMAGE).build());
        } else {
            current.setName(data);
        }
        this.target = attributes.getStack();
    }

    /**
     * Retrieve the base damage of the given item.
     * 
     * @param stack
     *            - the stack.
     * @return The base damage.
     */
    private int getBaseDamage(ItemStack stack) {
        // Yes - we have to hard code these values. Cannot use
        // Operation.ADD_PERCENTAGE either.
        switch (stack.getType()) {
            case WOOD_SWORD:
                return 4;
            case GOLD_SWORD:
                return 4;
            case STONE_SWORD:
                return 5;
            case IRON_SWORD:
                return 6;
            case DIAMOND_SWORD:
                return 7;

            case WOOD_AXE:
                return 3;
            case GOLD_AXE:
                return 3;
            case STONE_AXE:
                return 4;
            case IRON_AXE:
                return 5;
            case DIAMOND_AXE:
                return 6;
            default:
                return 0;
        }
    }

    /**
     * Retrieve the target stack. May have been changed.
     * 
     * @return The target stack.
     */
    public ItemStack getTarget() {
        return target;
    }

    /**
     * Retrieve an attribute by UUID.
     * 
     * @param attributes
     *            - the attribute.
     * @param id
     *            - the UUID to search for.
     * @return The first attribute associated with this UUID, or NULL.
     */
    private Attribute getAttribute(Attributes attributes, UUID id) {
        for (Attribute attribute : attributes.values()) {
            if (Objects.equal(attribute.getUUID(), id)) {
                return attribute;
            }
        }
        return null;
    }
}
