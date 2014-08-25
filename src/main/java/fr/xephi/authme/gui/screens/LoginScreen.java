package fr.xephi.authme.gui.screens;

import java.util.List;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.getspout.spoutapi.event.screen.ButtonClickEvent;
import org.getspout.spoutapi.gui.Button;
import org.getspout.spoutapi.gui.Color;
import org.getspout.spoutapi.gui.GenericLabel;
import org.getspout.spoutapi.gui.GenericPopup;
import org.getspout.spoutapi.gui.GenericTextField;
import org.getspout.spoutapi.gui.RenderPriority;
import org.getspout.spoutapi.gui.Widget;
import org.getspout.spoutapi.gui.WidgetAnchor;
import org.getspout.spoutapi.player.SpoutPlayer;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.gui.Clickable;
import fr.xephi.authme.gui.CustomButton;
import fr.xephi.authme.settings.SpoutCfg;

public class LoginScreen extends GenericPopup implements Clickable {

    public AuthMe plugin = AuthMe.getInstance();
    private SpoutCfg spoutCfg = SpoutCfg.getInstance();
    private CustomButton exitBtn;
    private CustomButton loginBtn;
    private GenericTextField passBox;
    private GenericLabel titleLbl;
    private GenericLabel textLbl;
    private GenericLabel errorLbl;

    String exitTxt = spoutCfg.getString("LoginScreen.exit button");
    String loginTxt = spoutCfg.getString("LoginScreen.login button");
    String exitMsg = spoutCfg.getString("LoginScreen.exit message");
    String title = spoutCfg.getString("LoginScreen.title");
    @SuppressWarnings("unchecked")
    List<String> textlines = (List<String>) spoutCfg.getList("LoginScreen.text");
    public SpoutPlayer splayer;

    public LoginScreen(SpoutPlayer player) {
        this.splayer = player;
        createScreen();
    }

    private void createScreen() {
        int objects = textlines.size() + 4;
        int part = !(textlines.size() <= 5) ? 195 / objects : 20;
        int h = 3 * part / 4, w = 8 * part;
        titleLbl = new GenericLabel();
        titleLbl.setText(title).setTextColor(new Color(1.0F, 0, 0, 1.0F)).setAlign(WidgetAnchor.TOP_CENTER).setHeight(h).setWidth(w).setX(maxWidth / 2).setY(25);
        this.attachWidget(plugin, titleLbl);
        int ystart = 25 + h + part / 2;
        for (int x = 0; x < textlines.size(); x++) {
            textLbl = new GenericLabel();
            textLbl.setText(textlines.get(x)).setAlign(WidgetAnchor.TOP_CENTER).setHeight(h).setWidth(w).setX(maxWidth / 2).setY(ystart + x * part);
            this.attachWidget(plugin, textLbl);
        }
        passBox = new GenericTextField();
        passBox.setMaximumCharacters(18).setMaximumLines(1).setHeight(h - 2).setWidth(w - 2).setY(220 - h - 2 * part);
        passBox.setPasswordField(true);
        setXToMid(passBox);
        this.attachWidget(plugin, passBox);
        errorLbl = new GenericLabel();
        errorLbl.setText("").setTextColor(new Color(1.0F, 0, 0, 1.0F)).setHeight(h).setWidth(w).setX(passBox.getX() + passBox.getWidth() + 2).setY(passBox.getY());
        this.attachWidget(plugin, errorLbl);
        loginBtn = new CustomButton(this);
        loginBtn.setText(loginTxt).setHeight(h).setWidth(w).setY(220 - h - part);
        setXToMid(loginBtn);
        this.attachWidget(plugin, loginBtn);
        exitBtn = new CustomButton(this);
        exitBtn.setText(exitTxt).setHeight(h).setWidth(w).setY(220 - h);
        setXToMid(exitBtn);
        this.attachWidget(plugin, exitBtn);
        this.setPriority(RenderPriority.Highest);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void handleClick(ButtonClickEvent event) {
        Button b = event.getButton();
        SpoutPlayer player = event.getPlayer();
        if (event.isCancelled() || event == null || event.getPlayer() == null)
            return;
        if (b.equals(loginBtn)) {
            plugin.management.performLogin(player, passBox.getText(), false);
        } else if (b.equals(exitBtn)) {
            event.getPlayer().kickPlayer(exitMsg);
        }
    }

    private void setXToMid(Widget w) {
        w.setX((maxWidth - w.getWidth()) / 2);
    }

}
