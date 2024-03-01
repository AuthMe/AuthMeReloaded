package fr.xephi.authme.command.executable.authme.debug;

import fr.xephi.authme.ClassCollector;
import fr.xephi.authme.TestHelper;
import fr.xephi.authme.permission.AdminPermission;
import fr.xephi.authme.permission.PermissionNode;
import fr.xephi.authme.permission.PermissionsManager;
import fr.xephi.authme.service.BukkitService;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;

/**
 * Test for {@link HasPermissionChecker}.
 */
@ExtendWith(MockitoExtension.class)
class HasPermissionCheckerTest {

    @InjectMocks
    private HasPermissionChecker hasPermissionChecker;

    @Mock
    private PermissionsManager permissionsManager;

    @Mock
    private BukkitService bukkitService;

    @Test
    void shouldListAllPermissionNodeClasses() {
        // given
        List<Class<? extends PermissionNode>> permissionClasses =
            new ClassCollector(TestHelper.SOURCES_FOLDER, TestHelper.PROJECT_ROOT)
                .collectClasses(PermissionNode.class).stream()
                .filter(clz -> !clz.isInterface())
                .collect(Collectors.toList());

        // when / then
        assertThat(HasPermissionChecker.PERMISSION_NODE_CLASSES, containsInAnyOrder(permissionClasses.toArray()));
    }

    @Test
    void shouldShowUsageInfo() {
        // given
        CommandSender sender = mock(CommandSender.class);

        // when
        hasPermissionChecker.execute(sender, emptyList());

        // then
        ArgumentCaptor<String> msgCaptor = ArgumentCaptor.forClass(String.class);
        verify(sender, atLeast(2)).sendMessage(msgCaptor.capture());
        assertThat(
            msgCaptor.getAllValues().stream().anyMatch(msg -> msg.contains("/authme debug perm bobby my.perm.node")),
            equalTo(true));
    }

    @Test
    void shouldShowSuccessfulTestWithRegularPlayer() {
        // given
        String name = "Chuck";
        Player player = mock(Player.class);
        given(bukkitService.getPlayerExact(name)).willReturn(player);
        PermissionNode permission = AdminPermission.CHANGE_EMAIL;
        given(permissionsManager.hasPermission(player, permission)).willReturn(true);
        CommandSender sender = mock(CommandSender.class);

        // when
        hasPermissionChecker.execute(sender, asList(name, permission.getNode()));

        // then
        verify(bukkitService).getPlayerExact(name);
        verify(permissionsManager).hasPermission(player, permission);
        verify(sender).sendMessage(argThat(containsString("Success: player '" + player.getName()
            + "' has permission '" + permission.getNode() + "'")));
    }
}
