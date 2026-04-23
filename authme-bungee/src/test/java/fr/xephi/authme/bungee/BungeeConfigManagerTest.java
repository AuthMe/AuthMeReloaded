package fr.xephi.authme.bungee;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BungeeConfigManagerTest {

    @TempDir
    private Path tempDirectory;

    @Test
    void shouldCreateConfigFileWithDefaults() {
        BungeeConfigManager configManager = new BungeeConfigManager(tempDirectory);

        assertTrue(Files.exists(tempDirectory.resolve("config.yml")));
        assertTrue(configManager.getConfiguration().authServers().contains("lobby"));
        assertFalse(configManager.getConfiguration().allServersAreAuthServers());
        assertTrue(configManager.getConfiguration().commandsRequireAuth());
        assertTrue(configManager.getConfiguration().chatRequiresAuth());
        assertTrue(configManager.getConfiguration().serverSwitchRequiresAuth());
        assertFalse(configManager.getConfiguration().autoLoginEnabled());
        assertFalse(configManager.getConfiguration().sharedSecret().isEmpty());
    }

    @Test
    void shouldPreserveExistingSharedSecret() throws IOException {
        Files.writeString(tempDirectory.resolve("config.yml"), "proxySharedSecret: my-existing-secret\n");

        BungeeConfigManager configManager = new BungeeConfigManager(tempDirectory);

        assertEquals("my-existing-secret", configManager.getConfiguration().sharedSecret());
    }

    @Test
    void shouldNormalizeConfiguredSettings() throws IOException {
        Files.writeString(tempDirectory.resolve("config.yml"), """
            authServers:
            - Lobby
            - HUB
            allServersAreAuthServers: false
            commands:
              whitelist:
              - login
              - /REG
            chatRequiresAuth: false
            serverSwitch:
              requiresAuth: false
              kickMessage: Please authenticate first.
            autoLogin: true
            sendOnLogout: true
            unloggedUserServer: LiMbO
            """);

        BungeeProxyConfiguration configuration = new BungeeConfigManager(tempDirectory).getConfiguration();

        assertTrue(configuration.authServers().contains("lobby"));
        assertTrue(configuration.authServers().contains("hub"));
        assertTrue(configuration.isWhitelistedCommand("login"));
        assertTrue(configuration.isWhitelistedCommand("/reg"));
        assertFalse(configuration.chatRequiresAuth());
        assertFalse(configuration.serverSwitchRequiresAuth());
        assertEquals("Please authenticate first.", configuration.serverSwitchKickMessage());
        assertTrue(configuration.autoLoginEnabled());
        assertTrue(configuration.sendOnLogoutEnabled());
        assertEquals("limbo", configuration.sendOnLogoutTarget());
    }
}
