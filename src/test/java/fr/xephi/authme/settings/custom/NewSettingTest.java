package fr.xephi.authme.settings.custom;

import fr.xephi.authme.settings.custom.domain.Property;
import fr.xephi.authme.settings.custom.domain.PropertyType;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.File;
import java.net.URL;
import java.util.List;

import static fr.xephi.authme.settings.custom.domain.Property.newProperty;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;

public class NewSettingTest {

    private static final String CONFIG_FILE = "437-config-test.yml";

    @Test
    public void shouldReturnIntegerFromFile() {
        // given
        YamlConfiguration file = mock(YamlConfiguration.class);
        Property<Integer> config = TestConfiguration.DURATION_IN_SECONDS;
        given(file.getInt("test.duration", 4)).willReturn(18);
        NewSetting settings = new NewSetting(file, "conf.txt");

        // when
        int retrieve = settings.getOption(config);

        // then
        assertThat(retrieve, equalTo(18));
    }

    @Test
    public void shouldLoadAllConfigs() {
        // given
        YamlConfiguration file = mock(YamlConfiguration.class);

        given(file.getString(anyString(), anyString())).willAnswer(new Answer<String>() {
            @Override
            public String answer(InvocationOnMock invocation) throws Throwable {
                // Return the second parameter -> the default
                return (String) invocation.getArguments()[1];
            }
        });

        given(file.getInt(eq(EmailSettings.RECOVERY_PASSWORD_LENGTH.getPath()), anyInt()))
            .willReturn(20);
        given(file.getBoolean(eq(SecuritySettings.REMOVE_PASSWORD_FROM_CONSOLE.getPath()), anyBoolean()))
            .willReturn(false);

        // when
        NewSetting settings = new NewSetting(file, "conf.txt");

        // then
        // Expect the value we told the YAML mock to return:
        assertThat(settings.getOption(EmailSettings.RECOVERY_PASSWORD_LENGTH), equalTo(20));
        // Expect the default:
        assertThat(settings.getOption(EmailSettings.SMTP_HOST), equalTo(EmailSettings.SMTP_HOST.getDefaultValue()));
        // Expect the value we told the YAML mock to return:
        assertThat(settings.getOption(SecuritySettings.REMOVE_PASSWORD_FROM_CONSOLE), equalTo(false));
    }

    @Test
    public void executeIntegrationTest() {
        // given
        YamlConfiguration yamlFile = YamlConfiguration.loadConfiguration(getConfigFile());
        NewSetting settings = new NewSetting(yamlFile, "conf.txt");

        // when
        int result = settings.getOption(TestConfiguration.DURATION_IN_SECONDS);
        String systemName = settings.getOption(TestConfiguration.SYSTEM_NAME);
        String helpHeader = settings.getOption(newProperty("settings.helpHeader", ""));
        List<String> unsafePasswords = settings.getOption(
            newProperty(PropertyType.STRING_LIST, "Security.unsafePasswords"));

        // then
        assertThat(result, equalTo(22));
        assertThat(systemName, equalTo(TestConfiguration.SYSTEM_NAME.getDefaultValue()));
        assertThat(helpHeader, equalTo("AuthMeReloaded"));
        assertThat(unsafePasswords, contains("123456", "qwerty", "54321"));
    }

    private File getConfigFile() {
        URL url = getClass().getClassLoader().getResource(CONFIG_FILE);
        if (url == null) {
            throw new RuntimeException("File '" + CONFIG_FILE + "' could not be loaded");
        }
        return new File(url.getFile());
    }

    private static class TestConfiguration {

        public static final Property<Integer> DURATION_IN_SECONDS =
            newProperty("test.duration", 4);

        public static final Property<String> SYSTEM_NAME =
            newProperty("test.systemName", "[TestDefaultValue]");
    }

}
