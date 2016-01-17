package fr.xephi.authme.settings.domain;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyDouble;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test for {@link PropertyType} and the contained subtypes.
 */
public class PropertyTypeTest {

    private static YamlConfiguration configuration;

    @BeforeClass
    public static void setUpYamlConfigurationMock() {
        configuration = mock(YamlConfiguration.class);

        when(configuration.getBoolean(eq("bool.path.test"), anyBoolean())).thenReturn(true);
        when(configuration.getBoolean(eq("bool.path.wrong"), anyBoolean())).thenAnswer(secondParameter());
        when(configuration.getDouble(eq("double.path.test"), anyDouble())).thenReturn(-6.4);
        when(configuration.getDouble(eq("double.path.wrong"), anyDouble())).thenAnswer(secondParameter());
        when(configuration.getInt(eq("int.path.test"), anyInt())).thenReturn(27);
        when(configuration.getInt(eq("int.path.wrong"), anyInt())).thenAnswer(secondParameter());
        when(configuration.getString(eq("str.path.test"), anyString())).thenReturn("Test value");
        when(configuration.getString(eq("str.path.wrong"), anyString())).thenAnswer(secondParameter());
        when(configuration.isList("list.path.test")).thenReturn(true);
        when(configuration.getStringList("list.path.test")).thenReturn(Arrays.asList("test1", "Test2", "3rd test"));
        when(configuration.isList("list.path.wrong")).thenReturn(false);
    }

    /* Boolean */
    @Test
    public void shouldGetBoolValue() {
        // given
        Property<Boolean> property = Property.newProperty("bool.path.test", false);

        // when
        boolean result = property.getFromFile(configuration);

        // then
        assertThat(result, equalTo(true));
    }

    @Test
    public void shouldGetBoolDefault() {
        // given
        Property<Boolean> property = Property.newProperty("bool.path.wrong", true);

        // when
        boolean result = property.getFromFile(configuration);

        // then
        assertThat(result, equalTo(true));
    }

    /* Double */
    @Test
    public void shouldGetDoubleValue() {
        // given
        Property<Double> property = Property.newProperty(PropertyType.DOUBLE, "double.path.test", 3.8);

        // when
        double result = property.getFromFile(configuration);

        // then
        assertThat(result, equalTo(-6.4));
    }

    @Test
    public void shouldGetDoubleDefault() {
        // given
        Property<Double> property = Property.newProperty(PropertyType.DOUBLE, "double.path.wrong", 12.0);

        // when
        double result = property.getFromFile(configuration);

        // then
        assertThat(result, equalTo(12.0));
    }

    /* Integer */
    @Test
    public void shouldGetIntValue() {
        // given
        Property<Integer> property = Property.newProperty("int.path.test", 3);

        // when
        int result = property.getFromFile(configuration);

        // then
        assertThat(result, equalTo(27));
    }

    @Test
    public void shouldGetIntDefault() {
        // given
        Property<Integer> property = Property.newProperty("int.path.wrong", -10);

        // when
        int result = property.getFromFile(configuration);

        // then
        assertThat(result, equalTo(-10));
    }

    /* String */
    @Test
    public void shouldGetStringValue() {
        // given
        Property<String> property = Property.newProperty("str.path.test", "unused default");

        // when
        String result = property.getFromFile(configuration);

        // then
        assertThat(result, equalTo("Test value"));
    }

    @Test
    public void shouldGetStringDefault() {
        // given
        Property<String> property = Property.newProperty("str.path.wrong", "given default value");

        // when
        String result = property.getFromFile(configuration);

        // then
        assertThat(result, equalTo("given default value"));
    }

    /* String list */
    @Test
    public void shouldGetStringListValue() {
        // given
        Property<List<String>> property = Property.newProperty(PropertyType.STRING_LIST, "list.path.test", "1", "b");

        // when
        List<String> result = property.getFromFile(configuration);

        // then
        assertThat(result, contains("test1", "Test2", "3rd test"));
    }

    @Test
    public void shouldGetStringListDefault() {
        // given
        Property<List<String>> property =
            Property.newProperty(PropertyType.STRING_LIST, "list.path.wrong", "default", "list", "elements");

        // when
        List<String> result = property.getFromFile(configuration);

        // then
        assertThat(result, contains("default", "list", "elements"));
    }

    private static <T> Answer<T> secondParameter() {
        return new Answer<T>() {
            @Override
            public T answer(InvocationOnMock invocation) throws Throwable {
                // Return the second parameter -> the default
                return (T) invocation.getArguments()[1];
            }
        };
    }
}
