package fr.xephi.authme.bungee;

public final class AuthMeBungeePlugin extends AbstractAuthMeBungeePlugin {

    private BungeeConfigManager configManager;
    private BungeeProxyBridge proxyBridge;

    @Override
    public void onEnable() {
        configManager = new BungeeConfigManager(getDataFolder().toPath());
        BungeeAuthenticationStore authenticationStore = new BungeeAuthenticationStore();
        proxyBridge = new BungeeProxyBridge(getProxy(), getLogger(), configManager.getConfiguration(), authenticationStore);
        getProxy().getPluginManager().registerListener(this, proxyBridge);
        getProxy().getPluginManager().registerCommand(this, new BungeeReloadCommand(configManager, proxyBridge));
        proxyBridge.logConfigurationDetails();
        proxyBridge.registerChannels();
    }

    @Override
    public void onDisable() {
        proxyBridge.shutdown();
    }
}
