package com.gamesbutnogames;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.KeyboardFocusManager;
import java.awt.KeyEventDispatcher;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.ui.PluginPanel;

/** A small, abstract flight game contained entirely in the plugin panel. */
final class TunnelFlightPanel extends PluginPanel
{
	private static final int WORLD_WIDTH = 288;
	private static final int WORLD_HEIGHT = 420;
	private static final int EDGE = 18;
	private static final int PLAYER_X = 67;
	private static final int PLAYER_SIZE = 13;
	private static final int BLOCK_WIDTH = 34;
	private static final int GAP = 122;
	private static final int HARD_GAP = 96;
	private static final int BLOCK_SPACING = 166;
	private static final int HARD_BLOCK_SPACING = 112;
	private static final Color FIELD = new Color(74, 76, 79);
	private static final Color BLOCK = new Color(194, 196, 198);
	private final ConfigManager configManager;
	private final JLabel scoreLabel = new JLabel("Score: 0   Best: 0", SwingConstants.CENTER);
	private final JLabel messageLabel = new JLabel("Press start, space, or click to play", SwingConstants.CENTER);
	private final JButton startButton = new JButton("Start");
	private final JButton difficultyButton = new JButton("Difficulty: Normal");
	private final JComboBox<String> triangleColorBox = new JComboBox<>(new String[]{"Red", "Yellow", "Green", "Blue", "Orange"});
	private final Arena arena = new Arena();
	private final Timer timer = new Timer(20, event -> tick());
	private boolean running;
	private boolean lost;
	private boolean spaceDown;
	private boolean newBestThisRun;
	private boolean resumeAfterHide;
	private final KeyEventDispatcher keyDispatcher = event ->
	{
		if (event.getID() == KeyEvent.KEY_RELEASED && event.getKeyCode() == KeyEvent.VK_SPACE)
		{
			spaceDown = false;
			return hasPanelFocus();
		}
		if (event.getID() == KeyEvent.KEY_PRESSED && event.getKeyCode() == KeyEvent.VK_SPACE && hasPanelFocus())
		{
			if (!spaceDown)
			{
				spaceDown = true;
				if (!running && !lost) startGame();
				else if (running) flap();
				else restartGame();
			}
			return true;
		}
		return false;
	};
	private double y;
	private double velocity;
	private int score;
	private int best;
	private int hardBest;
	private boolean hardMode;
	private Color triangleColor;
	private int[] gaps = new int[3];
	private int[] xs = new int[3];

	TunnelFlightPanel(GamesButNoGamesConfig config, ConfigManager configManager)
	{
		this.configManager = configManager;
		best = config.tunnelFlightHighScore();
		hardBest = config.tunnelFlightHardHighScore();
		triangleColor = colorFor(config.tunnelFlightTriangleColor());
		triangleColorBox.setSelectedItem(colorNameFor(triangleColor));
		setLayout(new BorderLayout(0, 7));
		setBorder(BorderFactory.createEmptyBorder(12, 8, 12, 8));
		JLabel title = new JLabel("TUNNEL FLIGHT", SwingConstants.CENTER);
		title.setFont(title.getFont().deriveFont(Font.BOLD, 16f));
		scoreLabel.setForeground(Color.WHITE);
		messageLabel.setForeground(new Color(210, 213, 216));
		JPanel header = new JPanel(new BorderLayout(0, 4));
		header.setOpaque(false);
		header.add(title, BorderLayout.NORTH);
		header.add(scoreLabel, BorderLayout.CENTER);
		header.add(messageLabel, BorderLayout.SOUTH);
		add(header, BorderLayout.NORTH);
		arena.setPreferredSize(new Dimension(WORLD_WIDTH, WORLD_HEIGHT));
		arena.addMouseListener(new MouseAdapter()
		{
			@Override public void mousePressed(MouseEvent event)
			{
				requestKeyboardFocus();
				if (!running && !lost) startGame(); else if (running) flap(); else restartGame();
			}
		});
		add(arena, BorderLayout.CENTER);
		startButton.addActionListener(event ->
		{
			if (lost) restartGame(); else if (running) pauseGame(); else startGame();
		});
		JPanel controls = new JPanel(new GridLayout(3, 1, 0, 5));
		controls.setOpaque(false);
		difficultyButton.addActionListener(event -> toggleDifficulty());
		triangleColorBox.addActionListener(event ->
		{
			String colorName = (String) triangleColorBox.getSelectedItem();
			triangleColor = colorFor(colorName);
			configManager.setConfiguration("games-but-no-games", "tunnelFlightTriangleColor", colorName);
			arena.repaint();
		});
		controls.add(triangleColorBox);
		controls.add(difficultyButton);
		controls.add(startButton);
		add(controls, BorderLayout.SOUTH);
		resetWorld();
		KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(keyDispatcher);
		updateLabels();
	}

