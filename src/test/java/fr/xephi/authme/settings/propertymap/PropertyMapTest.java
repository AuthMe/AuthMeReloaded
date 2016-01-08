package fr.xephi.authme.settings.propertymap;

import fr.xephi.authme.settings.domain.Property;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;


import static org.hamcrest.Matchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test for {@link PropertyMap}.
 */
public class PropertyMapTest {

    @Test
    public void shouldKeepEntriesByInsertionAndGroup() {
        // given
        List<String> paths = Arrays.asList("japan", "indonesia.jakarta", "japan.tokyo", "china.shanghai", "egypt.cairo",
            "china.shenzhen", "china", "indonesia.jakarta.tugu", "egypt", "japan.nagoya", "japan.tokyo.taito");
        PropertyMap map = new PropertyMap();

        // when
        for (String path : paths) {
            Property<?> property = createPropertyWithPath(path);
            map.put(property, new String[0]);
        }

        // then
        Set<Map.Entry<Property<?>, String[]>> entrySet = map.entrySet();
        List<String> resultPaths = new ArrayList<>(entrySet.size());
        for (Map.Entry<Property<?>, String[]> entry : entrySet) {
            resultPaths.add(entry.getKey().getPath());
        }

        Assert.assertThat(resultPaths, contains("japan", "japan.tokyo", "japan.tokyo.taito", "japan.nagoya",
            "indonesia.jakarta", "indonesia.jakarta.tugu", "china", "china.shanghai", "china.shenzhen",
            "egypt", "egypt.cairo"));
    }

    private static Property<?> createPropertyWithPath(String path) {
        Property<?> property = mock(Property.class);
        when(property.getPath()).thenReturn(path);
        return property;
    }
}
