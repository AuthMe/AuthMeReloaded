package fr.xephi.authme.datasource;

import ch.jalu.datasourcecolumns.data.DataSourceValue;
import ch.jalu.datasourcecolumns.data.DataSourceValueImpl;
import com.google.common.collect.Lists;
import fr.xephi.authme.data.auth.PlayerAuth;
import fr.xephi.authme.security.crypts.HashedPassword;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static fr.xephi.authme.AuthMeMatchers.equalToHash;
import static fr.xephi.authme.AuthMeMatchers.hasAuthBasicData;
import static fr.xephi.authme.AuthMeMatchers.hasAuthLocation;
import static fr.xephi.authme.AuthMeMatchers.hasRegistrationInfo;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assume.assumeThat;

/**
 * Abstract class for data source integration tests.
 */
public abstract class AbstractDataSourceIntegrationTest {

    protected DataSource getDataSource() {
        return getDataSource("salt");
    }

    protected abstract DataSource getDataSource(String saltColumn);

    @Test
    public void shouldReturnIfAuthIsAvailableOrNot() {
        // given
        DataSource dataSource = getDataSource();

        // when
        boolean isBobbyAvailable = dataSource.isAuthAvailable("bobby");
        boolean isChrisAvailable = dataSource.isAuthAvailable("chris");
        boolean isUserAvailable = dataSource.isAuthAvailable("USER");

        // then
        assertThat(isBobbyAvailable, equalTo(true));
        assertThat(isChrisAvailable, equalTo(false));
        assertThat(isUserAvailable, equalTo(true));
    }

    @Test
    public void shouldReturnPassword() {
        // given
        DataSource dataSource = getDataSource();

        // when
        HashedPassword bobbyPassword = dataSource.getPassword("bobby");
        HashedPassword invalidPassword = dataSource.getPassword("doesNotExist");
        HashedPassword userPassword = dataSource.getPassword("User");

        // then
        assertThat(bobbyPassword, equalToHash("$SHA$11aa0706173d7272$dbba966"));
        assertThat(invalidPassword, nullValue());
        assertThat(userPassword, equalToHash("b28c32f624a4eb161d6adc9acb5bfc5b", "f750ba32"));
    }

    @Test
    public void shouldReturnPasswordWithEmptySaltColumn() {
        // given
        DataSource dataSource = getDataSource("");

        // when
        HashedPassword bobbyPassword = dataSource.getPassword("bobby");
        HashedPassword invalidPassword = dataSource.getPassword("doesNotExist");
        HashedPassword userPassword = dataSource.getPassword("user");

        // then
        assertThat(bobbyPassword, equalToHash("$SHA$11aa0706173d7272$dbba966"));
        assertThat(invalidPassword, nullValue());
        assertThat(userPassword, equalToHash("b28c32f624a4eb161d6adc9acb5bfc5b"));
    }

    @Test
    public void shouldGetAuth() {
        // given
        DataSource dataSource = getDataSource();

        // when
        PlayerAuth invalidAuth = dataSource.getAuth("notInDB");
        PlayerAuth bobbyAuth = dataSource.getAuth("Bobby");
        PlayerAuth userAuth = dataSource.getAuth("user");

        // then
        assertThat(invalidAuth, nullValue());

        assertThat(bobbyAuth, hasAuthBasicData("bobby", "Bobby", null, "123.45.67.89"));
        assertThat(bobbyAuth, hasAuthLocation(1.05, 2.1, 4.2, "world", -0.44f, 2.77f));
        assertThat(bobbyAuth, hasRegistrationInfo("127.0.4.22", 1436778723L));
        assertThat(bobbyAuth.getLastLogin(), equalTo(1449136800L));
        assertThat(bobbyAuth.getPassword(), equalToHash("$SHA$11aa0706173d7272$dbba966"));
        assertThat(bobbyAuth.getTotpKey(), equalTo("JBSWY3DPEHPK3PXP"));

        assertThat(userAuth, hasAuthBasicData("user", "user", "user@example.org", "34.56.78.90"));
        assertThat(userAuth, hasAuthLocation(124.1, 76.3, -127.8, "nether", 0.23f, 4.88f));
        assertThat(userAuth, hasRegistrationInfo(null, 0));
        assertThat(userAuth.getLastLogin(), equalTo(1453242857L));
        assertThat(userAuth.getPassword(), equalToHash("b28c32f624a4eb161d6adc9acb5bfc5b", "f750ba32"));
        assertThat(userAuth.getTotpKey(), nullValue());
    }

