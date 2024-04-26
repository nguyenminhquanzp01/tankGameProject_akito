package coreGame.game.powerup;

import coreGame.game.powerups;
import coreGame.game.tank;

import java.awt.image.BufferedImage;

public class resetHp extends powerups {
	public resetHp(float y, float x, BufferedImage img) {
		super(y, x, img);
	}
	
	@Override
	public void doPowerUp(tank t) {
		t.resetHp();
	}
	
	@Override
	public void setVisible(boolean b) {
		this.visible = b;
	}
}