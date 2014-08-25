package com.comphenix.attribute;

import java.io.BufferedInputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.AbstractList;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.inventory.ItemStack;

import com.google.common.base.Splitter;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Lists;
import com.google.common.collect.MapMaker;
import com.google.common.io.Closeables;
import com.google.common.io.Files;
import com.google.common.io.InputSupplier;
import com.google.common.io.OutputSupplier;
import com.google.common.primitives.Primitives;

public class NbtFactory {

    // Convert between NBT id and the equivalent class in java
    private static final BiMap<Integer, Class<?>> NBT_CLASS = HashBiMap.create();
    private static final BiMap<Integer, NbtType> NBT_ENUM = HashBiMap.create();

    /**
     * Whether or not to enable stream compression.
     * 
     * @author Kristian
     */
    public enum StreamOptions {
        NO_COMPRESSION,
        GZIP_COMPRESSION,
    }

    private enum NbtType {
        TAG_END(0, Void.class),
        TAG_BYTE(1, byte.class),
        TAG_SHORT(2, short.class),
        TAG_INT(3, int.class),
        TAG_LONG(4, long.class),
        TAG_FLOAT(5, float.class),
        TAG_DOUBLE(6, double.class),
        TAG_BYTE_ARRAY(7, byte[].class),
        TAG_INT_ARRAY(11, int[].class),
        TAG_STRING(8, String.class),
        TAG_LIST(9, List.class),
        TAG_COMPOUND(10, Map.class);

        // Unique NBT id
        public final int id;

        private NbtType(int id, Class<?> type) {
            this.id = id;
            NBT_CLASS.put(id, type);
            NBT_ENUM.put(id, this);
        }

        private String getFieldName() {
            if (this == TAG_COMPOUND)
                return "map";
            else if (this == TAG_LIST)
                return "list";
            else return "data";
        }
    }

    // The NBT base class
    private Class<?> BASE_CLASS;
    private Class<?> COMPOUND_CLASS;
    private Class<?> STREAM_TOOLS;
    private Class<?> READ_LIMITER_CLASS;
    private Method NBT_CREATE_TAG;
    private Method NBT_GET_TYPE;
    private Field NBT_LIST_TYPE;
    private final Field[] DATA_FIELD = new Field[12];

    // CraftItemStack
    private Class<?> CRAFT_STACK;
    private Field CRAFT_HANDLE;
    private Field STACK_TAG;

    // Loading/saving compounds
    private LoadCompoundMethod LOAD_COMPOUND;
    private Method SAVE_COMPOUND;

    // Shared instance
    private static NbtFactory INSTANCE;

    /**
     * Represents a root NBT compound.
     * <p>
     * All changes to this map will be reflected in the underlying NBT compound.
     * Values may only be one of the following:
     * <ul>
     * <li>Primitive types</li>
     * <li>{@link java.lang.String String}</li>
     * <li>{@link NbtList}</li>
     * <li>{@link NbtCompound}</li>
     * </ul>
     * <p>
     * See also:
     * <ul>
     * <li>{@link NbtFactory#createCompound()}</li>
     * <li>{@link NbtFactory#fromCompound(Object)}</li>
     * </ul>
     * 
     * @author Kristian
     */
    public final class NbtCompound extends ConvertedMap {

        private NbtCompound(Object handle) {
            super(handle, getDataMap(handle));
        }

        // Simplifiying access to each value
        public Byte getByte(String key, Byte defaultValue) {
            return containsKey(key) ? (Byte) get(key) : defaultValue;
        }

        public Short getShort(String key, Short defaultValue) {
            return containsKey(key) ? (Short) get(key) : defaultValue;
        }

        public Integer getInteger(String key, Integer defaultValue) {
            return containsKey(key) ? (Integer) get(key) : defaultValue;
        }

        public Long getLong(String key, Long defaultValue) {
            return containsKey(key) ? (Long) get(key) : defaultValue;
        }

