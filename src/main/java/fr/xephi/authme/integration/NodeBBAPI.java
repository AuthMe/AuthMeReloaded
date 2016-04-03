package fr.xephi.authme.integration;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.cache.auth.PlayerAuth;
import fr.xephi.authme.settings.properties.DatabaseSettings;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class NodeBBAPI {

    private static final String FORCE_LOGIN_PASS = "dontneed";
    private static final String MSG_EMAIL_ALREADY_TAKEN = "Email taken";
    private static final String BODY = "{\"_uid\":1,\"username\":\"%username%\",\"password\":\"%password%\", "
                                        + "\"email\":\"%email%\"}";
    private static final String BODY_WITHOUT_EMAIL = "{\"_uid\":1,\"username\":\"%username%\","
                                                        + "\"password\":\"%password%\"}";
    private static final String KEY_USERNAME = "%username%";
    private static final String KEY_PASSWORD = "%password%";
    private static final String KEY_EMAIL = "%email%";

    private static final String SUCCESSFUL = "NodeBBAPI: The account %username% was be created!";

    private static NodeBBAPI sInstance;

    private String mEndpoint;
    private String mMasterKey;

    public NodeBBAPI() {
        mEndpoint = AuthMe.getInstance().getSettings().getProperty(DatabaseSettings.NODEBB_API_ENDPOINT);
        mMasterKey = AuthMe.getInstance().getSettings().getProperty(DatabaseSettings.NODEBB_API_MASTER_KEY);
    }

    public void doRegisterRequest(PlayerAuth auth, String password) {
        if (password.equalsIgnoreCase(FORCE_LOGIN_PASS)) return;

        String input;
        if (auth.getEmail().isEmpty()) {
            input = BODY_WITHOUT_EMAIL;
            input = input.replace(KEY_USERNAME, auth.getRealName());
            input = input.replace(KEY_PASSWORD, password);
        } else {
            input = BODY;
            input = input.replace(KEY_USERNAME, auth.getRealName());
            input = input.replace(KEY_PASSWORD, password);
            input = input.replace(KEY_EMAIL, auth.getEmail());
        }

        try {
            URL url = new URL(mEndpoint);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.addRequestProperty("Authorization", mMasterKey);
            conn.addRequestProperty("Content-Type", "application/json");

            OutputStream os = conn.getOutputStream();
            os.write(input.getBytes());
            os.flush();

            if (conn.getResponseCode() == HttpURLConnection.HTTP_INTERNAL_ERROR) {
                ConsoleLogger.info("Error 1 " + conn.getResponseMessage());
                BufferedReader br = new BufferedReader(new InputStreamReader(
                    (conn.getInputStream())));

                String output;
                while ((output = br.readLine()) != null) {
                    if (output.contains(MSG_EMAIL_ALREADY_TAKEN)) {
                        ConsoleLogger.info("Error 2");
                        auth.setEmail("");
                        conn.disconnect();
                        doRegisterRequest(auth, password);
                        return;
                    }
                }
                return;
            } else if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
                throw new RuntimeException("Failed " + auth.getRealName() + ": HTTP error code : "
                    + conn.getResponseCode());
            }

            conn.disconnect();
            ConsoleLogger.info(SUCCESSFUL.replace(KEY_USERNAME, auth.getRealName()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static NodeBBAPI getInstance() {
        if (sInstance == null) sInstance = new NodeBBAPI();
        return sInstance;
    }

}
