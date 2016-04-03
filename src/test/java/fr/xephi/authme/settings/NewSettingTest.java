package fr.xephi.authme.settings;

import fr.xephi.authme.ConsoleLoggerTestInitializer;
import fr.xephi.authme.settings.domain.Property;
import fr.xephi.authme.settings.properties.RegistrationSettings;
import fr.xephi.authme.settings.properties.TestConfiguration;
import fr.xephi.authme.settings.properties.TestEnum;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.internal.stubbing.answers.ReturnsArgumentAt;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.List;

import static fr.xephi.authme.settings.properties.PluginSettings.MESSAGES_LANGUAGE;
import static fr.xephi.authme.util.StringUtils.makePath;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyDouble;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link NewSetting}.
 */
public class NewSettingTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();
    private File testPluginFolder;

    @BeforeClass
    public static void setUpLogger() {
        ConsoleLoggerTestInitializer.setupLogger();
    }

    @Before
    public void setUpTestPluginFolder() throws IOException {
        testPluginFolder = temporaryFolder.newFolder();
    }

    @Test
    public void shouldLoadAllConfigs() {
        // given
        YamlConfiguration configuration = mock(YamlConfiguration.class);
        given(configuration.getString(anyString(), anyString())).willAnswer(new ReturnsArgumentAt(1));
        given(configuration.getBoolean(anyString(), anyBoolean())).willAnswer(new ReturnsArgumentAt(1));
        given(configuration.getDouble(anyString(), anyDouble())).willAnswer(new ReturnsArgumentAt(1));
        given(configuration.getInt(anyString(), anyInt())).willAnswer(new ReturnsArgumentAt(1));

        setReturnValue(configuration, TestConfiguration.VERSION_NUMBER, 20);
        setReturnValue(configuration, TestConfiguration.SKIP_BORING_FEATURES, true);
        setReturnValue(configuration, TestConfiguration.RATIO_ORDER, TestEnum.THIRD);
        setReturnValue(configuration, TestConfiguration.SYSTEM_NAME, "myTestSys");

        // when / then
        NewSetting settings = new NewSetting(configuration, null, null, null, null);

        assertThat(settings.getProperty(TestConfiguration.VERSION_NUMBER), equalTo(20));
        assertThat(settings.getProperty(TestConfiguration.SKIP_BORING_FEATURES), equalTo(true));
        assertThat(settings.getProperty(TestConfiguration.RATIO_ORDER), equalTo(TestEnum.THIRD));
        assertThat(settings.getProperty(TestConfiguration.SYSTEM_NAME), equalTo("myTestSys"));

        assertDefaultValue(TestConfiguration.DURATION_IN_SECONDS, settings);
        assertDefaultValue(TestConfiguration.DUST_LEVEL, settings);
        assertDefaultValue(TestConfiguration.COOL_OPTIONS, settings);
    }

    @Test
    public void shouldReturnDefaultFile() throws IOException {
        // given
        YamlConfiguration configuration = mock(YamlConfiguration.class);
        NewSetting settings = new NewSetting(configuration, null, null, null, null);

        // when
        String defaultFile = settings.getDefaultMessagesFile();

        // then
        assertThat(defaultFile, not(nullValue()));
        InputStream stream = this.getClass().getResourceAsStream(defaultFile);
        assertThat(stream, not(nullValue()));
        assertThat(stream.read(), not(equalTo(0)));
    }

    @Test
    public void shouldSetProperty() {
        // given
        YamlConfiguration configuration = mock(YamlConfiguration.class);
        NewSetting settings = new NewSetting(configuration, null, null, null, null);

        // when
        settings.setProperty(TestConfiguration.DUST_LEVEL, -4);

        // then
        verify(configuration).set(TestConfiguration.DUST_LEVEL.getPath(), -4);
    }

    @Test
    public void shouldReturnMessagesFile() {
        // given
        // Use some code that is for sure not present in our JAR
        String languageCode = "notinjar";
        File file = new File(testPluginFolder, makePath("messages", "messages_" + languageCode + ".yml"));
        createFile(file);

        YamlConfiguration configuration = mock(YamlConfiguration.class);
        given(configuration.contains(anyString())).willReturn(true);
        setReturnValue(configuration, MESSAGES_LANGUAGE, languageCode);
        NewSetting settings = new NewSetting(configuration, null, testPluginFolder,
            TestConfiguration.generatePropertyMap(), TestSettingsMigrationServices.alwaysFulfilled());

        // when
        File messagesFile = settings.getMessagesFile();

        // then
        assertThat(messagesFile.getPath(), endsWith("messages_" + languageCode + ".yml"));
        assertThat(messagesFile.exists(), equalTo(true));
    }

    @Test
    public void shouldCopyDefaultForUnknownLanguageCode() {
        // given
        YamlConfiguration configuration = mock(YamlConfiguration.class);
        given(configuration.contains(anyString())).willReturn(true);
        setReturnValue(configuration, MESSAGES_LANGUAGE, "doesntexist");
        NewSetting settings = new NewSetting(configuration, null, testPluginFolder,
            TestConfiguration.generatePropertyMap(), TestSettingsMigrationServices.alwaysFulfilled());

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

        YamlConfiguration configuration = mock(YamlConfiguration.class);
        setReturnValue(configuration, RegistrationSettings.USE_WELCOME_MESSAGE, true);
        NewSetting settings = new NewSetting(configuration, null, testPluginFolder,
            TestConfiguration.generatePropertyMap(), TestSettingsMigrationServices.alwaysFulfilled());

        // when
        List<String> result = settings.getWelcomeMessage();

        // then
        assertThat(result, hasSize(2));
        assertThat(result.get(0), equalTo(welcomeMessage.split("\\n")[0]));
        assertThat(result.get(1), equalTo(welcomeMessage.split("\\n")[1]));
    }

    @Test
    public void shouldLoadEmailMessage() throws IOException {
        // given
        String emailMessage = "Sample email message\nThat's all!";
        File emailFile = new File(testPluginFolder, "email.html");
        createFile(emailFile);
        Files.write(emailFile.toPath(), emailMessage.getBytes());

        YamlConfiguration configuration = mock(YamlConfiguration.class);
        NewSetting settings = new NewSetting(configuration, null, testPluginFolder,
            TestConfiguration.generatePropertyMap(), TestSettingsMigrationServices.alwaysFulfilled());

        // when
        String result = settings.getEmailMessage();

        // then
        assertThat(result, equalTo(emailMessage));
    }

    private static <T> void setReturnValue(YamlConfiguration config, Property<T> property, T value) {
        if (value instanceof String) {
            when(config.getString(eq(property.getPath()), anyString())).thenReturn((String) value);
        } else if (value instanceof Integer) {
            when(config.getInt(eq(property.getPath()), anyInt())).thenReturn((Integer) value);
        } else if (value instanceof Boolean) {
            when(config.getBoolean(eq(property.getPath()), anyBoolean())).thenReturn((Boolean) value);
        } else if (value instanceof Enum<?>) {
            when(config.getString(property.getPath())).thenReturn(((Enum<?>) value).name());
        } else {
            throw new UnsupportedOperationException("Value has unsupported type '"
                + (value == null ? "null" : value.getClass().getSimpleName()) + "'");
        }
    }

    private static void assertDefaultValue(Property<?> property, NewSetting setting) {
        assertThat(property.getPath() + " has default value",
            setting.getProperty(property).equals(property.getDefaultValue()), equalTo(true));
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
