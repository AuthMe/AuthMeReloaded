package fr.xephi.authme.settings.properties;

import ch.jalu.configme.SettingsHolder;
import ch.jalu.configme.properties.Property;

import java.util.List;

import static ch.jalu.configme.properties.PropertyInitializer.newListProperty;
import static ch.jalu.configme.properties.PropertyInitializer.newProperty;

/**
 * Sample properties for testing purposes.
 */
public final class TestConfiguration implements SettingsHolder {

    public static final Property<Integer> DURATION_IN_SECONDS =
        newProperty("test.duration", 4);

    public static final Property<String> SYSTEM_NAME =
        newProperty("test.systemName", "[TestDefaultValue]");

    public static final Property<TestEnum> RATIO_ORDER =
        newProperty(TestEnum.class, "sample.ratio.order", TestEnum.SECOND);

    public static final Property<List<String>> RATIO_FIELDS =
        newListProperty("sample.ratio.fields", "a", "b", "c");

    public static final Property<Integer> VERSION_NUMBER =
        newProperty("version", 32046);

    public static final Property<Boolean> SKIP_BORING_FEATURES =
        newProperty("features.boring.skip", false);

    public static final Property<List<String>> BORING_COLORS =
        newListProperty("features.boring.colors");

    public static final Property<Integer> DUST_LEVEL =
        newProperty("features.boring.dustLevel", -1);

    public static final Property<Boolean> USE_COOL_FEATURES =
        newProperty("features.cool.enabled", false);

    public static final Property<List<String>> COOL_OPTIONS =
        newListProperty("features.cool.options", "Sparks", "Sprinkles");


    private TestConfiguration() {
    }
}
