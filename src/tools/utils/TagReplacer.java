package utils;

import fr.xephi.authme.util.StringUtils;

import java.util.Date;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class responsible for replacing template tags to actual content.
 * For all files, the following tags are defined:
 * <ul>
 *   <li>{gen_date} – the generation date</li>
 *   <li>{gen_warning} - warning not to edit the generated file directly</li>
 * </ul>
 */
public class TagReplacer {

    private TagReplacer() {
    }

    /**
     * Replace a template with default tags and custom ones supplied by a map.
     *
     * @param template The template to process
     * @param tags Map with additional tags, e.g. a map entry with key "foo" and value "bar" will replace
     *  any occurrences of "{foo}" to "bar".
     * @return The filled template
     */
    public static String applyReplacements(String template, Map<String, String> tags) {
        String result = template;
        for (Map.Entry<String, String> tagRule : tags.entrySet()) {
            final String name = tagRule.getKey();
            final String value = tagRule.getValue();

            result = replaceOptionalTag(result, name, value)
                .replace("{" + name + "}", value);
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

    private static String replaceOptionalTag(String text, String tagName, String tagValue) {
        Pattern regex = Pattern.compile("\\[" + tagName + "\\](.*?)\\[/" + tagName + "\\]", Pattern.DOTALL);
        Matcher matcher = regex.matcher(text);

        if (!matcher.find()) {
            // Couldn't find results, so just return text as it is
            return text;
        } else if (StringUtils.isEmpty(tagValue)) {
            // Tag is empty, replace [tagName]some_text[/tagName] to nothing
            return matcher.replaceAll("");
        } else {
            // Tag is not empty, so replace [tagName]some_text[/tagName] to some_text
            return matcher.replaceAll(matcher.group(1));
        }
    }


}
