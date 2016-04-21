import javax.swing.JPanel;
import javax.swing.JFrame;
import javax.swing.JButton;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Clip;
import java.awt.event.KeyListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.BasicStroke;
import java.awt.geom.Rectangle2D;
import java.awt.GraphicsEnvironment;
import java.awt.GraphicsDevice;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.HashMap;
import java.util.Date;
import java.io.File;
import java.io.FileNotFoundException;
import java.lang.Thread;

class Key {
	static int LEFT = 65;
	static int RIGHT = 68;
	static int JUMP = 32;
	static int RESTART = 82;
}

class GraphicsUtils {
	public static void boxShadow(Graphics2D gfx, int x, int y, int width, int height) {
		Color c = new Color(0, 0, 0, 20);
		for (int i = 40; i >0; i -= 1) {
			gfx.setColor(c);
			gfx.fillRect(
				x - i,
				y - i,
				width + (i * 2),
				height + (i * 2)
			);
		}
	}

	public static void drawDoneMessage(Graphics2D gfx, Game.State state, JFrame window) {
		Color textColor = null;
		String str = "";
		if (state == Game.State.WON) {
			textColor = Color.GREEN;
			str = "Congratulations! You won!";
		} else if (state == Game.State.LOST) {
			textColor = Color.RED;
			str = "You lost!";
		}

		gfx.setFont(new Font("Serif", Font.PLAIN, 32));

		FontMetrics metrics = gfx.getFontMetrics();
		Rectangle2D bounds = metrics.getStringBounds(str, gfx);

		GraphicsUtils.boxShadow(
			gfx,
			(window.getWidth() / 2) - ((int)bounds.getWidth() / 2) - 5,
			window.getHeight() / 2 - (int)bounds.getHeight() - 40,
			(int)bounds.getWidth() + 10,
			(int)bounds.getHeight() + 10
		);
		gfx.setColor(textColor);
		gfx.drawString(
			str,
			(window.getWidth() / 2) - ((int)bounds.getWidth() / 2),
			(window.getHeight() / 2) - 40
		);

		gfx.setFont(new Font("Serif", Font.PLAIN, 20));
		gfx.setColor(Color.BLACK);
		gfx.drawString(
			"Press R to play again.",
			(window.getWidth() / 2) - ((int)bounds.getWidth() / 2),
			(window.getHeight() / 2) + 60
		);

		if (Coin.totalCoins != 0) {
			gfx.drawString(
				"you got "+(Coin.totalCoins - Coin.curCoins)+"/"+(Coin.totalCoins)+" coins.",
				(window.getWidth() / 2) - ((int)bounds.getWidth() / 2),
				(window.getHeight() / 2) + 90
			);
		}
	}
}

enum Side {
	LEFT,
	RIGHT,
	TOP,
	BOTTOM
}

class Vec2 {
	public double x;
	public double y;

	Vec2(double x, double y) {
		this.x = x;
		this.y = y;
	}

	public Vec2 clone() {
		return new Vec2(x, y);
	}

	public Vec2 scale(double n) {
		x *= n;
		y *= n;
		return this;
	}

	public Vec2 set(Vec2 vec) {
		return set(vec.x, vec.y);
	}
	public Vec2 set(double ax, double ay) {
		x = ax;
		y = ay;
		return this;
	}

	public Vec2 add(Vec2 vec) {
		return add(vec.x, vec.y);
	}
	public Vec2 add(double ax, double ay) {
		x += ax;
		y += ay;
		return this;
	}

	public String identify() {
		return "Vec2, x: "+x+", y: "+y;
	}
}

class Entity {
	protected double mass;
	private Vec2 force;
	private boolean shouldBounceX;
	private boolean shouldBounceY;
	private double bounceForceX;
	private Vec2 bounceForce;
	public Vec2 vel;
	public Vec2 pos;
	public int width;
	public int height;
	public int lastDTime = 0;
	public boolean dead = false;;
	public Game game;

	Entity(int x, int y, int w, int h) {
		pos = new Vec2(x, y);

		width = w;
		height = h;
		mass = width * height;

		bounceForce = new Vec2(0, 0);
		vel = new Vec2(0, 0);
		force = new Vec2(0, 0);
	}

	public void bounceX(double n) {
		bounceX();
		bounceForce.x += n;
	}
	public void bounceX() {
		shouldBounceX = true;
	}

	public void bounceY(double n) {
		bounceY();
		bounceForce.y += n;
	}
	public void bounceY() {
		shouldBounceY = true;
	}

	public Vec2 getCenter() {
		return new Vec2(pos.x + (width * 0.5), pos.y + (height * 0.5));
	}