	private void resetWorld()
	{
		y = WORLD_HEIGHT / 2.0;
		velocity = 0;
		score = 0;
		newBestThisRun = false;
		for (int i = 0; i < xs.length; i++)
		{
			xs[i] = WORLD_WIDTH + 36 + i * blockSpacing();
			gaps[i] = 92 + (i * 67 % 176);
		}
		arena.repaint();
	}

	private void startGame()
	{
		running = true;
		lost = false;
		timer.start();
		updateLabels();
		arena.requestFocusInWindow();
	}

	private void pauseGame()
	{
		running = false;
		timer.stop();
		updateLabels();
	}

	private void restartGame()
	{
		timer.stop();
		resetWorld();
		lost = false;
		running = true;
		velocity = -3.4;
		timer.start();
		updateLabels();
	}

	private void flap()
	{
		velocity = -3.8;
	}

	private int blockSpacing()
	{
		return hardMode ? HARD_BLOCK_SPACING : BLOCK_SPACING;
	}

	private int gapSize()
	{
		return hardMode ? HARD_GAP : GAP;
	}

	private int currentBest()
	{
		return hardMode ? hardBest : best;
	}

	private static Color colorFor(String name)
	{
		if ("Red".equals(name)) return new Color(225, 74, 68);
		if ("Green".equals(name)) return new Color(89, 202, 111);
		if ("Blue".equals(name)) return new Color(76, 151, 235);
		if ("Orange".equals(name)) return new Color(242, 145, 59);
		return new Color(241, 193, 86);
	}

	private static String colorNameFor(Color color)
	{
		if (color.equals(colorFor("Red"))) return "Red";
		if (color.equals(colorFor("Green"))) return "Green";
		if (color.equals(colorFor("Blue"))) return "Blue";
		if (color.equals(colorFor("Orange"))) return "Orange";
		return "Yellow";
	}

	private void toggleDifficulty()
	{
		timer.stop();
		running = false;
		lost = false;
		hardMode = !hardMode;
		difficultyButton.setText(hardMode ? "Difficulty: Hard" : "Difficulty: Normal");
		resetWorld();
		updateLabels();
	}

	private void tick()
	{
		velocity = Math.min(4.8, velocity + 0.23);
		y += velocity;
		for (int i = 0; i < xs.length; i++)
		{
			int previousX = xs[i];
			xs[i] -= 2;
			if (xs[i] + BLOCK_WIDTH < 0)
			{
				int farthestX = xs[0];
				for (int x : xs)
				{
					farthestX = Math.max(farthestX, x);
				}
				xs[i] = farthestX + blockSpacing();
				int maxGapTop = WORLD_HEIGHT - EDGE - gapSize() - 52;
				gaps[i] = 52 + (int) (Math.random() * (maxGapTop - 51));
			}
			if (previousX + BLOCK_WIDTH >= PLAYER_X && xs[i] + BLOCK_WIDTH < PLAYER_X) score++;
			if (PLAYER_X + PLAYER_SIZE > xs[i] && PLAYER_X - PLAYER_SIZE < xs[i] + BLOCK_WIDTH
				&& (y - PLAYER_SIZE < gaps[i] || y + PLAYER_SIZE > gaps[i] + gapSize()))
			{
				endGame();
				return;
			}
		}
		if (y - PLAYER_SIZE <= EDGE || y + PLAYER_SIZE >= WORLD_HEIGHT - EDGE) endGame();
		updateLabels();
		arena.repaint();
	}