    @Test
    public void shouldCountAuthsByEmail() {
        // given
        DataSource dataSource = getDataSource();

        // when
        int userMailCount = dataSource.countAuthsByEmail("user@example.ORG");
        int invalidMailCount = dataSource.countAuthsByEmail("not.in.db@example.com");
        boolean response = dataSource.saveAuth(
            PlayerAuth.builder().name("Test").email("user@EXAMPLE.org").build());
        int newUserCount = dataSource.countAuthsByEmail("user@Example.org");

        // then
        assertThat(userMailCount, equalTo(1));
        assertThat(invalidMailCount, equalTo(0));
        assertThat(response, equalTo(true));
        assertThat(newUserCount, equalTo(2));
    }

    @Test
    public void shouldReturnAllAuths() {
        // given
        DataSource dataSource = getDataSource();

        // when
        List<PlayerAuth> authList = dataSource.getAllAuths();
        boolean response = dataSource.saveAuth(
            PlayerAuth.builder().name("Test").email("user@EXAMPLE.org").build());
        List<PlayerAuth> newAuthList = dataSource.getAllAuths();

        // then
        assertThat(response, equalTo(true));
        assertThat(authList, hasSize(2));
        assertThat(authList, hasItem(hasAuthBasicData("bobby", "Bobby", null, "123.45.67.89")));
        assertThat(newAuthList, hasSize(3));
        assertThat(newAuthList, hasItem(hasAuthBasicData("bobby", "Bobby", null, "123.45.67.89")));
    }

    @Test
    public void shouldUpdatePassword() {
        // given
        DataSource dataSource = getDataSource();
        HashedPassword newHash = new HashedPassword("new_hash");

        // when
        boolean response1 = dataSource.updatePassword("user", newHash);
        boolean response2 = dataSource.updatePassword("non-existent-name", new HashedPassword("sd"));

        // then
        assertThat(response1, equalTo(true));
        assertThat(response2, equalTo(false)); // no record modified
        assertThat(dataSource.getPassword("user"), equalToHash(newHash));
    }

    @Test
    public void shouldUpdatePasswordWithNoSalt() {
        // given
        DataSource dataSource = getDataSource("");
        HashedPassword newHash = new HashedPassword("new_hash", "1241");

        // when
        boolean response1 = dataSource.updatePassword("user", newHash);
        boolean response2 = dataSource.updatePassword("non-existent-name", new HashedPassword("asdfasdf", "a1f34ec"));

        // then
        assertThat(response1, equalTo(true));
        assertThat(response2, equalTo(false)); // no record modified
        assertThat(dataSource.getPassword("user"), equalToHash("new_hash"));
    }

    @Test
    public void shouldUpdatePasswordWithPlayerAuth() {
        // given
        DataSource dataSource = getDataSource("salt");
        PlayerAuth bobbyAuth = PlayerAuth.builder().name("bobby").password(new HashedPassword("tt", "cc")).build();
        PlayerAuth invalidAuth = PlayerAuth.builder().name("invalid").password(new HashedPassword("tt", "cc")).build();

        // when
        boolean response1 = dataSource.updatePassword(bobbyAuth);
        boolean response2 = dataSource.updatePassword(invalidAuth);

        // then
        assertThat(response1, equalTo(true));
        assertThat(response2, equalTo(false)); // no record modified
        assertThat(dataSource.getPassword("bobby"), equalToHash("tt", "cc"));
    }

