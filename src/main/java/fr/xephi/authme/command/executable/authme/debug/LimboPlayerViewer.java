package fr.xephi.authme.command.executable.authme.debug;

import fr.xephi.authme.data.limbo.LimboPlayer;
import fr.xephi.authme.data.limbo.LimboService;
import fr.xephi.authme.data.limbo.persistence.LimboPersistence;
import fr.xephi.authme.permission.PermissionsManager;
import fr.xephi.authme.service.BukkitService;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import static fr.xephi.authme.command.executable.authme.debug.DebugSectionUtils.formatLocation;
import static fr.xephi.authme.command.executable.authme.debug.DebugSectionUtils.applyToLimboPlayersMap;

/**
 * Shows the data stored in LimboPlayers and the equivalent properties on online players.
 */
class LimboPlayerViewer implements DebugSection {

    @Inject
    private LimboService limboService;

    @Inject
    private LimboPersistence limboPersistence;

    @Inject
    private BukkitService bukkitService;

    @Inject
    private PermissionsManager permissionsManager;

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
            sender.sendMessage("Available limbo records: " + applyToLimboPlayersMap(limboService, Map::keySet));
            return;
        }

        LimboPlayer memoryLimbo = limboService.getLimboPlayer(arguments.get(0));
        Player player = bukkitService.getPlayerExact(arguments.get(0));
        LimboPlayer diskLimbo = player != null ? limboPersistence.getLimboPlayer(player) : null;
        if (memoryLimbo == null && player == null) {
            sender.sendMessage("No limbo info and no player online with name '" + arguments.get(0) + "'");
            return;
        }

        sender.sendMessage(ChatColor.GOLD + "Showing disk limbo / limbo / player info for '" + arguments.get(0) + "'");
        new InfoDisplayer(sender, diskLimbo, memoryLimbo, player)
            .sendEntry("Is op", LimboPlayer::isOperator, Player::isOp)
            .sendEntry("Walk speed", LimboPlayer::getWalkSpeed, Player::getWalkSpeed)
            .sendEntry("Can fly", LimboPlayer::isCanFly, Player::getAllowFlight)
            .sendEntry("Fly speed", LimboPlayer::getFlySpeed, Player::getFlySpeed)
            .sendEntry("Location", l -> formatLocation(l.getLocation()), p -> formatLocation(p.getLocation()))
            .sendEntry("Group", LimboPlayer::getGroup, permissionsManager::getPrimaryGroup);
    }

    /**
     * Displays the info for the given LimboPlayer and Player to the provided CommandSender.
     */
    private static final class InfoDisplayer {
        private final CommandSender sender;
        private final Optional<LimboPlayer> diskLimbo;
        private final Optional<LimboPlayer> memoryLimbo;
        private final Optional<Player> player;

        /**
         * Constructor.
         *
         * @param sender command sender to send the information to
         * @param memoryLimbo the limbo player to get data from
         * @param player the player to get data from
         */
        InfoDisplayer(CommandSender sender, LimboPlayer diskLimbo, LimboPlayer memoryLimbo, Player player) {
            this.sender = sender;
            this.diskLimbo = Optional.ofNullable(diskLimbo);
            this.memoryLimbo = Optional.ofNullable(memoryLimbo);
            this.player = Optional.ofNullable(player);

            if (memoryLimbo == null) {
                sender.sendMessage("Note: no Limbo information available");
            }
            if (player == null) {
                sender.sendMessage("Note: player is not online");
            } else if (diskLimbo == null) {
                sender.sendMessage("Note: no Limbo on disk available");
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
                + getData(diskLimbo, limboGetter)
                + " / "
                + getData(memoryLimbo, limboGetter)
                + " / "
                + getData(player, playerGetter));
            return this;
        }

        static <E, T> String getData(Optional<E> entity, Function<E, T> getter) {
            return entity.map(getter).map(String::valueOf).orElse(" -- ");
        }
    }
}
