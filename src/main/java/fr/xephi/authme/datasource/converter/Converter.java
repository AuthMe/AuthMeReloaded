package fr.xephi.authme.datasource.converter;

import org.bukkit.command.CommandSender;

/**
 * Interface for AuthMe converters.
 */
public interface Converter {

    /**
     * Execute the conversion.
     *
     * @param sender the sender who initialized the conversion
     */
    void execute(CommandSender sender);
}
