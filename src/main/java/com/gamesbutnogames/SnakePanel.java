package com.gamesbutnogames;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.awt.event.ActionEvent;
import java.awt.KeyboardFocusManager;
import java.awt.KeyEventDispatcher;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Random;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.ui.PluginPanel;

/** A standalone Snake game that only handles input while its RuneLite panel is focused. */
final class SnakePanel extends PluginPanel
{
	private static final int LARGE_GRID_SIZE = 18;
	private static final int MEDIUM_GRID_SIZE = 14;
	private static final int SMALL_GRID_SIZE = 10;
	private static final int CELL_SIZE = 18;
	private static final int TICK_MS = 145;
	private static final Color BACKGROUND = new Color(25, 29, 34);
	private static final Color SNAKE = new Color(111, 205, 127);
	private static final Color FOOD = new Color(235, 102, 91);

	private final Deque<Point> snake = new ArrayDeque<>();
	private final Deque<Direction> turnQueue = new ArrayDeque<>();
	private final GamesButNoGamesConfig config;
	private final ConfigManager configManager;
	private final Random random = new Random();
	private final JLabel scoreLabel = new JLabel("Score: 0", SwingConstants.CENTER);
	private final JLabel messageLabel = new JLabel("", SwingConstants.CENTER);
	private final Board board = new Board();
	private final Timer timer = new Timer(TICK_MS, event -> tick());
	private final JButton sizeButton = new JButton("Board: Large (18×18)");
	private final JButton startPauseButton = new JButton("Start");
	private Point food;
	private Direction direction = Direction.RIGHT;
	private int gridSize = LARGE_GRID_SIZE;
	private int score;
	private int bestScore;
	private boolean gameOver;
	private boolean lost;
	private boolean spaceDown;
	private boolean newBestThisRun;
	private boolean resumeAfterHide;
	private final KeyEventDispatcher arrowKeyDispatcher = event ->
	{
		if (event.getID() == KeyEvent.KEY_RELEASED && event.getKeyCode() == KeyEvent.VK_SPACE)
		{
			spaceDown = false;
			return hasPanelFocus();
		}
		if (event.getID() != KeyEvent.KEY_PRESSED || !hasPanelFocus())
		{
			return false;
		}
		if (event.getKeyCode() == KeyEvent.VK_SPACE)
		{
			if (!spaceDown)
			{
				spaceDown = true;
				toggleStartPause();
			}
			return true;
		}
		Direction requested;
		switch (event.getKeyCode())
		{
			case KeyEvent.VK_UP: requested = Direction.UP; break;
			case KeyEvent.VK_DOWN: requested = Direction.DOWN; break;
			case KeyEvent.VK_LEFT: requested = Direction.LEFT; break;
			case KeyEvent.VK_RIGHT: requested = Direction.RIGHT; break;
			default: return false;
		}
		requestTurn(requested);
		return true;
	};

	SnakePanel(GamesButNoGamesConfig config, ConfigManager configManager)
	{
		this.config = config;
		this.configManager = configManager;
		bestScore = config.snakeHighScore();
		setLayout(new BorderLayout(0, 8));
		setBorder(BorderFactory.createEmptyBorder(12, 8, 12, 8));

		JLabel title = new JLabel("SNAKE", SwingConstants.CENTER);
		title.setFont(title.getFont().deriveFont(Font.BOLD, 18f));
		scoreLabel.setForeground(Color.WHITE);
		messageLabel.setForeground(new Color(195, 201, 208));
		JPanel header = new JPanel(new BorderLayout(0, 5));
		header.setOpaque(false);
		header.add(title, BorderLayout.NORTH);
		header.add(scoreLabel, BorderLayout.CENTER);
		header.add(messageLabel, BorderLayout.SOUTH);
		add(header, BorderLayout.NORTH);
		add(board, BorderLayout.CENTER);

		JPanel controls = new JPanel(new BorderLayout(0, 4));
		sizeButton.addActionListener(event -> cycleBoardSize());
		controls.add(sizeButton, BorderLayout.NORTH);
		startPauseButton.addActionListener(event -> toggleStartPause());
		controls.add(startPauseButton, BorderLayout.SOUTH);
		add(controls, BorderLayout.SOUTH);
		bindKey(KeyEvent.VK_W, "upW", Direction.UP);
		bindKey(KeyEvent.VK_S, "downS", Direction.DOWN);
		bindKey(KeyEvent.VK_A, "leftA", Direction.LEFT);
		bindKey(KeyEvent.VK_D, "rightD", Direction.RIGHT);
		KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(arrowKeyDispatcher);
		resetGame();
	}

