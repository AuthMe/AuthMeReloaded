package tools.docs.translations;

import ch.jalu.configme.resource.PropertyReader;
import ch.jalu.configme.resource.YamlFileReader;
import fr.xephi.authme.message.MessageKey;
import fr.xephi.authme.message.MessagePathHelper;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static tools.utils.FileIoUtils.listFilesOrThrow;
import static tools.utils.ToolsConstants.MAIN_RESOURCES_ROOT;

/**
 * Gathers all available translations of AuthMe.
 */
public class TranslationsGatherer {

    private static final String MESSAGES_FOLDER = MAIN_RESOURCES_ROOT + MessagePathHelper.MESSAGES_FOLDER;

    private List<TranslationInfo> translationInfo = new ArrayList<>();

    public TranslationsGatherer() {
        gatherTranslations();
        translationInfo.sort(Comparator.comparing(TranslationsGatherer::getSortCode));
    }

    public List<TranslationInfo> getTranslationInfo() {
        return translationInfo;
    }

    private void gatherTranslations() {
        File[] files = listFilesOrThrow(new File(MESSAGES_FOLDER));
        for (File file : files) {
            String code = MessagePathHelper.getLanguageIfIsMessagesFile(file.getName());
            if (code != null) {
                processMessagesFile(code, file);
            }
        }
    }

    private void processMessagesFile(String code, File file) {
        PropertyReader reader = new YamlFileReader(file);
        int availableMessages = 0;
        for (MessageKey key : MessageKey.values()) {
            if (reader.contains(key.getKey())) {
                ++availableMessages;
            }
        }
        translationInfo.add(new TranslationInfo(code, (double) availableMessages / MessageKey.values().length));
    }

    /**
     * Returns the language code from the translation info for sorting purposes.
     * Returns "a" for "en" language code to sort English on top.
     *
     * @param info the translation info
     * @return the language code for sorting
     */
    private static String getSortCode(TranslationInfo info) {
        return MessagePathHelper.DEFAULT_LANGUAGE.equals(info.code) ? "a" : info.code;
    }

    public static final class TranslationInfo {
        private final String code;
        private final double percentTranslated;

        TranslationInfo(String code, double percentTranslated) {
            this.code = code;
            this.percentTranslated = percentTranslated;
        }

        public String getCode() {
            return code;
        }

        public double getPercentTranslated() {
            return percentTranslated;
        }
    }

}
