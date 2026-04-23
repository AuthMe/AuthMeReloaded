package fr.xephi.authme.platform;

import fr.xephi.authme.process.register.RegisterSecondaryArgument;
import fr.xephi.authme.process.register.RegistrationType;
import net.md_5.bungee.api.dialog.MultiActionDialog;
import net.md_5.bungee.api.dialog.action.RunCommandAction;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class SpigotDialogHelperTest {

    @Test
    public void showLoginDialogSendsCorrectCommandTemplate() {
        // given
        Player player = mock(Player.class);

        // when
        SpigotDialogHelper.showLoginDialog(player);

        // then
        RunCommandAction action = captureRunCommandAction(player);
        assertThat(action.template(), is("login $(password)"));
    }

    @Test
    public void showTotpDialogSendsCorrectCommandTemplate() {
        // given
        Player player = mock(Player.class);

        // when
        SpigotDialogHelper.showTotpDialog(player);

        // then
        RunCommandAction action = captureRunCommandAction(player);
        assertThat(action.template(), is("2fa code $(code)"));
    }

    @Test
    public void showRegisterDialogWithPasswordAndNoSecondArgSendsPasswordTemplate() {
        // given
        Player player = mock(Player.class);

        // when
        SpigotDialogHelper.showRegisterDialog(player, RegistrationType.PASSWORD, RegisterSecondaryArgument.NONE);

        // then
        RunCommandAction action = captureRunCommandAction(player);
        assertThat(action.template(), is("register $(password)"));
    }

    @Test
    public void showRegisterDialogWithPasswordAndConfirmationSendsPasswordConfirmTemplate() {
        // given
        Player player = mock(Player.class);

        // when
        SpigotDialogHelper.showRegisterDialog(player, RegistrationType.PASSWORD, RegisterSecondaryArgument.CONFIRMATION);

        // then
        RunCommandAction action = captureRunCommandAction(player);
        assertThat(action.template(), is("register $(password) $(confirm)"));
    }

    @Test
    public void showRegisterDialogWithPasswordAndEmailMandatorySendsPasswordEmailTemplate() {
        // given
        Player player = mock(Player.class);

        // when
        SpigotDialogHelper.showRegisterDialog(player, RegistrationType.PASSWORD, RegisterSecondaryArgument.EMAIL_MANDATORY);

        // then
        RunCommandAction action = captureRunCommandAction(player);
        assertThat(action.template(), is("register $(password) $(email)"));
    }

    @Test
    public void showRegisterDialogWithEmailAndNoSecondArgSendsEmailTemplate() {
        // given
        Player player = mock(Player.class);

        // when
        SpigotDialogHelper.showRegisterDialog(player, RegistrationType.EMAIL, RegisterSecondaryArgument.NONE);

        // then
        RunCommandAction action = captureRunCommandAction(player);
        assertThat(action.template(), is("register $(email)"));
    }

    @Test
    public void showRegisterDialogWithEmailAndConfirmationSendsEmailConfirmTemplate() {
        // given
        Player player = mock(Player.class);

        // when
        SpigotDialogHelper.showRegisterDialog(player, RegistrationType.EMAIL, RegisterSecondaryArgument.CONFIRMATION);

        // then
        RunCommandAction action = captureRunCommandAction(player);
        assertThat(action.template(), is("register $(email) $(confirm)"));
    }

    private static RunCommandAction captureRunCommandAction(Player player) {
        ArgumentCaptor<MultiActionDialog> captor = ArgumentCaptor.forClass(MultiActionDialog.class);
        verify(player).showDialog(captor.capture());
        return (RunCommandAction) captor.getValue().actions().get(0).action();
    }
}