	private void bindKey(int keyCode, String actionName, Direction requested)
	{
		board.getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(keyCode, 0), actionName);
		board.getActionMap().put(actionName, new AbstractAction()
		{
			@Override
			public void actionPerformed(ActionEvent event)
			{
				requestTurn(requested);
			}
		});
	}

	private boolean hasPanelFocus()
	{
		java.awt.Component focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
		return focusOwner != null && SwingUtilities.isDescendingFrom(focusOwner, this);
	}

	private void requestTurn(Direction requested)
	{
		if (gameOver || turnQueue.size() >= 2)
		{
			return;
		}
		Direction planned = turnQueue.isEmpty() ? direction : turnQueue.peekLast();
		if (requested != planned && requested != planned.opposite())
		{
			turnQueue.addLast(requested);
		}
	}

	private void resetGame()
	{
		timer.stop();
		snake.clear();
		turnQueue.clear();
		int startX = gridSize / 2;
		int startY = gridSize / 2;
		snake.add(new Point(startX, startY));
		snake.add(new Point(startX - 1, startY));
		snake.add(new Point(startX - 2, startY));
		direction = Direction.RIGHT;
		score = 0;
		gameOver = true;
		lost = false;
		newBestThisRun = false;
		placeFood();
		updateScoreLabel();
		setMessage("Press Start or Space to play\nMove with arrows or WASD");
		startPauseButton.setText("Start");
		board.repaint();
	}

	private void toggleStartPause()
	{
		if (lost)
		{
			resetGame();
			startGame();
		}
		else if (timer.isRunning())
		{
			pauseGame();
		}
		else
		{
			startGame();
		}
	}

	private void startGame()
	{
		gameOver = false;
		setPlayingMessage();
		startPauseButton.setText("Pause");
		timer.start();
	}

	private void pauseGame()
	{
		timer.stop();
		gameOver = true;
		setMessage("Paused — Start or Space to resume\nMove with arrows or WASD");
		startPauseButton.setText("Start");
	}

	private void setMessage(String message)
	{
		messageLabel.setText("<html><center>" + message.replace("\n", "<br>") + "</center></html>");
	}

	private void cycleBoardSize()
	{
		if (gridSize == LARGE_GRID_SIZE)
		{
			gridSize = MEDIUM_GRID_SIZE;
			sizeButton.setText("Board: Medium (14×14)");
		}
		else if (gridSize == MEDIUM_GRID_SIZE)
		{
			gridSize = SMALL_GRID_SIZE;
			sizeButton.setText("Board: Small (10×10)");
		}
		else
		{
			gridSize = LARGE_GRID_SIZE;
			sizeButton.setText("Board: Large (18×18)");
		}
		bestScore = getBestScore();
		resetGame();
		timer.stop();
		gameOver = true;
		setMessage("Press Start or Space to play\nMove with arrows or WASD");
	}

	private int getBestScore()
	{
		if (gridSize == MEDIUM_GRID_SIZE)
		{
			return config.snakeMediumHighScore();
		}
		if (gridSize == SMALL_GRID_SIZE)
		{
			return config.snakeSmallHighScore();
		}
		return config.snakeHighScore();
	}

	private String getBestScoreKey()
	{
		if (gridSize == MEDIUM_GRID_SIZE)
		{
			return "snakeMediumHighScore";
		}
		if (gridSize == SMALL_GRID_SIZE)
		{
			return "snakeSmallHighScore";
		}
		return "snakeHighScore";
	}

	private void tick()
	{
		if (!turnQueue.isEmpty())
		{
			direction = turnQueue.removeFirst();
		}
		Point head = snake.peekFirst();
		Point next = new Point(head.x + direction.dx, head.y + direction.dy);
		boolean eating = next.equals(food);
		if (next.x < 0 || next.x >= gridSize || next.y < 0 || next.y >= gridSize
			|| (snake.contains(next) && (eating || !next.equals(snake.peekLast()))))
		{
			finishGame();
			return;
		}
		snake.addFirst(next);
		if (eating)
		{
			score++;
			if (score > bestScore)
			{
				bestScore = score;
				newBestThisRun = true;
				configManager.setConfiguration("games-but-no-games", getBestScoreKey(), bestScore);
			}
			updateScoreLabel();
			placeFood();
		}
		else
		{
			snake.removeLast();
		}
		board.repaint();
	}

	private void updateScoreLabel()
	{
		scoreLabel.setText("Score: " + score + "   Best: " + bestScore);
	}

	private void setPlayingMessage()
	{
		setMessage("Arrows/WASD to move · Space to pause");
	}

	private void placeFood()
	{
		if (snake.size() >= gridSize * gridSize)
		{
			finishGame();
			return;
		}
		List<Point> available = new ArrayList<>();
		for (int y = 0; y < gridSize; y++)
		{
			for (int x = 0; x < gridSize; x++)
			{
				Point candidate = new Point(x, y);
				if (!snake.contains(candidate))
				{
					available.add(candidate);
				}
			}
		}
		food = available.get(random.nextInt(available.size()));
		board.repaint();
	}

	private void finishGame()
	{
		timer.stop();
		gameOver = true;
		lost = true;
		setMessage(newBestThisRun
			? "New best: " + score + "! Game over\nPress Restart or Space"
			: "Game over — press Restart or Space\nMove with arrows or WASD");
		startPauseButton.setText("Restart");
		board.repaint();
	}

	void stop()
	{
		timer.stop();
		KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(arrowKeyDispatcher);
	}

	void requestKeyboardFocus()
	{
		board.requestFocusInWindow();
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

	private final class Board extends JPanel
	{
		Board()
		{
			setPreferredSize(new Dimension(LARGE_GRID_SIZE * CELL_SIZE, LARGE_GRID_SIZE * CELL_SIZE));
			setBackground(BACKGROUND);
			setFocusable(true);
		}

		@Override
		protected void paintComponent(Graphics graphics)
		{
			super.paintComponent(graphics);
			int cellSize = Math.max(1, Math.min(getWidth(), getHeight()) / gridSize);
			int boardSize = gridSize * cellSize;
			int offsetX = (getWidth() - boardSize) / 2;
			int offsetY = (getHeight() - boardSize) / 2;
			graphics.setColor(new Color(43, 49, 56));
			for (int cell = 0; cell <= gridSize; cell++)
			{
				int pos = cell * cellSize;
				graphics.drawLine(offsetX + pos, offsetY, offsetX + pos, offsetY + boardSize);
				graphics.drawLine(offsetX, offsetY + pos, offsetX + boardSize, offsetY + pos);
			}
			if (food != null)
			{
				graphics.setColor(FOOD);
				int inset = Math.max(2, cellSize / 6);
				graphics.fillOval(offsetX + food.x * cellSize + inset, offsetY + food.y * cellSize + inset,
					cellSize - 2 * inset, cellSize - 2 * inset);
			}
			boolean head = true;
			for (Point segment : snake)
			{
				graphics.setColor(head ? new Color(155, 231, 161) : SNAKE);
				graphics.fillRoundRect(offsetX + segment.x * cellSize + 1, offsetY + segment.y * cellSize + 1,
					cellSize - 2, cellSize - 2, 5, 5);
				head = false;
			}
		}
	}

	private static final class Point
	{
		private final int x;
		private final int y;

		Point(int x, int y)
		{
			this.x = x;
			this.y = y;
		}

		@Override
		public boolean equals(Object other)
		{
			if (!(other instanceof Point))
			{
				return false;
			}
			Point point = (Point) other;
			return x == point.x && y == point.y;
		}

		@Override
		public int hashCode()
		{
			return 31 * x + y;
		}
	}

	private enum Direction
	{
		UP(0, -1), DOWN(0, 1), LEFT(-1, 0), RIGHT(1, 0);

		private final int dx;
		private final int dy;

		Direction(int dx, int dy)
		{
			this.dx = dx;
			this.dy = dy;
		}

		Direction opposite()
		{
			switch (this)
			{
				case UP: return DOWN;
				case DOWN: return UP;
				case LEFT: return RIGHT;
			default: return LEFT;
			}
		}
	}
}
