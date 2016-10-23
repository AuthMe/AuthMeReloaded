package fr.xephi.authme.settings;

import com.github.authme.configme.knownproperties.ConfigurationData;
import com.github.authme.configme.knownproperties.ConfigurationDataBuilder;
import com.github.authme.configme.resource.PropertyResource;
import fr.xephi.authme.TestHelper;
import fr.xephi.authme.settings.properties.RegistrationSettings;
import fr.xephi.authme.settings.properties.TestConfiguration;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.arrayWithSize;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for {@link Settings}.
 */
public class SettingsTest {
    
    private static final ConfigurationData CONFIG_DATA =
        ConfigurationDataBuilder.collectData(TestConfiguration.class);

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();
    private File testPluginFolder;

    @BeforeClass
    public static void setUpLogger() {
        TestHelper.setupLogger();
    }

    @Before
    public void setUpTestPluginFolder() throws IOException {
        testPluginFolder = temporaryFolder.newFolder();
    }

    @Test
    public void shouldLoadWelcomeMessage() throws IOException {
        // given
        String welcomeMessage = "This is my welcome message for testing\nBye!";
        File welcomeFile = new File(testPluginFolder, "welcome.txt");
        createFile(welcomeFile);
        Files.write(welcomeFile.toPath(), welcomeMessage.getBytes());

        PropertyResource resource = mock(PropertyResource.class);
        given(resource.getBoolean(RegistrationSettings.USE_WELCOME_MESSAGE.getPath())).willReturn(true);
        Settings settings = new Settings(testPluginFolder, resource,
            TestSettingsMigrationServices.alwaysFulfilled(), CONFIG_DATA);

        // when
        String[] result = settings.getWelcomeMessage();

        // then
        assertThat(result, arrayWithSize(2));
        assertThat(result, arrayContaining(welcomeMessage.split("\\n")));
    }

    @Test
    public void shouldLoadEmailMessage() throws IOException {
        // given
        String emailMessage = "Sample email message\nThat's all!";
        File emailFile = new File(testPluginFolder, "email.html");
        createFile(emailFile);
        Files.write(emailFile.toPath(), emailMessage.getBytes());

        PropertyResource resource = mock(PropertyResource.class);
        Settings settings = new Settings(testPluginFolder, resource,
            TestSettingsMigrationServices.alwaysFulfilled(), CONFIG_DATA);

        // when
        String result = settings.getPasswordEmailMessage();

        // then
        assertThat(result, equalTo(emailMessage));
    }

    private static void createFile(File file) {
        try {
            file.getParentFile().mkdirs();
            file.createNewFile();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

}
