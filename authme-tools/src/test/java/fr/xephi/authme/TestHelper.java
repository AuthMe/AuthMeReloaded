package fr.xephi.authme;

import ch.jalu.configme.properties.Property;
import fr.xephi.authme.settings.Settings;
import tools.utils.ToolsConstants;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

/**
 * Minimal test utilities required by the tooling module.
 */
public final class TestHelper {

    public static final String SOURCES_FOLDER = ToolsConstants.MAIN_SOURCE_ROOT;
    public static final String TEST_SOURCES_FOLDER = ToolsConstants.CORE_TEST_SOURCE_ROOT;
    public static final String PROJECT_ROOT = "/fr/xephi/authme/";

    private TestHelper() {
    }

    /**
     * Configures the Settings mock to return the property's default value for any given property.
     *
     * @param settings the settings mock
     */
    @SuppressWarnings("unchecked")
    public static void returnDefaultsForAllProperties(Settings settings) {
        given(settings.getProperty(any(Property.class)))
            .willAnswer(invocation -> ((Property<?>) invocation.getArgument(0)).getDefaultValue());
    }
}
