package uk.org.whoami.authme.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import uk.org.whoami.authme.AuthMe;
import uk.org.whoami.authme.cache.auth.PlayerCache;
import uk.org.whoami.authme.security.RandomString;
import uk.org.whoami.authme.settings.Messages;
import uk.org.whoami.authme.settings.Settings;

public class CaptchaCommand implements CommandExecutor {

	public AuthMe plugin;
    private Messages m = Messages.getInstance();
    public static RandomString rdm = new RandomString(Settings.captchaLength);

    public CaptchaCommand(AuthMe plugin) {
    	this.plugin = plugin;
    }

	@Override
	public boolean onCommand(CommandSender sender, Command cmnd,
			String label, String[] args) {

        if (!(sender instanceof Player)) {
            return true;
        }

        Player player = (Player) sender;
        String name = player.getName().toLowerCase();

        if (args.length == 0) {
            player.sendMessage(m._("usage_captcha"));
            return true;
        }

        if (PlayerCache.getInstance().isAuthenticated(name)) {
            player.sendMessage(m._("logged_in"));
            return true;
        }

        if (!plugin.authmePermissible(player, "authme." + label.toLowerCase())) {
            player.sendMessage(m._("no_perm"));
            return true;
        }

        if (!Settings.useCaptcha) {
        	player.sendMessage(m._("usage_log"));
        	return true;
        }

		if(!plugin.cap.containsKey(name)) {
        	player.sendMessage(m._("usage_log"));
        	return true;
		}

        if(Settings.useCaptcha && !args[0].equals(plugin.cap.get(name))) {
        	plugin.cap.remove(name);
        	plugin.cap.put(name, rdm.nextString());
        	player.sendMessage(m._("wrong_captcha").replaceAll("THE_CAPTCHA", plugin.cap.get(name)));
        	return true;
        }
        try {
            plugin.captcha.remove(name);
            plugin.cap.remove(name);
        } catch (NullPointerException npe) {
        }
        player.sendMessage(m._("valid_captcha"));
        player.sendMessage(m._("login_msg"));
        return true;
	}

}