        public Float getFloat(String key, Float defaultValue) {
            return containsKey(key) ? (Float) get(key) : defaultValue;
        }

        public Double getDouble(String key, Double defaultValue) {
            return containsKey(key) ? (Double) get(key) : defaultValue;
        }

        public String getString(String key, String defaultValue) {
            return containsKey(key) ? (String) get(key) : defaultValue;
        }

        public byte[] getByteArray(String key, byte[] defaultValue) {
            return containsKey(key) ? (byte[]) get(key) : defaultValue;
        }

        public int[] getIntegerArray(String key, int[] defaultValue) {
            return containsKey(key) ? (int[]) get(key) : defaultValue;
        }

        /**
         * Retrieve the list by the given name.
         * 
         * @param key
         *            - the name of the list.
         * @param createNew
         *            - whether or not to create a new list if its missing.
         * @return An existing list, a new list or NULL.
         */
        public NbtList getList(String key, boolean createNew) {
            NbtList list = (NbtList) get(key);

            if (list == null && createNew)
                put(key, list = createList());
            return list;
        }

        /**
         * Retrieve the map by the given name.
         * 
         * @param key
         *            - the name of the map.
         * @param createNew
         *            - whether or not to create a new map if its missing.
         * @return An existing map, a new map or NULL.
         */
        public NbtCompound getMap(String key, boolean createNew) {
            return getMap(Arrays.asList(key), createNew);
        }

        // Done

        /**
         * Set the value of an entry at a given location.
         * <p>
         * Every element of the path (except the end) are assumed to be
         * compounds, and will be created if they are missing.
         * 
         * @param path
         *            - the path to the entry.
         * @param value
         *            - the new value of this entry.
         * @return This compound, for chaining.
         */
        public NbtCompound putPath(String path, Object value) {
            List<String> entries = getPathElements(path);
            Map<String, Object> map = getMap(entries.subList(0, entries.size() - 1), true);

            map.put(entries.get(entries.size() - 1), value);
            return this;
        }

        /**
         * Retrieve the value of a given entry in the tree.
         * <p>
         * Every element of the path (except the end) are assumed to be
         * compounds. The retrieval operation will be cancelled if any of them
         * are missing.
         * 
         * @param path
         *            - path to the entry.
         * @return The value, or NULL if not found.
         */
        @SuppressWarnings("unchecked")
        public <T> T getPath(String path) {
            List<String> entries = getPathElements(path);
            NbtCompound map = getMap(entries.subList(0, entries.size() - 1), false);

            if (map != null) {
                return (T) map.get(entries.get(entries.size() - 1));
            }
            return null;
        }

        /**
         * Save the content of a NBT compound to a stream.
         * <p>
         * Use {@link Files#newOutputStreamSupplier(java.io.File)} to provide a
         * stream supplier to a file.
         * 
         * @param stream
         *            - the output stream.
         * @param option
         *            - whether or not to compress the output.
         * @throws IOException
         *             If anything went wrong.
         */
        public void saveTo(OutputSupplier<? extends OutputStream> stream,
                StreamOptions option) throws IOException {
            saveStream(this, stream, option);
        }

        /**
         * Retrieve a map from a given path.
         * 
         * @param path
         *            - path of compounds to look up.
         * @param createNew
         *            - whether or not to create new compounds on the way.
         * @return The map at this location.
         */
        private NbtCompound getMap(Iterable<String> path, boolean createNew) {
            NbtCompound current = this;

            for (String entry : path) {
                NbtCompound child = (NbtCompound) current.get(entry);

                if (child == null) {
                    if (!createNew)
                        return null;
                    current.put(entry, child = createCompound());
                }
                current = child;
            }
            return current;
        }

        /**
         * Split the path into separate elements.
         * 
         * @param path
         *            - the path to split.
         * @return The elements.
         */
        private List<String> getPathElements(String path) {
            return Lists.newArrayList(Splitter.on(".").omitEmptyStrings().split(path));
        }
    }

