package fr.xephi.authme.settings;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Xephi59
 */
public class PlayersLogs extends CustomConfiguration {

    private static PlayersLogs pllog = null;
    public List<String> players;

    public PlayersLogs() {
        super(new File("." + File.separator + "plugins" + File.separator + "AuthMe" + File.separator + "players.yml"));
        pllog = this;
        load();
        save();
        players = this.getStringList("players");
    }

    public void clear() {
        set("players", new ArrayList<String>());
        save();
    }

    public static PlayersLogs getInstance() {
        if (pllog == null) {
            pllog = new PlayersLogs();
        }
        return pllog;
    }

    public void addPlayer(String user) {
        players = this.getStringList("players");
        if (!players.contains(user)) {
            players.add(user);
            set("players", players);
            save();
        }
    }

    public void removePlayer(String user) {
        players = this.getStringList("players");
        if (players.contains(user)) {
            players.remove(user);
            set("players", players);
            save();
        }
    }
}
