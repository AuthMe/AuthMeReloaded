package hashmethods;

import com.google.common.collect.ImmutableMap;
import fr.xephi.authme.security.HashAlgorithm;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.util.WrapperMock;
import utils.FileUtils;
import utils.TagReplacer;
import utils.ToolTask;
import utils.ToolsConstants;

import java.util.Map;
import java.util.Scanner;

/**
 * Task for generating the markdown page describing the AuthMe hash algorithms.
 *
 * @see {@link fr.xephi.authme.security.HashAlgorithm}
 */
public class HashAlgorithmsDescriptionTask implements ToolTask {

    private static final String CUR_FOLDER = ToolsConstants.TOOLS_SOURCE_ROOT + "hashmethods/";
    private static final String OUTPUT_FILE = ToolsConstants.DOCS_FOLDER + "hash_algorithms.md";

    @Override
    public void execute(Scanner scanner) {
        // Unfortunately, we need the Wrapper to be around to work with Settings, and certain encryption methods
        // directly read from the Settings file
        WrapperMock.createInstance();
        Settings.bCryptLog2Rounds = 8;
        Settings.saltLength = 8;

        // Gather info and construct a row for each method
        EncryptionMethodInfoGatherer infoGatherer = new EncryptionMethodInfoGatherer();
        Map<HashAlgorithm, MethodDescription> descriptions = infoGatherer.getDescriptions();
        final String methodRows = constructMethodRows(descriptions);

        // Write to the docs file
        Map<String, String> tags = ImmutableMap.of("method_rows", methodRows);
        FileUtils.generateFileFromTemplate(CUR_FOLDER + "hash_algorithms.tpl.md", OUTPUT_FILE, tags);
    }

    private static String constructMethodRows(Map<HashAlgorithm, MethodDescription> descriptions) {
        final String rowTemplate = FileUtils.readFromFile(CUR_FOLDER + "hash_algorithms_row.tpl.md");
        StringBuilder result = new StringBuilder();
        for (Map.Entry<HashAlgorithm, MethodDescription> entry : descriptions.entrySet()) {
            MethodDescription description = entry.getValue();
            Map<String, String> tags = ImmutableMap.<String, String>builder()
                .put("name",             asString(entry.getKey()))
                .put("recommendation",   asString(description.getUsage()))
                .put("hash_length",      asString(description.getHashLength()))
                .put("ascii_restricted", asString(description.isAsciiRestricted()))
                .put("salt_type",        asString(description.getSaltType()))
                .put("salt_length",      asString(description.getSaltLength()))
                .put("separate_salt",    asString(description.hasSeparateSalt()))
                .build();
            result.append(TagReplacer.applyReplacements(rowTemplate, tags));
        }
        return result.toString();
    }

    @Override
    public String getTaskName() {
        return "describeHashAlgos";
    }

    // ----
    // String representations
    // ----
    private static String asString(boolean value) {
        return value ? "Y" : "";
    }

    private static String asString(int value) {
        return String.valueOf(value);
    }

    private static String asString(Integer value) {
        if (value == null) {
            return "";
        }
        return String.valueOf(value);
    }

    private static String asString(HashAlgorithm value) {
        return value.toString();
    }

    private static <E extends Enum<E>> String asString(E value) {
        if (value == null) {
            return "";
        }
        // Get the enum name and replace something like "DO_NOT_USE" to "Do not use"
        String enumName = value.toString().replace("_", " ");
        return enumName.length() > 2
            ? enumName.substring(0, 1) + enumName.substring(1).toLowerCase()
            : enumName;
    }

}