    @Test
    public void shouldRemovePlayerAuth() {
        // given
        DataSource dataSource = getDataSource();

        // when
        boolean response1 = dataSource.removeAuth("Bobby");
        boolean response2 = dataSource.removeAuth("does-not-exist");

        // then
        assertThat(response1 && response2, equalTo(true));
        assertThat(dataSource.getAuth("bobby"), nullValue());
        assertThat(dataSource.isAuthAvailable("bobby"), equalTo(false));
    }

    @Test
    public void shouldUpdateSession() {
        // given
        DataSource dataSource = getDataSource();
        PlayerAuth bobby = PlayerAuth.builder()
            .name("bobby").realName("BOBBY").lastLogin(123L)
            .lastIp("12.12.12.12").build();

        // when
        boolean response = dataSource.updateSession(bobby);

        // then
        assertThat(response, equalTo(true));
        PlayerAuth result = dataSource.getAuth("bobby");
        assertThat(result, hasAuthBasicData("bobby", "BOBBY", null, "12.12.12.12"));
        assertThat(result.getLastLogin(), equalTo(123L));
    }

    @Test
    public void shouldUpdateLastLoc() {
        // given
        DataSource dataSource = getDataSource();
        PlayerAuth user = PlayerAuth.builder()
            .name("user").locX(143).locY(-42.12).locZ(29.47)
            .locWorld("the_end").locYaw(2.2f).locPitch(0.45f).build();

        // when
        boolean response = dataSource.updateQuitLoc(user);

        // then
        assertThat(response, equalTo(true));
        assertThat(dataSource.getAuth("user"), hasAuthLocation(user));
    }

    @Test
    public void shouldDeletePlayers() {
        // given
        DataSource dataSource = getDataSource();
        Set<String> playersToDelete = new HashSet<>(Arrays.asList("bobby", "doesNotExist"));
        assumeThat(dataSource.getAccountsRegistered(), equalTo(2));

        // when
        dataSource.purgeRecords(playersToDelete);

        // then
        assertThat(dataSource.getAccountsRegistered(), equalTo(1));
        assertThat(dataSource.isAuthAvailable("bobby"), equalTo(false));
        assertThat(dataSource.isAuthAvailable("user"), equalTo(true));
    }

    @Test
    public void shouldUpdateEmail() {
        // given
        DataSource dataSource = getDataSource();
        String email = "new-user@mail.tld";
        PlayerAuth userAuth = PlayerAuth.builder().name("user").email(email).build();
        PlayerAuth invalidAuth = PlayerAuth.builder().name("invalid").email("addr@example.com").build();

        // when
        boolean response1 = dataSource.updateEmail(userAuth);
        boolean response2 = dataSource.updateEmail(invalidAuth);

        // then
        assertThat(response1, equalTo(true));
        assertThat(response2, equalTo(false)); // no record modified
        assertThat(dataSource.getAllAuths(), hasItem(hasAuthBasicData("user", "user", email, "34.56.78.90")));
    }

    @Test
    public void shouldCountAuths() {
        // given
        DataSource dataSource = getDataSource();

        // when
        int initialCount = dataSource.getAccountsRegistered();
        for (int i = 0; i < 4; ++i) {
            dataSource.saveAuth(PlayerAuth.builder().name("test-" + i).build());
        }
        int endCount = dataSource.getAccountsRegistered();

        // then
        assertThat(initialCount, equalTo(2));
        assertThat(endCount, equalTo(6));
    }

    @Test
    public void shouldGetAllUsersByIp() {
        // given
        DataSource dataSource = getDataSource();

        // when
        List<String> initialList = dataSource.getAllAuthsByIp("123.45.67.89");
        List<String> emptyList = dataSource.getAllAuthsByIp("8.8.8.8");
        for (int i = 0; i < 3; ++i) {
            PlayerAuth auth = PlayerAuth.builder().name("test-" + i).lastIp("123.45.67.89").build();
            dataSource.saveAuth(auth);
            dataSource.updateSession(auth); // trigger storage of last IP
        }
        List<String> updatedList = dataSource.getAllAuthsByIp("123.45.67.89");

        // then
        assertThat(initialList, hasSize(1));
        assertThat(initialList.get(0), equalTo("bobby"));
        assertThat(emptyList, hasSize(0));
        assertThat(updatedList, hasSize(4));
        assertThat(updatedList, hasItem(equalTo("bobby")));
        assertThat(updatedList, hasItem(equalTo("test-1")));
    }

