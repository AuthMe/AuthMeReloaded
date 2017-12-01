package fr.xephi.authme.command.executable.authme.debug;

import fr.xephi.authme.data.auth.PlayerAuth;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.permission.DebugSectionPermissions;
import fr.xephi.authme.permission.PermissionNode;
import fr.xephi.authme.service.GeoIpService;
import fr.xephi.authme.service.ValidationService;
import fr.xephi.authme.settings.properties.ProtectionSettings;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import javax.inject.Inject;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Shows the GeoIP information as returned by the geoIpService.
 */
class CountryLookup implements DebugSection {

    private static final Pattern IS_IP_ADDR = Pattern.compile("(\\d{1,3}\\.){3}\\d{1,3}");

    @Inject
    private GeoIpService geoIpService;

    @Inject
    private DataSource dataSource;

    @Inject
    private ValidationService validationService;

    @Override
    public String getName() {
        return "cty";
    }

    @Override
    public String getDescription() {
        return "Check country protection / country data";
    }

    @Override
    public void execute(CommandSender sender, List<String> arguments) {
        sender.sendMessage(ChatColor.BLUE + "AuthMe country lookup");
        if (arguments.isEmpty()) {
            sender.sendMessage("Check player: /authme debug cty Bobby");
            sender.sendMessage("Check IP address: /authme debug cty 127.123.45.67");
            return;
        }

        String argument = arguments.get(0);
        if (IS_IP_ADDR.matcher(argument).matches()) {
            outputInfoForIpAddr(sender, argument);
        } else {
            outputInfoForPlayer(sender, argument);
        }
    }

    @Override
    public PermissionNode getRequiredPermission() {
        return DebugSectionPermissions.COUNTRY_LOOKUP;
    }

    private void outputInfoForIpAddr(CommandSender sender, String ipAddr) {
        sender.sendMessage("IP '" + ipAddr + "' maps to country '" + geoIpService.getCountryCode(ipAddr)
            + "' (" + geoIpService.getCountryName(ipAddr) + ")");
        if (validationService.isCountryAdmitted(ipAddr)) {
            sender.sendMessage(ChatColor.DARK_GREEN + "This IP address' country is not blocked");
        } else {
            sender.sendMessage(ChatColor.DARK_RED + "This IP address' country is blocked from the server");
        }
        sender.sendMessage("Note: if " + ProtectionSettings.ENABLE_PROTECTION + " is false no country is blocked");
    }

    // TODO #1366: Extend with registration IP?
    private void outputInfoForPlayer(CommandSender sender, String name) {
        PlayerAuth auth = dataSource.getAuth(name);
        if (auth == null) {
            sender.sendMessage("No player with name '" + name + "'");
        } else if (auth.getLastIp() == null) {
            sender.sendMessage("No last IP address known for '" + name + "'");
        } else {
            sender.sendMessage("Player '" + name + "' has IP address " + auth.getLastIp());
            outputInfoForIpAddr(sender, auth.getLastIp());
        }
    }
}