	//Apply force; move for an arbitrary number of frames (e.g movement)
	public void force(Vec2 vec) {
		force.add(vec);
	}

	//Apply impulse; set velocity for one frame (e.g jumping)
	public void impulse(Vec2 vec) {
		vel.add(vec);
	}

	//Move, accounting for variable frame rate
	public void move(int deltaTime) {
		if (shouldBounceX)
			vel.x = -vel.x + bounceForce.x;
		if (shouldBounceY)
			vel.y = -vel.y + bounceForce.y;

		shouldBounceX = false;
		shouldBounceY = false;
		bounceForce.set(0, 0);

		lastDTime = deltaTime;
		vel.add(force.scale(1/mass).scale(deltaTime));
		pos.add(vel.clone().scale(deltaTime));
		force.set(0, 0);
	}

	public boolean shouldUpdate() {
		return !(
			-game.camera.x > pos.x + width ||
			(-game.camera.x + game.window.getWidth()) + 100 < pos.x
		);
	}

	public void init() {};

	public void update() {};

	public void draw(Graphics2D gfx) {};

	public void die() {
		dead = true;
	}

	public Side intersects(Entity e) {
		if (!(
			pos.x < e.pos.x + e.width && pos.x + width > e.pos.x &&
			pos.y < e.pos.y + e.height && pos.y + height > e.pos.y
		 )) {
			return null;
		 }

		if (pos.y + (height / 2) <= e.pos.y + (vel.y + e.vel.y) * lastDTime) {
			return Side.TOP;
		} else if (pos.y >= e.pos.y + e.height - (-vel.y + e.vel.y) * lastDTime) {
			return Side.BOTTOM;
		} else if (pos.x + width < e.pos.x + (e.width / 2)) {
			return Side.LEFT;
		} else {
			return Side.RIGHT;
		}
	}

	public String identify() {
		return this.getClass().getName()+" at "+pos.x+", "+pos.y;
	}
}

class Victory extends Entity {
	Victory(int x, int y) {
		super(x, y, 30, 30);
	}

	@Override public void update() {
		if (intersects(game.player) != null)
			game.win();
	}

	@Override public void draw(Graphics2D gfx) {
		gfx.setColor(new Color(20, 20, 255));
		gfx.fillRect(0, 0, width, height);
		gfx.setColor(new Color(10, 10, 155));
		gfx.drawRect(0, 0, width, height);
	}
}

class Coin extends Entity {
	public static int totalCoins = 0;
	public static int curCoins = 0;

	Coin(int x, int y) {
		super(x, y, 10, 10);
		totalCoins += 1;
		curCoins += 1;
	}

	//We always want to update coins, as we always want to show
	//how many coins are left in the upper left
	@Override public boolean shouldUpdate() {
		return true;
	}

	@Override public void update() {
		if (intersects(game.player) != null) {
			curCoins -= 1;
			die();
			if (totalCoins <= 0)
				game.win();
		}
	}

	@Override public void draw(Graphics2D gfx) {
		gfx.setColor(new Color(255, 255, 20));
		gfx.fillOval(0, 0, width, height);
		gfx.setColor(new Color(155, 155, 10));
		gfx.setStroke(new BasicStroke(3));
		gfx.drawOval(0, 0, width, height);

		game.displayText(this, "Coins left: "+curCoins);
	}

	public static void reset() {
		totalCoins = 0;
		curCoins = 0;
	}
}

class Platform extends Entity {
	Platform(int x, int y, int w, int h) {
		super(x, y, w, h);
	}

	@Override public void draw(Graphics2D gfx) {
		gfx.drawRect(0, 0, width, height);
	}
}

class Player extends Entity {
	private final double moveForce = 0.4;
	private final double gravityForce = 0.6;
	private final double jumpForce = 0.6;
	private final double airResistance = 0.5;
	private final double friction = 2;
	private boolean onGround = false;

	Player(int x, int y) {
		super(x, y, 20, 20);
	}

	@Override public void init() {
		game.camera.x = -pos.x + (game.window.getWidth() / 2);
		game.camera.y = -pos.y + (game.window.getHeight() / 2);
	}