    @Test
    public void shouldUpdateRealName() {
        // given
        DataSource dataSource = getDataSource();

        // when
        boolean response1 = dataSource.updateRealName("bobby", "BOBBY");
        boolean response2 = dataSource.updateRealName("notExists", "NOTEXISTS");

        // then
        assertThat(response1, equalTo(true));
        assertThat(response2, equalTo(false)); // no record modified
        assertThat(dataSource.getAuth("bobby"), hasAuthBasicData("bobby", "BOBBY", null, "123.45.67.89"));
    }

    @Test
    public void shouldGetRecordsToPurge() {
        // given
        DataSource dataSource = getDataSource();
        // 1453242857 -> user, 1449136800 -> bobby

        PlayerAuth potato = PlayerAuth.builder().name("potato")
            .registrationDate(0L).lastLogin(1_455_000_000L).build();
        PlayerAuth tomato = PlayerAuth.builder().name("tomato")
            .registrationDate(1_457_000_000L).lastLogin(null).build();
        PlayerAuth lettuce = PlayerAuth.builder().name("Lettuce")
            .registrationDate(1_400_000_000L).lastLogin(1_453_000_000L).build();
        PlayerAuth onion = PlayerAuth.builder().name("onion")
            .registrationDate(1_200_000_000L).lastLogin(1_300_000_000L).build();
        Stream.of(potato, tomato, lettuce, onion).forEach(auth -> {
            dataSource.saveAuth(auth);
            dataSource.updateSession(auth);
        });

        // when
        Set<String> records1 = dataSource.getRecordsToPurge(1_450_000_000);
        Set<String> records2 = dataSource.getRecordsToPurge(1_460_000_000);

        // then
        assertThat(records1, containsInAnyOrder("bobby", "onion"));
        assertThat(records2, containsInAnyOrder("bobby", "onion", "user", "tomato", "potato", "lettuce"));
        // check that the entry was not deleted because of running this command
        assertThat(dataSource.isAuthAvailable("bobby"), equalTo(true));
        assertThat(dataSource.isAuthAvailable("tomato"), equalTo(true));
        assertThat(dataSource.isAuthAvailable("Lettuce"), equalTo(true));
    }

    @Test
    public void shouldPerformOperationsOnIsLoggedColumnSuccessfully() {
        DataSource dataSource = getDataSource();
        // on startup no one should be marked as logged
        assertThat(dataSource.isLogged("user"), equalTo(false));
        assertThat(dataSource.isLogged("bobby"), equalTo(false));

        // Mark user as logged
        dataSource.setLogged("user");
        // non-existent user should not break database
        dataSource.setLogged("does-not-exist");

        assertThat(dataSource.isLogged("user"), equalTo(true));
        assertThat(dataSource.isLogged("bobby"), equalTo(false));

        // Set bobby logged and unlog user
        dataSource.setLogged("bobby");
        dataSource.setUnlogged("user");

        assertThat(dataSource.isLogged("user"), equalTo(false));
        assertThat(dataSource.isLogged("bobby"), equalTo(true));

        // Set both as logged (even if Bobby already is logged)
        dataSource.setLogged("user");
        dataSource.setLogged("bobby");
        dataSource.purgeLogged();
        assertThat(dataSource.isLogged("user"), equalTo(false));
        assertThat(dataSource.isLogged("bobby"), equalTo(false));
    }

