package fr.xephi.authme.settings;

import fr.xephi.authme.settings.domain.Property;
import fr.xephi.authme.settings.properties.TestConfiguration;
import fr.xephi.authme.settings.properties.TestEnum;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.File;

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
import static org.mockito.Mockito.when;

/**
 * Test for {@link NewSetting}.
 */
public class NewSettingTest {

    @Test
    public void shouldLoadAllConfigs() {
        // given
        YamlConfiguration configuration = mock(YamlConfiguration.class);
        given(configuration.getString(anyString(), anyString())).willAnswer(withDefaultArgument());
        given(configuration.getBoolean(anyString(), anyBoolean())).willAnswer(withDefaultArgument());
        given(configuration.getDouble(anyString(), anyDouble())).willAnswer(withDefaultArgument());
        given(configuration.getInt(anyString(), anyInt())).willAnswer(withDefaultArgument());

        setReturnValue(configuration, TestConfiguration.VERSION_NUMBER, 20);
        setReturnValue(configuration, TestConfiguration.SKIP_BORING_FEATURES, true);
        setReturnValue(configuration, TestConfiguration.RATIO_ORDER, TestEnum.THIRD);
        setReturnValue(configuration, TestConfiguration.SYSTEM_NAME, "myTestSys");

        // when / then
        NewSetting settings = new NewSetting(configuration, null, null);

        assertThat(settings.getProperty(TestConfiguration.VERSION_NUMBER), equalTo(20));
        assertThat(settings.getProperty(TestConfiguration.SKIP_BORING_FEATURES), equalTo(true));
        assertThat(settings.getProperty(TestConfiguration.RATIO_ORDER), equalTo(TestEnum.THIRD));
        assertThat(settings.getProperty(TestConfiguration.SYSTEM_NAME), equalTo("myTestSys"));

        assertDefaultValue(TestConfiguration.DURATION_IN_SECONDS, settings);
        assertDefaultValue(TestConfiguration.DUST_LEVEL, settings);
        assertDefaultValue(TestConfiguration.COOL_OPTIONS, settings);
    }

    @Test
    public void shouldReturnDefaultFile() {
        // given
        YamlConfiguration configuration = mock(YamlConfiguration.class);
        NewSetting settings = new NewSetting(configuration, null, null);

        // when
        File defaultFile = settings.getDefaultMessagesFile();

        // then
        assertThat(defaultFile, not(nullValue()));
        assertThat(defaultFile.exists(), equalTo(true));
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

    private static <T> Answer<T> withDefaultArgument() {
        return new Answer<T>() {
            @Override
            public T answer(InvocationOnMock invocation) throws Throwable {
                // Return the second parameter -> the default
                return (T) invocation.getArguments()[1];
            }
        };
    }

}
