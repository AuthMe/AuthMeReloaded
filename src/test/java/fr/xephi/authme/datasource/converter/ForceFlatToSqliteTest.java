package fr.xephi.authme.datasource.converter;

import com.google.common.io.Files;
import fr.xephi.authme.TestHelper;
import fr.xephi.authme.data.auth.PlayerAuth;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.datasource.DataSourceType;
import fr.xephi.authme.datasource.FlatFile;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static fr.xephi.authme.AuthMeMatchers.hasAuthBasicData;
import static fr.xephi.authme.AuthMeMatchers.hasAuthLocation;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Test for {@link ForceFlatToSqlite}.
 */
public class ForceFlatToSqliteTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private FlatFile flatFile;

    @BeforeClass
    public static void setup() {
        TestHelper.setupLogger();
    }

    @Before
    public void copyFile() throws IOException {
        File source = TestHelper.getJarFile(TestHelper.PROJECT_ROOT + "datasource/flatfile-test.txt");
        File destination = temporaryFolder.newFile();
        Files.copy(source, destination);
        flatFile = new FlatFile(destination);
    }

    @Test
    public void shouldConvertToSqlite() {
        // given
        DataSource dataSource = mock(DataSource.class);
        given(dataSource.getType()).willReturn(DataSourceType.MYSQL);
        ForceFlatToSqlite converter = new ForceFlatToSqlite(flatFile, dataSource);

        // when
        converter.execute(null);

        // then
        ArgumentCaptor<PlayerAuth> authCaptor = ArgumentCaptor.forClass(PlayerAuth.class);
        verify(dataSource, times(7)).saveAuth(authCaptor.capture());
        List<PlayerAuth> auths = authCaptor.getAllValues();
        assertThat(auths, hasItem(hasAuthBasicData("bobby", "Player", null, "123.45.67.89")));
        assertThat(auths, hasItem(hasAuthLocation(1.05, 2.1, 4.2, "world", 0, 0)));
        assertThat(auths, hasItem(hasAuthBasicData("user", "Player", "user@example.org", "34.56.78.90")));
        assertThat(auths, hasItem(hasAuthLocation(124.1, 76.3, -127.8, "nether", 0, 0)));
        assertThat(auths, hasItem(hasAuthBasicData("eightfields", "Player", null, "6.6.6.66")));
        assertThat(auths, hasItem(hasAuthLocation(8.8, 17.6, 26.4, "eightworld", 0, 0)));
    }

}
