package fr.xephi.authme.settings;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.cache.auth.PlayerAuth;
import fr.xephi.authme.cache.auth.PlayerCache;
import fr.xephi.authme.datasource.DataSource;

/**
 *
 * @author Xephi59
 */
public class PlayersLogs extends CustomConfiguration {

    private static PlayersLogs pllog = null;

    public PlayersLogs() {
        super(new File("." + File.separator + "plugins" + File.separator + "AuthMe" + File.separator + "players.yml"));
        pllog = this;
        load();
        save();
    }

    public void loadPlayers() {
        DataSource database = AuthMe.getInstance().database;
        List<String> list = this.getStringList("players");
        if (list == null || list.isEmpty())
            return;
        for (String s : list) {
            PlayerAuth auth = database.getAuth(s);
            if (auth == null)
                continue;
            auth.setLastLogin(new Date().getTime());
            database.updateSession(auth);
            PlayerCache.getInstance().addPlayer(auth);
        }
    }

    public static PlayersLogs getInstance() {
        if (pllog == null) {
            pllog = new PlayersLogs();
        }
        return pllog;
    }

    public void savePlayerLogs() {
        List<String> players = new ArrayList<String>();
        for (String s : PlayerCache.getInstance().getCache().keySet()) {
            players.add(s);
        }
        this.set("players", players);
        this.save();
    }

    public void clear() {
        this.set("players", new ArrayList<String>());
        this.save();
    }

}
