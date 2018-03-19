package tools.docs.translations;

import fr.xephi.authme.message.MessageKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import tools.utils.ToolsConstants;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static tools.utils.FileIoUtils.listFilesOrThrow;

/**
 * Gathers all available translations of AuthMe.
 */
public class TranslationsGatherer {

    private static final Pattern MESSAGES_PATTERN = Pattern.compile("messages_([a-z]{2,4})\\.yml");
    private static final String MESSAGES_FOLDER = ToolsConstants.MAIN_RESOURCES_ROOT + "messages/";

    private List<TranslationInfo> translationInfo = new ArrayList<>();

    public TranslationsGatherer() {
        gatherTranslations();
        translationInfo.sort((e1, e2) -> getSortCode(e1).compareTo(getSortCode(e2)));
    }

    public List<TranslationInfo> getTranslationInfo() {
        return translationInfo;
    }

    private void gatherTranslations() {
        File[] files = listFilesOrThrow(new File(MESSAGES_FOLDER));
        for (File file : files) {
            String code = getLanguageCode(file.getName());
            if (code != null) {
                processMessagesFile(code, file);
            }
        }
    }

    private void processMessagesFile(String code, File file) {
        FileConfiguration configuration = YamlConfiguration.loadConfiguration(file);
        int availableMessages = 0;
        for (MessageKey key : MessageKey.values()) {
            if (configuration.contains(key.getKey())) {
                ++availableMessages;
            }
        }
        translationInfo.add(new TranslationInfo(code, (double) availableMessages / MessageKey.values().length));
    }

    private String getLanguageCode(String messagesFile) {
        Matcher matcher = MESSAGES_PATTERN.matcher(messagesFile);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    /**
     * Returns the language code from the translation info for sorting purposes.
     * Returns "a" for "en" language code to sort English on top.
     *
     * @param info the translation info
     * @return the language code for sorting
     */
    private static String getSortCode(TranslationInfo info) {
        return "en".equals(info.code) ? "a" : info.code;
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
