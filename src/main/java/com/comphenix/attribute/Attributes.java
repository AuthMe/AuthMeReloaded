package com.comphenix.attribute;

import java.util.Collections;
import java.util.Iterator;
import java.util.UUID;
import java.util.concurrent.ConcurrentMap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.bukkit.inventory.ItemStack;

import com.comphenix.attribute.NbtFactory.NbtCompound;
import com.comphenix.attribute.NbtFactory.NbtList;
import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.collect.Iterators;
import com.google.common.collect.Maps;

public class Attributes {

    public enum Operation {
        ADD_NUMBER(0),
        MULTIPLY_PERCENTAGE(1),
        ADD_PERCENTAGE(2);

        private int id;

        private Operation(int id) {
            this.id = id;
        }

        public int getId() {
            return id;
        }

        public static Operation fromId(int id) {
            // Linear scan is very fast for small N
            for (Operation op : values()) {
                if (op.getId() == id) {
                    return op;
                }
            }
            throw new IllegalArgumentException("Corrupt operation ID " + id + " detected.");
        }
    }

    public static class AttributeType {

        private static ConcurrentMap<String, AttributeType> LOOKUP = Maps.newConcurrentMap();
        public static final AttributeType GENERIC_MAX_HEALTH = new AttributeType("generic.maxHealth").register();
        public static final AttributeType GENERIC_FOLLOW_RANGE = new AttributeType("generic.followRange").register();
        public static final AttributeType GENERIC_ATTACK_DAMAGE = new AttributeType("generic.attackDamage").register();
        public static final AttributeType GENERIC_MOVEMENT_SPEED = new AttributeType("generic.movementSpeed").register();
        public static final AttributeType GENERIC_KNOCKBACK_RESISTANCE = new AttributeType("generic.knockbackResistance").register();

        private final String minecraftId;

        /**
         * Construct a new attribute type.
         * <p>
         * Remember to {@link #register()} the type.
         * 
         * @param minecraftId
         *            - the ID of the type.
         */
        public AttributeType(String minecraftId) {
            this.minecraftId = minecraftId;
        }

        /**
         * Retrieve the associated minecraft ID.
         * 
         * @return The associated ID.
         */
        public String getMinecraftId() {
            return minecraftId;
        }

        /**
         * Register the type in the central registry.
         * 
         * @return The registered type.
         */
        // Constructors should have no side-effects!
        public AttributeType register() {
            AttributeType old = LOOKUP.putIfAbsent(minecraftId, this);
            return old != null ? old : this;
        }

        /**
         * Retrieve the attribute type associated with a given ID.
         * 
         * @param minecraftId
         *            The ID to search for.
         * @return The attribute type, or NULL if not found.
         */
        public static AttributeType fromId(String minecraftId) {
            return LOOKUP.get(minecraftId);
        }

        /**
         * Retrieve every registered attribute type.
         * 
         * @return Every type.
         */
        public static Iterable<AttributeType> values() {
            return LOOKUP.values();
        }
    }

    public static class Attribute {

        private NbtCompound data;

        public Attribute(Builder builder) {
            data = NbtFactory.createCompound();
            setAmount(builder.amount);
            setOperation(builder.operation);
            setAttributeType(builder.type);
            setName(builder.name);
            setUUID(builder.uuid);
        }

        private Attribute(NbtCompound data) {
            this.data = data;
        }

        public double getAmount() {
            return data.getDouble("Amount", 0.0);
        }

        public void setAmount(double amount) {
            data.put("Amount", amount);
        }

        public Operation getOperation() {
            return Operation.fromId(data.getInteger("Operation", 0));
        }

        public void setOperation(@Nonnull Operation operation) {
            Preconditions.checkNotNull(operation, "operation cannot be NULL.");
            data.put("Operation", operation.getId());
        }

        public AttributeType getAttributeType() {
            return AttributeType.fromId(data.getString("AttributeName", null));
        }

        public void setAttributeType(@Nonnull AttributeType type) {
            Preconditions.checkNotNull(type, "type cannot be NULL.");
            data.put("AttributeName", type.getMinecraftId());
        }

        public String getName() {
            return data.getString("Name", null);
        }

        public void setName(@Nonnull String name) {
            Preconditions.checkNotNull(name, "name cannot be NULL.");
            data.put("Name", name);
        }

        public UUID getUUID() {
            return new UUID(data.getLong("UUIDMost", null), data.getLong("UUIDLeast", null));
        }

        public void setUUID(@Nonnull UUID id) {
            Preconditions.checkNotNull("id", "id cannot be NULL.");
            data.put("UUIDLeast", id.getLeastSignificantBits());
            data.put("UUIDMost", id.getMostSignificantBits());
        }

