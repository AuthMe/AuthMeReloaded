package fr.xephi.authme.platform;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import com.mojang.brigadier.tree.LiteralCommandNode;

import java.util.Collection;

/**
 * Immutable registration data for a single Paper Brigadier root command.
 */
final class PaperBrigadierCommandRegistration {

    private final LiteralCommandNode<CommandSourceStack> node;
    private final String description;
    private final Collection<String> aliases;

    PaperBrigadierCommandRegistration(LiteralCommandNode<CommandSourceStack> node, String description,
                                      Collection<String> aliases) {
        this.node = node;
        this.description = description;
        this.aliases = aliases;
    }

    LiteralCommandNode<CommandSourceStack> getNode() {
        return node;
    }

    String getDescription() {
        return description;
    }

    Collection<String> getAliases() {
        return aliases;
    }
}
