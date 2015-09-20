package fr.xephi.authme.listener;

import com.Acrobot.ChestShop.Events.PreTransactionEvent;
import com.Acrobot.ChestShop.Events.PreTransactionEvent.TransactionOutcome;
import fr.xephi.authme.AuthMe;
import fr.xephi.authme.Utils;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class AuthMeChestShopListener implements Listener {

    public AuthMe plugin;

    public AuthMeChestShopListener(AuthMe plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onPreTransaction(PreTransactionEvent event) {
        if (Utils.checkAuth(event.getClient()))
            return;
        event.setCancelled(TransactionOutcome.OTHER);
    }
}
