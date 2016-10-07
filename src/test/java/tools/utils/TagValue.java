package tools.utils;

import java.util.ArrayList;
import java.util.List;

/**
 * Value of a tag.
 *
 * @param <T> the tag value type
 * @see TagReplacer
 */
public abstract class TagValue<T> {

    private final T value;

    public TagValue(T value) {
        this.value = value;
    }

    public T getValue() {
        return value;
    }

    public abstract boolean isEmpty();

    /**
     * Text value.
     */
    public static final class TextTagValue extends TagValue<String> {
        public TextTagValue(String value) {
            super(value);
        }

        @Override
        public boolean isEmpty() {
            return getValue().isEmpty();
        }
    }

    /**
     * List value (iterable).
     */
    public static final class NestedTagValue extends TagValue<List<TagValueHolder>> {
        public NestedTagValue() {
            super(new ArrayList<>());
        }

        @Override
        public boolean isEmpty() {
            return getValue().isEmpty();
        }

        public void add(TagValueHolder entry) {
            getValue().add(entry);
        }
    }
}
