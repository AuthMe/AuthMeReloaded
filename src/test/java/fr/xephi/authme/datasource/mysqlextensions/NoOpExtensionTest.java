package fr.xephi.authme.datasource.mysqlextensions;

import fr.xephi.authme.TestHelper;
import fr.xephi.authme.data.auth.PlayerAuth;
import fr.xephi.authme.datasource.Columns;
import fr.xephi.authme.security.crypts.HashedPassword;
import fr.xephi.authme.settings.Settings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.Connection;
import java.sql.SQLException;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Test for {@link NoOpExtension}.
 */
@ExtendWith(MockitoExtension.class)
class NoOpExtensionTest {

    private NoOpExtension extension;

    @BeforeEach
    void createExtension() {
        Settings settings = mock(Settings.class);
        TestHelper.returnDefaultsForAllProperties(settings);
        Columns columns = new Columns(settings);
        extension = new NoOpExtension(settings, columns);
    }

    @Test
    void shouldNotHaveAnyInteractionsWithConnection() throws SQLException {
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
        verifyNoInteractions(connection, auth);
    }
}
