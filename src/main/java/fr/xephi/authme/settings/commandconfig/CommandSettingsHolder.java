package fr.xephi.authme.settings.commandconfig;

import com.github.authme.configme.SectionComments;
import com.github.authme.configme.SettingsHolder;
import com.github.authme.configme.beanmapper.BeanProperty;
import com.github.authme.configme.properties.Property;

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
        String[] comments = {
            "This configuration file allows you to execute commands on various events.",
            "%p in commands will be replaced with the player name.",
            "For example, if you want to message a welcome message to a player who just registered:",
            "onRegister:",
            "  welcome:",
            "    command: 'msg %p Welcome to the server!'",
            "    as: CONSOLE",
            "",
            "This will make the console execute the msg command to the player.",
            "Each command under an event has a name you can choose freely (e.g. 'welcome' as above),",
            "after which a mandatory 'command' field defines the command to run, ",
            "and 'as' defines who will run the command (either PLAYER or CONSOLE). Longer example:",
            "onLogin:",
            "  welcome:",
            "    command: 'msg %p Welcome back!'",
            "    # as: PLAYER  # player is the default, you can leave this out if you want",
            "  broadcast:",
            "    command: 'broadcast %p has joined, welcome back!'",
            "    as: CONSOLE"
        };
        Map<String, String[]> commentMap = new HashMap<>();
        commentMap.put("onLogin", comments);
        return commentMap;
    }

}
