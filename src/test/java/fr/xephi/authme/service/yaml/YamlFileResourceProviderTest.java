package fr.xephi.authme.service.yaml;

import ch.jalu.configme.resource.YamlFileResource;
import fr.xephi.authme.TestHelper;
import org.junit.Test;
import org.yaml.snakeyaml.parser.ParserException;

import java.io.File;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * Test for {@link YamlFileResourceProvider}.
 */
public class YamlFileResourceProviderTest {

    @Test
    public void shouldLoadValidFile() {
        // given
        File yamlFile = TestHelper.getJarFile(TestHelper.PROJECT_ROOT + "service/yaml/validYaml.yml");

        // when
        YamlFileResource resource = YamlFileResourceProvider.loadFromFile(yamlFile);

        // then
        assertThat(resource.createReader().getString("test.jkl"), equalTo("Test test"));
    }

    @Test
    public void shouldThrowForInvalidFile() {
        // given
        File yamlFile = TestHelper.getJarFile(TestHelper.PROJECT_ROOT + "service/yaml/invalidYaml.yml");

        // when
        try {
            YamlFileResourceProvider.loadFromFile(yamlFile).createReader();

            // then
            fail("Expected exception to be thrown");
        } catch (YamlParseException e) {
            assertThat(e.getFile(), equalTo(yamlFile.getPath()));
            assertThat(e.getCause(), instanceOf(ParserException.class));
        }
    }
}
