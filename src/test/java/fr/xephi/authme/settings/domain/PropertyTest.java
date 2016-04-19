package fr.xephi.authme.settings.domain;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.internal.stubbing.answers.ReturnsArgumentAt;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test for {@link Property} and the contained subtypes.
 */
public class PropertyTest {

    private static YamlConfiguration configuration;

    @BeforeClass
    public static void setUpYamlConfigurationMock() {
        configuration = mock(YamlConfiguration.class);

        when(configuration.getBoolean(eq("bool.path.test"), anyBoolean())).thenReturn(true);
        when(configuration.getBoolean(eq("bool.path.wrong"), anyBoolean())).thenAnswer(new ReturnsArgumentAt(1));
        when(configuration.getInt(eq("int.path.test"), anyInt())).thenReturn(27);
        when(configuration.getInt(eq("int.path.wrong"), anyInt())).thenAnswer(new ReturnsArgumentAt(1));
        when(configuration.getString(eq("str.path.test"), anyString())).thenReturn("Test value");
        when(configuration.getString(eq("str.path.wrong"), anyString())).thenAnswer(new ReturnsArgumentAt(1));
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
        Property<List<String>> property = Property.newListProperty("list.path.test", "1", "b");

        // when
        List<String> result = property.getFromFile(configuration);

        // then
        assertThat(result, contains("test1", "Test2", "3rd test"));
    }

    @Test
    public void shouldGetStringListDefault() {
        // given
        Property<List<String>> property =
            Property.newListProperty("list.path.wrong", "default", "list", "elements");

        // when
        List<String> result = property.getFromFile(configuration);

        // then
        assertThat(result, contains("default", "list", "elements"));
    }

}
