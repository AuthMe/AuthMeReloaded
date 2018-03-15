package tools.docs.translations;

import com.google.common.collect.ImmutableMap;
import tools.docs.translations.TranslationsGatherer.TranslationInfo;
import tools.utils.AutoToolTask;
import tools.utils.FileIoUtils;
import tools.utils.TagValue.NestedTagValue;
import tools.utils.TagValueHolder;
import tools.utils.ToolsConstants;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import static com.google.common.base.MoreObjects.firstNonNull;

/**
 * Generates the translations page in docs.
 */
public class TranslationPageGenerator implements AutoToolTask {

    private static final String DOCS_PAGE = ToolsConstants.DOCS_FOLDER + "translations.md";
    private static final String TEMPLATE_FILE = ToolsConstants.TOOLS_SOURCE_ROOT + "docs/translations/translations.tpl.md";
    private static final Map<String, String> LANGUAGE_NAMES = buildLanguageNames();

    // Color configuration for the bars shown next to translation percentage
    /**
     * Percentage threshold under which the color will be computed from COLOR_0 to COLOR_1;
     * above which COLOR_1 to COLOR_2 is used.
     */
    private static final int COLOR_1_PERCENTAGE = 75;
    // Colors are in RGB format, displayed as an int array of three values whose entries are in the range [0, 15].
    private static final int[] COLOR_0 = { 9,  0, 0};
    private static final int[] COLOR_1 = {12,  9, 0};
    private static final int[] COLOR_2 = { 6, 15, 6};

    @Override
    public String getTaskName() {
        return "updateTranslations";
    }

    @Override
    public void executeDefault() {
        TranslationsGatherer gatherer = new TranslationsGatherer();
        NestedTagValue translationValuesHolder = new NestedTagValue();

        for (TranslationInfo translation : gatherer.getTranslationInfo()) {
            int percentage = (int) Math.round(translation.getPercentTranslated() * 100);
            String name = firstNonNull(LANGUAGE_NAMES.get(translation.getCode()), "?");
            TagValueHolder valueHolder = TagValueHolder.create()
                .put("code", translation.getCode())
                .put("name", name)
                .put("percentage", Integer.toString(percentage))
                .put("color", computeColor(percentage));
            translationValuesHolder.add(valueHolder);
        }

        TagValueHolder tags = TagValueHolder.create().put("languages", translationValuesHolder);
        FileIoUtils.generateFileFromTemplate(TEMPLATE_FILE, DOCS_PAGE, tags);
        System.out.println("Wrote to '" + DOCS_PAGE + "'");
    }

    /**
     * Returns the color for the given percentage as a 6-digit hex color code.
     *
     * @param percentage the percentage to generate a color for
     * @return the color
     */
    private String computeColor(int percentage) {
        int[] color;
        if (percentage < COLOR_1_PERCENTAGE) {
            color = computeColor(percentage, COLOR_0, COLOR_1, 0, COLOR_1_PERCENTAGE);
        } else {
            color = computeColor(percentage, COLOR_1, COLOR_2, COLOR_1_PERCENTAGE, 100);
        }

        return Arrays.stream(color)
            .mapToObj(i -> Integer.toString(i, 16))
            .map(s -> s + s)
            .collect(Collectors.joining());
    }

    /**
     * Computes the color as the transition between two given colors.
     *
     * @param percentage the percentage to compute the color for
     * @param colorA the color at the start of the range
     * @param colorB the color at the end of the range
     * @param rangeMin range start
     * @param rangeMax range end
     * @return color for the given percentage
     */
    private static int[] computeColor(int percentage, int[] colorA, int[] colorB, int rangeMin, int rangeMax) {
        double max = rangeMax - rangeMin;
        double n = percentage - rangeMin;

        return new int[]{
            (int) (colorA[0] + n / max * (colorB[0] - colorA[0])),
            (int) (colorA[1] + n / max * (colorB[1] - colorA[1])),
            (int) (colorA[2] + n / max * (colorB[2] - colorA[2]))
        };
    }

    /**
     * @return map of language code -> language name
     */
    private static Map<String, String> buildLanguageNames() {
        return ImmutableMap.<String, String>builder()
            .put("bg", "Bulgarian")
            .put("br", "Brazilian")
            .put("cz", "Czech")
            .put("de", "German")
            .put("en", "English")
            .put("eo", "Esperanto")
            .put("es", "Spanish")
            .put("et", "Estonian")
            .put("eu", "Basque")
            .put("fi", "Finnish")
            .put("fr", "French")
            .put("gl", "Galician")
            .put("hu", "Hungarian")
            .put("id", "Indonesian")
            .put("it", "Italian")
            .put("ko", "Korean")
            .put("lt", "Lithuanian")
            .put("nl", "Dutch")
            .put("pl", "Polish")
            .put("pt", "Portuguese")
            .put("ro", "Romanian")
            .put("ru", "Russian")
            .put("sk", "Slovakian")
            .put("tr", "Turkish")
            .put("uk", "Ukrainian")
            .put("vn", "Vietnamese")
            .put("zhcn", "Chinese (China)")
            .put("zhhk", "Chinese (Hong Kong)")
            .put("zhmc", "Chinese (Macau)")
            .put("zhtw", "Chinese (Taiwan)")
            .build();
    }
}
