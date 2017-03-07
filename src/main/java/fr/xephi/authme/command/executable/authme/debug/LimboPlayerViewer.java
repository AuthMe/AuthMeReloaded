package fr.xephi.authme.command.executable.authme.debug;

import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.data.limbo.LimboPlayer;
import fr.xephi.authme.data.limbo.LimboService;
import fr.xephi.authme.service.BukkitService;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import javax.inject.Inject;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import static fr.xephi.authme.command.executable.authme.debug.DebugSectionUtils.formatLocation;

/**
 * Shows the data stored in LimboPlayers and the equivalent properties on online players.
 */
class LimboPlayerViewer implements DebugSection {

    @Inject
    private LimboService limboService;

    @Inject
    private BukkitService bukkitService;

    private Field limboServiceEntries;

    @Override
    public String getName() {
        return "limbo";
    }

    @Override
    public String getDescription() {
        return "View LimboPlayers and player's \"limbo stats\"";
    }

    @Override
    public void execute(CommandSender sender, List<String> arguments) {
        if (arguments.isEmpty()) {
            sender.sendMessage("/authme debug limbo <player>: show a player's limbo info");
            sender.sendMessage("Available limbo records: " + getLimboKeys());
            return;
        }

        LimboPlayer limbo = limboService.getLimboPlayer(arguments.get(0));
        Player player = bukkitService.getPlayerExact(arguments.get(0));
        if (limbo == null && player == null) {
            sender.sendMessage("No limbo info and no player online with name '" + arguments.get(0) + "'");
            return;
        }

        sender.sendMessage(ChatColor.GOLD + "Showing limbo / player info for '" + arguments.get(0) + "'");
        new InfoDisplayer(sender, limbo, player)
            .sendEntry("Is op", LimboPlayer::isOperator, Player::isOp)
            .sendEntry("Walk speed", LimboPlayer::getWalkSpeed, Player::getWalkSpeed)
            .sendEntry("Can fly", LimboPlayer::isCanFly, Player::getAllowFlight)
            .sendEntry("Fly speed", LimboPlayer::getFlySpeed, Player::getFlySpeed)
            .sendEntry("Location", l -> formatLocation(l.getLocation()), p -> formatLocation(p.getLocation()))
            .sendEntry("Group", LimboPlayer::getGroup, p -> "");
        sender.sendMessage("Note: group is only shown for LimboPlayer");
    }

    /**
     * Gets the names of the LimboPlayers in the LimboService. As we don't want to expose this
     * information in non-debug settings, this is done over reflections. Since this is not a
     * crucial feature, we generously catch all Exceptions
     *
     * @return player names for which there is a LimboPlayer (or error message upon failure)
     */
    @SuppressWarnings("unchecked")
    private Set<String> getLimboKeys() {
        // Lazy initialization
        if (limboServiceEntries == null) {
            try {
                Field limboServiceEntries = LimboService.class.getDeclaredField("entries");
                limboServiceEntries.setAccessible(true);
                this.limboServiceEntries = limboServiceEntries;
            } catch (Exception e) {
                ConsoleLogger.logException("Could not retrieve LimboService entries field:", e);
                return Collections.singleton("Error retrieving LimboPlayer collection");
            }
        }

        try {
            return (Set) ((Map) limboServiceEntries.get(limboService)).keySet();
        } catch (Exception e) {
            ConsoleLogger.logException("Could not retrieve LimboService values:", e);
            return Collections.singleton("Error retrieving LimboPlayer values");
        }
    }

    /**
     * Displays the info for the given LimboPlayer and Player to the provided CommandSender.
     */
    private static final class InfoDisplayer {
        private final CommandSender sender;
        private final Optional<LimboPlayer> limbo;
        private final Optional<Player> player;

        /**
         * Constructor.
         *
         * @param sender command sender to send the information to
         * @param limbo the limbo player to get data from
         * @param player the player to get data from
         */
        InfoDisplayer(CommandSender sender, LimboPlayer limbo, Player player) {
            this.sender = sender;
            this.limbo = Optional.ofNullable(limbo);
            this.player = Optional.ofNullable(player);

            if (limbo == null) {
                sender.sendMessage("Note: no Limbo information available");
            } else if (player == null) {
                sender.sendMessage("Note: player is not online");
            }
        }

        /**
         * Displays a piece of information to the command sender.
         *
         * @param title the designation of the piece of information
         * @param limboGetter getter for data retrieval on the LimboPlayer
         * @param playerGetter getter for data retrieval on Player
         * @param <T> the data type
         * @return this instance (for chaining)
         */
        <T> InfoDisplayer sendEntry(String title,
                                    Function<LimboPlayer, T> limboGetter,
                                    Function<Player, T> playerGetter) {
            sender.sendMessage(
                title + ": "
                + limbo.map(limboGetter).map(String::valueOf).orElse("--")
                + " / "
                + player.map(playerGetter).map(String::valueOf).orElse("--"));
            return this;
        }
    }
}