	private void endGame()
	{
		timer.stop();
		running = false;
		lost = true;
		if (score > currentBest())
		{
			newBestThisRun = true;
			if (hardMode)
			{
				hardBest = score;
				configManager.setConfiguration("games-but-no-games", "tunnelFlightHardHighScore", hardBest);
			}
			else
			{
				best = score;
				configManager.setConfiguration("games-but-no-games", "tunnelFlightHighScore", best);
			}
		}
		updateLabels();
	}

	private void updateLabels()
	{
		scoreLabel.setText("Score: " + score + "   Best: " + currentBest());
		messageLabel.setText(lost
			? newBestThisRun ? "New best: " + score + "! · Space to restart" : "Flight ended — press start or space"
			: running ? "Click/Space: rise · Button: pause" : "Start, Space, or click to play");
		startButton.setText(lost ? "Restart" : running ? "Pause" : "Start");
		arena.repaint();
	}

	boolean hasPanelFocus()
	{
		return isShowing() && SwingUtilities.isDescendingFrom(KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner(), this);
	}

	void requestKeyboardFocus()
	{
		arena.requestFocusInWindow();
	}

	void onGameHidden()
	{
		resumeAfterHide = timer.isRunning();
		if (resumeAfterHide)
		{
			pauseGame();
		}
	}

	void onGameShown()
	{
		if (resumeAfterHide && !lost)
		{
			resumeAfterHide = false;
			startGame();
		}
	}

	void stop()
	{
		timer.stop();
		KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(keyDispatcher);
	}

	private final class Arena extends JPanel
	{
		Arena() { setFocusable(true); setBackground(FIELD); }

		@Override protected void paintComponent(Graphics graphics)
		{
			super.paintComponent(graphics);
			Graphics2D g = (Graphics2D) graphics.create();
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			double scale = Math.min(getWidth() / (double) WORLD_WIDTH, getHeight() / (double) WORLD_HEIGHT);
			int ox = (int) ((getWidth() - WORLD_WIDTH * scale) / 2);
			int oy = (int) ((getHeight() - WORLD_HEIGHT * scale) / 2);
			g.translate(ox, oy);
			g.scale(scale, scale);
			g.setColor(FIELD);
			g.fillRect(0, 0, WORLD_WIDTH, WORLD_HEIGHT);
			g.setColor(Color.BLACK);
			g.fillRect(0, 0, WORLD_WIDTH, EDGE);
			g.fillRect(0, WORLD_HEIGHT - EDGE, WORLD_WIDTH, EDGE);
			g.setColor(BLOCK);
			for (int i = 0; i < xs.length; i++)
			{
				g.fillRect(xs[i], EDGE, BLOCK_WIDTH, gaps[i] - EDGE);
				g.fillRect(xs[i], gaps[i] + gapSize(), BLOCK_WIDTH, WORLD_HEIGHT - EDGE - gaps[i] - gapSize());
				g.setColor(new Color(75, 77, 80));
				g.drawRect(xs[i], EDGE, BLOCK_WIDTH, gaps[i] - EDGE);
				g.drawRect(xs[i], gaps[i] + gapSize(), BLOCK_WIDTH, WORLD_HEIGHT - EDGE - gaps[i] - gapSize());
				g.setColor(BLOCK);
			}
			g.setColor(triangleColor);
			double angle = Math.max(-0.58, Math.min(0.58, velocity * 0.12));
			Polygon triangle = new Polygon(new int[]{PLAYER_X + PLAYER_SIZE, PLAYER_X - PLAYER_SIZE, PLAYER_X - PLAYER_SIZE},
				new int[]{(int) y, (int) y - PLAYER_SIZE, (int) y + PLAYER_SIZE}, 3);
			g.rotate(angle, PLAYER_X, y);
			g.fillPolygon(triangle);
			g.setColor(new Color(64, 53, 37));
			g.drawPolygon(triangle);
			g.dispose();
		}
	}
}
