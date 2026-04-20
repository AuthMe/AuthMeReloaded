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
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Implementation of {@link PropertyReader} which can read a file or a stream with
 * a specified charset.
 */
final class MessageMigraterPropertyReader implements PropertyReader {

    private static final Charset CHARSET = StandardCharsets.UTF_8;

    private Map<String, Object> root;

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
        try (InputStream is = new FileInputStream(file)) {
            return loadFromStream(is);
        } catch (IOException e) {
            throw new IllegalStateException("Error while reading file '" + file + "'", e);
        }
    }

    public static MessageMigraterPropertyReader loadFromStream(InputStream inputStream) {
        Map<String, Object> valuesMap = readStreamToMap(inputStream);
        return new MessageMigraterPropertyReader(valuesMap);
    }

    @Override
    public boolean contains(String path) {
        return getObject(path) != null;
    }

    @Override
    public Set<String> getKeys(boolean b) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<String> getChildKeys(String s) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object getObject(String path) {
        if (path.isEmpty()) {
            return root.get("");
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
    public String getString(String path) {
        Object o = getObject(path);
        return o instanceof String ? (String) o : null;
    }

    @Override
    public Integer getInt(String path) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Double getDouble(String path) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Boolean getBoolean(String path) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<?> getList(String path) {
        throw new UnsupportedOperationException();
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
