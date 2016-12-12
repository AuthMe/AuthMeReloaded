package fr.xephi.authme.settings;

import com.github.authme.configme.knownproperties.ConfigurationData;
import com.github.authme.configme.properties.Property;
import fr.xephi.authme.settings.properties.AuthMeSettingsRetriever;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.fail;

/**
 * Tests the consistency of the settings configuration.
 */
public class SettingsConsistencyTest {

    /**
     * Maximum characters one comment line may have (prevents horizontal scrolling).
     */
    private static final int MAX_COMMENT_LENGTH = 90;

    private static ConfigurationData configurationData;

    @BeforeClass
    public static void buildConfigurationData() {
        configurationData = AuthMeSettingsRetriever.buildConfigurationData();
    }

    @Test
    public void shouldHaveCommentOnEachProperty() {
        // given
        List<Property<?>> properties = configurationData.getProperties();

        // when / then
        for (Property<?> property : properties) {
            if (configurationData.getCommentsForSection(property.getPath()).length == 0) {
                fail("No comment defined for '" + property + "'");
            }
        }
    }

    @Test
    public void shouldNotHaveVeryLongCommentLines() {
        // given
        List<Property<?>> properties = configurationData.getProperties();
        List<Property<?>> badProperties = new ArrayList<>();

        // when
        for (Property<?> property : properties) {
            for (String comment : configurationData.getCommentsForSection(property.getPath())) {
                if (comment.length() > MAX_COMMENT_LENGTH) {
                    badProperties.add(property);
                    break;
                }
            }
        }

        // then
        if (!badProperties.isEmpty()) {
            fail("Comment lines should not be longer than " + MAX_COMMENT_LENGTH + " chars, "
                + "but found too long comments for:\n- "
                + badProperties.stream().map(Property::getPath).collect(Collectors.joining("\n- ")));
        }
    }

}
