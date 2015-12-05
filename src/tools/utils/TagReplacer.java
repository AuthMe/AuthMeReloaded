package utils;

import java.util.Date;
import java.util.Map;

/**
 * Class responsible for replacing template tags to actual content.
 * For all files, the following tags are defined:
 * <ul>
 *   <li>{gen_date} – the generation date</li>
 *   <li>{gen_warning} - warning not to edit the generated file directly</li>
 * </ul>
 */
public class TagReplacer {

    /**
     * Replace a template with default tags and custom ones supplied by a map.
     *
     * @param template The template to process
     * @param tags Map with additional tags, e.g. a map entry with key "foo" and value "bar" will replace
     *  any occurrences of "{foo}" to "bar".
     * @return The filled template
     */
    public static String applyReplacements(String template, Map<String, Object> tags) {
        String result = template;
        for (Map.Entry<String, Object> tagRule : tags.entrySet()) {
            result = result.replace("{" + tagRule.getKey() + "}", tagRule.getValue().toString());
        }

        return applyReplacements(result);
    }

    /**
     * Apply the default tag replacements.
     *
     * @param template The template to process
     * @return The filled template
     */
    public static String applyReplacements(String template) {
        return template
            .replace("{gen_date}", new Date().toString())
            .replace("{gen_warning}", "AUTO-GENERATED FILE! Do not edit this directly");
    }


}
