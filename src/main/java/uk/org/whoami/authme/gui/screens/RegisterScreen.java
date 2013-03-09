package uk.org.whoami.authme.gui.screens;

/**
 * @Author Hoezef
 */
import org.getspout.spoutapi.event.screen.ButtonClickEvent;
import org.getspout.spoutapi.gui.GenericLabel;
import org.getspout.spoutapi.gui.GenericPopup;
import org.getspout.spoutapi.gui.GenericTextField;
import org.getspout.spoutapi.gui.WidgetAnchor;
import org.getspout.spoutapi.player.SpoutPlayer;

import uk.org.whoami.authme.gui.Clickable;
import uk.org.whoami.authme.settings.Settings;

public class RegisterScreen extends GenericPopup implements Clickable{

	@SuppressWarnings("unused")
	private SpoutPlayer splayer;
	@SuppressWarnings("unused")
	private boolean forcedRegistrationEnabled;
	private String title;
	
	GenericLabel titleLbl = null;
	GenericTextField messageTF = null;
	
	public RegisterScreen(SpoutPlayer player, Settings settings) {
		this.splayer = player;
		this.forcedRegistrationEnabled = Settings.isForcedRegistrationEnabled;
		this.title = ""; // needs to be set...
		
		createScreen();
	}

	private void createScreen() {
		titleLbl = new GenericLabel();
		titleLbl.setText(title)
			.setAnchor(WidgetAnchor.TOP_CENTER);
		
		
	}

	public void handleClick(ButtonClickEvent event) {
		// TODO Auto-generated method stub
		
	}
	
}
