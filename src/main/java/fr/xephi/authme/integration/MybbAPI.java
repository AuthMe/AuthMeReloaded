package fr.xephi.authme.integration;

import com.zaxxer.hikari.HikariDataSource;
import fr.xephi.authme.AuthMe;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.cache.auth.PlayerAuth;
import fr.xephi.authme.settings.properties.DatabaseSettings;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;

public class MybbAPI {

    private static MybbAPI sInstance;

    private static final int MAX_LIFE_TIME = 180000; // 3 min
    private static final int IDLE_TIMEOUT = 60000; // 1 min
    private static final int MINIMUM_IDLE = 2;

    private HikariDataSource mDataSource;
    private String mHost;
    private String mPort;
    private String mDatabase;
    private String mTable;
    private String mUsername;
    private String mPassword;

    public MybbAPI() {
        setupArguments();
        setConnectionArguments();
    }

    public void register(PlayerAuth auth, String password, String email) {
        try {
            Connection conn = mDataSource.getConnection();
            //mybbPart
            String sqlForo = String.format("INSERT INTO %s ( `username`, `password`, `salt`,`loginkey`, `email`,`usergroup`, `regdate`,`regip`) VALUES ( ?, ?, ?, ?, ?, ?, ?, ?)",
                mTable);
            String salt = new BigInteger(40, new SecureRandom()).toString(32);
            String loginKey = new BigInteger(250, new SecureRandom()).toString(32);
            PreparedStatement ps2 = conn.prepareStatement(sqlForo);
            ps2.setString(1, auth.getRealName());
            ps2.setString(2, md5hash(md5hash(salt) + md5hash(password)));
            ps2.setString(3, salt);
            ps2.setString(4, loginKey);
            ps2.setString(5, email);
            ps2.setInt(6, 2);
            ps2.setLong(7, (System.currentTimeMillis() / 1000L));
            ps2.setString(8, auth.getIp());
            ps2.executeUpdate();
            ps2.close();
        } catch (Exception e) {
            ConsoleLogger.info("MyBBApi: Something went wrong when i tried to register the user " + auth.getNickname());
        }

    }

    private void setupArguments() {
        mHost = AuthMe.getInstance().getSettings().getProperty(DatabaseSettings.MYSQL_MYBB_IP);
        mPort = AuthMe.getInstance().getSettings().getProperty(DatabaseSettings.MYSQL_MYBB_PORT);
        mDatabase = AuthMe.getInstance().getSettings().getProperty(DatabaseSettings.MYSQL_MYBB_DATABASE);
        mUsername = AuthMe.getInstance().getSettings().getProperty(DatabaseSettings.MYSQL_MYBB_USERNAME);
        mPassword = AuthMe.getInstance().getSettings().getProperty(DatabaseSettings.MYSQL_MYBB_PASSWORD);
        mTable = AuthMe.getInstance().getSettings().getProperty(DatabaseSettings.MYSQL_MYBB_TABLE);
    }

    private synchronized void setConnectionArguments() throws RuntimeException {
        mDataSource = new HikariDataSource();
        mDataSource.setPoolName("AuthMeMyBBAPIMYSQLPool");
        mDataSource.setDriverClassName("com.mysql.jdbc.Driver");
        mDataSource.setJdbcUrl("jdbc:mysql://" + mHost + ":" + mPort + "/" + mDatabase);
        mDataSource.addDataSourceProperty("rewriteBatchedStatements", "true");
        mDataSource.addDataSourceProperty("jdbcCompliantTruncation", "false");
        mDataSource.addDataSourceProperty("cachePrepStmts", "true");
        mDataSource.addDataSourceProperty("prepStmtCacheSize", "250");
        mDataSource.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

        //set utf-8 as default encoding
        mDataSource.addDataSourceProperty("characterEncoding", "utf8");
        mDataSource.addDataSourceProperty("encoding", "UTF-8");
        mDataSource.addDataSourceProperty("useUnicode", "true");

        mDataSource.setUsername(mUsername);
        mDataSource.setPassword(mPassword);
        mDataSource.setInitializationFailFast(true); // Don't start the plugin if the database is unavailable
        mDataSource.setMaxLifetime(MAX_LIFE_TIME);
        mDataSource.setIdleTimeout(IDLE_TIMEOUT);
        mDataSource.setMinimumIdle(MINIMUM_IDLE);
        mDataSource.setMaximumPoolSize((Runtime.getRuntime().availableProcessors() * 2) + 1);
        ConsoleLogger.info("MyBBApi Connection arguments loaded, Hikari ConnectionPool ready!");
    }

    private String md5hash(String s) {
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        md.update(s.getBytes());

        byte byteData[] = md.digest();

        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < byteData.length; i++) {
            sb.append(Integer.toString((byteData[i] & 0xff) + 0x100, 16).substring(1));
        }

        return sb.toString();
    }


    public static MybbAPI getInstance() {
        if (sInstance == null) sInstance = new MybbAPI();
        return sInstance;
    }
}
