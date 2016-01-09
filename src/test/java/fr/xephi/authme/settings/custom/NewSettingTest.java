package fr.xephi.authme.settings.custom;

import fr.xephi.authme.settings.domain.Property;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.File;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
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
        YamlConfiguration file = mock(YamlConfiguration.class);
        given(file.getString(anyString(), anyString())).willAnswer(withDefaultArgument());
        given(file.getBoolean(anyString(), anyBoolean())).willAnswer(withDefaultArgument());
        given(file.getDouble(anyString(), anyDouble())).willAnswer(withDefaultArgument());
        given(file.getInt(anyString(), anyInt())).willAnswer(withDefaultArgument());

        setReturnValue(file, TestConfiguration.VERSION_NUMBER, 20);
        setReturnValue(file, TestConfiguration.SKIP_BORING_FEATURES, true);
        setReturnValue(file, TestConfiguration.RATIO_LIMIT, 4.25);
        setReturnValue(file, TestConfiguration.SYSTEM_NAME, "myTestSys");

        // when / then
        NewSetting settings = new NewSetting(file, new File("conf.txt"), null);

        assertThat(settings.getProperty(TestConfiguration.VERSION_NUMBER), equalTo(20));
        assertThat(settings.getProperty(TestConfiguration.SKIP_BORING_FEATURES), equalTo(true));
        assertThat(settings.getProperty(TestConfiguration.RATIO_LIMIT), equalTo(4.25));
        assertThat(settings.getProperty(TestConfiguration.SYSTEM_NAME), equalTo("myTestSys"));

        assertDefaultValue(TestConfiguration.DURATION_IN_SECONDS, settings);
        assertDefaultValue(TestConfiguration.DUST_LEVEL, settings);
        assertDefaultValue(TestConfiguration.COOL_OPTIONS, settings);
    }

    private static <T> void setReturnValue(YamlConfiguration config, Property<T> property, T value) {
        if (value instanceof String) {
            when(config.getString(eq(property.getPath()), anyString())).thenReturn((String) value);
        } else if (value instanceof Integer) {
            when(config.getInt(eq(property.getPath()), anyInt())).thenReturn((Integer) value);
        } else if (value instanceof Boolean) {
            when(config.getBoolean(eq(property.getPath()), anyBoolean())).thenReturn((Boolean) value);
        } else if (value instanceof Double) {
            when(config.getDouble(eq(property.getPath()), anyDouble())).thenReturn((Double) value);
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
