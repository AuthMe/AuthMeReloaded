package fr.xephi.authme.data;

import fr.xephi.authme.permission.PermissionsManager;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.ProtectionSettings;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;

/**
 * Test for {@link QuickCommandsProtectionManager}.
 */
@RunWith(MockitoJUnitRunner.class)
public class QuickCommandsProtectionManagerTest {

    @Mock
    private Settings settings;

    @Mock
    private PermissionsManager permissionsManager;

    @Test
    public void shouldAllowCommand() {
        // given
        String name1 = "TestName1";
        String name2 = "TestName2";
        given(settings.getProperty(ProtectionSettings.QUICK_COMMANDS_DENIED_BEFORE_MILLISECONDS)).willReturn(0);

        QuickCommandsProtectionManager qcpm = createQuickCommandsProtectioneManager();
        qcpm.setLogin(name2);

        // when
        boolean test1 = qcpm.isAllowed(name1);
        boolean test2 = qcpm.isAllowed(name2);

        // then
        assertThat(test1, equalTo(true));
        assertThat(test2, equalTo(true));
    }

    @Test
    public void shouldDenyCommand() {
        // given
        String name = "TestName1";
        given(settings.getProperty(ProtectionSettings.QUICK_COMMANDS_DENIED_BEFORE_MILLISECONDS)).willReturn(5000);

        QuickCommandsProtectionManager qcpm = createQuickCommandsProtectioneManager();
        qcpm.setLogin(name);

        // when
        boolean test = qcpm.isAllowed(name);

        // then
        assertThat(test, equalTo(false));
    }

    private QuickCommandsProtectionManager createQuickCommandsProtectioneManager() {
        return new QuickCommandsProtectionManager(settings, permissionsManager);
    }
}
