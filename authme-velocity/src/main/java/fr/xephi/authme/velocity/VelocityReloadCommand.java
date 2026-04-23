package fr.xephi.authme.velocity;

import com.velocitypowered.api.command.SimpleCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

final class VelocityReloadCommand implements SimpleCommand {

    private final VelocityConfigManager configManager;
    private final VelocityProxyBridge proxyBridge;

    VelocityReloadCommand(VelocityConfigManager configManager, VelocityProxyBridge proxyBridge) {
        this.configManager = configManager;
        this.proxyBridge = proxyBridge;
    }

    @Override
    public void execute(Invocation invocation) {
        proxyBridge.reload(configManager.reload());
        invocation.source().sendMessage(
            Component.text("AuthMe Velocity configuration reloaded!", NamedTextColor.GREEN));
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("authmevelocity.reload");
    }
}
