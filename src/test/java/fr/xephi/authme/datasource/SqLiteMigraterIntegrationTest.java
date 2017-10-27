package fr.xephi.authme.datasource;

import com.google.common.collect.Lists;
import com.google.common.io.Files;
import fr.xephi.authme.TestHelper;
import fr.xephi.authme.data.auth.PlayerAuth;
import fr.xephi.authme.settings.Settings;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;

import static fr.xephi.authme.AuthMeMatchers.hasAuthBasicData;
import static fr.xephi.authme.AuthMeMatchers.hasAuthLocation;
import static fr.xephi.authme.datasource.SqlDataSourceTestUtil.createSqlite;
import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Integration test for {@link SqLiteMigrater}. Uses a real SQLite database.
 */
public class SqLiteMigraterIntegrationTest {

    private File dataFolder;
    private SQLite sqLite;

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Before
    public void setup() throws SQLException, IOException, NoSuchMethodException {
        TestHelper.setupLogger();

        Settings settings = mock(Settings.class);
        TestHelper.returnDefaultsForAllProperties(settings);

        File sqliteDbFile = TestHelper.getJarFile(TestHelper.PROJECT_ROOT + "datasource/sqlite.april2016.db");
        dataFolder = temporaryFolder.newFolder();
        File tempFile = new File(dataFolder, "authme.db");
        Files.copy(sqliteDbFile, tempFile);

        Connection con = DriverManager.getConnection("jdbc:sqlite:" + tempFile.getPath());
        sqLite = createSqlite(settings, dataFolder, con);

    }

    @Test
    public void shouldRun() throws ClassNotFoundException, SQLException {
        // given / when
        sqLite.setup();
        sqLite.migrateIfNeeded();

        // then
        List<PlayerAuth> auths = sqLite.getAllAuths();
        assertThat(Lists.transform(auths, PlayerAuth::getNickname),
            containsInAnyOrder("mysql1", "mysql2", "mysql3", "mysql4", "mysql5", "mysql6"));
        PlayerAuth auth1 = getByNameOrFail("mysql1", auths);
        assertThat(auth1, hasAuthBasicData("mysql1", "mysql1", "user1@example.com", "192.168.4.41"));
        assertThat(auth1, hasAuthLocation(0, 0, 0, "world1", 0, 0));
        assertThat(auth1.getLastLogin(), equalTo(1472992664137L));
        PlayerAuth auth2 = getByNameOrFail("mysql2", auths);
        assertThat(auth2, hasAuthBasicData("mysql2", "Player", "user2@example.com", null));
        assertThat(auth2, hasAuthLocation(0, 0, 0, "world2", 0, 0));
        assertThat(auth2.getLastLogin(), equalTo(1472992668391L));
        PlayerAuth auth3 = getByNameOrFail("mysql3", auths);
        assertThat(auth3, hasAuthBasicData("mysql3", "mysql3", null, "132.54.76.98"));
        assertThat(auth3, hasAuthLocation(0, 0, 0, "world3", 0, 0));
        assertThat(auth3.getLastLogin(), equalTo(1472992672790L));
        PlayerAuth auth4 = getByNameOrFail("mysql4", auths);
        assertThat(auth4, hasAuthBasicData("mysql4", "MySQL4", null, null));
        assertThat(auth4, hasAuthLocation(25, 4, 17, "world4", 0, 0));
        assertThat(auth4.getLastLogin(), equalTo(1472992676790L));
        PlayerAuth auth5 = getByNameOrFail("mysql5", auths);
        assertThat(auth5, hasAuthBasicData("mysql5", "mysql5", null, null));
        assertThat(auth5, hasAuthLocation(0, 0, 0, "world5", 0, 0));
        assertThat(auth5.getLastLogin(), equalTo(1472992680922L));
        PlayerAuth auth6 = getByNameOrFail("mysql6", auths);
        assertThat(auth6, hasAuthBasicData("mysql6", "MySql6", "user6@example.com", "44.45.67.188"));
        assertThat(auth6, hasAuthLocation(28.5, 53.43, -147.23, "world6", 0, 0));
        assertThat(auth6.getLastLogin(), equalTo(1472992686300L));

        // Check that backup was made
        File backupsFolder = new File(dataFolder, "backups");
        assertThat(backupsFolder.exists(), equalTo(true));
        assertThat(backupsFolder.isDirectory(), equalTo(true));
        assertThat(backupsFolder.list(), arrayContaining(containsString("authme")));
    }

    private static PlayerAuth getByNameOrFail(String name, List<PlayerAuth> auths) {
        return auths.stream()
            .filter(auth -> name.equals(auth.getNickname()))
            .findFirst().orElseThrow(() -> new IllegalStateException("No PlayerAuth with name '" + name + "'"));
    }
}
