package fr.xephi.authme.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.cache.auth.PlayerCache;
import fr.xephi.authme.security.RandomString;
import fr.xephi.authme.settings.Messages;
import fr.xephi.authme.settings.Settings;

public class CaptchaCommand implements CommandExecutor {

    public AuthMe plugin;
    private Messages m = Messages.getInstance();
    public static RandomString rdm = new RandomString(Settings.captchaLength);

    public CaptchaCommand(AuthMe plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmnd, String label,
            String[] args) {

        if (!(sender instanceof Player)) {
            return true;
        }

        Player player = (Player) sender;
        String name = player.getName().toLowerCase();

        if (args.length == 0) {
            m.send(player, "usage_captcha");
            return true;
        }

        if (PlayerCache.getInstance().isAuthenticated(name)) {
            m.send(player, "logged_in");
            return true;
        }

        if (!plugin.authmePermissible(player, "authme." + label.toLowerCase())) {
            m.send(player, "no_perm");
            return true;
        }

        if (!Settings.useCaptcha) {
            m.send(player, "usage_log");
            return true;
        }

        if (!plugin.cap.containsKey(name)) {
            m.send(player, "usage_log");
            return true;
        }

        if (Settings.useCaptcha && !args[0].equals(plugin.cap.get(name))) {
            plugin.cap.remove(name);
            plugin.cap.put(name, rdm.nextString());
            for (String s : m.send("wrong_captcha")) {
                player.sendMessage(s.replace("THE_CAPTCHA", plugin.cap.get(name)));
            }
            return true;
        }
        try {
            plugin.captcha.remove(name);
            plugin.cap.remove(name);
        } catch (NullPointerException npe) {
        }
        m.send(player, "valid_captcha");
        m.send(player, "login_msg");
        return true;
    }

}