    @Test
    public void shouldPerformPurgeOperation() {
        // given
        List<String> names = Arrays.asList("Bobby", "USER", "DoesnotExist");
        DataSource dataSource = getDataSource();

        // when
        dataSource.purgeRecords(names);

        // then
        assertThat(dataSource.getAllAuths(), empty());
    }

    @Test
    public void shouldFetchEmail() {
        // given
        String user1 = "user";
        String user2 = "Bogus";
        DataSource dataSource = getDataSource();

        // when
        DataSourceValue<String> email1 = dataSource.getEmail(user1);
        DataSourceValue<String> email2 = dataSource.getEmail(user2);

        // then
        assertThat(email1.getValue(), equalTo("user@example.org"));
        assertThat(email2, is(DataSourceValueImpl.unknownRow()));
    }

    @Test
    public void shouldGetLoggedPlayersWithoutEmail() {
        // given
        DataSource dataSource = getDataSource();
        dataSource.setLogged("bobby");
        dataSource.setLogged("user");

        // when
        List<String> loggedPlayersWithEmptyMail = dataSource.getLoggedPlayersWithEmptyMail();

        // then
        assertThat(loggedPlayersWithEmptyMail, contains("Bobby"));
    }

    @Test
    public void shouldGrantAndRetrieveSessionFlag() {
        // given
        DataSource dataSource = getDataSource();

        // when
        dataSource.grantSession("Bobby");
        dataSource.grantSession("doesNotExist");

        // then
        assertThat(dataSource.hasSession("bobby"), equalTo(true));
        assertThat(dataSource.hasSession("user"), equalTo(false));
        assertThat(dataSource.hasSession("bogus"), equalTo(false));
    }

    @Test
    public void shouldRevokeSession() {
        // given
        DataSource dataSource = getDataSource();
        dataSource.grantSession("bobby");
        dataSource.grantSession("user");

        // when
        dataSource.revokeSession("bobby");
        dataSource.revokeSession("userNotInDatabase");

        // then
        assertThat(dataSource.hasSession("bobby"), equalTo(false));
        assertThat(dataSource.hasSession("user"), equalTo(true));
        assertThat(dataSource.hasSession("nonExistentName"), equalTo(false));
    }

    @Test
    public void shouldGetRecentlyLoggedInPlayers() {
        // given
        DataSource dataSource = getDataSource();
        String[] names = {"user3", "user8", "user2", "user4", "user7",
            "user11", "user14", "user12", "user18", "user16",
            "user28", "user29", "user22", "user20", "user24"};
        long timestamp = 1461024000; // 2016-04-19 00:00:00
        for (int i = 0; i < names.length; ++i) {
            PlayerAuth auth = PlayerAuth.builder().name(names[i])
                .registrationDate(1234567)
                .lastLogin(timestamp + i * 3600)
                .build();
            dataSource.saveAuth(auth);
            dataSource.updateSession(auth);
        }

        // when
        List<PlayerAuth> recentPlayers = dataSource.getRecentlyLoggedInPlayers();

        // then
        assertThat(Lists.transform(recentPlayers, PlayerAuth::getNickname),
            contains("user24", "user20", "user22", "user29", "user28",
                "user16", "user18", "user12", "user14", "user11"));
    }

    @Test
    public void shouldSetTotpKey() {
        // given
        DataSource dataSource = getDataSource();
        String newTotpKey = "My new TOTP key";

        // when
        dataSource.setTotpKey("BObBy", newTotpKey);
        dataSource.setTotpKey("does-not-exist", "bogus");

        // then
        assertThat(dataSource.getAuth("bobby").getTotpKey(), equalTo(newTotpKey));
    }

    @Test
    public void shouldRemoveTotpKey() {
        // given
        DataSource dataSource = getDataSource();

        // when
        dataSource.removeTotpKey("BoBBy");
        dataSource.removeTotpKey("user");
        dataSource.removeTotpKey("does-not-exist");

        // then
        assertThat(dataSource.getAuth("bobby").getTotpKey(), nullValue());
        assertThat(dataSource.getAuth("user").getTotpKey(), nullValue());
    }
}
