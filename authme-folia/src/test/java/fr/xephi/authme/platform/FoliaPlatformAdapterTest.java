package fr.xephi.authme.platform;

import fr.xephi.authme.listener.BlockListener;
import fr.xephi.authme.listener.EntityListener;
import fr.xephi.authme.listener.FoliaChatListener;
import fr.xephi.authme.listener.FoliaPlayerSpawnLocationListener;
import fr.xephi.authme.listener.PaperDialogFlowListener;
import fr.xephi.authme.listener.PaperLoginValidationListener;
import fr.xephi.authme.listener.PlayerListener;
import fr.xephi.authme.listener.PlayerOpenSignListener;
import fr.xephi.authme.listener.ServerListener;
import fr.xephi.authme.service.CancellableTask;
import io.papermc.paper.threadedregions.scheduler.AsyncScheduler;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import fr.xephi.authme.process.register.RegisterSecondaryArgument;
import fr.xephi.authme.process.register.RegistrationType;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerKickEvent;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;

import java.util.concurrent.TimeUnit;

public class FoliaPlatformAdapterTest {

    private final FoliaPlatformAdapter adapter = new FoliaPlatformAdapter();
    private final DialogWindowSpec dialog = new DialogWindowSpec("Title",
        List.of(new DialogInputSpec("password", "Password", 100)),
        "Submit",
        "Cancel",
        false,
        false);

    @Test
    public void getPlatformNameReturnsExpectedValue() {
        assertThat(adapter.getPlatformName(), is("folia-1.21"));
    }

    @Test
    public void getCompatibilityErrorReturnsNullForCompatibleClasspath() {
        assertThat(adapter.getCompatibilityError(), is((String) null));
    }

    @Test
    public void isDialogSupportedReturnsTrueWhenApiIsOnClasspath() {
        // The Paper dialog API is a test dependency, so DIALOG_AVAILABLE is true
        assertThat(adapter.isDialogSupported(), is(true));
    }

    @Test
    public void getListenersContainsCoreAndFoliaListeners() {
        List<Class<? extends Listener>> listeners = adapter.getListeners();

        assertThat(listeners, containsInAnyOrder(
            PlayerListener.class,
            BlockListener.class,
            EntityListener.class,
            ServerListener.class,
            FoliaChatListener.class,
            PaperDialogFlowListener.class,
            FoliaPlayerSpawnLocationListener.class,
            PaperLoginValidationListener.class,
            PlayerOpenSignListener.class));
    }

    @Test
    public void showLoginDialogDelegatesToPaperDialogHelper() {
        // given
        Player player = mock(Player.class);

        // when / then - FoliaDialogHelper uses API calls that require a running server.
        // Use mockStatic to verify delegation without invoking the full Paper server stack.
        try (MockedStatic<PaperDialogHelper> helperMock = mockStatic(PaperDialogHelper.class)) {
            adapter.showLoginDialog(player, dialog);

            helperMock.verify(() -> PaperDialogHelper.showLoginDialog(player, dialog));
        }
    }

    @Test
    public void showRegisterDialogDelegatesToPaperDialogHelper() {
        // given
        Player player = mock(Player.class);

        // when / then
        try (MockedStatic<PaperDialogHelper> helperMock = mockStatic(PaperDialogHelper.class)) {
            adapter.showRegisterDialog(player, RegistrationType.EMAIL, RegisterSecondaryArgument.CONFIRMATION, dialog);

            helperMock.verify(() -> PaperDialogHelper.showRegisterDialog(
                eq(player),
                eq(RegistrationType.EMAIL),
                eq(RegisterSecondaryArgument.CONFIRMATION),
                eq(dialog)));
        }
    }

    @Test
    public void teleportPlayerCallsTeleportAsync() {
        // given
        Player player = mock(Player.class);
        Location location = mock(Location.class);

        // when
        adapter.teleportPlayer(player, location);

        // then
        verify(player).teleportAsync(location);
    }

    @Test
    public void getKickReasonReturnsPlainTextFromComponent() {
        // given
        // PlayerKickEvent is not final; reason() is a Paper API method, stubable with Mockito 5
        PlayerKickEvent event = mock(PlayerKickEvent.class);
        given(event.reason()).willReturn(Component.text("You were kicked"));

        // when
        String reason = adapter.getKickReason(event);

        // then
        assertThat(reason, is("You were kicked"));
    }

    @Test
    public void runAsyncTaskUsesFoliaAsyncScheduler() {
        // given
        Runnable task = mock(Runnable.class);
        AsyncScheduler asyncScheduler = mock(AsyncScheduler.class);
        ScheduledTask scheduledTask = mock(ScheduledTask.class);
        given(asyncScheduler.runNow(isNull(), any())).willAnswer(invocation -> {
            invocation.<java.util.function.Consumer<ScheduledTask>>getArgument(1).accept(scheduledTask);
            return scheduledTask;
        });

        // when
        CancellableTask cancellableTask;
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getAsyncScheduler).thenReturn(asyncScheduler);
            cancellableTask = adapter.runAsyncTask(null, task);
        }
        cancellableTask.cancel();

        // then
        verify(task).run();
        verify(asyncScheduler).runNow(isNull(), any());
        verify(scheduledTask).cancel();
    }

    @Test
    public void runAsyncTaskTimerConvertsTicksToMilliseconds() {
        // given
        Runnable task = mock(Runnable.class);
        AsyncScheduler asyncScheduler = mock(AsyncScheduler.class);
        ScheduledTask scheduledTask = mock(ScheduledTask.class);
        long delay = 4L;
        long period = 7L;
        given(asyncScheduler.runAtFixedRate(isNull(), any(), eq(200L), eq(350L), eq(TimeUnit.MILLISECONDS)))
            .willReturn(scheduledTask);

        // when
        CancellableTask cancellableTask;
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getAsyncScheduler).thenReturn(asyncScheduler);
            cancellableTask = adapter.runAsyncTaskTimer(null, task, delay, period);
        }
        cancellableTask.cancel();

        // then
        verify(asyncScheduler).runAtFixedRate(isNull(), any(), eq(200L), eq(350L), eq(TimeUnit.MILLISECONDS));
        verify(scheduledTask).cancel();
    }
}


