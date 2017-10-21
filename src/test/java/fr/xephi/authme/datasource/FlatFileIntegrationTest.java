package fr.xephi.authme.datasource;

import com.google.common.io.Files;
import fr.xephi.authme.TestHelper;
import fr.xephi.authme.data.auth.PlayerAuth;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

import static fr.xephi.authme.AuthMeMatchers.equalToHash;
import static fr.xephi.authme.AuthMeMatchers.hasAuthBasicData;
import static fr.xephi.authme.AuthMeMatchers.hasAuthLocation;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;

/**
 * Integration test for the deprecated {@link FlatFile} datasource. The flatfile datasource is no longer used.
 * Essentially, the only time we use it is in {@link fr.xephi.authme.datasource.converter.ForceFlatToSqlite},
 * which requires {@link FlatFile#getAllAuths()}.
 */
public class FlatFileIntegrationTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private DataSource dataSource;

    @Before
    public void copyFileToTemporaryFolder() throws IOException {
        File originalFile = TestHelper.getJarFile(TestHelper.PROJECT_ROOT + "datasource/flatfile-test.txt");
        File copy = temporaryFolder.newFile();
        Files.copy(originalFile, copy);
        dataSource = new FlatFile(copy);
    }

    @Test
    public void shouldReturnIfAuthIsAvailableOrNot() {
        // given / when
        boolean isBobbyAvailable = dataSource.isAuthAvailable("bobby");
        boolean isChrisAvailable = dataSource.isAuthAvailable("chris");
        boolean isUserAvailable = dataSource.isAuthAvailable("USER");

        // then
        assertThat(isBobbyAvailable, equalTo(true));
        assertThat(isChrisAvailable, equalTo(false));
        assertThat(isUserAvailable, equalTo(true));
    }

    @Test
    public void shouldReturnAllAuths() {
        // given / when
        List<PlayerAuth> authList = dataSource.getAllAuths();

        // then
        assertThat(authList, hasSize(7));
        assertThat(getName("bobby", authList), hasAuthBasicData("bobby", "bobby", null, "123.45.67.89"));
        assertThat(getName("bobby", authList), hasAuthLocation(1.05, 2.1, 4.2, "world", 0, 0));
        assertThat(getName("bobby", authList).getPassword(), equalToHash("$SHA$11aa0706173d7272$dbba966"));
        assertThat(getName("twofields", authList), hasAuthBasicData("twofields", "twofields", null, null));
        assertThat(getName("twofields", authList).getPassword(), equalToHash("hash1234"));
        assertThat(getName("threefields", authList), hasAuthBasicData("threefields", "threefields", null, "33.33.33.33"));
        assertThat(getName("fourfields", authList), hasAuthBasicData("fourfields", "fourfields", null, "4.4.4.4"));
        assertThat(getName("fourfields", authList).getLastLogin(), equalTo(404040404L));
        assertThat(getName("sevenfields", authList), hasAuthLocation(7.7, 14.14, 21.21, "world", 0, 0));
        assertThat(getName("eightfields", authList), hasAuthLocation(8.8, 17.6, 26.4, "eightworld", 0, 0));
        assertThat(getName("eightfields", authList).getLastLogin(), equalTo(1234567888L));
        assertThat(getName("eightfields", authList).getPassword(), equalToHash("hash8168"));
    }

    @Test
    public void shouldAddAuth() {
        // given / when
        boolean response = dataSource.saveAuth(
            PlayerAuth.builder().name("Test").email("user@EXAMPLE.org").lastIp("123.45.67.77").build());
        List<PlayerAuth> authList = dataSource.getAllAuths();

        // then
        assertThat(response, equalTo(true));
        assertThat(authList, hasSize(8));
        assertThat(authList, hasItem(hasAuthBasicData("test", "test", "user@EXAMPLE.org", "123.45.67.77")));
    }

    private static PlayerAuth getName(String name, Collection<PlayerAuth> auths) {
        for (PlayerAuth auth : auths) {
            if (name.equals(auth.getNickname())) {
                return auth;
            }
        }
        throw new IllegalStateException("Did not find auth with name '" + name + "'");
    }

}
