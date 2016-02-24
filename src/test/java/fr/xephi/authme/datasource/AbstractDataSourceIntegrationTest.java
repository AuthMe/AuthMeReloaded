package fr.xephi.authme.datasource;

import fr.xephi.authme.cache.auth.PlayerAuth;
import fr.xephi.authme.security.crypts.HashedPassword;
import org.junit.Test;

import static fr.xephi.authme.datasource.AuthMeMatchers.equalToHash;
import static fr.xephi.authme.datasource.AuthMeMatchers.hasAuthBasicData;
import static fr.xephi.authme.datasource.AuthMeMatchers.hasAuthLocation;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

/**
 * Abstract class for data source integration tests.
 */
public abstract class AbstractDataSourceIntegrationTest {

    protected abstract DataSource getDataSource();

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
        HashedPassword userPassword = dataSource.getPassword("user");

        // then
        assertThat(bobbyPassword, equalToHash("$SHA$11aa0706173d7272$dbba966"));
        assertThat(invalidPassword, nullValue());
        assertThat(userPassword, equalToHash("b28c32f624a4eb161d6adc9acb5bfc5b", "f750ba32"));
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

        assertThat(bobbyAuth, hasAuthBasicData("bobby", "Bobby", "your@email.com", "123.45.67.89"));
        assertThat(bobbyAuth, hasAuthLocation(1.05, 2.1, 4.2, "world"));
        assertThat(bobbyAuth.getLastLogin(), equalTo(1449136800L));
        assertThat(bobbyAuth.getPassword(), equalToHash("$SHA$11aa0706173d7272$dbba966"));

        assertThat(userAuth, hasAuthBasicData("user", "user", "user@example.org", "34.56.78.90"));
        assertThat(userAuth, hasAuthLocation(124.1, 76.3, -127.8, "nether"));
        assertThat(userAuth.getLastLogin(), equalTo(1453242857L));
        assertThat(userAuth.getPassword(), equalToHash("b28c32f624a4eb161d6adc9acb5bfc5b", "f750ba32"));
    }

    @Test
    public void shouldFindIfEmailExists() {
        // given
        DataSource dataSource = getDataSource();

        // when
        boolean isUserMailPresent = dataSource.isEmailStored("user@example.org");
        boolean isUserMailPresentCaseInsensitive = dataSource.isEmailStored("user@example.ORG");
        boolean isInvalidMailPresent = dataSource.isEmailStored("not-in-database@example.com");

        // then
        assertThat(isUserMailPresent, equalTo(true));
        assertThat(isUserMailPresentCaseInsensitive, equalTo(true));
        assertThat(isInvalidMailPresent, equalTo(false));
    }

    @Test
    public void shouldCountAuthsByEmail() {
        // given
        DataSource dataSource = getDataSource();

        // when
        int userMailCount = dataSource.countAuthsByEmail("user@example.ORG");
        int invalidMailCount = dataSource.countAuthsByEmail("not.in.db@example.com");
        dataSource.saveAuth(PlayerAuth.builder().name("Test").email("user@EXAMPLE.org").build());
        int newUserCount = dataSource.countAuthsByEmail("user@Example.org");

        // then
        assertThat(userMailCount, equalTo(1));
        assertThat(invalidMailCount, equalTo(0));
        assertThat(newUserCount, equalTo(2));
    }

}
