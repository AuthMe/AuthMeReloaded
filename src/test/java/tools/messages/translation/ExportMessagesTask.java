package tools.messages.translation;

import com.google.common.io.CharStreams;
import com.google.gson.Gson;
import fr.xephi.authme.message.MessageKey;
import fr.xephi.authme.util.StringUtils;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import tools.utils.ToolTask;
import tools.utils.ToolsConstants;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Task to export a language's messages to the remote translation service.
 */
public class ExportMessagesTask implements ToolTask {

    /** The folder containing the messages files. */
    protected static final String MESSAGES_FOLDER = ToolsConstants.MAIN_RESOURCES_ROOT + "messages/";
    /** The remote URL to send an updated file to. */
    private static final String UPDATE_URL = "http://jalu.ch/ext/authme/update.php";
    private final Gson gson = new Gson();

    @Override
    public String getTaskName() {
        return "exportMessages";
    }

    @Override
    public void execute(Scanner scanner) {
        System.out.println("Enter language code of messages to export:");
        String languageCode = scanner.nextLine().trim();

        File file = new File(MESSAGES_FOLDER + "messages_" + languageCode + ".yml");
        if (!file.exists()) {
            throw new IllegalStateException("File '" + file.getAbsolutePath() + "' does not exist");
        }

        FileConfiguration configuration = YamlConfiguration.loadConfiguration(file);
        String json = convertToJson(languageCode, loadDefaultMessages(), configuration);

        String result = sendJsonToRemote(languageCode, json);
        System.out.println("Answer: " + result);
    }

    protected String convertToJson(String code, FileConfiguration defaultMessages, FileConfiguration messageFile) {
        List<MessageExport> list = new ArrayList<>();
        for (MessageKey key : MessageKey.values()) {
            list.add(new MessageExport(key.getKey(), key.getTags(), getString(key, defaultMessages),
                getString(key, messageFile)));
        }

        return gson.toJson(new LanguageExport(code, list));
    }

    protected FileConfiguration loadDefaultMessages() {
        return YamlConfiguration.loadConfiguration(new File(MESSAGES_FOLDER + "messages_en.yml"));
    }

    private static String getString(MessageKey key, FileConfiguration configuration) {
        return configuration.getString(key.getKey(), "");
    }

    private static String sendJsonToRemote(String language, String json) {
        try {
            String encodedData = "file=" + URLEncoder.encode(json, "UTF-8")
                + "&language=" + URLEncoder.encode(language, "UTF-8");

            URL url = new URL(UPDATE_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setRequestProperty("Content-Length", String.valueOf(encodedData.length()));
            OutputStream os = conn.getOutputStream();
            os.write(encodedData.getBytes());
            os.flush();
            os.close();

            return "Response code: " + conn.getResponseCode()
                + "\n" + inputStreamToString(conn.getInputStream());
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String inputStreamToString(InputStream is) {
        try (InputStreamReader isr = new InputStreamReader(is)) {
            return CharStreams.toString(isr);
        } catch (IOException e) {
            return "Failed to read output - " + StringUtils.formatException(e);
        }
    }

}