	@Override public void update() {
		//Movement
		if (game.isKeyPressed(Key.LEFT))
			force(new Vec2(-moveForce, 0));
		if (game.isKeyPressed(Key.RIGHT))
			force(new Vec2(moveForce, 0));

		//Collide with platforms
		onGround = false;
		for (Entity e: game.entities) {
			if (e == this)
				continue;

			Side side = intersects(e);
			if (e instanceof Platform && side != null) {
				if (side == Side.TOP) {
					onGround = true;

					if (vel.y > 0)
						vel.y = 0;

					pos.y = e.pos.y - height;
				} else if (side == Side.LEFT) {
					bounceX();
					pos.x = e.pos.x - width;;
				} else if (side == Side.RIGHT) {
					bounceX();
					pos.x = e.pos.x + e.width;
				} else if (side == Side.BOTTOM) {
					bounceY();
				}
			}
		}

		//Jump
		if (onGround && game.isKeyPressed(Key.JUMP))
			impulse(new Vec2(0, -jumpForce));

		//Gravity
		if (!onGround)
			force(new Vec2(0, gravityForce));

		//"Friction" (kind of) and air resistance
		if (onGround)
			force(new Vec2(-vel.x * friction, 0));
		else
			force(new Vec2(-vel.x * airResistance, 0));

		//Update camera
		Vec2 dist = new Vec2(
			-pos.x + (game.window.getWidth() / 2) - game.camera.x,
			-pos.y + (game.window.getHeight() / 2) - game.camera.y
		);

		game.camera.set(dist.scale(0.1).add(game.camera));

		//Die if we fall through the ground
		if (pos.y > 300)
			die();
	}

	@Override public void draw(Graphics2D gfx) {
		gfx.setColor(new Color(20, 255, 20));
		gfx.fillRect(0, 0, width, height);
		gfx.setColor(new Color(10, 155, 10));
		gfx.drawRect(0, 0, width, height);
	}

	@Override public void die() {
		super.die();
		game.lose();
	}
}

class Enemy extends Entity {
	protected final double moveForce = 0.1;
	protected final double gravityForce = 0.4;
	protected final double jumpForce = 0.3;
	protected final double airResistance = 0.5;
	protected final double friction = 2;
	protected double resurrectTimeout = 0;
	protected boolean onGround = false;
	protected boolean collidedTop;
	protected boolean collidedBottom;
	protected boolean collidedLeft;
	protected boolean collidedRight;
	private int timesJumped = 0;
	protected int maxResurrectTimeout = 5000;

	Enemy(int x, int y, int w, int h) {
		super(x, y, w, h);
	}

	@Override public void update() {

		//Check if we're on ground
		onGround = false;
		collidedTop = false;
		collidedLeft = false;
		collidedRight = false;
		collidedBottom = false;
		for (Entity e: game.entities) {
			if (e == this)
				continue;

			Side side = intersects(e);

			//Collide with platforms
			if (e instanceof Platform && side != null) {
					collidedTop = true;
				if (side == Side.TOP) {
					collidedTop = true;
					onGround = true;

					if (vel.y > 0)
						vel.y = 0;

					pos.y = e.pos.y - height;
				} else if (side == Side.LEFT) {
					collidedLeft = true;
					vel.x = -vel.x;
					pos.x = e.pos.x - width;
				} else if (side == Side.RIGHT) {
					collidedRight = true;
					vel.x = -vel.x;
					pos.x = e.pos.x + e.width;
				} else if (side == Side.BOTTOM) {
					collidedBottom = true;
					bounceY();
				}
			}
		}

		//Die when a player hits the enemy on top,
		//kill the player if it's hit on the sides or bottom
		Side side = intersects(game.player);
		if (side != null) {
			if (side == Side.BOTTOM) {
				game.player.bounceY(vel.y);
				if (maxResurrectTimeout > 0 && timesJumped < 1) {
					resurrectTimeout = maxResurrectTimeout / lastDTime;
					onGround = false;
					timesJumped += 1;
					height = height / 2;
				}
				else {
					die();
				}
			} else {
				if (timesJumped == 0) {
					game.player.die();
				} else {
					vel.x = game.player.vel.x * 2;
				}
			}
		}

		//Gravity
		if (!onGround)
			force(new Vec2(0, gravityForce));

		//Friction and air resistance
		if (onGround)
			force(new Vec2(-vel.x * friction, 0));
		else
			force(new Vec2(-vel.x * airResistance, 0));

		if (resurrectTimeout > 0) {
			resurrectTimeout -= 1;
			if (resurrectTimeout == 0) {
				height *= timesJumped + 1;
				timesJumped = 0;
			}
		}
	}

	@Override public void draw(Graphics2D gfx) {
		gfx.setColor(new Color(255, 20, 20));
		gfx.fillRect(0, 0, width, height);
		gfx.setColor(new Color(155, 10, 10));
		gfx.drawRect(0, 0, width, height);
	}
}

class EnemyWalker extends Enemy {
	private int direction = -1;

	EnemyWalker(int x, int y, int dir) {
		super(x, y, 30, 30);
		direction = dir;
	}

