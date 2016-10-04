package fr.xephi.authme.settings;

import com.github.authme.configme.knownproperties.PropertyEntry;
import com.github.authme.configme.knownproperties.PropertyFieldsCollector;
import com.github.authme.configme.migration.PlainMigrationService;
import com.github.authme.configme.properties.Property;
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
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;

import static fr.xephi.authme.settings.properties.PluginSettings.MESSAGES_LANGUAGE;
import static fr.xephi.authme.util.FileUtils.makePath;
import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.arrayWithSize;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link Settings}.
 */
public class SettingsTest {
    
    private static final List<PropertyEntry> knownProperties =
        PropertyFieldsCollector.getAllProperties(TestConfiguration.class);

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
    public void shouldReturnDefaultFile() throws IOException {
        // given
        PropertyResource resource = mock(PropertyResource.class);
        List<PropertyEntry> knownProperties = Collections.emptyList();
        Settings settings = new Settings(testPluginFolder, resource, new PlainMigrationService(), knownProperties);

        // when
        String defaultFile = settings.getDefaultMessagesFile();

        // then
        assertThat(defaultFile, not(nullValue()));
        InputStream stream = this.getClass().getResourceAsStream(defaultFile);
        assertThat(stream, not(nullValue()));
        assertThat(stream.read(), not(equalTo(0)));
    }

    @Test
    public void shouldReturnMessagesFile() {
        // given
        // Use some code that is for sure not present in our JAR
        String languageCode = "notinjar";
        File file = new File(testPluginFolder, makePath("messages", "messages_" + languageCode + ".yml"));
        createFile(file);

        PropertyResource resource = mock(PropertyResource.class);
        given(resource.contains(anyString())).willReturn(true);
        setReturnValue(resource, MESSAGES_LANGUAGE, languageCode);
        Settings settings = new Settings(testPluginFolder, resource,
            TestSettingsMigrationServices.alwaysFulfilled(), knownProperties);

        // when
        File messagesFile = settings.getMessagesFile();

        // then
        assertThat(messagesFile.getPath(), endsWith("messages_" + languageCode + ".yml"));
        assertThat(messagesFile.exists(), equalTo(true));
    }

    @Test
    public void shouldCopyDefaultForUnknownLanguageCode() {
        // given
        PropertyResource resource = mock(PropertyResource.class);
        given(resource.contains(anyString())).willReturn(true);
        setReturnValue(resource, MESSAGES_LANGUAGE, "doesntexist");
        Settings settings = new Settings(testPluginFolder, resource,
            TestSettingsMigrationServices.alwaysFulfilled(), knownProperties);

        // when
        File messagesFile = settings.getMessagesFile();

        // then
        assertThat(messagesFile.getPath(), endsWith("messages_en.yml"));
        assertThat(messagesFile.exists(), equalTo(true));
    }

    @Test
    public void shouldLoadWelcomeMessage() throws IOException {
        // given
        String welcomeMessage = "This is my welcome message for testing\nBye!";
        File welcomeFile = new File(testPluginFolder, "welcome.txt");
        createFile(welcomeFile);
        Files.write(welcomeFile.toPath(), welcomeMessage.getBytes());

        PropertyResource resource = mock(PropertyResource.class);
        setReturnValue(resource, RegistrationSettings.USE_WELCOME_MESSAGE, true);
        Settings settings = new Settings(testPluginFolder, resource,
            TestSettingsMigrationServices.alwaysFulfilled(), knownProperties);

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
            TestSettingsMigrationServices.alwaysFulfilled(), knownProperties);

        // when
        String result = settings.getPasswordEmailMessage();

        // then
        assertThat(result, equalTo(emailMessage));
    }

    private static <T> void setReturnValue(PropertyResource resource, Property<T> property, T value) {
        if (value instanceof String) {
            when(resource.getString(eq(property.getPath()))).thenReturn((String) value);
        } else if (value instanceof Integer) {
            when(resource.getInt(eq(property.getPath()))).thenReturn((Integer) value);
        } else if (value instanceof Boolean) {
            when(resource.getBoolean(eq(property.getPath()))).thenReturn((Boolean) value);
        } else if (value instanceof Enum<?>) {
            when(resource.getString(property.getPath())).thenReturn(((Enum<?>) value).name());
        } else {
            throw new UnsupportedOperationException("Value has unsupported type '"
                + (value == null ? "null" : value.getClass().getSimpleName()) + "'");
        }
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
