package fr.xephi.authme.velocity;

import com.velocitypowered.api.proxy.ProxyServer;
import org.slf4j.Logger;

import java.nio.file.Path;

abstract class AbstractAuthMeVelocityPlugin {

    static final String PLUGIN_ID = "authmevelocity";
    static final String PLUGIN_NAME = "AuthMe Velocity";
    static final String PLUGIN_VERSION = "${pluginDescription.version}";

    protected AbstractAuthMeVelocityPlugin(ProxyServer server, Logger logger, Path dataDirectory) {
    }
}
