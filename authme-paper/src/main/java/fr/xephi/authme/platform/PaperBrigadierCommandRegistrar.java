package fr.xephi.authme.platform;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import fr.xephi.authme.AuthMe;
import fr.xephi.authme.command.CommandArgumentDescription;
import fr.xephi.authme.command.CommandDescription;
import fr.xephi.authme.command.CommandUtils;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.BiFunction;

/**
 * Registers AuthMe commands in Paper's Brigadier tree while delegating all execution to the existing command handler.
 */
final class PaperBrigadierCommandRegistrar {

    private static final int COMMAND_SUCCESS = 1;
    private static final String FALLBACK_ARGUMENT_NAME = "unparsed";
    private static final String EXTRA_ARGUMENTS_NAME = "extraArgs";

    private final BiFunction<CommandSender, List<String>, Boolean> commandExecutor;

    PaperBrigadierCommandRegistrar(BiFunction<CommandSender, List<String>, Boolean> commandExecutor) {
        this.commandExecutor = commandExecutor;
    }

    void registerCommands(AuthMe plugin, Collection<CommandDescription> commands) {
        plugin.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            Commands registrar = event.registrar();
            for (PaperBrigadierCommandRegistration command : buildRegistrations(commands)) {
                registrar.register(command.getNode(), command.getDescription(), command.getAliases());
            }
        });
    }

    List<PaperBrigadierCommandRegistration> buildRegistrations(Collection<CommandDescription> commands) {
        List<PaperBrigadierCommandRegistration> registrations = new ArrayList<>(commands.size());
        for (CommandDescription command : commands) {
            registrations.add(buildRegistration(command));
        }
        return registrations;
    }

    private PaperBrigadierCommandRegistration buildRegistration(CommandDescription command) {
        return new PaperBrigadierCommandRegistration(
            buildLiteralNode(command.getLabels().get(0), command).build(),
            command.getDescription(),
            command.getLabels().subList(1, command.getLabels().size()));
    }

    private LiteralArgumentBuilder<CommandSourceStack> buildLiteralNode(String label, CommandDescription command) {
        LiteralArgumentBuilder<CommandSourceStack> builder = LiteralArgumentBuilder.<CommandSourceStack>literal(label)
            .executes(this::executeInput);

        if (!command.getChildren().isEmpty()) {
            for (CommandDescription child : command.getChildren()) {
                for (String childLabel : child.getLabels()) {
                    builder.then(buildLiteralNode(childLabel, child));
                }
            }
            builder.then(createFallbackArgument(FALLBACK_ARGUMENT_NAME));
            return builder;
        }

        addArgumentChain(builder, command.getArguments());
        if (command.getArguments().isEmpty()) {
            builder.then(createFallbackArgument(EXTRA_ARGUMENTS_NAME));
        }
        return builder;
    }

    private void addArgumentChain(ArgumentBuilder<CommandSourceStack, ?> parent,
                                  List<CommandArgumentDescription> arguments) {
        ArgumentBuilder<CommandSourceStack, ?> currentParent = parent;
        for (int index = 0; index < arguments.size(); ++index) {
            CommandArgumentDescription argument = arguments.get(index);
            boolean isLastArgument = index == arguments.size() - 1;
            RequiredArgumentBuilder<CommandSourceStack, String> argumentBuilder =
                RequiredArgumentBuilder.<CommandSourceStack, String>argument(argument.getName(),
                    isLastArgument ? StringArgumentType.greedyString() : StringArgumentType.word())
                    .executes(this::executeInput);
            currentParent.then(argumentBuilder);
            currentParent = argumentBuilder;
        }
    }

    private RequiredArgumentBuilder<CommandSourceStack, String> createFallbackArgument(String name) {
        return RequiredArgumentBuilder.<CommandSourceStack, String>argument(name, StringArgumentType.greedyString())
            .executes(this::executeInput);
    }

    private int executeInput(CommandContext<CommandSourceStack> context) {
        commandExecutor.apply(context.getSource().getSender(), CommandUtils.splitInput(context.getInput()));
        return COMMAND_SUCCESS;
    }
}
