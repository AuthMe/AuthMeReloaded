package fr.xephi.authme.settings.domain;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Test for {@link EnumPropertyType}.
 */
public class EnumPropertyTypeTest {

    @Test
    public void shouldReturnCorrectEnumValue() {
        // given
        PropertyType<TestEnum> propertyType = new EnumPropertyType<>(TestEnum.class);
        Property<TestEnum> property = Property.newProperty(TestEnum.class, "enum.path", TestEnum.ENTRY_C);
        YamlConfiguration configuration = mock(YamlConfiguration.class);
        given(configuration.getString(property.getPath())).willReturn("Entry_B");

        // when
        TestEnum result = propertyType.getFromFile(property, configuration);

        // then
        assertThat(result, equalTo(TestEnum.ENTRY_B));
    }

    @Test
    public void shouldFallBackToDefaultForInvalidValue() {
        // given
        PropertyType<TestEnum> propertyType = new EnumPropertyType<>(TestEnum.class);
        Property<TestEnum> property = Property.newProperty(TestEnum.class, "enum.path", TestEnum.ENTRY_C);
        YamlConfiguration configuration = mock(YamlConfiguration.class);
        given(configuration.getString(property.getPath())).willReturn("Bogus");

        // when
        TestEnum result = propertyType.getFromFile(property, configuration);

        // then
        assertThat(result, equalTo(TestEnum.ENTRY_C));
    }

    @Test
    public void shouldFallBackToDefaultForNonExistentValue() {
        // given
        PropertyType<TestEnum> propertyType = new EnumPropertyType<>(TestEnum.class);
        Property<TestEnum> property = Property.newProperty(TestEnum.class, "enum.path", TestEnum.ENTRY_C);
        YamlConfiguration configuration = mock(YamlConfiguration.class);
        given(configuration.getString(property.getPath())).willReturn(null);

        // when
        TestEnum result = propertyType.getFromFile(property, configuration);

        // then
        assertThat(result, equalTo(TestEnum.ENTRY_C));
    }

    @Test
    public void shouldReturnTrueForContainsCheck() {
        // given
        PropertyType<TestEnum> propertyType = new EnumPropertyType<>(TestEnum.class);
        Property<TestEnum> property = Property.newProperty(TestEnum.class, "my.test.path", TestEnum.ENTRY_C);
        YamlConfiguration configuration = mock(YamlConfiguration.class);
        given(configuration.contains(property.getPath())).willReturn(true);
        given(configuration.getString(property.getPath())).willReturn("ENTRY_B");

        // when
        boolean result = propertyType.contains(property, configuration);

        // then
        assertThat(result, equalTo(true));
    }

    @Test
    public void shouldReturnFalseForFileWithoutConfig() {
        // given
        PropertyType<TestEnum> propertyType = new EnumPropertyType<>(TestEnum.class);
        Property<TestEnum> property = Property.newProperty(TestEnum.class, "my.test.path", TestEnum.ENTRY_C);
        YamlConfiguration configuration = mock(YamlConfiguration.class);
        given(configuration.contains(property.getPath())).willReturn(false);

        // when
        boolean result = propertyType.contains(property, configuration);

        // then
        assertThat(result, equalTo(false));
    }

    @Test
    public void shouldReturnFalseForUnknownValue() {
        // given
        PropertyType<TestEnum> propertyType = new EnumPropertyType<>(TestEnum.class);
        Property<TestEnum> property = Property.newProperty(TestEnum.class, "my.test.path", TestEnum.ENTRY_C);
        YamlConfiguration configuration = mock(YamlConfiguration.class);
        given(configuration.contains(property.getPath())).willReturn(true);
        given(configuration.getString(property.getPath())).willReturn("wrong value");

        // when
        boolean result = propertyType.contains(property, configuration);

        // then
        assertThat(result, equalTo(false));
    }


    private enum TestEnum {

        ENTRY_A,

        ENTRY_B,

        ENTRY_C

    }
}
