package fr.xephi.authme.settings;

import fr.xephi.authme.ConsoleLoggerTestInitializer;
import fr.xephi.authme.settings.domain.Property;
import fr.xephi.authme.settings.properties.TestConfiguration;
import fr.xephi.authme.settings.properties.TestEnum;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.internal.stubbing.answers.ReturnsArgumentAt;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import static fr.xephi.authme.settings.properties.PluginSettings.MESSAGES_LANGUAGE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
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

    @BeforeClass
    public static void setUpLogger() {
        ConsoleLoggerTestInitializer.setupLogger();
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
        NewSetting settings = new NewSetting(configuration, null, null, null);

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
        NewSetting settings = new NewSetting(configuration, null, null, null);

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
        NewSetting settings = new NewSetting(configuration, null, null, null);

        // when
        settings.setProperty(TestConfiguration.DUST_LEVEL, -4);

        // then
        verify(configuration).set(TestConfiguration.DUST_LEVEL.getPath(), -4);
    }

    @Test
    public void shouldReturnMessagesFile() {
        // given
        YamlConfiguration configuration = mock(YamlConfiguration.class);
        given(configuration.contains(anyString())).willReturn(true);
        given(configuration.getString(eq(MESSAGES_LANGUAGE.getPath()), anyString())).willReturn("fr");
        NewSetting settings = new NewSetting(configuration, null, TestConfiguration.generatePropertyMap(),
            new PlainSettingsMigrationService());

        // when
        File messagesFile = settings.getMessagesFile();

        // then
        System.out.println(messagesFile.getPath());
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

}
