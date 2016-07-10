package tools.utils;

import tools.utils.TagValue.NestedTagValue;
import tools.utils.TagValue.TextTagValue;

import java.util.Date;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class responsible for replacing template tags to actual content.
 * For all files, the following tags are defined:
 * <ul>
 *   <li>{gen_date}    – the generation date</li>
 *   <li>{gen_warning} - warning not to edit the generated file directly</li>
 *   <li>{gen_footer}  - info footer with a link to the dev repo so users can find the most up-to-date
 *                       version (in case the page is viewed on a fork)</li>
 * </ul>
 */
public class TagReplacer {

    private TagReplacer() {
    }

    /**
     * Replace a template with default tags and custom ones supplied by a map.
     *
     * @param template The template to process
     * @param tagValues Container with tags and their associated values
     * @return The filled template
     */
    public static String applyReplacements(String template, TagValueHolder tagValues) {
        String result = template;
        for (Map.Entry<String, TagValue<?>> tagRule : tagValues.getValues().entrySet()) {
            final String name = tagRule.getKey();

            if (tagRule.getValue() instanceof TextTagValue) {
                final TextTagValue value = (TextTagValue) tagRule.getValue();
                result = replaceOptionalTag(result, name, value)
                    .replace("{" + name + "}", value.getValue());
            } else if (tagRule.getValue() instanceof NestedTagValue) {
                final NestedTagValue value = (NestedTagValue) tagRule.getValue();
                result = replaceIterateTag(replaceOptionalTag(result, name, value), name, value);
            } else {
                throw new IllegalStateException("Unknown tag value type");
            }
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
        String curDate = new Date().toString();
        return template
            .replace("{gen_date}", curDate)
            .replace("{gen_warning}", "AUTO-GENERATED FILE! Do not edit this directly")
            .replace("{gen_footer}", "---\n\nThis page was automatically generated on the"
                + " [AuthMe/AuthMeReloaded repository](" + ToolsConstants.DOCS_FOLDER_URL + ")"
                + " on " + curDate);
    }

    private static String replaceOptionalTag(String text, String tagName, TagValue<?> tagValue) {
        Pattern regex = Pattern.compile("\\[" + tagName + "](.*?)\\[/" + tagName + "]", Pattern.DOTALL);
        Matcher matcher = regex.matcher(text);

        if (!matcher.find()) {
            // Couldn't find results, so just return text as it is
            return text;
        } else if (tagValue.isEmpty()) {
            // Tag is empty, replace [tagName]some_text[/tagName] to nothing
            return matcher.replaceAll("");
        } else {
            // Tag is not empty, so replace [tagName]some_text[/tagName] to some_text
            return matcher.replaceAll(matcher.group(1));
        }
    }

    /**
     * Replace iterating tags with the value. Tags of the type [#tag]...[/#tag] specify to iterate over the
     * entries in {@link NestedTagValue} and to apply any replacements in there.
     *
     * @param text The file text
     * @param tagName The tag name to handle
     * @param tagValue The associated value
     * @return The text with the applied replacement
     */
    private static String replaceIterateTag(String text, String tagName, NestedTagValue tagValue) {
        Pattern regex = Pattern.compile("\\[#" + tagName + "](.*?)\\[/#" + tagName + "]\\s?", Pattern.DOTALL);
        Matcher matcher = regex.matcher(text);

        if (!matcher.find()) {
            return text;
        } else if (tagValue.isEmpty()) {
            return matcher.replaceAll("");
        }
        final String innerTemplate = matcher.group(1).trim() + "\n";
        String result = "";
        for (TagValueHolder entry : tagValue.getValue()) {
            result += applyReplacements(innerTemplate, entry);
        }
        return matcher.replaceAll(result);
    }

}
