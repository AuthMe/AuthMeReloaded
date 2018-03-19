package fr.xephi.authme.settings.commandconfig;

import ch.jalu.configme.SectionComments;
import ch.jalu.configme.SettingsHolder;
import ch.jalu.configme.properties.BeanProperty;
import ch.jalu.configme.properties.Property;

import java.util.HashMap;
import java.util.Map;

/**
 * Settings holder class for the commands.yml settings.
 */
public final class CommandSettingsHolder implements SettingsHolder {

    public static final Property<CommandConfig> COMMANDS =
        new BeanProperty<>(CommandConfig.class, "", new CommandConfig());


    private CommandSettingsHolder() {
    }

    @SectionComments
    public static Map<String, String[]> sectionComments() {
        String[] rootComments = {
            "This configuration file allows you to execute commands on various events.",
            "Supported placeholders in commands:",
            "  %p is replaced with the player name.",
            "  %nick is replaced with the player's nick name",
            "  %ip is replaced with the player's IP address",
            "  %country is replaced with the player's country",
            "",
            "For example, if you want to send a welcome message to a player who just registered:",
            "onRegister:",
            "  welcome:",
            "    command: 'msg %p Welcome to the server!'",
            "    executor: CONSOLE",
            "",
            "This will make the console execute the msg command to the player.",
            "Each command under an event has a name you can choose freely (e.g. 'welcome' as above),",
            "after which a mandatory 'command' field defines the command to run,",
            "and 'executor' defines who will run the command (either PLAYER or CONSOLE). Longer example:",
            "onLogin:",
            "  welcome:",
            "    command: 'msg %p Welcome back!'",
            "    executor: PLAYER",
            "  broadcast:",
            "    command: 'broadcast %p has joined, welcome back!'",
            "    executor: CONSOLE",
            "",
            "Supported command events: onLogin, onSessionLogin, onFirstLogin, onJoin, onLogout, onRegister, "
                + "onUnregister",
            "",
            "For onLogin and onFirstLogin, you can use 'ifNumberOfAccountsLessThan' and 'ifNumberOfAccountsAtLeast'",
            "to specify limits to how many accounts a player can have (matched by IP) for a command to be run:",
            "onLogin:",
            "  warnOnManyAccounts:",
            "    command: 'say Uh oh! %p has many alt accounts!'",
            "    executor: CONSOLE",
            "    ifNumberOfAccountsAtLeast: 5"
        };

        Map<String, String[]> commentMap = new HashMap<>();
        commentMap.put("", rootComments);
        commentMap.put("onFirstLogin", new String[]{
            "Commands to run for players logging in whose 'last login date' was empty"
        });
        commentMap.put("onUnregister", new String[]{
            "Commands to run whenever a player is unregistered (by himself, or by an admin)"
        });
        commentMap.put("onLogout", new String[]{
            "These commands are called whenever a logged in player uses /logout or quits.",
            "The commands are not run if a player that was not logged in quits the server.",
            "Note: if your server crashes, these commands won't be run, so don't rely on them to undo",
            "'onLogin' commands that would be dangerous for non-logged in players to have!"
        });
        return commentMap;
    }

}