    /**
     * Represents a root NBT list. See also:
     * <ul>
     * <li>{@link NbtFactory#createNbtList()}</li>
     * <li>{@link NbtFactory#fromList(Object)}</li>
     * </ul>
     * 
     * @author Kristian
     */
    public final class NbtList extends ConvertedList {

        private NbtList(Object handle) {
            super(handle, getDataList(handle));
        }
    }

    /**
     * Represents an object that provides a view of a native NMS class.
     * 
     * @author Kristian
     */
    public static interface Wrapper {

        /**
         * Retrieve the underlying native NBT tag.
         * 
         * @return The underlying NBT.
         */
        public Object getHandle();
    }

    /**
     * Retrieve or construct a shared NBT factory.
     * 
     * @return The factory.
     */
    private static NbtFactory get() {
        if (INSTANCE == null)
            INSTANCE = new NbtFactory();
        return INSTANCE;
    }

    /**
     * Construct an instance of the NBT factory by deducing the class of
     * NBTBase.
     */
    private NbtFactory() {
        if (BASE_CLASS == null) {
            try {
                // Keep in mind that I do use hard-coded field names - but it's
                // okay as long as we're dealing
                // with CraftBukkit or its derivatives. This does not work in
                // MCPC+ however.
                ClassLoader loader = NbtFactory.class.getClassLoader();

                String packageName = getPackageName();
                Class<?> offlinePlayer = loader.loadClass(packageName + ".CraftOfflinePlayer");

                // Prepare NBT
                COMPOUND_CLASS = getMethod(0, Modifier.STATIC, offlinePlayer, "getData").getReturnType();
                BASE_CLASS = COMPOUND_CLASS.getSuperclass();
                NBT_GET_TYPE = getMethod(0, Modifier.STATIC, BASE_CLASS, "getTypeId");
                NBT_CREATE_TAG = getMethod(Modifier.STATIC, 0, BASE_CLASS, "createTag", byte.class);

                // Prepare CraftItemStack
                CRAFT_STACK = loader.loadClass(packageName + ".inventory.CraftItemStack");
                CRAFT_HANDLE = getField(null, CRAFT_STACK, "handle");
                STACK_TAG = getField(null, CRAFT_HANDLE.getType(), "tag");

                // Loading/saving
                String nmsPackage = BASE_CLASS.getPackage().getName();
                initializeNMS(loader, nmsPackage);

                LOAD_COMPOUND = READ_LIMITER_CLASS != null ? new LoadMethodSkinUpdate(STREAM_TOOLS, READ_LIMITER_CLASS) : new LoadMethodWorldUpdate(STREAM_TOOLS);
                SAVE_COMPOUND = getMethod(Modifier.STATIC, 0, STREAM_TOOLS, null, BASE_CLASS, DataOutput.class);

            } catch (ClassNotFoundException e) {
                throw new IllegalStateException("Unable to find offline player.", e);
            }
        }
    }

    private void initializeNMS(ClassLoader loader, String nmsPackage) {
        try {
            STREAM_TOOLS = loader.loadClass(nmsPackage + ".NBTCompressedStreamTools");
            READ_LIMITER_CLASS = loader.loadClass(nmsPackage + ".NBTReadLimiter");
        } catch (ClassNotFoundException e) {
            // Ignore - we will detect this later
        }
    }

