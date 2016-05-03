package tools.utils;

import tools.utils.TagValue.TextTagValue;

import java.util.HashMap;
import java.util.Map;


public class TagValueHolder {

    private Map<String, TagValue<?>> values;

    private TagValueHolder() {
        this.values = new HashMap<>();
    }

    public static TagValueHolder create() {
        return new TagValueHolder();
    }

    public TagValueHolder put(String key, TagValue<?> value) {
        values.put(key, value);
        return this;
    }

    public TagValueHolder put(String key, String value) {
        values.put(key, new TextTagValue(value));
        return this;
    }

    public Map<String, TagValue<?>> getValues() {
        return values;
    }
}
