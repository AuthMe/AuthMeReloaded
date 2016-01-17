package fr.xephi.authme.settings.custom;

import fr.xephi.authme.settings.domain.Property;
import fr.xephi.authme.settings.domain.PropertyType;
import fr.xephi.authme.settings.domain.SettingsClass;

import java.util.List;

import static fr.xephi.authme.settings.domain.Property.newProperty;

/**
 * Sample properties for testing purposes.
 */
class TestConfiguration implements SettingsClass {

    public static final Property<Integer> DURATION_IN_SECONDS =
        newProperty("test.duration", 4);

    public static final Property<String> SYSTEM_NAME =
        newProperty("test.systemName", "[TestDefaultValue]");

    public static final Property<Double> RATIO_LIMIT =
        newProperty(PropertyType.DOUBLE, "sample.ratio.limit", 3.0);

    public static final Property<List<String>> RATIO_FIELDS =
        newProperty(PropertyType.STRING_LIST, "sample.ratio.fields", "a", "b", "c");

    public static final Property<Integer> VERSION_NUMBER =
        newProperty("version", 32046);

    public static final Property<Boolean> SKIP_BORING_FEATURES =
        newProperty("features.boring.skip", false);

    public static final Property<List<String>> BORING_COLORS =
        newProperty(PropertyType.STRING_LIST, "features.boring.colors");

    public static final Property<Double> DUST_LEVEL =
        newProperty(PropertyType.DOUBLE, "features.boring.dustLevel", 0.2);

    public static final Property<Boolean> USE_COOL_FEATURES =
        newProperty("features.cool.enabled", false);

    public static final Property<List<String>> COOL_OPTIONS =
        newProperty(PropertyType.STRING_LIST, "features.cool.options", "Sparks", "Sprinkles");

}