	@Override public void update() {
		super.update();

		if (resurrectTimeout > 0)
			return;

		if (collidedLeft) {
			direction = -1;
		} else if (collidedRight) {
			direction = 1;
		}

		force(new Vec2(moveForce * direction, 0));
	}
}

class EnemyJumper extends Enemy {
	EnemyJumper(int x, int y) {
		super(x, y, 20, 30);
	}

	@Override public void update() {
		super.update();

		if (resurrectTimeout > 0)
			return;

		if (!collidedRight && game.player.pos.x + game.player.width <= pos.x)
			force(new Vec2(-moveForce, 0));
		else if (!collidedLeft && game.player.pos.x >= pos.x + width)
			force(new Vec2(moveForce, 0));

		if (onGround && (collidedRight || collidedLeft))
			impulse(new Vec2(0, -jumpForce));

		if (collidedLeft || collidedRight)
			vel.x = 0;
	}
}

class EnemyFlyer extends Enemy {
	private final double gravityForce = 0;
	private double flyForce = 1;
	private int targetY;

	EnemyFlyer(int x, int y) {
		super(x, y, 30, 15);
		targetY = y;
		impulse(new Vec2(0, -0.5));
	}

	@Override public void update() {
		super.update();

		if (resurrectTimeout > 0)
			return;

		if (pos.y > targetY) {
			force(new Vec2(0, -flyForce));
		} else {
			force(new Vec2(0, flyForce));
		}
	}
}

@SuppressWarnings("serial")
class Game extends JPanel implements KeyListener {
	private static Clip playAudio(String filename) throws Exception {
		File file = new File(filename);
		AudioInputStream stream = AudioSystem.getAudioInputStream(file);
		AudioFormat format = stream.getFormat();
		DataLine.Info info = new DataLine.Info(Clip.class, format);
		Clip clip = (Clip)AudioSystem.getLine(info);

		clip.open(stream);
		clip.start();

		return clip;
	}

	public enum State {
		RUNNING,
		WON,
		LOST
	}
	private State state;

	private ArrayList<Integer> pressedKeys;
	private final int targetDTime = 16;
	private HashMap<String, String> entityMessages;
	private File level;
	public ArrayList<Entity> entities;
	public Vec2 camera;
	public Entity player;
	public Clip music;
	public boolean gameEnded;
	public JFrame window;

	Game(JFrame window) {
		state = State.RUNNING;
		pressedKeys = new ArrayList<Integer>();
		entityMessages = new HashMap<String, String>();
		entities = new ArrayList<Entity>();
		camera = new Vec2(0, 0);
		player = null;
		music = null;
		gameEnded = false;

		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		GraphicsDevice gd = ge.getDefaultScreenDevice();
		gd.setFullScreenWindow(window);

		this.window = window;
		window.addKeyListener(this);
		window.add(this);
	}

	public void addEntity(Entity e) {
		e.game = this;
		entities.add(e);
		e.init();
		if (e instanceof Player)
			player = e;
	}

	public void run() throws InterruptedException {
		while (true) {
			if (state != State.RUNNING) {
				Thread.sleep(100);
				continue;
			}

			long startTime = new Date().getTime();

			for (int i = 0; i < entities.size(); ++i) {
				Entity e = entities.get(i);

				if (e.dead) {
					entities.remove(i);
					continue;
				}

				if (!e.shouldUpdate())
					continue;

				e.move(targetDTime);
			}

			for (int i = 0; i < entities.size(); ++i) {
				Entity e = entities.get(i);

				if (!e.shouldUpdate())
					continue;

				e.update();
			}

			paintComponent((Graphics2D)getGraphics());
			window.revalidate();
			window.repaint();

			int sleepTime = targetDTime - (int)(new Date().getTime() - startTime);
			if (sleepTime > 0)
				Thread.sleep(sleepTime);

			//Reset entity messages
			entityMessages = new HashMap<String, String>();
		}
	}

	@Override public void paintComponent(Graphics g) {
		Graphics2D gfx = (Graphics2D)g;

		resetGfx(gfx);
		if (state == State.RUNNING) {
			gfx.translate(camera.x, camera.y);
			for (int i = 0; i < entities.size(); ++i) {
				Entity e = entities.get(i);

				if (!e.shouldUpdate())
					continue;

				gfx.translate(e.pos.x, e.pos.y);
				e.draw(gfx);
				resetGfx(gfx);
				gfx.translate(-e.pos.x, -e.pos.y);
			}
			gfx.translate(-camera.x, -camera.y);

			int i = 0;
			for (String s: entityMessages.values()) {
				gfx.drawString(s, 10, 20 * (i + 1));
				i += 1;
			}
		} else {
			GraphicsUtils.drawDoneMessage(gfx, state, window);
		}
	}

