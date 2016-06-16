package tools.messages.translation;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.IOException;
import java.util.regex.Pattern;

/**
 * Extension of {@link YamlConfiguration} to customize the writing style.
 */
public class AuthMeYamlConfiguration extends YamlConfiguration {

    // Differences to YamlConfiguration: Texts are always in single quotes
    // and line breaks are only applied after 200 chars
    @Override
    public String saveToString() {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setDefaultScalarStyle(DumperOptions.ScalarStyle.SINGLE_QUOTED);
        options.setPrettyFlow(true);
        options.setWidth(200);
        Yaml yaml = new Yaml(options);

        String header = buildHeader();
        String dump = yaml.dump(getValues(false));

        if (dump.equals(BLANK_CONFIG)) {
            dump = "";
        }
        // By setting the scalar style to SINGLE_QUOTED both keys and values will be enclosed in single quotes.
        // We want all texts wrapped in single quotes, but not the keys. Seems like this is not possible in SnakeYAML
        dump = Pattern.compile("^'([a-zA-Z0-9-_]+)': ", Pattern.MULTILINE)
                      .matcher(dump).replaceAll("$1: ");

        return header + dump;
    }

    /**
     * Behaves similarly to {@link YamlConfiguration#loadConfiguration(File)} but returns an object
     * of this class instead.
     *
     * @param file the file to load
     * @return the constructed AuthMeYamlConfiguration instance
     */
    public static AuthMeYamlConfiguration loadConfiguration(File file) {
        AuthMeYamlConfiguration config = new AuthMeYamlConfiguration();
        try {
            config.load(file);
        } catch (IOException | InvalidConfigurationException ex) {
            throw new IllegalStateException(ex);
        }
        return config;
    }
}
