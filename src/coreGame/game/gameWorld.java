package coreGame.game;

import coreGame.launcher;
import coreGame.constants;
import coreGame.resource;
import coreGame.sound;
import coreGame.game.powerup.*;

import javax.swing.*;
import java.awt.*;
import java.lang.Integer;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.awt.geom.AffineTransform;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class gameWorld extends JPanel implements Runnable {
	/**
	 * 
	 */
	//private static final long serialVersionUID = 1L;
	private BufferedImage world;
	private tank t1, t2;
	private launcher lf;
	private long tick = 0;

	private sound sand;
	
	List<wall> walls = new ArrayList<wall>();
	List<powerups> Powerups = new ArrayList<powerups>();
	
	public gameWorld(launcher lf) {
		this.lf = lf;
	}
	
	@Override
	public void run() {
		try {
			this.resetGame();
			sand.run();
			
			while (true) {
				this.tick++;
				
				this.t1.update(t2);
				this.t2.update(t1);
				
				for (int i = 0; i < walls.size(); i++) {
					wall x = this.walls.get(i);
					
					if (x.getHitBox().intersects(t1.getHitBox()))
						t1.handleCollision(x);
					
					if (x.getHitBox().intersects(t2.getHitBox()))
						t2.handleCollision(x);
					
					t1.shootOther(x);
					t2.shootOther(x);
				}
				
				for (int i = 0; i < Powerups.size(); i++) {
					powerups t = this.Powerups.get(i);
					
					if (t.getHitBox().intersects(this.t1.getHitBox())) {
						t.handleCollision(t1);
						Powerups.remove(t);
					}
					
					if (t.getHitBox().intersects(this.t2.getHitBox())) {
						t.handleCollision(t2);
						Powerups.remove(t);
					}
				}
				
				if (this.t1.getIsDead() || this.t2.getIsDead())  {
					sand.stopSound();
					
					if (t1.getIsDead()) lf.setWinner(false);
					
					if (t2.getIsDead()) lf.setWinner(true);
					
					this.lf.setFrame("end");
					return;
				}
				
				this.repaint();
				Thread.sleep(1000 / 144);
			}
		} catch (InterruptedException ignored) {
			Thread.currentThread().interrupt();
			System.out.println(ignored);
		}
	}
	
	public void resetGame() {
		this.tick = 0;
		this.t1.setX(300);
		this.t1.setY(300);
		this.t2.setX(1400);
		this.t2.setY(1000);
	}
	
	public void InitializeGame() {
		resource.initResources();
		
		this.world = new BufferedImage(constants.MAP_WIDTH,
				constants.MAP_HEIGHT, BufferedImage.TYPE_INT_RGB);
		
		this.sand = new sound(resource.getClip("sand"));
		
		t1 = new tank(300, 300, 0, 0, (short) 0, resource.getImage("tank2img"));
		tankControl tc1 = new tankControl(t1, KeyEvent.VK_W, KeyEvent.VK_S, KeyEvent.VK_A, KeyEvent.VK_D, KeyEvent.VK_SPACE);
		this.lf.getJf().addKeyListener(tc1);
		
		t2 = new tank(1400, 1000, 0, 0, (short) 0, resource.getImage("tank1img"));
		tankControl tc2 = new tankControl(t2, KeyEvent.VK_UP, KeyEvent.VK_DOWN, KeyEvent.VK_LEFT, KeyEvent.VK_RIGHT, KeyEvent.VK_ENTER);
		this.lf.getJf().addKeyListener(tc2);
		
		try (BufferedReader mapReader = new BufferedReader(new InputStreamReader(gameWorld.class.getClassLoader().getResourceAsStream("resources/rttt/map.txt")))) {
			String[] size = mapReader.readLine().split(",");
			
			int numRows = Integer.parseInt(size[0]);
			int numCols = Integer.parseInt(size[1]);
			
			for (int i = 0; mapReader.ready(); i++) {
				String[] items = mapReader.readLine().split("");
				
				for (int j = 0; j < items.length; j++) {
					switch (items[j]) {
						case "3", "9" -> {
							wall w = new wall(i * 30, j * 30, resource.getImage("wall"));
							walls.add(w);
						}
						
						case "2" -> {
							cactus c = new cactus(i * 30, j * 30, resource.getImage("cactus"));
							walls.add(c);
						}
						
						case "5" -> {
							addLife l = new addLife(i * 30, j * 30, resource.getImage("addLife"));
							Powerups.add(l);
						}
						
						case "6" -> {
							addSpeed s = new addSpeed(i * 30, j * 30, resource.getImage("addSpeed"));
							Powerups.add(s);
						}
						
						case "7" -> {
							resetHp h = new resetHp(i * 30, j * 30, resource.getImage("resetHealth"));
							Powerups.add(h);
						}
						
						case "8" -> {
							slowRotate r = new slowRotate(i * 30, j * 30, resource.getImage("slowRotate"));
							Powerups.add(r);
						}
					}
				}
			}
		} catch (IOException e) {
			System.out.println(e);
			e.printStackTrace();
			System.exit(-2);
		}
	}
	
	@Override
	public void paintComponent(Graphics g) {
		Graphics2D g2d = (Graphics2D) g;
		Graphics2D buffer = world.createGraphics();
		
		buffer.setColor(Color.black);
		buffer.fillRect(0, 0, constants.MAP_WIDTH, constants.MAP_HEIGHT);
		
		for (int i = 0; i < constants.MAP_WIDTH; i += 32)
			for (int j = 0; j < constants.MAP_HEIGHT; j += 32)
				buffer.drawImage(resource.getImage("floor"), i, j, null);
		
		for (Iterator<wall> it = walls.iterator(); it.hasNext(); ) {
			wall nextWall = it.next();
			if (!nextWall.isVisible())
				it.remove();
		}
		
		walls.forEach(w -> w.drawImage(buffer));
		
		Powerups.forEach(p -> p.drawImage(buffer));
		
		this.t1.drawImage(buffer);
		this.t2.drawImage(buffer);
		
		g2d.drawImage(world, 0, 0, null);
		this.drawSplitScreen(world, g2d);
		this.drawMinimap(world, g2d);
	}
	
	private void drawSplitScreen(BufferedImage world, Graphics2D g2) {
        BufferedImage lh = world.getSubimage(t1.getScreen_x(), t1.getScreen_y(), constants.GAME_SCREEN_WIDTH/ 2, constants.GAME_SCREEN_HEIGHT);
        g2.drawImage(lh, 0, 0, null);

        BufferedImage rh = world.getSubimage(t2.getScreen_x(), t2.getScreen_y(), constants.GAME_SCREEN_WIDTH/ 2, constants.GAME_SCREEN_HEIGHT);
        g2.drawImage(rh, constants.GAME_SCREEN_WIDTH/2, 0, null);

        g2.setColor(Color.black);
        g2.drawRect(constants.GAME_SCREEN_WIDTH/2 -2, 0, 4, constants.GAME_SCREEN_HEIGHT);
        g2.fillRect(constants.GAME_SCREEN_WIDTH/2 -2, 0, 4, constants.GAME_SCREEN_HEIGHT);
    }
	
	private void drawMinimap(BufferedImage world, Graphics2D g2d) {
		BufferedImage minimap = world.getSubimage(0, 0, constants.MAP_WIDTH, constants.MAP_HEIGHT);
		AffineTransform at = new AffineTransform();
		
		at.translate(constants.GAME_SCREEN_WIDTH / 2f - (constants.MAP_WIDTH * 0.2) / 2f,
				constants.GAME_SCREEN_HEIGHT - constants.MAP_HEIGHT * 0.3);
		
		at.scale(.2, .2);
		g2d.drawImage(minimap, at, null);
	}
}