        /**
         * Construct a new attribute builder with a random UUID and default
         * operation of adding numbers.
         * 
         * @return The attribute builder.
         */
        public static Builder newBuilder() {
            return new Builder().uuid(UUID.randomUUID()).operation(Operation.ADD_NUMBER);
        }

        // Makes it easier to construct an attribute
        public static class Builder {

            private double amount;
            private Operation operation = Operation.ADD_NUMBER;
            private AttributeType type;
            private String name;
            private UUID uuid;

            private Builder() {
                // Don't make this accessible
            }

            public Builder(double amount, Operation operation,
                    AttributeType type, String name, UUID uuid) {
                this.amount = amount;
                this.operation = operation;
                this.type = type;
                this.name = name;
                this.uuid = uuid;
            }

            public Builder amount(double amount) {
                this.amount = amount;
                return this;
            }

            public Builder operation(Operation operation) {
                this.operation = operation;
                return this;
            }

            public Builder type(AttributeType type) {
                this.type = type;
                return this;
            }

            public Builder name(String name) {
                this.name = name;
                return this;
            }

            public Builder uuid(UUID uuid) {
                this.uuid = uuid;
                return this;
            }

            public Attribute build() {
                return new Attribute(this);
            }
        }
    }

    // This may be modified
    public ItemStack stack;
    private NbtList attributes;

    public Attributes(ItemStack stack) {
        // Create a CraftItemStack (under the hood)
        this.stack = NbtFactory.getCraftItemStack(stack);
        loadAttributes(false);
    }

    /**
     * Load the NBT list from the TAG compound.
     * 
     * @param createIfMissing
     *            - create the list if its missing.
     */
    private void loadAttributes(boolean createIfMissing) {
        if (this.attributes == null) {
            NbtCompound nbt = NbtFactory.fromItemTag(this.stack);
            this.attributes = nbt.getList("AttributeModifiers", createIfMissing);
        }
    }

    /**
     * Remove the NBT list from the TAG compound.
     */
    private void removeAttributes() {
        NbtCompound nbt = NbtFactory.fromItemTag(this.stack);
        nbt.remove("AttributeModifiers");
        this.attributes = null;
    }

    /**
     * Retrieve the modified item stack.
     * 
     * @return The modified item stack.
     */
    public ItemStack getStack() {
        return stack;
    }

    /**
     * Retrieve the number of attributes.
     * 
     * @return Number of attributes.
     */
    public int size() {
        return attributes != null ? attributes.size() : 0;
    }

    /**
     * Add a new attribute to the list.
     * 
     * @param attribute
     *            - the new attribute.
     */
    public void add(Attribute attribute) {
        Preconditions.checkNotNull(attribute.getName(), "must specify an attribute name.");
        loadAttributes(true);
        attributes.add(attribute.data);
    }

    /**
     * Remove the first instance of the given attribute.
     * <p>
     * The attribute will be removed using its UUID.
     * 
     * @param attribute
     *            - the attribute to remove.
     * @return TRUE if the attribute was removed, FALSE otherwise.
     */
    public boolean remove(Attribute attribute) {
        if (attributes == null)
            return false;
        UUID uuid = attribute.getUUID();

        for (Iterator<Attribute> it = values().iterator(); it.hasNext();) {
            if (Objects.equal(it.next().getUUID(), uuid)) {
                it.remove();

                // Last removed attribute?
                if (size() == 0) {
                    removeAttributes();
                }
                return true;
            }
        }
        return false;
    }

    /**
     * Remove every attribute.
     */
    public void clear() {
        removeAttributes();
    }

    /**
     * Retrieve the attribute at a given index.
     * 
     * @param index
     *            - the index to look up.
     * @return The attribute at that index.
     */
    public Attribute get(int index) {
        if (size() == 0)
            throw new IllegalStateException("Attribute list is empty.");
        return new Attribute((NbtCompound) attributes.get(index));
    }

    // We can't make Attributes itself iterable without splitting it up into
    // separate classes
    public Iterable<Attribute> values() {
        return new Iterable<Attribute>() {

            @Override
            public Iterator<Attribute> iterator() {
                // Handle the empty case
                if (size() == 0)
                    return Collections.<Attribute> emptyList().iterator();

                return Iterators.transform(attributes.iterator(), new Function<Object, Attribute>() {

                    @Override
                    public Attribute apply(@Nullable Object element) {
                        return new Attribute((NbtCompound) element);
                    }
                });
            }
        };
    }
}
