package fr.xephi.authme.platform;

import fr.xephi.authme.listener.PlayerSignOpenListener;
import fr.xephi.authme.process.register.RegisterSecondaryArgument;
import fr.xephi.authme.process.register.RegistrationType;
import net.md_5.bungee.api.dialog.MultiActionDialog;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class SpigotPlatformAdapterTest {

    private final SpigotPlatformAdapter adapter = new SpigotPlatformAdapter();

    @Test
    public void getPlatformNameReturnsExpectedValue() {
        assertThat(adapter.getPlatformName(), is("spigot-1.21"));
    }

    @Test
    public void isDialogSupportedReturnsTrueWhenApiIsOnClasspath() {
        // The BungeeCord dialog API is a test dependency, so DIALOG_AVAILABLE is true
        assertThat(adapter.isDialogSupported(), is(true));
    }

    @Test
    public void getAdditionalListenersContainsPlayerSignOpenListener() {
        List<Class<? extends Listener>> listeners = adapter.getAdditionalListeners();

        assertThat(listeners, contains(PlayerSignOpenListener.class));
    }

    @Test
    public void showLoginDialogDelegatesToPlayer() {
        // given
        Player player = mock(Player.class);

        // when
        adapter.showLoginDialog(player);

        // then
        verify(player).showDialog(any(MultiActionDialog.class));
    }

    @Test
    public void showRegisterDialogDelegatesToPlayer() {
        // given
        Player player = mock(Player.class);

        // when
        adapter.showRegisterDialog(player, RegistrationType.PASSWORD, RegisterSecondaryArgument.NONE);

        // then
        verify(player).showDialog(any(MultiActionDialog.class));
    }
}

