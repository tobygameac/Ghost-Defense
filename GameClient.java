import java.awt.*;
import javax.swing.*;

import javazoom.jl.decoder.JavaLayerException;
import javazoom.jl.player.advanced.AdvancedPlayer;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferStrategy;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;

@SuppressWarnings("serial")
public class GameClient extends JFrame implements Runnable, GameConstants {
	// Version
	private final double version = 1.120614;

	// Frame location
	private int locationX;
	private int locationY;

	// Font
	private final Font font = new Font("SketchFlow Print", Font.BOLD, 12);

	// Container
	private JPanel contentPane;

	// Cursor
	private Image cursorImage;
	private Cursor cursor;

	// Sound
	private SimpleSoundPlayer clickButton;
	private SimpleSoundPlayer shoot;
	private SimpleSoundPlayer miss;
	private SimpleSoundPlayer bump;
	private SimpleSoundPlayer hit;
	private SimpleSoundPlayer armor;
	private SimpleSoundPlayer win;
	private SimpleSoundPlayer lose;
	private SimpleSoundPlayer warning;
	private SimpleSoundPlayer evilLaugh;

	// Background image
	private Image backImage[];
	private Image background;

	// Menu button
	private JButton startButton;
	private JButton reButton;
	private JButton aboutButton;
	private JButton exitButton;

	// Monster

	// Ghost
	private ArrayList<Sprite> ghosts = new ArrayList<Sprite>();
	private Image ghostImage[][];
	private int ghostNumbers;

	// Boss
	private Sprite boss;
	private Image bossImage[];
	private double bossLife;
	private double bossLifeNow;
	private boolean bossChangeA;
	private boolean bossChangeB;

	// Game
	private int level;
	private double timeLeft;
	private boolean starting;

	// Cannon & Bullet
	private Image cannonImage;
	private double cannonAngle1;
	private double cannonAngle2;
	private Point cannonLocation1 = new Point(672, 768);
	private Point cannonLocation2 = new Point(352, 768);
	private ArrayList<Sprite> bullets = new ArrayList<Sprite>();
	private Image bulletImage;
	private Point now = new Point(0, 0);
	private int bulletNumbers;
	private int bulletLeft;
	private int bulletPower;
	private int bulletPower1;
	private int bulletPower2;
	private boolean powerup;

	// Explosion
	private Sprite bomb;
	private Image bombImage;
	private double bombTime;

	// Connection
	private boolean connection;
	private int port;
	private String host;
	private Socket socket;
	private DataInputStream fromServer;
	private DataOutputStream toServer;
	private int playerID;
	private boolean go = true;
	private boolean waiting = false;

	public GameClient() {
		// Center of the Screen
		locationX = Toolkit.getDefaultToolkit().getScreenSize().width / 2 - 512;
		locationY = Toolkit.getDefaultToolkit().getScreenSize().height / 2 - 384;

		setBounds(locationX, locationY, 1024, 768);

		contentPane = new JPanel();
		contentPane.setLayout(null);
		setContentPane(contentPane);
		((JComponent) contentPane).setOpaque(false);

		// keyboard control
		addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent k) {
				// Power
				if (k.getKeyCode() == KeyEvent.VK_SPACE) {
					if (bulletPower < 100 && starting) {
						if (bulletLeft > 0)
							powerup = true;
						else
							powerup = false;
					}
				}

				// Cheat
				else if (k.getKeyCode() == KeyEvent.VK_F5) {
					if (starting && !connection) {
						timeLeft = 9999;
						bulletLeft = 9999;
					}
				}

				// Pause
				else if (k.getKeyCode() == KeyEvent.VK_ESCAPE) {

				}
			}

