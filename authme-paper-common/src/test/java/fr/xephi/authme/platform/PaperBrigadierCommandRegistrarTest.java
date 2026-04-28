package fr.xephi.authme.platform;

import com.mojang.brigadier.CommandDispatcher;
import fr.xephi.authme.command.CommandArgumentDescription;
import fr.xephi.authme.command.CommandDescription;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.command.CommandSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.WARN)
public class PaperBrigadierCommandRegistrarTest {

    private PaperBrigadierCommandRegistrar registrar;
    private CommandDispatcher<CommandSourceStack> dispatcher;
    private AtomicReference<CommandSender> executingSender;
    private List<List<String>> executedCommands;

    @Mock
    private CommandSourceStack sourceStack;
    @Mock
    private CommandSender sender;

    @BeforeEach
    public void setUp() {
        executingSender = new AtomicReference<>();
        executedCommands = new ArrayList<>();
        registrar = new PaperBrigadierCommandRegistrar((commandSender, parts) -> {
            executingSender.set(commandSender);
            executedCommands.add(parts);
            return true;
        });
        dispatcher = new CommandDispatcher<>();
        lenient().when(sourceStack.getSender()).thenReturn(sender);

        for (PaperBrigadierCommandRegistration registration
            : registrar.buildRegistrations(createCommands())) {
            dispatcher.getRoot().addChild(registration.getNode());
        }
    }

    @Test
    public void shouldExposeBaseCommandAliases() {
        PaperBrigadierCommandRegistration registration = registrar.buildRegistrations(createCommands())
            .stream()
            .filter(command -> "login".equals(command.getNode().getLiteral()))
            .findFirst()
            .orElse(null);

        assertThat(registration, notNullValue());
        assertThat(registration.getAliases(), contains("l", "log"));
    }

    @Test
    public void shouldDelegateBaseCommandExecutionToExistingHandler() throws Exception {
        dispatcher.execute("authme", sourceStack);

        assertThat(executingSender.get(), equalTo(sender));
        assertThat(executedCommands, contains(List.of("authme")));
    }

    @Test
    public void shouldDelegateUnknownSubcommandThroughFallback() throws Exception {
        dispatcher.execute("authme typo", sourceStack);

        assertThat(executedCommands, contains(List.of("authme", "typo")));
    }

    @Test
    public void shouldDelegateMissingArgumentsToExistingHandler() throws Exception {
        dispatcher.execute("login", sourceStack);

        assertThat(executedCommands, contains(List.of("login")));
    }

    @Test
    public void shouldDelegateExtraArgumentsToExistingHandler() throws Exception {
        dispatcher.execute("login password extra", sourceStack);

        assertThat(executedCommands, contains(List.of("login", "password", "extra")));
    }

    @Test
    public void shouldHandleAtSignInNonLastArgument() throws Exception {
        dispatcher.execute("authme register test@gmail.com myPassword", sourceStack);

        assertThat(executedCommands, contains(List.of("authme", "register", "test@gmail.com", "myPassword")));
    }

    @Test
    public void shouldRegisterChildAliasesInBrigadierTree() {
        assertThat(dispatcher.getCompletionSuggestions(dispatcher.parse("authme ", sourceStack)).join()
            .getList().stream().map(suggestion -> suggestion.getText()).toList(), hasItem("reg"));
    }

    private static Collection<CommandDescription> createCommands() {
        CommandDescription authmeBase = command(List.of("authme"), "AuthMe root", List.of(), List.of());
        CommandDescription authmeRegister = command(List.of("register", "reg"),
            "Register player", List.of(
                new CommandArgumentDescription("player", "Player name", false),
                new CommandArgumentDescription("password", "Password", false)),
            List.of());
        lenient().when(authmeBase.getChildren()).thenReturn(List.of(authmeRegister));

        CommandDescription loginBase = command(List.of("login", "l", "log"),
            "Login", List.of(new CommandArgumentDescription("password", "Password", false)), List.of());

        return List.of(authmeBase, loginBase);
    }

    private static CommandDescription command(List<String> labels, String description,
                                              List<CommandArgumentDescription> arguments,
                                              List<CommandDescription> children) {
        CommandDescription command = mock(CommandDescription.class);
        lenient().when(command.getLabels()).thenReturn(labels);
        lenient().when(command.getDescription()).thenReturn(description);
        lenient().when(command.getArguments()).thenReturn(arguments);
        lenient().when(command.getChildren()).thenReturn(children);
        return command;
    }
}
