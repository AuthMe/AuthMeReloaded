package fr.xephi.authme.settings.custom;

import org.junit.Test;

import java.io.File;
import java.net.URL;

import static org.junit.Assert.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * Test for the save() function of new settings
 */
public class NewSettingsWriteTest {

    private static final String CONFIG_FILE = "437-write-test.yml";

    @Test
    public void shouldWriteProperties() {
        File file = getConfigFile();
        NewSetting setting = new NewSetting(file);
        setting.save();

        // assert that we can load the file again -- i.e. that it's valid YAML!
        NewSetting newSetting = new NewSetting(file);
        assertThat(newSetting.getOption(SecuritySettings.CAPTCHA_LENGTH),
            equalTo(SecuritySettings.CAPTCHA_LENGTH.getDefaultValue()));
        assertThat(newSetting.getOption(ProtectionSettings.COUNTRIES_BLACKLIST),
            equalTo(ProtectionSettings.COUNTRIES_BLACKLIST.getDefaultValue()));
    }



    private File getConfigFile() {
        URL url = getClass().getClassLoader().getResource(CONFIG_FILE);
        if (url == null) {
            throw new RuntimeException("File '" + CONFIG_FILE + "' could not be loaded");
        }
        return new File(url.getFile());
    }

}
