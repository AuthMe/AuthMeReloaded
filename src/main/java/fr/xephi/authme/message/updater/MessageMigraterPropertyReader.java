package fr.xephi.authme.message.updater;

import ch.jalu.configme.exception.ConfigMeException;
import ch.jalu.configme.resource.PropertyReader;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Implementation of {@link PropertyReader} which can read a file or a stream with
 * a specified charset.
 */
public final class MessageMigraterPropertyReader implements PropertyReader {

    public static final Charset CHARSET = StandardCharsets.UTF_8;

    private Map<String, Object> root;
    /** See same field in {@link ch.jalu.configme.resource.YamlFileReader} for details. */
    private boolean hasObjectAsRoot = false;

    private MessageMigraterPropertyReader(Map<String, Object> valuesMap) {
        root = valuesMap;
    }

    /**
     * Creates a new property reader for the given file.
     *
     * @param file the file to load
     * @return the created property reader
     */
    public static MessageMigraterPropertyReader loadFromFile(File file) {
        Map<String, Object> valuesMap;
        try (InputStream is = new FileInputStream(file)) {
            valuesMap = readStreamToMap(is);
        } catch (IOException e) {
            throw new IllegalStateException("Error while reading file '" + file + "'", e);
        }

        return new MessageMigraterPropertyReader(valuesMap);
    }

    public static MessageMigraterPropertyReader loadFromStream(InputStream inputStream) {
        Map<String, Object> valuesMap = readStreamToMap(inputStream);
        return new MessageMigraterPropertyReader(valuesMap);
    }

    @Override
    public Object getObject(String path) {
        if (path.isEmpty()) {
            return hasObjectAsRoot ? root.get("") : root;
        }
        Object node = root;
        String[] keys = path.split("\\.");
        for (String key : keys) {
            node = getIfIsMap(key, node);
            if (node == null) {
                return null;
            }
        }
        return node;
    }

    @Override
    public <T> T getTypedObject(String path, Class<T> clazz) {
        Object value = getObject(path);
        if (clazz.isInstance(value)) {
            return clazz.cast(value);
        }
        return null;
    }

    @Override
    public void set(String path, Object value) {
        Objects.requireNonNull(path);

        if (path.isEmpty()) {
            root.clear();
            root.put("", value);
            hasObjectAsRoot = true;
        } else if (hasObjectAsRoot) {
            throw new ConfigMeException("The root path is a bean property; you cannot set values to any subpath. "
                + "Modify the bean at the root or set a new one instead.");
        } else {
            setValueInChildPath(path, value);
        }
    }

    /**
     * Sets the value at the given path. This method is used when the root is a map and not a specific object.
     *
     * @param path the path to set the value at
     * @param value the value to set
     */
    @SuppressWarnings("unchecked")
    private void setValueInChildPath(String path, Object value) {
        Map<String, Object> node = root;
        String[] keys = path.split("\\.");
        for (int i = 0; i < keys.length - 1; ++i) {
            Object child = node.get(keys[i]);
            if (child instanceof Map<?, ?>) {
                node = (Map<String, Object>) child;
            } else { // child is null or some other value - replace with map
                Map<String, Object> newEntry = new HashMap<>();
                node.put(keys[i], newEntry);
                if (value == null) {
                    // For consistency, replace whatever value/null here with an empty map,
                    // but if the value is null our work here is done.
                    return;
                }
                node = newEntry;
            }
        }
        // node now contains the parent map (existing or newly created)
        if (value == null) {
            node.remove(keys[keys.length - 1]);
        } else {
            node.put(keys[keys.length - 1], value);
        }
    }

    @Override
    public void reload() {
        throw new UnsupportedOperationException("Reload not supported by this implementation");
    }

    private static Map<String, Object> readStreamToMap(InputStream inputStream) {
        try (InputStreamReader isr = new InputStreamReader(inputStream, CHARSET)) {
            Object obj = new Yaml().load(isr);
            return obj == null ? new HashMap<>() : (Map<String, Object>) obj;
        } catch (IOException e) {
            throw new ConfigMeException("Could not read stream", e);
        } catch (ClassCastException e) {
            throw new ConfigMeException("Top-level is not a map", e);
        }
    }

    private static Object getIfIsMap(String key, Object value) {
        if (value instanceof Map<?, ?>) {
            return ((Map<?, ?>) value).get(key);
        }
        return null;
    }
}
