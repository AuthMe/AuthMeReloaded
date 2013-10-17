package fr.xephi.authme.settings;

import java.io.File;
import java.util.List;

import org.bukkit.entity.Player;

/**
*
* @author Xephi59
*/
public class PlayersLogs extends CustomConfiguration {
	private static PlayersLogs pllog = null;
	public static List<String> players;

	@SuppressWarnings("unchecked")
	public PlayersLogs() {
		super(new File("./plugins/AuthMe/players.yml"));
		pllog = this;
		load();
		save();
		players = (List<String>) this.getList("players");
	}

	public static PlayersLogs getInstance() {
        if (pllog == null) {
            pllog = new PlayersLogs();
        }
        return pllog;
    }
    
    public void addPlayer(Player player) {
        List<String> players = this.getStringList("players");
        if (!players.contains(player.getName())) {
            players.add(player.getName());
            this.set("players", players);
            save();
        }
    }
}
