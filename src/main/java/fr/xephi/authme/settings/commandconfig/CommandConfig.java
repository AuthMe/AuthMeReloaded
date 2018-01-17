package fr.xephi.authme.settings.commandconfig;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Command configuration.
 *
 * @see CommandManager
 */
public class CommandConfig {

    private Map<String, Command> onJoin = new LinkedHashMap<>();
    private Map<String, OnLoginCommand> onLogin = new LinkedHashMap<>();
    private Map<String, Command> onSessionLogin = new LinkedHashMap<>();
    private Map<String, OnLoginCommand> onFirstLogin = new LinkedHashMap<>();
    private Map<String, Command> onRegister = new LinkedHashMap<>();
    private Map<String, Command> onUnregister = new LinkedHashMap<>();
    private Map<String, Command> onLogout = new LinkedHashMap<>();

    public Map<String, Command> getOnJoin() {
        return onJoin;
    }

    public void setOnJoin(Map<String, Command> onJoin) {
        this.onJoin = onJoin;
    }

    public Map<String, OnLoginCommand> getOnLogin() {
        return onLogin;
    }

    public void setOnLogin(Map<String, OnLoginCommand> onLogin) {
        this.onLogin = onLogin;
    }

    public Map<String, Command> getOnSessionLogin() {
        return onSessionLogin;
    }

    public void setOnSessionLogin(Map<String, Command> onSessionLogin) {
        this.onSessionLogin = onSessionLogin;
    }

    public Map<String, OnLoginCommand> getOnFirstLogin() {
        return onFirstLogin;
    }

    public void setOnFirstLogin(Map<String, OnLoginCommand> onFirstLogin) {
        this.onFirstLogin = onFirstLogin;
    }

    public Map<String, Command> getOnRegister() {
        return onRegister;
    }

    public void setOnRegister(Map<String, Command> onRegister) {
        this.onRegister = onRegister;
    }

    public Map<String, Command> getOnUnregister() {
        return onUnregister;
    }

    public void setOnUnregister(Map<String, Command> onUnregister) {
        this.onUnregister = onUnregister;
    }

    public Map<String, Command> getOnLogout() {
        return onLogout;
    }

    public void setOnLogout(Map<String, Command> onLogout) {
        this.onLogout = onLogout;
    }
}
