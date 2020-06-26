package fr.xephi.authme.datasource.converter;

import fr.xephi.authme.annotation.ShouldBeAsync;
import fr.xephi.authme.util.BukkitThreadSafety;
import org.bukkit.command.CommandSender;

/**
 * Abstract class for AuthMe converters.
 */
public abstract class AbstractConverter {

    /**
     * Execute the conversion.
     *
     * @param sender the sender who initialized the conversion
     */
    @ShouldBeAsync
    public void execute(CommandSender sender) {
        BukkitThreadSafety.shouldBeAsync();
        executeInternal(sender);
    }

    @ShouldBeAsync
    protected abstract void executeInternal(CommandSender sender);
}
