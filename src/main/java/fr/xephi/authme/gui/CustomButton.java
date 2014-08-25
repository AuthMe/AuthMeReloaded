package fr.xephi.authme.gui;

import org.getspout.spoutapi.event.screen.ButtonClickEvent;
import org.getspout.spoutapi.gui.GenericButton;

public class CustomButton extends GenericButton {

    public Clickable handleRef = null;

    public CustomButton(Clickable c) {
        handleRef = c;
    }

    @Override
    public void onButtonClick(ButtonClickEvent event) {
        handleRef.handleClick(event);
    }

    public CustomButton setMidPos(int x, int y) {
        this.setX(x).setY(y).shiftXPos(-(width / 2)).shiftYPos(-(height / 2));
        return this;
    }

}
