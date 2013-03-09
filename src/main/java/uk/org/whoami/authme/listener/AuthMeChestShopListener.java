package uk.org.whoami.authme.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import com.Acrobot.ChestShop.Events.PreTransactionEvent;
import com.Acrobot.ChestShop.Events.PreTransactionEvent.TransactionOutcome;

import uk.org.whoami.authme.AuthMe;
import uk.org.whoami.authme.Utils;
import uk.org.whoami.authme.cache.auth.PlayerCache;
import uk.org.whoami.authme.datasource.DataSource;
import uk.org.whoami.authme.plugin.manager.CombatTagComunicator;
import uk.org.whoami.authme.settings.Settings;

public class AuthMeChestShopListener implements Listener {
	
	public DataSource database;
	public AuthMe plugin;
	
	public AuthMeChestShopListener(DataSource database, AuthMe plugin) {
		this.database = database;
		this.plugin = plugin;
	}
	
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPreTransaction(PreTransactionEvent event) {
        if (event.isCancelled() || event.getClient() == null || event == null) {
            return;
        }
        
        Player player = event.getClient();
        String name = player.getName().toLowerCase();

        if (plugin.getCitizensCommunicator().isNPC(player, plugin) || Utils.getInstance().isUnrestricted(player) || CombatTagComunicator.isNPC(player)) {
            return;
        }

        if (PlayerCache.getInstance().isAuthenticated(name)) {
            return;
        }

        if (!database.isAuthAvailable(name)) {
            if (!Settings.isForcedRegistrationEnabled) {
                return;
            }
        }
        event.setCancelled(TransactionOutcome.OTHER);
        
    }

}
