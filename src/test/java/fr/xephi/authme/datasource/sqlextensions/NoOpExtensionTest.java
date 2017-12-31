package fr.xephi.authme.datasource.sqlextensions;

import fr.xephi.authme.TestHelper;
import fr.xephi.authme.data.auth.PlayerAuth;
import fr.xephi.authme.datasource.Columns;
import fr.xephi.authme.security.crypts.HashedPassword;
import fr.xephi.authme.settings.Settings;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.sql.Connection;
import java.sql.SQLException;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyZeroInteractions;

/**
 * Test for {@link NoOpExtension}.
 */
@RunWith(MockitoJUnitRunner.class)
public class NoOpExtensionTest {

    private NoOpExtension extension;

    @Before
    public void createExtension() {
        Settings settings = mock(Settings.class);
        TestHelper.returnDefaultsForAllProperties(settings);
        Columns columns = new Columns(settings);
        extension = new NoOpExtension(settings, columns);
    }

    @Test
    public void shouldNotHaveAnyInteractionsWithConnection() throws SQLException {
        // given
        Connection connection = mock(Connection.class);
        PlayerAuth auth = mock(PlayerAuth.class);
        int id = 3;
        String name = "Bobby";
        HashedPassword password = new HashedPassword("test", "toast");


        // when
        extension.extendAuth(auth, id, connection);
        extension.changePassword(name, password, connection);
        extension.removeAuth(name, connection);
        extension.saveAuth(auth, connection);

        // then
        verifyZeroInteractions(connection, auth);
    }
}