    private String getPackageName() {
        Server server = Bukkit.getServer();
        String name = server != null ? server.getClass().getPackage().getName() : null;

        if (name != null && name.contains("craftbukkit")) {
            return name;
        } else {
            // Fallback
            return "org.bukkit.craftbukkit.v1_7_R3";
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getDataMap(Object handle) {
        return (Map<String, Object>) getFieldValue(getDataField(NbtType.TAG_COMPOUND, handle), handle);
    }

    @SuppressWarnings("unchecked")
    private List<Object> getDataList(Object handle) {
        return (List<Object>) getFieldValue(getDataField(NbtType.TAG_LIST, handle), handle);
    }

    /**
     * Construct a new NBT list of an unspecified type.
     * 
     * @return The NBT list.
     */
    public static NbtList createList(Object... content) {
        return createList(Arrays.asList(content));
    }

    /**
     * Construct a new NBT list of an unspecified type.
     * 
     * @return The NBT list.
     */
    public static NbtList createList(Iterable<? extends Object> iterable) {
        NbtList list = get().new NbtList(INSTANCE.createNbtTag(NbtType.TAG_LIST, null));

        // Add the content as well
        for (Object obj : iterable)
            list.add(obj);
        return list;
    }

    /**
     * Construct a new NBT compound.
     * <p>
     * Use {@link NbtCompound#asMap()} to modify it.
     * 
     * @return The NBT compound.
     */
    public static NbtCompound createCompound() {
        return get().new NbtCompound(INSTANCE.createNbtTag(NbtType.TAG_COMPOUND, null));
    }

    /**
     * Construct a new NBT wrapper from a list.
     * 
     * @param nmsList
     *            - the NBT list.
     * @return The wrapper.
     */
    public static NbtList fromList(Object nmsList) {
        return get().new NbtList(nmsList);
    }

    /**
     * Load the content of a file from a stream.
     * <p>
     * Use {@link Files#newInputStreamSupplier(java.io.File)} to provide a
     * stream from a file.
     * 
     * @param stream
     *            - the stream supplier.
     * @param option
     *            - whether or not to decompress the input stream.
     * @return The decoded NBT compound.
     * @throws IOException
     *             If anything went wrong.
     */
    public static NbtCompound fromStream(
            InputSupplier<? extends InputStream> stream, StreamOptions option)
            throws IOException {
        InputStream input = null;
        DataInputStream data = null;
        boolean suppress = true;

        try {
            input = stream.getInput();
            data = new DataInputStream(new BufferedInputStream(option == StreamOptions.GZIP_COMPRESSION ? new GZIPInputStream(input) : input));

            NbtCompound result = fromCompound(get().LOAD_COMPOUND.loadNbt(data));
            suppress = false;
            return result;

        } finally {
            if (data != null)
                Closeables.close(data, suppress);
            else if (input != null)
                Closeables.close(input, suppress);
        }
    }

    /**
     * Save the content of a NBT compound to a stream.
     * <p>
     * Use {@link Files#newOutputStreamSupplier(java.io.File)} to provide a
     * stream supplier to a file.
     * 
     * @param source
     *            - the NBT compound to save.
     * @param stream
     *            - the stream.
     * @param option
     *            - whether or not to compress the output.
     * @throws IOException
     *             If anything went wrong.
     */
    public static void saveStream(NbtCompound source,
            OutputSupplier<? extends OutputStream> stream, StreamOptions option)
            throws IOException {
        OutputStream output = null;
        DataOutputStream data = null;
        boolean suppress = true;

        try {
            output = stream.getOutput();
            data = new DataOutputStream(option == StreamOptions.GZIP_COMPRESSION ? new GZIPOutputStream(output) : output);

            invokeMethod(get().SAVE_COMPOUND, null, source.getHandle(), data);
            suppress = false;

        } finally {
            if (data != null)
                Closeables.close(data, suppress);
            else if (output != null)
                Closeables.close(output, suppress);
        }
    }

    /**
     * Construct a new NBT wrapper from a compound.
     * 
     * @param nmsCompound
     *            - the NBT compund.
     * @return The wrapper.
     */
    public static NbtCompound fromCompound(Object nmsCompound) {
        return get().new NbtCompound(nmsCompound);
    }

    /**
     * Set the NBT compound tag of a given item stack.
     * <p>
     * The item stack must be a wrapper for a CraftItemStack. Use
     * {@link MinecraftReflection#getBukkitItemStack(ItemStack)} if not.
     * 
     * @param stack
     *            - the item stack, cannot be air.
     * @param compound
     *            - the new NBT compound, or NULL to remove it.
     * @throws IllegalArgumentException
     *             If the stack is not a CraftItemStack, or it represents air.
     */
    public static void setItemTag(ItemStack stack, NbtCompound compound) {
        checkItemStack(stack);
        Object nms = getFieldValue(get().CRAFT_HANDLE, stack);

        // Now update the tag compound
        setFieldValue(get().STACK_TAG, nms, compound.getHandle());
    }

    /**
     * Construct a wrapper for an NBT tag stored (in memory) in an item stack.
     * This is where auxillary data such as enchanting, name and lore is stored.
     * It does not include items material, damage value or count.
     * <p>
     * The item stack must be a wrapper for a CraftItemStack.
     * 
     * @param stack
     *            - the item stack.
     * @return A wrapper for its NBT tag.
     */
    public static NbtCompound fromItemTag(ItemStack stack) {
        checkItemStack(stack);
        Object nms = getFieldValue(get().CRAFT_HANDLE, stack);
        Object tag = getFieldValue(get().STACK_TAG, nms);

        // Create the tag if it doesn't exist
        if (tag == null) {
            NbtCompound compound = createCompound();
            setItemTag(stack, compound);
            return compound;
        }
        return fromCompound(tag);
    }

    /**
     * Retrieve a CraftItemStack version of the stack.
     * 
     * @param stack
     *            - the stack to convert.
     * @return The CraftItemStack version.
     */
    public static ItemStack getCraftItemStack(ItemStack stack) {
        // Any need to convert?
        if (stack == null || get().CRAFT_STACK.isAssignableFrom(stack.getClass()))
            return stack;
        try {
            // Call the private constructor
            Constructor<?> caller = INSTANCE.CRAFT_STACK.getDeclaredConstructor(ItemStack.class);
            caller.setAccessible(true);
            return (ItemStack) caller.newInstance(stack);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to convert " + stack + " + to a CraftItemStack.");
        }
    }

    /**
     * Ensure that the given stack can store arbitrary NBT information.
     * 
     * @param stack
     *            - the stack to check.
     */
    private static void checkItemStack(ItemStack stack) {
        if (stack == null)
            throw new IllegalArgumentException("Stack cannot be NULL.");
        if (!get().CRAFT_STACK.isAssignableFrom(stack.getClass()))
            throw new IllegalArgumentException("Stack must be a CraftItemStack.");
        if (stack.getType() == Material.AIR)
            throw new IllegalArgumentException("ItemStacks representing air cannot store NMS information.");
    }

    /**
     * Convert wrapped List and Map objects into their respective NBT
     * counterparts.
     * 
     * @param name
     *            - the name of the NBT element to create.
     * @param value
     *            - the value of the element to create. Can be a List or a Map.
     * @return The NBT element.
     */
    private Object unwrapValue(Object value) {
        if (value == null)
            return null;

        if (value instanceof Wrapper) {
            return ((Wrapper) value).getHandle();

        } else if (value instanceof List) {
            throw new IllegalArgumentException("Can only insert a WrappedList.");
        } else if (value instanceof Map) {
            throw new IllegalArgumentException("Can only insert a WrappedCompound.");

        } else {
            return createNbtTag(getPrimitiveType(value), value);
        }
    }

    /**
     * Convert a given NBT element to a primitive wrapper or List/Map
     * equivalent.
     * <p>
     * All changes to any mutable objects will be reflected in the underlying
     * NBT element(s).
     * 
     * @param nms
     *            - the NBT element.
     * @return The wrapper equivalent.
     */
    private Object wrapNative(Object nms) {
        if (nms == null)
            return null;

        if (BASE_CLASS.isAssignableFrom(nms.getClass())) {
            final NbtType type = getNbtType(nms);

            // Handle the different types
            switch (type) {
                case TAG_COMPOUND:
                    return new NbtCompound(nms);
                case TAG_LIST:
                    return new NbtList(nms);
                default:
                    return getFieldValue(getDataField(type, nms), nms);
            }
        }
        throw new IllegalArgumentException("Unexpected type: " + nms);
    }

    /**
     * Construct a new NMS NBT tag initialized with the given value.
     * 
     * @param type
     *            - the NBT type.
     * @param value
     *            - the value, or NULL to keep the original value.
     * @return The created tag.
     */
    private Object createNbtTag(NbtType type, Object value) {
        Object tag = invokeMethod(NBT_CREATE_TAG, null, (byte) type.id);

        if (value != null) {
            setFieldValue(getDataField(type, tag), tag, value);
        }
        return tag;
    }

    /**
     * Retrieve the field where the NBT class stores its value.
     * 
     * @param type
     *            - the NBT type.
     * @param nms
     *            - the NBT class instance.
     * @return The corresponding field.
     */
    private Field getDataField(NbtType type, Object nms) {
        if (DATA_FIELD[type.id] == null)
            DATA_FIELD[type.id] = getField(nms, null, type.getFieldName());
        return DATA_FIELD[type.id];
    }

    /**
     * Retrieve the NBT type from a given NMS NBT tag.
     * 
     * @param nms
     *            - the native NBT tag.
     * @return The corresponding type.
     */
    private NbtType getNbtType(Object nms) {
        int type = (Byte) invokeMethod(NBT_GET_TYPE, nms);
        return NBT_ENUM.get(type);
    }

    /**
     * Retrieve the nearest NBT type for a given primitive type.
     * 
     * @param primitive
     *            - the primitive type.
     * @return The corresponding type.
     */
    private NbtType getPrimitiveType(Object primitive) {
        NbtType type = NBT_ENUM.get(NBT_CLASS.inverse().get(Primitives.unwrap(primitive.getClass())));

        // Display the illegal value at least
        if (type == null)
            throw new IllegalArgumentException(String.format("Illegal type: %s (%s)", primitive.getClass(), primitive));
        return type;
    }

    /**
     * Invoke a method on the given target instance using the provided
     * parameters.
     * 
     * @param method
     *            - the method to invoke.
     * @param target
     *            - the target.
     * @param params
     *            - the parameters to supply.
     * @return The result of the method.
     */
    private static Object invokeMethod(Method method, Object target,
            Object... params) {
        try {
            return method.invoke(target, params);
        } catch (Exception e) {
            throw new RuntimeException("Unable to invoke method " + method + " for " + target, e);
        }
    }

    private static void setFieldValue(Field field, Object target, Object value) {
        try {
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException("Unable to set " + field + " for " + target, e);
        }
    }

    private static Object getFieldValue(Field field, Object target) {
        try {
            return field.get(target);
        } catch (Exception e) {
            throw new RuntimeException("Unable to retrieve " + field + " for " + target, e);
        }
    }

    /**
     * Search for the first publically and privately defined method of the given
     * name and parameter count.
     * 
     * @param requireMod
     *            - modifiers that are required.
     * @param bannedMod
     *            - modifiers that are banned.
     * @param clazz
     *            - a class to start with.
     * @param methodName
     *            - the method name, or NULL to skip.
     * @param params
     *            - the expected parameters.
     * @return The first method by this name.
     * @throws IllegalStateException
     *             If we cannot find this method.
     */
    private static Method getMethod(int requireMod, int bannedMod,
            Class<?> clazz, String methodName, Class<?>... params) {
        for (Method method : clazz.getDeclaredMethods()) {
            // Limitation: Doesn't handle overloads
            if ((method.getModifiers() & requireMod) == requireMod && (method.getModifiers() & bannedMod) == 0 && (methodName == null || method.getName().equals(methodName)) && Arrays.equals(method.getParameterTypes(), params)) {

                method.setAccessible(true);
                return method;
            }
        }
        // Search in every superclass
        if (clazz.getSuperclass() != null)
            return getMethod(requireMod, bannedMod, clazz.getSuperclass(), methodName, params);
        throw new IllegalStateException(String.format("Unable to find method %s (%s).", methodName, Arrays.asList(params)));
    }

    /**
     * Search for the first publically and privately defined field of the given
     * name.
     * 
     * @param instance
     *            - an instance of the class with the field.
     * @param clazz
     *            - an optional class to start with, or NULL to deduce it from
     *            instance.
     * @param fieldName
     *            - the field name.
     * @return The first field by this name.
     * @throws IllegalStateException
     *             If we cannot find this field.
     */
    private static Field getField(Object instance, Class<?> clazz,
            String fieldName) {
        if (clazz == null)
            clazz = instance.getClass();
        // Ignore access rules
        for (Field field : clazz.getDeclaredFields()) {
            if (field.getName().equals(fieldName)) {
                field.setAccessible(true);
                return field;
            }
        }
        // Recursively fild the correct field
        if (clazz.getSuperclass() != null)
            return getField(instance, clazz.getSuperclass(), fieldName);
        throw new IllegalStateException("Unable to find field " + fieldName + " in " + instance);
    }

    /**
     * Represents a class for caching wrappers.
     * 
     * @author Kristian
     */
    private final class CachedNativeWrapper {

        // Don't recreate wrapper objects
        private final ConcurrentMap<Object, Object> cache = new MapMaker().weakKeys().makeMap();

        public Object wrap(Object value) {
            Object current = cache.get(value);

            if (current == null) {
                current = wrapNative(value);

                // Only cache composite objects
                if (current instanceof ConvertedMap || current instanceof ConvertedList) {
                    cache.put(value, current);
                }
            }
            return current;
        }
    }

    /**
     * Represents a map that wraps another map and automatically converts
     * entries of its type and another exposed type.
     * 
     * @author Kristian
     */
    private class ConvertedMap extends AbstractMap<String, Object> implements
            Wrapper {

        private final Object handle;
        private final Map<String, Object> original;

        private final CachedNativeWrapper cache = new CachedNativeWrapper();

        public ConvertedMap(Object handle, Map<String, Object> original) {
            this.handle = handle;
            this.original = original;
        }

        // For converting back and forth
        protected Object wrapOutgoing(Object value) {
            return cache.wrap(value);
        }

        protected Object unwrapIncoming(Object wrapped) {
            return unwrapValue(wrapped);
        }

        // Modification
        @Override
        public Object put(String key, Object value) {
            return wrapOutgoing(original.put((String) key, unwrapIncoming(value)));
        }

        // Performance
        @Override
        public Object get(Object key) {
            return wrapOutgoing(original.get(key));
        }

        @Override
        public Object remove(Object key) {
            return wrapOutgoing(original.remove(key));
        }

        @Override
        public boolean containsKey(Object key) {
            return original.containsKey(key);
        }

        @Override
        public Set<Entry<String, Object>> entrySet() {
            return new AbstractSet<Entry<String, Object>>() {

                @Override
                public boolean add(Entry<String, Object> e) {
                    String key = e.getKey();
                    Object value = e.getValue();

                    original.put(key, unwrapIncoming(value));
                    return true;
                }

                @Override
                public int size() {
                    return original.size();
                }

                @Override
                public Iterator<Entry<String, Object>> iterator() {
                    return ConvertedMap.this.iterator();
                }
            };
        }

        private Iterator<Entry<String, Object>> iterator() {
            final Iterator<Entry<String, Object>> proxy = original.entrySet().iterator();

            return new Iterator<Entry<String, Object>>() {

                @Override
                public boolean hasNext() {
                    return proxy.hasNext();
                }

                @Override
                public Entry<String, Object> next() {
                    Entry<String, Object> entry = proxy.next();

                    return new SimpleEntry<String, Object>(entry.getKey(), wrapOutgoing(entry.getValue()));
                }

                @Override
                public void remove() {
                    proxy.remove();
                }
            };
        }

        @Override
        public Object getHandle() {
            return handle;
        }
    }

    /**
     * Represents a list that wraps another list and converts elements of its
     * type and another exposed type.
     * 
     * @author Kristian
     */
    private class ConvertedList extends AbstractList<Object> implements Wrapper {

        private final Object handle;

        private final List<Object> original;
        private final CachedNativeWrapper cache = new CachedNativeWrapper();

        public ConvertedList(Object handle, List<Object> original) {
            if (NBT_LIST_TYPE == null)
                NBT_LIST_TYPE = getField(handle, null, "type");
            this.handle = handle;
            this.original = original;
        }

        protected Object wrapOutgoing(Object value) {
            return cache.wrap(value);
        }

        protected Object unwrapIncoming(Object wrapped) {
            return unwrapValue(wrapped);
        }

        @Override
        public Object get(int index) {
            return wrapOutgoing(original.get(index));
        }

        @Override
        public int size() {
            return original.size();
        }

        @Override
        public Object set(int index, Object element) {
            return wrapOutgoing(original.set(index, unwrapIncoming(element)));
        }

        @Override
        public void add(int index, Object element) {
            Object nbt = unwrapIncoming(element);

            // Set the list type if its the first element
            if (size() == 0)
                setFieldValue(NBT_LIST_TYPE, handle, (byte) getNbtType(nbt).id);
            original.add(index, nbt);
        }

        @Override
        public Object remove(int index) {
            return wrapOutgoing(original.remove(index));
        }

        @Override
        public boolean remove(Object o) {
            return original.remove(unwrapIncoming(o));
        }

        @Override
        public Object getHandle() {
            return handle;
        }
    }

    /**
     * Represents a method for loading an NBT compound.
     * 
     * @author Kristian
     */
    private static abstract class LoadCompoundMethod {

        protected Method staticMethod;

        protected void setMethod(Method method) {
            this.staticMethod = method;
            this.staticMethod.setAccessible(true);
        }

        /**
         * Load an NBT compound from a given stream.
         * 
         * @param input
         *            - the input stream.
         * @return The loaded NBT compound.
         */
        public abstract Object loadNbt(DataInput input);
    }

    /**
     * Load an NBT compound from the NBTCompressedStreamTools static method in
     * 1.7.2 - 1.7.5
     */
    private static class LoadMethodWorldUpdate extends LoadCompoundMethod {

        public LoadMethodWorldUpdate(Class<?> streamClass) {
            setMethod(getMethod(Modifier.STATIC, 0, streamClass, null, DataInput.class));
        }

        @Override
        public Object loadNbt(DataInput input) {
            return invokeMethod(staticMethod, null, input);
        }
    }

    /**
     * Load an NBT compound from the NBTCompressedStreamTools static method in
     * 1.7.8
     */
    private static class LoadMethodSkinUpdate extends LoadCompoundMethod {

        private Object readLimiter;

        public LoadMethodSkinUpdate(Class<?> streamClass,
                Class<?> readLimiterClass) {
            setMethod(getMethod(Modifier.STATIC, 0, streamClass, null, DataInput.class, readLimiterClass));

            // Find the unlimited read limiter
            for (Field field : readLimiterClass.getDeclaredFields()) {
                if (readLimiterClass.isAssignableFrom(field.getType())) {
                    try {
                        readLimiter = field.get(null);
                    } catch (Exception e) {
                        throw new RuntimeException("Cannot retrieve read limiter.", e);
                    }
                }
            }
        }

        @Override
        public Object loadNbt(DataInput input) {
            return invokeMethod(staticMethod, null, input, readLimiter);
        }
    }
}
