package fr.xephi.authme.settings.custom;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assume.assumeThat;

/**
 * Test for {@link NewSetting} and the project's config.yml,
 * verifying that no settings are missing from the file.
 */
public class ConfigFileConsistencyTest {

    @Test
    public void shouldHaveAllConfigs() throws IOException {
        URL url = this.getClass().getResource("/config.yml");
        File configFile = new File(url.getFile());

        // given
        assumeThat(configFile.exists(), equalTo(true));
        NewSetting settings = new NewSetting(configFile);

        // when
        boolean result = settings.containsAllSettings(SettingsFieldRetriever.getAllPropertyFields());

        // then
        assertThat(result, equalTo(true));
    }

}