	public void win() {
		System.out.println("Congratulations! You win!");
		state = State.WON;
		window.repaint();
		gameEnded = true;
	}

	public void lose() {
		System.out.println("You lost!");
		state = State.LOST;
		window.repaint();
		gameEnded = true;
	}

	public void displayText(Entity e, String str) {
		entityMessages.put(e.getClass().getName(), str);
	}

	public void loadLevel(File file) throws Exception {
		System.out.println("Loading level...");
		Coin.reset();
		level = file;
		Scanner s = new Scanner(file);
		int lineNum = 0;

		try {
			while (s.hasNextLine()) {
				lineNum += 1;

				String line = s.nextLine();

				String[] tokens = line.split("\\s+");
				String name = tokens[0];

				//If the line starts with #, it's a comment
				if (name.equals("#"))
					continue;

				//Play audio if the line starts with 'audio'
				if (name.equals("audio")) {
					if (music == null)
						music = playAudio(line.replace("audio", "").trim());

					continue;
				}

				//Ignore empty lines
				if (name.equals(""))
					continue;

				int[] args = new int[tokens.length - 1];

				for (int i = 0; i < args.length; ++i) {
					args[i] = Integer.parseInt(tokens[i + 1]);
				}

				switch (name) {
				case "player":
					addEntity(new Player(args[0], args[1]));
					break;
				case "platform":
					addEntity(new Platform(args[0], args[1], args[2], args[3]));
					break;
				case "enemyWalker":
					addEntity(new EnemyWalker(args[0], args[1], args[2]));
					break;
				case "enemyJumper":
					addEntity(new EnemyJumper(args[0], args[1]));
					break;
				case "enemyFlyer":
					addEntity(new EnemyFlyer(args[0], args[1]));
					break;
				case "victory":
					addEntity(new Victory(args[0], args[1]));
					break;
				case "coin":
					addEntity(new Coin(args[0], args[1]));
					break;
				default:
					System.out.println("Unknown entity "+name+" on line "+lineNum);
				}
			}
		} catch (ArrayIndexOutOfBoundsException e) {
			System.out.println("Too few arguments on line "+lineNum);
		}

		System.out.println("Done.");
	}

	public void resetGfx(Graphics2D gfx) {
		gfx.setStroke(new BasicStroke(1));
		gfx.setColor(new Color(0, 0, 0));
		gfx.setFont(new Font("Serif", Font.PLAIN, 13));
	}

	public boolean isKeyPressed(int c) {
		return (pressedKeys.indexOf(c) != -1);
	}

	@Override public void keyPressed(KeyEvent e) {
		if (pressedKeys.indexOf(e.getKeyCode()) == -1)
			pressedKeys.add(e.getKeyCode());

		if (gameEnded && isKeyPressed(Key.RESTART)) {
			gameEnded = false;
			System.out.println("restarting");
			state = State.RUNNING;
			entities.clear();
			window.repaint();
			try {
				loadLevel(level);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	}
	@Override public void keyReleased(KeyEvent e) {
		int i = pressedKeys.indexOf(e.getKeyCode());

		if (i != -1)
			pressedKeys.remove(i);
	}

	@Override public void keyTyped(KeyEvent e) {}
}


class MinOppgave5 {
	private static JFrame window;

	private static void start(File file) throws Exception {
		window = new JFrame("Game");
		window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		window.setSize(1920, 1080);
		window.setExtendedState(JFrame.MAXIMIZED_BOTH);
		window.setUndecorated(true);
		window.setVisible(true);

		Game game = new Game(window);
		game.loadLevel(file);
		game.run();
	}

	public static void main(String[] args) throws Exception {
		Scanner in = new Scanner(System.in);

		File level;

		if (args.length != 1) {
			System.out.println("Usage: MinOppgave5 <level>");
			System.out.println("Available levels:");
			for (File f: new File(".").listFiles()) {
				String name = f.toPath().getFileName().toString();
				if (!name.endsWith(".level"))
					continue;

				System.out.println("* "+name);
			}

			System.exit(1);
			return;
		} else {
			level = new File(args[0]);
			start(level);
		}

		System.out.println(" ---------------- ");
		System.out.println("| Controls:      |");
		System.out.println("| A: Move Left   |");
		System.out.println("| D: Move Right  |");
		System.out.println("| Space: Jump    |");
		System.out.println(" ---------------- ");
		System.out.println("Press Enter to start.");
		in.nextLine();
	}
}
