package fr.xephi.authme.datasource.converter;

import fr.xephi.authme.annotation.ShouldBeAsync;
import org.bukkit.command.CommandSender;

/**
 * Interface for AuthMe converters.
 */
public interface Converter {

    /**
     * Execute the conversion.
     * TODO: apply the ShouldBeAsync annotation to all the converters
     *
     * @param sender the sender who initialized the conversion
     */
    @ShouldBeAsync
    void execute(CommandSender sender);
}