			public void keyReleased(KeyEvent k) {
				// Shoot
				if (k.getKeyCode() == KeyEvent.VK_SPACE) {
					powerup = false;
					if (starting && (level == 1 || level == 2)) {
						if (bulletLeft > 0) {
							newBullet();
							playSound(shoot);
						} else
							playSound(warning);
					}
					bulletPower = 0;
				}
			}
		});

		// Mouse control
		addMouseListener(new MouseAdapter() {

			@Override
			public void mousePressed(MouseEvent e) {
				// Power
				if (bulletPower < 100 && starting) {
					if (bulletLeft > 0)
						powerup = true;
					else
						powerup = false;
				}
			}

			@Override
			public void mouseReleased(MouseEvent e) {
				// Shoot
				powerup = false;
				if (starting && (level == 1 || level == 2)) {
					if (bulletLeft > 0) {
						newBullet();

						playSound(shoot);
					} else
						playSound(warning);
				}
				bulletPower = 0;
			}
		});

		// Mouse location
		addMouseMotionListener(new MouseMotionAdapter() {
			@Override
			public void mouseDragged(MouseEvent e) {
				now = e.getPoint();
				Point cannonlocation = new Point();
				if (connection) {
					if (playerID == PLAYER1) {
						cannonlocation.x = cannonLocation1.x;
						cannonlocation.y = cannonLocation1.y;
					} else {
						cannonlocation.x = cannonLocation2.x;
						cannonlocation.y = cannonLocation2.y;
					}
				} else {
					cannonlocation.x = 512;
					cannonlocation.y = 768;
				}
				double a = (now.x - cannonlocation.x)
						* (now.x - cannonlocation.x);
				double c = (cannonlocation.y - now.y)
						* (cannonlocation.y - now.y);
				double b = Math.sqrt(a * a + c * c);
				double angle = Math.acos((b * b + c * c - a * a) / (2 * b * c));
				if (now.x < cannonlocation.x)
					angle *= -1;
				if (connection) {
					if (playerID == PLAYER1)
						cannonAngle1 = angle;
					else
						cannonAngle2 = angle;
					try {
						toServer.writeDouble(CANNON_MOVE);
						toServer.writeDouble(angle);
					} catch (IOException e1) {
					}
				} else {
					cannonAngle1 = angle;
				}
			}

			@Override
			public void mouseMoved(MouseEvent e) {
				now = e.getPoint();
				Point cannonlocation = new Point();
				if (connection) {
					if (playerID == PLAYER1) {
						cannonlocation.x = cannonLocation1.x;
						cannonlocation.y = cannonLocation1.y;
					} else {
						cannonlocation.x = cannonLocation2.x;
						cannonlocation.y = cannonLocation2.y;
					}
				} else {
					cannonlocation.x = 512;
					cannonlocation.y = 768;
				}
				double a = (now.x - cannonlocation.x)
						* (now.x - cannonlocation.x);
				double c = (cannonlocation.y - now.y)
						* (cannonlocation.y - now.y);
				double b = Math.sqrt(a * a + c * c);
				double angle = Math.acos((b * b + c * c - a * a) / (2 * b * c));
				if (now.x < cannonlocation.x)
					angle *= -1;
				if (connection) {
					if (playerID == PLAYER1)
						cannonAngle1 = angle;
					else
						cannonAngle2 = angle;
					try {
						toServer.writeDouble(CANNON_MOVE);
						toServer.writeDouble(angle);
					} catch (IOException e1) {
					}
				} else {
					cannonAngle1 = angle;
				}
			}
		});

		// Start button
		startButton = new JButton("Start");
		startButton.setBackground(new Color(0, 0, 0, 0));
		startButton.setBounds(472, 150, 80, 40);
		startButton.setFont(font);
		startButton.setFocusable(false);
		startButton.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (startButton.isEnabled()) {
					if (connection)
						try {
							toServer.writeDouble(START);
						} catch (IOException e1) {
						}
					playSound(clickButton);
					if (startButton.getText() != "Back") {
						aboutButton.setVisible(false);
						exitButton.setVisible(false);
						startButton.setText("Back");
						startButton.setBounds(0, getHeight() - 40, 80, 40);
						reButton.setVisible(true);
						setLevel(1);
					} else {
						aboutButton.setVisible(true);
						exitButton.setVisible(true);
						startButton.setText("Start");
						startButton.setBounds(472, 150, 80, 40);
						reButton.setVisible(false);
						setLevel(0);
					}
				}
			}
		});
		contentPane.add(startButton);

		// Restart button
		reButton = new JButton("Again");
		reButton.setVisible(false);
		reButton.setBackground(new Color(0, 0, 0, 0));
		reButton.setBounds(80, 728, 80, 40);
		reButton.setFont(font);
		reButton.setFocusable(false);
		reButton.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (reButton.isEnabled()) {
					if (connection)
						try {
							toServer.writeDouble(RESTART);
						} catch (IOException e1) {
						}
					playSound(clickButton);
					setLevel(1);
				}
			}
		});
		contentPane.add(reButton);

		// About button
		aboutButton = new JButton("About");
		aboutButton.setBackground(new Color(0, 0, 0, 0));
		aboutButton.setBounds(472, 190, 80, 40);
		aboutButton.setFont(font);
		aboutButton.setFocusable(false);
		aboutButton.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (aboutButton.isEnabled()) {
					if (connection)
						try {
							toServer.writeDouble(ABOUT);
						} catch (IOException e1) {
						}
					playSound(clickButton);
					if (aboutButton.getText() != "Back") {
						startButton.setVisible(false);
						exitButton.setVisible(false);
						aboutButton.setText("Back");
						aboutButton.setBounds(0, getHeight() - 40, 80, 40);
						setLevel(-1);
					} else {
						startButton.setVisible(true);
						exitButton.setVisible(true);
						aboutButton.setText("About");
						aboutButton.setBounds(472, 190, 80, 40);
						setLevel(0);
					}
				}
			}
		});
		contentPane.add(aboutButton);

		// Exit button
		exitButton = new JButton("Exit");
		exitButton.setBackground(new Color(0, 0, 0, 0));
		exitButton.setBounds(472, 230, 80, 40);
		exitButton.setFont(font);
		exitButton.setFocusable(false);
		exitButton.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent arg0) {
				if (connection)
					try {
						toServer.writeDouble(EXIT);
					} catch (IOException e1) {
					}
				System.exit(0);
			}
		});
		contentPane.add(exitButton);
	}

	public void animationLoop() {
		long startTime = System.currentTimeMillis();
		long currTime = startTime;

		while (true) {
			long elapsedTime = System.currentTimeMillis() - currTime;
			// State change
			if (starting)
				timeLeft -= elapsedTime / 1000D;
			if (timeLeft < 0)
				timeLeft = 0;
			currTime += elapsedTime;

			// Update the sprite
			update(elapsedTime);
			BufferStrategy strategy = getBufferStrategy();
			Graphics2D g = (Graphics2D) strategy.getDrawGraphics();
			draw(g);
			g.dispose();
			strategy.show();

			// Waiting for another player
			if (waiting) {
				startButton.setEnabled(false);
				startButton.setText("Waiting");
			} else if (!waiting && !go) {
				go = true;
				startButton.setEnabled(true);
				startButton.setText("Start");
			}

			// Win
			if (bulletLeft >= 0 && timeLeft > 0 && starting) {
				if (level == 1 && ghostNumbers == 0)
					setLevel(2);

				else if (level == 2 && bossLifeNow <= 0 && bossChangeB) {
					playSound(win);
					setLevel(3);
				}
			}

			// Lose
			if ((timeLeft <= 0 || (bulletLeft <= 0 && bulletNumbers <= 0))
					&& starting)
				setLevel(-2);

			if (bombTime >= 0)
				bombTime -= 0.02;
			else
				bomb = null;
			if(level == 2 && bossLifeNow > bossLife)
				bossLife = bossLifeNow;
			if (level == 2 && bossLifeNow < bossLife && boss.getWidth() >= 100) {
				if (connection) {
					if (playerID == PLAYER1)
						bossLifeNow += 0.75;
				} else
					bossLifeNow += 0.05;
				if (bossChangeA) {
					if (connection) {
						if (playerID == PLAYER1)
							bossLifeNow += 0.15;
					} else
						bossLifeNow += 0.1;
				}
				if (bossChangeB) {
					if (connection) {
						if (playerID == PLAYER1)
							bossLifeNow += 0.3;
					} else
						bossLifeNow += 0.2;
				}
				if ((bossLifeNow / bossLife) <= 0.1 && !bossChangeA
						&& !bossChangeB) {
					double originalX = boss.getY();
					double originalY = boss.getY();
					boss = new SpriteGenerator(100, bossImage).getSprite();
					boss.setX(originalX);
					boss.setY(originalY);
					boss.setVelocityX(0.25D);
					boss.setVelocityY(0.25D);
					bossLifeNow = bossLife / 10D;
					playSound(evilLaugh);
					bossChangeA = true;

				} else if ((bossLifeNow / bossLife) <= 0.05 && bossChangeA
						&& !bossChangeB) {
					double originalX = boss.getY();
					double originalY = boss.getY();
					boss = new SpriteGenerator(101, bossImage).getSprite();
					boss.setX(originalX);
					boss.setY(originalY);
					boss.setVelocityX(0.5D);
					boss.setVelocityY(0.5D);
					bossLifeNow = bossLife / 20D;
					playSound(evilLaugh);
					bossChangeB = true;
				}
				if (connection && playerID == PLAYER1)
					try {
						toServer.writeDouble(BOSS_LIFE_CHANGE);
						toServer.writeDouble(bossLifeNow);
					} catch (IOException e) {
					}

			}

			try {
				Thread.sleep(20);
			} catch (InterruptedException ex) {
			}
		}
	}

	public void update(long elapsedTime) {

		// Outside check of bullet
		for (int i = 0; i < bulletNumbers; i++) {
			Sprite bullet;
			try {
				bullet = bullets.get(i);
				if (bullet.getX() < 0) {
					playSound(miss);
					bullets.remove(i);
					bulletNumbers--;
					if (level == 1) {
						if (!connection)
							newGhost();

					} else {
						if (playerID == PLAYER1) {
							bossLifeNow += bullet.getVelocity() * 100;
							if (connection)
								try {
									toServer.writeDouble(BOSS_LIFE_CHANGE);
									toServer.writeDouble(bossLifeNow);
								} catch (IOException e) {
								}
						}
					}
				} else if (bullet.getX() + bullet.getWidth() >= getWidth()) {
					playSound(miss);
					bullets.remove(i);
					bulletNumbers--;
					if (level == 1) {
						if (!connection)
							newGhost();
					} else {
						if (playerID == PLAYER1) {
							bossLifeNow += bullet.getVelocity() * 100;
							if (connection)
								try {
									toServer.writeDouble(BOSS_LIFE_CHANGE);
									toServer.writeDouble(bossLifeNow);
								} catch (IOException e) {
								}
						}
					}
				}

				else if (bullet.getY() < 0) {
					playSound(miss);
					bullets.remove(i);
					bulletNumbers--;
					if (level == 1) {
						if (!connection)
							newGhost();

					} else {
						if (playerID == PLAYER1) {
							bossLifeNow += bullet.getVelocity() * 100;
							if (connection)
								try {
									toServer.writeDouble(BOSS_LIFE_CHANGE);
									toServer.writeDouble(bossLifeNow);
								} catch (IOException e) {
								}
						}
					}
				}
				bullet.update(elapsedTime);
			} catch (Exception e) {
			}
		}

		// Edge bumped check of ghost
		for (int i = 0; i < ghostNumbers; i++) {
			Sprite ghost;
			try {
				ghost = ghosts.get(i);
			} catch (Exception e) {
				break;
			}
			edgeHandling(ghost);
			ghost.update(elapsedTime);
		}

		// Edge bumped check of boss
		if (level == 2) {
			if (edgeHandling(boss) && boss.getWidth() >= 100) {
				playSound(bump);
				new Thread(new Runnable() {
					public void run() {
						for (int i = 0; i < 10; i++) {
							setBounds(locationX + (int) (Math.random() * 25),
									locationY + (int) (Math.random() * 25),
									1024, 768);
						}
					}
				}).start();
			}
			boss.update(elapsedTime);
		}

	}

	public void draw(Graphics2D g) {
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_ON);// AA

		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
				RenderingHints.VALUE_TEXT_ANTIALIAS_ON);// AA of text

		g.drawImage(background, 0, 0, null);

		// Button repaint
		getLayeredPane().paintComponents(g);

		// Draw sprite
		AffineTransform transform = new AffineTransform();
		switch (level) {
		case 1:
			for (int i = 0; i < ghostNumbers; i++) {
				Sprite ghost;
				try {
					ghost = ghosts.get(i);
				} catch (Exception e) {
					break;
				}

				// Translate the sprite
				locationSetting(transform, ghost);
				if (ghost.getVelocityX() < 0) {
					transform.scale(-1, 1);
					transform.translate(-ghost.getWidth(), 0);
				}
				for (int j = 0; j < bulletNumbers; j++) {
					Sprite bullet;
					try {
						bullet = bullets.get(j);
					} catch (Exception e) {
						break;
					}
					if (bumped(bullet, ghost)) {
						// Ghost voice
						playSound(hit);

						// Explosion
						bomb = null;
						bombTime = 0.1;
						Animation anim = new Animation();
						anim.addFrame(bombImage, 250);
						bomb = new Sprite(anim);
						bomb.setX(bullet.getX());
						bomb.setY(bullet.getY());
						ghosts.remove(i);
						ghostNumbers--;
						bullets.remove(j);
						bulletNumbers--;
						break;
					}
				}
				g.drawImage(ghost.getImage(), transform, null);
			}
			break;
		case 2:
			for (int i = 0; i < bulletNumbers; i++) {
				Sprite bullet;
				try {
					bullet = bullets.get(i);
				} catch (Exception e) {
					break;
				}
				if (bumped(bullet, boss)) {
					// Hit voice
					if (boss.getWidth() <= 100)
						playSound(hit);
					else
						playSound(armor);

					// Attack
					double v = bullet.getVelocity();
					if (boss.getWidth() <= 100) {
						if (playerID == PLAYER1)
							bossLifeNow -= (Math.pow(v, 6.5) + 6);
					} else {
						if (playerID == PLAYER1)
							bossLifeNow -= (Math.pow(v, 4) + 2);
					}

					if (connection && playerID == PLAYER1) {
						try {
							toServer.writeDouble(BOSS_LIFE_CHANGE);
							toServer.writeDouble(bossLifeNow);
						} catch (IOException e) {
						}
					}

					// Explosion
					bomb = null;
					bombTime = 0.5;
					Animation anim = new Animation();
					anim.addFrame(bombImage, 500);
					bomb = new Sprite(anim);
					bomb.setX(bullet.getX());
					bomb.setY(bullet.getY());
					bullets.remove(i);
					bulletNumbers--;
				}
			}

			locationSetting(transform, boss);
			if (boss.getVelocityX() < 0) {
				transform.scale(-1, 1);
				transform.translate(-boss.getWidth(), 0);
			}
			g.drawImage(boss.getImage(), transform, null);
			break;
		default:
			break;
		}

		if (starting) {
			if (powerup && bulletPower < 100)
				bulletPower += 4;
			if (connection)
				try {
					toServer.writeDouble(BULLET_POWER_CHANGE);
					toServer.writeDouble(bulletPower);
				} catch (IOException e1) {
				}
			for (int i = 0; i < bulletNumbers; i++) {
				Sprite bullet;
				try {
					bullet = bullets.get(i);
				} catch (Exception e) {
					break;
				}
				locationSetting(transform, bullet);
				g.drawImage(bullet.getImage(), transform, null);
			}

			// Explosion
			if (bombTime >= 0 && bomb != null) {
				locationSetting(transform, bomb);
				g.drawImage(bomb.getImage(), transform, null);
			}

			// Cannon tower
			if (connection) {
				transform.setToTranslation(cannonLocation1.x - 17, 730);
				transform.rotate(cannonAngle1, 25, 50);
				g.drawImage(cannonImage, transform, null);
				transform.setToTranslation(cannonLocation2.x - 17, 730);
				transform.rotate(cannonAngle2, 25, 50);
				g.drawImage(cannonImage, transform, null);

			} else {
				transform.setToTranslation(495, 730);
				transform.rotate(cannonAngle1, 25, 50);
				g.drawImage(cannonImage, transform, null);
			}

			// Value about player
			g.setColor(Color.red);
			g.setFont(new Font("SketchFlow Print", Font.BOLD, 20));
			g.drawString("Times : " + String.valueOf((int) timeLeft), 0, 100);
			g.drawString("Bullet : " + String.valueOf(bulletLeft), 0, 600);
			if (connection) {
				if (playerID == PLAYER1)
					bulletPower1 = bulletPower;
				else
					bulletPower2 = bulletPower;
				g.drawString("Power : " + String.valueOf(bulletPower1) + " % ",
						572, 700);
				g.drawString("Power : " + String.valueOf(bulletPower2) + " % ",
						277, 700);
			} else {
				g.drawString("Power : " + String.valueOf(bulletPower) + " % ",
						512, 700);
			}

			if (level == 1)
				g.drawString("Ghost : " + String.valueOf(ghostNumbers), 780,
						600);

			// Boss life bar
			if (level == 2) {
				g.setPaint(new GradientPaint(0, 0,
						new Color(255, 128, 128, 200), 1024, 20, new Color(255,
								0, 0, 200)));
				g.fillRect(0, 0, 1024, 20);
				g.setPaint(new GradientPaint(
						0,
						0,
						new Color(128, 255, 128, 200),
						(int) ((double) bossLifeNow / (double) bossLife * 1024D),
						20, new Color(0, 255, 0, 200)));
				g.fillRect(0, 0, (int) ((double) bossLifeNow
						/ (double) bossLife * 1024D), 20);
				g.setColor(Color.blue);
				g.drawString("Life : " + String.valueOf((int) bossLifeNow)
						+ "/" + String.valueOf((int) bossLife), 0, 16);
			}

			// Bullet speed bar
			if (connection) {
				if (playerID == PLAYER1)
					bulletPower1 = bulletPower;
				else
					bulletPower2 = bulletPower;
				g.setPaint(new GradientPaint(572, 720, new Color(255, 128, 128,
						200), 572 + 200, 720 + 20, new Color(255, 0, 0, 200)));
				g.fillRect(572, 720, 200, 20);
				g.setPaint(new GradientPaint(572, 720, new Color(128, 255, 128,
						200), 572 + bulletPower1 * 2, 720 + 20, new Color(0,
						255, 0, 200)));
				g.fillRect(572, 720, bulletPower1 * 2, 20);

				g.setPaint(new GradientPaint(277, 720, new Color(255, 128, 128,
						200), 277 + 200, 720 + 20, new Color(255, 0, 0, 200)));
				g.fillRect(277, 720, 200, 20);
				g.setPaint(new GradientPaint(277, 720, new Color(128, 255, 128,
						200), 277 + bulletPower2 * 2, 720 + 20, new Color(0,
						255, 0, 200)));
				g.fillRect(277, 720, bulletPower2 * 2, 20);

			} else {
				g.setPaint(new GradientPaint(512, 720, new Color(255, 128, 128,
						200), 512 + 200, 720 + 20, new Color(255, 0, 0, 200)));
				g.fillRect(512, 720, 200, 20);
				g.setPaint(new GradientPaint(512, 720, new Color(128, 255, 128,
						200), 512 + bulletPower * 2, 720 + 20, new Color(0,
						255, 0, 200)));
				g.fillRect(512, 720, bulletPower * 2, 20);
			}
		}
	}

	// New bullet
	public void newBullet() {
		Point location = new Point();
		if (connection) {
			if (playerID == PLAYER1) {
				location.x = cannonLocation1.x;
				location.y = cannonLocation1.y;
			} else {
				location.x = cannonLocation2.x;
				location.y = cannonLocation2.y;
			}

		} else {
			location.x = 512;
			location.y = 768;
		}
		Sprite bullet = new SpriteGenerator(-1, bulletImage).getSprite();
		bullet.setX(location.x);
		bullet.setY(location.y);
		double xLen = (now.x - location.x);
		double YLen = (location.y - now.y);
		double len = Math.sqrt(xLen * xLen + YLen * YLen);
		double xV = (bulletPower + 10) / 50D * (now.x - location.x) / len;
		double yV = -(bulletPower + 10) / 50D * (location.y - now.y) / len;
		bullet.setVelocityX(xV);
		bullet.setVelocityY(yV);
		bullets.add(bullet);
		bulletNumbers++;
		if (connection) {
			try {
				toServer.writeDouble(NEW_BULLET);
				toServer.writeDouble(xV);
				toServer.writeDouble(yV);
			} catch (IOException e) {
			}
		}
		bulletPower = 0;

		if (bulletLeft > 0)
			--bulletLeft;
	}

	// New Ghost
	public void newGhost() {
		if (playerID == PLAYER1) {
			int type = (int) (Math.random() * 6);

			// temp
			if (connection) {
				while (type == 2)
					type = (int) (Math.random() * 6);
			}

			Sprite ghost = new SpriteGenerator(type, ghostImage[type])
					.getSprite();
			double x = Math.random() * (getWidth() - ghost.getWidth());
			double y = Math.random() * (getHeight() - ghost.getHeight());
			double xV = Math.random() - 0.5D;
			double yV = Math.random() - 0.5D;
			ghost.setX(x);
			ghost.setY(y);

			// select random velocity
			ghost.setVelocityX(xV);
			ghost.setVelocityY(yV);
			if (connection)
				try {
					toServer.writeDouble(NEW_GHOST);
					toServer.writeDouble(type);
					toServer.writeDouble(x);
					toServer.writeDouble(y);
					toServer.writeDouble(xV);
					toServer.writeDouble(yV);
				} catch (IOException e) {
				}
			ghosts.add(ghost);
			ghostNumbers++;
		}
	}

	// Setting location of transform matrix
	public void locationSetting(AffineTransform transform, Sprite sprite) {
		transform.setToTranslation(sprite.getX(), sprite.getY());
	}

	// Bumped handling
	public boolean bumped(Sprite spriteA, Sprite spriteB) {
		return (Math.abs(spriteA.getX() - spriteB.getX()) <= spriteB.getWidth() && Math
				.abs(spriteA.getY() - spriteB.getY()) <= spriteB.getHeight());
	}

	// Edge handling
	public boolean edgeHandling(Sprite sprite) {
		boolean bumped = false;
		double xVel = sprite.getVelocityX();
		double yVel = sprite.getVelocityY();

		if (sprite.getX() < 0) {
			sprite.setVelocityX(Math.abs(xVel));
			bumped = true;
		} else if (sprite.getX() + sprite.getWidth() >= getWidth()) {
			sprite.setVelocityX(-Math.abs(xVel));
			bumped = true;
		}

		if (sprite.getY() < 0) {
			sprite.setVelocityY(Math.abs(yVel));
			bumped = true;
		} else if (sprite.getY() + sprite.getHeight() >= getHeight()) {
			sprite.setVelocityY(-Math.abs(yVel));
			bumped = true;
		}
		return bumped;
	}

	// Level setting
	public void setLevel(int level) {
		this.level = level;

		// Clear the map
		starting = false;
		boss = null;
		bossChangeA = false;
		bossChangeB = false;
		ghosts.clear();
		ghostNumbers = 0;
		bullets.clear();
		bulletNumbers = 0;
		bulletPower1 = 0;
		bulletPower2 = 0;
		bombTime = 0;

		switch (level) {
		case -2:// Lose
			playSound(lose);
			background = backImage[0];
			break;
		case -1:// About
			background = backImage[3];
			break;
		case 0:// Menu
			background = backImage[1];
			break;
		case 1:// Level 1
			background = backImage[2];
			bulletLeft = 20;
			timeLeft = 30;
			for (int i = 0; i < 10; i++)
				newGhost();
			starting = true;
			break;
		case 2:// Boss
			if (playerID == PLAYER1 && connection)
				try {
					toServer.writeDouble(LEVEL_CHANGE_BOSS);
				} catch (IOException e) {
				}
			playSound(evilLaugh);
			background = backImage[4];
			timeLeft = 150;
			bulletLeft = 1000;
			boss = new SpriteGenerator(99, bossImage).getSprite();
			boss.setX(512);
			boss.setY(0);
			boss.setVelocityX(1.25D);
			boss.setVelocityY(1.25D);
			bossLife = 3000;
			bossLifeNow = 3000;
			if (connection) {
				bulletLeft *= 2;
				bossLife *= 2;
				bossLifeNow *= 2;
			}
			bossChangeA = false;
			bossChangeB = false;
			starting = true;
			break;
		case 3:// Win
			if (playerID == PLAYER1 && connection)
				try {
					toServer.writeDouble(LEVEL_CHANGE_WIN);
				} catch (IOException e) {
				}
			background = backImage[5];
			break;
		default:
			break;
		}
	}

	// Image load
	public void imageLoading() {
		// Cursor
		cursorImage = new ImageIcon(getClass()
				.getResource("images/cursor0.jpg")).getImage();
		cursor = Toolkit.getDefaultToolkit().createCustomCursor(cursorImage,
				new Point(0, 0), "myCursor");

		// Background
		backImage = new Image[6];
		backImage[0] = new ImageIcon(getClass().getResource(
				"images/gameover.jpg")).getImage();
		for (int i = 1; i <= 4; i++)
			backImage[i] = new ImageIcon(getClass().getResource(
					"images/background" + i + ".jpg")).getImage();
		backImage[5] = new ImageIcon(getClass().getResource("images/win.jpg"))
				.getImage();

		// Ghost
		ghostImage = new Image[6][];
		for (int i = 0; i < 6; i++) {
			switch (i) {
			case 0:
				ghostImage[i] = new Image[8];
				for (int j = 0; j < 8; j++)
					ghostImage[i][j] = new ImageIcon(getClass().getResource(
							"images/ghost" + i + "[" + (j + 1) + "]" + ".png"))
							.getImage();
				break;
			case 1:
				ghostImage[i] = new Image[12];
				for (int j = 0; j < 12; j++)
					ghostImage[i][j] = new ImageIcon(getClass().getResource(
							"images/ghost" + i + "[" + (j + 1) + "]" + ".png"))
							.getImage();
				break;
			case 2:
				ghostImage[i] = new Image[6];
				for (int j = 0; j < 6; j++)
					ghostImage[i][j] = new ImageIcon(getClass().getResource(
							"images/ghost" + i + "[" + (j + 1) + "]" + ".png"))
							.getImage();
				break;
			case 3:
				ghostImage[i] = new Image[4];
				for (int j = 0; j < 4; j++)
					ghostImage[i][j] = new ImageIcon(getClass().getResource(
							"images/ghost" + i + "[" + (j + 1) + "]" + ".png"))
							.getImage();
				break;
			case 4:
				ghostImage[i] = new Image[2];
				for (int j = 0; j < 2; j++)
					ghostImage[i][j] = new ImageIcon(getClass().getResource(
							"images/ghost" + i + "[" + (j + 1) + "]" + ".png"))
							.getImage();
				break;
			case 5:
				ghostImage[i] = new Image[12];
				for (int j = 0; j < 12; j++)
					ghostImage[i][j] = new ImageIcon(getClass().getResource(
							"images/ghost" + i + "[" + (j + 1) + "]" + ".png"))
							.getImage();
				break;
			}
		}

		// Boss
		bossImage = new Image[30];
		for (int i = 0; i < 30; i++)
			bossImage[i] = new ImageIcon(getClass().getResource(
					"images/boss" + "[" + (i + 1) + "].png")).getImage();

		// Cannon
		cannonImage = new ImageIcon(getClass().getResource("images/cannon.png"))
				.getImage();
		// Bullet
		bulletImage = new ImageIcon(getClass().getResource("images/bullet.png"))
				.getImage();
		bombImage = new ImageIcon(getClass().getResource("images/bomb.gif"))
				.getImage();
	}

	// Sound player
	public void playSound(final SimpleSoundPlayer sound) {
		new Thread(new Runnable() {
			public void run() {
				sound.play(new ByteArrayInputStream(sound.getSamples()));
			}
		}).start();
	}

	// Sound load
	public void soundLoading() {
		shoot = new SimpleSoundPlayer("musics/shoot.wav");
		miss = new SimpleSoundPlayer("musics/miss.wav");
		bump = new SimpleSoundPlayer("musics/bump.wav");
		clickButton = new SimpleSoundPlayer("musics/clickButton.wav");
		hit = new SimpleSoundPlayer("musics/hit.wav");
		armor = new SimpleSoundPlayer("musics/bossArmor.wav");
		win = new SimpleSoundPlayer("musics/win.wav");
		lose = new SimpleSoundPlayer("musics/lose.wav");
		warning = new SimpleSoundPlayer("musics/warning.wav");
		evilLaugh = new SimpleSoundPlayer("musics/evilLaugh.wav");
	}

	// Connection
	private void connectToServer() {
		try {
			socket = new Socket(InetAddress.getByName(host), port);
			fromServer = new DataInputStream(socket.getInputStream());
			toServer = new DataOutputStream(socket.getOutputStream());
			new Thread(this).start();
		} catch (Exception e) {
			setTitle("GhostDefense (Single Player)");
			connection = false;
			playerID = PLAYER1;
			e.printStackTrace();
		}
	}

	// Initial
	public void initial() {
		imageLoading();
		soundLoading();

		// Background music
		new Thread(new Runnable() {
			public void run() {
				try {
					while (true) {
						new AdvancedPlayer(new BufferedInputStream(getClass()
								.getResourceAsStream("musics/music.mp3")))
								.play();
					}
				} catch (JavaLayerException e) {
				}
			}
		}).start();

		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setUndecorated(true);
		setVisible(true);
		setIgnoreRepaint(true);
		setIconImage(bossImage[26]);
		setCursor(cursor);

		setLevel(0);
		createBufferStrategy(2);

		// Connection
		connection = true;
		if (connection) {
			connectToServer();
		}
		animationLoop();
	}

	public static void main(String[] args) {
		GameClient windows = new GameClient();
		if (args.length < 2) {
			windows.port = 8000;
			windows.host = "localhost";
			windows.host = "127.0.0.1";
		} else {
			windows.host = args[0];
			windows.port = Integer.parseInt(args[1]);
		}

		// Remove auto repaint
		new NullRepaintManager().install();
		windows.initial();
	}

	@Override
	public void run() {
		try {
			while (true && connection) {
				int messege = (int) fromServer.readDouble();
				switch (messege) {
				case PLAYER1:
					playerID = PLAYER1;
					setTitle("GhostDefense (Player1)");
					go = false;
					waiting = true;

					// Test
					locationX = 0;
					locationY = 0;
					setBounds(locationX, locationY, 1024, 768);
					break;
				case PLAYER2:
					playerID = PLAYER2;
					go = false;
					waiting = false;
					setTitle("GhostDefense (Player2)");

					// Test
					locationX = 500;
					locationY = 0;
					setBounds(locationX, locationY, 1024, 768);
					break;
				case READY:
					waiting = false;
					break;
				case START:
					playSound(clickButton);
					if (startButton.getText() != "Back") {
						aboutButton.setVisible(false);
						exitButton.setVisible(false);
						startButton.setText("Back");
						startButton.setBounds(0, getHeight() - 40, 80, 40);
						reButton.setVisible(true);
						setLevel(1);
					} else {
						aboutButton.setVisible(true);
						exitButton.setVisible(true);
						startButton.setText("Start");
						startButton.setBounds(472, 150, 80, 40);
						reButton.setVisible(false);
						setLevel(0);
					}
					break;
				case RESTART:
					playSound(clickButton);
					setLevel(1);
					break;
				case ABOUT:
					playSound(clickButton);
					if (aboutButton.getText() != "Back") {
						startButton.setVisible(false);
						exitButton.setVisible(false);
						aboutButton.setText("Back");
						aboutButton.setBounds(0, getHeight() - 40, 80, 40);
						setLevel(-1);
					} else {
						startButton.setVisible(true);
						exitButton.setVisible(true);
						aboutButton.setText("About");
						aboutButton.setBounds(472, 190, 80, 40);
						setLevel(0);
					}
					break;
				case EXIT:
					System.exit(0);
					break;
				case NEW_BULLET:
					Sprite bullet = new SpriteGenerator(-1, bulletImage)
							.getSprite();
					bullet.setVelocityX(fromServer.readDouble());
					bullet.setVelocityY(fromServer.readDouble());
					Point location = new Point();
					if (playerID == PLAYER1) {
						bulletPower2 = 0;
						location = cannonLocation2;
					} else {
						bulletPower1 = 0;
						location = cannonLocation1;
					}
					bullet.setX(location.x);
					bullet.setY(location.y);
					bullets.add(bullet);
					bulletNumbers++;
					if (bulletLeft > 0)
						--bulletLeft;
				case NEW_GHOST:
					int type = (int) fromServer.readDouble();
					if (type > 5 || type < 0)
						break;
					Sprite ghost = new SpriteGenerator(type, ghostImage[type])
							.getSprite();
					double x = fromServer.readDouble();
					double y = fromServer.readDouble();
					double xV = fromServer.readDouble();
					double yV = fromServer.readDouble();
					ghost.setX(x);
					ghost.setY(y);
					ghost.setVelocityX(xV);
					ghost.setVelocityY(yV);
					ghosts.add(ghost);
					ghostNumbers++;
					break;
				case BULLET_POWER_CHANGE:
					double power = fromServer.readDouble();
					if (power > 100)
						break;
					if (playerID == PLAYER1)
						bulletPower2 = (int) power;
					else
						bulletPower1 = (int) power;
					break;
				case CANNON_MOVE:
					double angle = fromServer.readDouble();
					if (playerID == PLAYER1)
						cannonAngle2 = angle;
					else
						cannonAngle1 = angle;
					break;
				case BOSS_LIFE_CHANGE:
					bossLifeNow = fromServer.readDouble();
					break;
				case LEVEL_CHANGE_BOSS:
					if (this.level != 2)
						setLevel(2);
					break;
				case LEVEL_CHANGE_WIN:
					if (this.level != 3)
						setLevel(3);
					break;
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
