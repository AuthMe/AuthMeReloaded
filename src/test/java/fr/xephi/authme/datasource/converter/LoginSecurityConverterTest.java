package fr.xephi.authme.datasource.converter;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import fr.xephi.authme.TestHelper;
import fr.xephi.authme.data.auth.PlayerAuth;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.ConverterSettings;
import org.bukkit.command.CommandSender;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import static fr.xephi.authme.AuthMeMatchers.equalToHash;
import static fr.xephi.authme.AuthMeMatchers.hasAuthLocation;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Test for {@link LoginSecurityConverter}.
 */
@ExtendWith(MockitoExtension.class)
class LoginSecurityConverterTest {

    private LoginSecurityConverter converter;

    @Mock
    private DataSource dataSource;
    @Mock
    private Settings settings;

    private File dataFolder = new File("."); // not used but required for injection

    @BeforeAll
    static void initLogger() {
        TestHelper.setupLogger();
    }

    @BeforeEach
    void setUpConverter() {
        given(settings.getProperty(ConverterSettings.LOGINSECURITY_USE_SQLITE)).willReturn(true);
        converter = new LoginSecurityConverter(dataFolder, dataSource, settings);
    }

    @Test
    void shouldConvertFromSqlite() throws SQLException {
        // given
        Connection connection = converter.createSqliteConnection(
            TestHelper.TEST_RESOURCES_FOLDER + TestHelper.PROJECT_ROOT + "datasource/converter/LoginSecurity.db");
        CommandSender sender = mock(CommandSender.class);

        // when
        converter.performConversion(sender, connection);

        // then
        ArgumentCaptor<PlayerAuth> captor = ArgumentCaptor.forClass(PlayerAuth.class);
        verify(dataSource, times(3)).saveAuth(captor.capture());
        assertThat(captor.getAllValues().get(0).getNickname(), equalTo("player1"));
        assertThat(captor.getAllValues().get(0).getRealName(), equalTo("Player1"));
        assertThat(captor.getAllValues().get(0).getLastLogin(), equalTo(1494242093652L));
        assertThat(captor.getAllValues().get(0).getRegistrationDate(), equalTo(1494242093400L));
        assertThat(captor.getAllValues().get(0).getPassword(), equalToHash("$2a$10$E1Ri7XKeIIBv4qVaiPplgepT7QH9xGFh3hbHfcmCjq7hiW.UBTiGK"));
        assertThat(captor.getAllValues().get(0).getLastIp(), nullValue());

        assertThat(captor.getAllValues().get(1).getNickname(), equalTo("player2"));
        assertThat(captor.getAllValues().get(1).getLastLogin(), equalTo(1494242174589L));
        assertThat(captor.getAllValues().get(1).getLastIp(), equalTo("127.4.5.6"));

        assertThat(captor.getAllValues().get(2).getRealName(), equalTo("Player3"));
        assertThat(captor.getAllValues().get(2).getPassword(), equalToHash("$2a$10$WFui8KSXMLDOVXKFpCLyPukPi4M82w1cv/rNojsAnwJjba3pp8sba"));
        assertThat(captor.getAllValues().get(2), hasAuthLocation(14.24, 67.99, -12.83, "hubb", -10f, 185f));
        assertThat(captor.getAllValues().get(2).getLastIp(), nullValue());
        assertIsCloseTo(captor.getAllValues().get(2).getRegistrationDate(), System.currentTimeMillis(), 500L);
    }

    // Note ljacqu 20171014: JDBC mapping of a Date column to a java.sql.Date is difficult to handle,
    // cf. https://stackoverflow.com/questions/9202857/timezones-in-sql-date-vs-java-sql-date
    // For H2 it looks like Date#getTime returns the millis of the date at 12AM in the system's timezone,
    // so we check with a margin of 12 hours to cover most cases...
    @Test
    void shouldConvertFromMySql() throws IOException, SQLException {
        // given
        Connection connection = initializeMySqlTable();
        CommandSender sender = mock(CommandSender.class);

        // when
        converter.performConversion(sender, connection);

        // then
        ArgumentCaptor<PlayerAuth> captor = ArgumentCaptor.forClass(PlayerAuth.class);
        verify(dataSource, times(3)).saveAuth(captor.capture());
        assertThat(captor.getAllValues().get(0).getNickname(), equalTo("player1"));
        assertThat(captor.getAllValues().get(0).getRealName(), equalTo("Player1"));
        assertThat(captor.getAllValues().get(0).getLastLogin(), equalTo(1494242093000L));
        assertThat(captor.getAllValues().get(0).getPassword(), equalToHash("$2a$10$E1Ri7XKeIIBv4qVaiPplgepT7QH9xGFh3hbHfcmCjq7hiW.UBTiGK"));
        assertThat(captor.getAllValues().get(0).getLastIp(), nullValue());
        assertIsCloseTo(captor.getAllValues().get(0).getRegistrationDate(), 1494201600000L, 12 * 60 * 60 * 1000);

        assertThat(captor.getAllValues().get(1).getNickname(), equalTo("player2"));
        assertThat(captor.getAllValues().get(1).getLastLogin(), equalTo(1489317753000L));
        assertThat(captor.getAllValues().get(1).getLastIp(), equalTo("127.4.5.6"));

        assertThat(captor.getAllValues().get(2).getRealName(), equalTo("Player3"));
        assertThat(captor.getAllValues().get(2).getPassword(), equalToHash("$2a$10$WFui8KSXMLDOVXKFpCLyPukPi4M82w1cv/rNojsAnwJjba3pp8sba"));
        assertThat(captor.getAllValues().get(2), hasAuthLocation(14.24, 67.99, -12.83, "hubb", -10f, 185f));
    }

    private Connection initializeMySqlTable() throws IOException, SQLException {
        File sqlInitFile = TestHelper.getJarFile(TestHelper.PROJECT_ROOT + "datasource/converter/loginsecurity.sql");
        String initStatement = new String(Files.readAllBytes(sqlInitFile.toPath()));

        HikariConfig config = new HikariConfig();
        config.setDataSourceClassName("org.h2.jdbcx.JdbcDataSource");
        config.setConnectionTestQuery("VALUES 1");
        config.addDataSourceProperty("URL", "jdbc:h2:mem:test");
        config.addDataSourceProperty("user", "sa");
        config.addDataSourceProperty("password", "sa");
        HikariDataSource ds = new HikariDataSource(config);
        Connection connection = ds.getConnection();

        try (Statement st = connection.createStatement()) {
            st.execute("DROP TABLE IF EXISTS authme");
            st.execute(initStatement);
        }
        return connection;
    }

    private static void assertIsCloseTo(long value1, long value2, long tolerance) {
        assertThat(Math.abs(value1 - value2), not(greaterThan(tolerance)));
    }
}
