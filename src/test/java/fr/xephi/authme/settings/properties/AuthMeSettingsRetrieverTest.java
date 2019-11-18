package fr.xephi.authme.settings.properties;

import ch.jalu.configme.configurationdata.ConfigurationData;
import org.junit.Test;

import static org.hamcrest.Matchers.closeTo;
import static org.junit.Assert.assertThat;

/**
 * Test for {@link AuthMeSettingsRetriever}.
 */
public class AuthMeSettingsRetrieverTest {

    @Test
    public void shouldReturnAllProperties() {
        // given / when
        ConfigurationData configurationData = AuthMeSettingsRetriever.buildConfigurationData();

        // then
        // Note ljacqu 20161123: Check that the number of properties returned corresponds to what we expect with
        // an error margin of 10: this prevents us from having to adjust the test every time the config is changed.
        // If this test fails, replace the first argument in closeTo() with the new number of properties
        assertThat((double) configurationData.getProperties().size(),
            closeTo(182, 10));
    }
}
