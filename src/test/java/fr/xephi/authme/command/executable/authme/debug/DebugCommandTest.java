package fr.xephi.authme.command.executable.authme.debug;

import ch.jalu.injector.factory.Factory;
import fr.xephi.authme.permission.DebugSectionPermissions;
import fr.xephi.authme.permission.PermissionNode;
import fr.xephi.authme.permission.PermissionsManager;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Collections.emptyList;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;

/**
 * Test for {@link DebugCommand}.
 */
@RunWith(MockitoJUnitRunner.class)
public class DebugCommandTest {

    /**
     * Number we test against if we expect an action to have been performed for each debug section.
     * This is a minimum number so tests don't fail each time a new debug section is added; however,
     * it should be close to the total.
     */
    private static final int MIN_DEBUG_SECTIONS = 9;

    @InjectMocks
    private DebugCommand command;

    @Mock
    private Factory<DebugSection> debugSectionFactory;

    @Mock
    private PermissionsManager permissionsManager;

    @Before
    @SuppressWarnings("unchecked")
    public void initFactory() {
        given(debugSectionFactory.newInstance(any(Class.class))).willAnswer(
            invocation -> {
                Class<?> classArgument = invocation.getArgument(0);
                checkArgument(DebugSection.class.isAssignableFrom(classArgument));
                return spy(classArgument);
            });
    }

    @Test
    public void shouldListAllAvailableDebugSections() {
        // given
        CommandSender sender = mock(CommandSender.class);
        given(permissionsManager.hasPermission(eq(sender), any(PermissionNode.class))).willReturn(false);
        given(permissionsManager.hasPermission(sender, DebugSectionPermissions.INPUT_VALIDATOR)).willReturn(true);
        given(permissionsManager.hasPermission(sender, DebugSectionPermissions.DATA_STATISTICS)).willReturn(true);

        // when
        command.executeCommand(sender, emptyList());

        // then
        verify(debugSectionFactory, atLeast(MIN_DEBUG_SECTIONS)).newInstance(any(Class.class));
        verify(permissionsManager, atLeast(MIN_DEBUG_SECTIONS)).hasPermission(eq(sender), any(DebugSectionPermissions.class));

        ArgumentCaptor<String> strCaptor = ArgumentCaptor.forClass(String.class);
        verify(sender, times(4)).sendMessage(strCaptor.capture());
        assertThat(strCaptor.getAllValues(), contains(
            equalTo(ChatColor.BLUE + "AuthMe debug utils"),
            equalTo("Sections available to you:"),
            containsString("stats: Outputs general data statistics"),
            containsString("valid: Checks if your config.yml allows a password / email")));
    }

    @Test
    public void shouldNotListAnyDebugSection() {
        // given
        CommandSender sender = mock(CommandSender.class);
        given(permissionsManager.hasPermission(eq(sender), any(PermissionNode.class))).willReturn(false);

        // when
        command.executeCommand(sender, emptyList());

        // then
        verify(debugSectionFactory, atLeast(MIN_DEBUG_SECTIONS)).newInstance(any(Class.class));
        verify(permissionsManager, atLeast(MIN_DEBUG_SECTIONS)).hasPermission(eq(sender), any(DebugSectionPermissions.class));

        ArgumentCaptor<String> strCaptor = ArgumentCaptor.forClass(String.class);
        verify(sender, times(3)).sendMessage(strCaptor.capture());
        assertThat(strCaptor.getAllValues(), contains(
            equalTo(ChatColor.BLUE + "AuthMe debug utils"),
            equalTo("Sections available to you:"),
            containsString("You don't have permission to view any debug section")));
    }

    @Test
    public void shouldRunSection() {
        // given
        DebugSection section = spy(InputValidator.class);
        doNothing().when(section).execute(any(CommandSender.class), anyList());
        // Mockito throws a runtime error if below we use the usual "given(factory.newInstance(...)).willReturn(...)"
        doReturn(section).when(debugSectionFactory).newInstance(InputValidator.class);

        CommandSender sender = mock(CommandSender.class);
        given(permissionsManager.hasPermission(sender, section.getRequiredPermission())).willReturn(true);
        List<String> arguments = Arrays.asList(section.getName().toUpperCase(Locale.ROOT), "test", "toast");

        // when
        command.executeCommand(sender, arguments);

        // then
        verify(permissionsManager).hasPermission(sender, section.getRequiredPermission());
        verify(section).execute(sender, Arrays.asList("test", "toast"));
    }

    @Test
    public void shouldNotRunSectionForMissingPermission() {
        // given
        DebugSection section = spy(InputValidator.class);
        // Mockito throws a runtime error if below we use the usual "given(factory.newInstance(...)).willReturn(...)"
        doReturn(section).when(debugSectionFactory).newInstance(InputValidator.class);

        CommandSender sender = mock(CommandSender.class);
        given(permissionsManager.hasPermission(sender, section.getRequiredPermission())).willReturn(false);
        List<String> arguments = Arrays.asList(section.getName().toUpperCase(Locale.ROOT), "test");

        // when
        command.executeCommand(sender, arguments);

        // then
        verify(permissionsManager).hasPermission(sender, section.getRequiredPermission());
        verify(section, never()).execute(any(CommandSender.class), anyList());
        verify(sender).sendMessage(argThat(containsString("You don't have permission")));
    }
}
