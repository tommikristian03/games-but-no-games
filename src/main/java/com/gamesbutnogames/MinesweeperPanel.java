package com.gamesbutnogames;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.KeyboardFocusManager;
import java.awt.KeyEventDispatcher;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.SwingUtilities;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.ui.PluginPanel;

final class MinesweeperPanel extends PluginPanel
{
	private static final Color[] NUMBER_COLORS = {
		Color.BLACK, new Color(43, 90, 201), new Color(45, 133, 63), new Color(204, 55, 48),
		new Color(49, 54, 147), new Color(143, 49, 40), new Color(31, 137, 143), Color.DARK_GRAY, Color.GRAY
	};

	private final GamesButNoGamesConfig config;
	private final ConfigManager configManager;
	private final JLabel statusLabel = new JLabel("Click a square to begin", SwingConstants.CENTER);
	private final JLabel timeLabel = new JLabel("Time: 0s   Best: --", SwingConstants.CENTER);
	private final JButton difficultyButton = new JButton("Difficulty: Easy");
	private final Board board = new Board();
	private final Timer clock = new Timer(1000, event -> updateClock());
	private boolean[][] mines;
	private boolean[][] revealed;
	private boolean[][] flagged;
	private boolean generated;
	private boolean finished;
	private boolean started;
	private int elapsedSeconds;
	private int revealedSafe;
	private Difficulty difficulty = Difficulty.EASY;
	private boolean spaceDown;
	private boolean resumeClockAfterHide;
	private final KeyEventDispatcher restartKeyDispatcher = event ->
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
				newGame();
			}
			return true;
		}
		return false;
	};

	MinesweeperPanel(GamesButNoGamesConfig config, ConfigManager configManager)
	{
		this.config = config;
		this.configManager = configManager;
		KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(restartKeyDispatcher);
		setLayout(new BorderLayout(0, 8));
		setBorder(BorderFactory.createEmptyBorder(12, 8, 12, 8));
		JLabel title = new JLabel("MINESWEEPER", SwingConstants.CENTER);
		title.setFont(title.getFont().deriveFont(Font.BOLD, 17f));
		statusLabel.setForeground(new Color(195, 201, 208));
		timeLabel.setForeground(Color.WHITE);
		JPanel header = new JPanel(new BorderLayout(0, 5));
		header.setOpaque(false);
		header.add(title, BorderLayout.NORTH);
		header.add(timeLabel, BorderLayout.CENTER);
		header.add(statusLabel, BorderLayout.SOUTH);
		add(header, BorderLayout.NORTH);
		add(board, BorderLayout.CENTER);
		JPanel buttons = new JPanel(new BorderLayout(5, 0));
		difficultyButton.addActionListener(event ->
		{
			difficulty = difficulty.next();
			newGame();
		});
		buttons.add(difficultyButton, BorderLayout.CENTER);
		JButton restart = new JButton("New Game");
		restart.addActionListener(event -> newGame());
		buttons.add(restart, BorderLayout.SOUTH);
		add(buttons, BorderLayout.SOUTH);
		board.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mousePressed(MouseEvent event)
			{
				board.requestFocusInWindow();
				int cell = board.cellSize();
				int xOffset = board.xOffset(cell);
				int yOffset = board.yOffset(cell);
				int col = (event.getX() - xOffset) / cell;
				int row = (event.getY() - yOffset) / cell;
				if (event.getX() < xOffset || event.getY() < yOffset
					|| row < 0 || row >= difficulty.rows || col < 0 || col >= difficulty.cols)
				{
					return;
				}
				if (event.getButton() == MouseEvent.BUTTON3)
				{
					toggleFlag(row, col);
				}
				else if (event.getButton() == MouseEvent.BUTTON1)
				{
					reveal(row, col);
				}
			}
		});
		newGame();
	}

	private void newGame()
	{
		clock.stop();
		mines = new boolean[difficulty.rows][difficulty.cols];
		revealed = new boolean[difficulty.rows][difficulty.cols];
		flagged = new boolean[difficulty.rows][difficulty.cols];
		generated = false;
		finished = false;
		started = false;
		elapsedSeconds = 0;
		revealedSafe = 0;
		difficultyButton.setText("Difficulty: " + difficulty.label);
		statusLabel.setText("<html><center>Click: reveal · Right-click: flag<br>Space: new board</center></html>");
		updateTimeLabel();
		board.repaint();
	}

	private boolean hasPanelFocus()
	{
		java.awt.Component focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
		return focusOwner != null && SwingUtilities.isDescendingFrom(focusOwner, this);
	}

	private void generateMines(int safeRow, int safeCol)
	{
		List<Integer> cells = new ArrayList<>();
		for (int row = 0; row < difficulty.rows; row++)
		{
			for (int col = 0; col < difficulty.cols; col++)
			{
				if (Math.abs(row - safeRow) > 1 || Math.abs(col - safeCol) > 1)
				{
					cells.add(row * difficulty.cols + col);
				}
			}
		}
		Collections.shuffle(cells);
		for (int i = 0; i < difficulty.mines; i++)
		{
			int cell = cells.get(i);
			mines[cell / difficulty.cols][cell % difficulty.cols] = true;
		}
		generated = true;
	}

	private void reveal(int row, int col)
	{
		if (finished || flagged[row][col] || revealed[row][col])
		{
			return;
		}
		if (!generated)
		{
			generateMines(row, col);
		}
		if (!started)
		{
			started = true;
			clock.start();
		}
		if (mines[row][col])
		{
			revealed[row][col] = true;
			finished = true;
			clock.stop();
			statusLabel.setText("Boom! Press New Game to try again");
			board.repaint();
			return;
		}
		revealArea(row, col);
		if (revealedSafe == difficulty.rows * difficulty.cols - difficulty.mines)
		{
			finished = true;
			clock.stop();
			int best = bestTime();
			boolean newBest = best == 0 || elapsedSeconds < best;
			if (newBest)
			{
				configManager.setConfiguration("games-but-no-games", bestTimeKey(), elapsedSeconds);
				statusLabel.setText("New best! Cleared in " + elapsedSeconds + "s");
			}
			else
			{
				statusLabel.setText("You cleared it!");
			}
			updateTimeLabel();
		}
		board.repaint();
	}

	private void revealArea(int row, int col)
	{
		if (row < 0 || row >= difficulty.rows || col < 0 || col >= difficulty.cols
			|| revealed[row][col] || flagged[row][col] || mines[row][col])
		{
			return;
		}
		revealed[row][col] = true;
		revealedSafe++;
		if (adjacentMines(row, col) == 0)
		{
			for (int dr = -1; dr <= 1; dr++)
			{
				for (int dc = -1; dc <= 1; dc++)
				{
					if (dr != 0 || dc != 0)
					{
						revealArea(row + dr, col + dc);
					}
				}
			}
		}
	}

	private void toggleFlag(int row, int col)
	{
		if (!finished && !revealed[row][col])
		{
			flagged[row][col] = !flagged[row][col];
			board.repaint();
		}
	}

	private int adjacentMines(int row, int col)
	{
		int count = 0;
		for (int dr = -1; dr <= 1; dr++)
		{
			for (int dc = -1; dc <= 1; dc++)
			{
				int r = row + dr;
				int c = col + dc;
				if (r >= 0 && r < difficulty.rows && c >= 0 && c < difficulty.cols && mines[r][c])
				{
					count++;
				}
			}
		}
		return count;
	}

	private void updateClock()
	{
		elapsedSeconds++;
		updateTimeLabel();
	}

	private void updateTimeLabel()
	{
		int best = bestTime();
		timeLabel.setText("Time: " + elapsedSeconds + "s   Best: " + (best == 0 ? "--" : best + "s"));
	}

	private int bestTime()
	{
		switch (difficulty)
		{
			case HARD: return config.minesweeperIntermediateBestTime();
			default: return config.minesweeperBestTime();
		}
	}

	private String bestTimeKey()
	{
		switch (difficulty)
		{
			case HARD: return "minesweeperIntermediateBestTime";
			default: return "minesweeperBestTime";
		}
	}

	void stop()
	{
		clock.stop();
		KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(restartKeyDispatcher);
	}

	void onGameHidden()
	{
		resumeClockAfterHide = clock.isRunning();
		if (resumeClockAfterHide)
		{
			clock.stop();
		}
	}

	void onGameShown()
	{
		if (resumeClockAfterHide && started && !finished)
		{
			resumeClockAfterHide = false;
			clock.start();
		}
	}

	private final class Board extends JPanel
	{
		Board()
		{
			setPreferredSize(new Dimension(216, 216));
			setBackground(new Color(28, 32, 37));
			setFocusable(true);
		}

		int cellSize()
		{
			return Math.max(1, Math.min(getWidth() / difficulty.cols, getHeight() / difficulty.rows));
		}

		int xOffset(int cell)
		{
			return (getWidth() - difficulty.cols * cell) / 2;
		}

		int yOffset(int cell)
		{
			return (getHeight() - difficulty.rows * cell) / 2;
		}

		@Override
		protected void paintComponent(Graphics graphics)
		{
			super.paintComponent(graphics);
			int cell = cellSize();
			int x0 = xOffset(cell);
			int y0 = yOffset(cell);
			for (int row = 0; row < difficulty.rows; row++)
			{
				for (int col = 0; col < difficulty.cols; col++)
				{
					int x = x0 + col * cell;
					int y = y0 + row * cell;
					if (revealed[row][col])
					{
						graphics.setColor(new Color(172, 177, 182));
					}
					else
					{
						graphics.setColor(new Color(103, 112, 122));
					}
					graphics.fill3DRect(x, y, cell, cell, !revealed[row][col]);
					graphics.setColor(new Color(45, 51, 58));
					graphics.drawRect(x, y, cell, cell);
					String text = null;
					Color textColor = Color.BLACK;
					if (flagged[row][col] && !finished)
					{
						text = "F";
						textColor = new Color(210, 60, 55);
					}
					else if (revealed[row][col] && mines[row][col])
					{
						text = "✹";
						textColor = Color.BLACK;
					}
					else if (revealed[row][col])
					{
						int adjacent = adjacentMines(row, col);
						if (adjacent > 0)
						{
							text = Integer.toString(adjacent);
							textColor = NUMBER_COLORS[adjacent];
						}
					}
					else if (finished && mines[row][col])
					{
						text = "✹";
						textColor = Color.BLACK;
					}
					if (text != null)
					{
						graphics.setColor(textColor);
						graphics.setFont(graphics.getFont().deriveFont(Font.BOLD, Math.max(6f, cell * 0.68f)));
						int textWidth = graphics.getFontMetrics().stringWidth(text);
						int textHeight = graphics.getFontMetrics().getAscent();
						graphics.drawString(text, x + (cell - textWidth) / 2, y + (cell + textHeight) / 2 - 2);
					}
				}
			}
		}
	}

	private enum Difficulty
	{
		EASY("Easy", 9, 9, 10),
		HARD("Hard", 16, 16, 40);

		private final String label;
		private final int rows;
		private final int cols;
		private final int mines;

		Difficulty(String label, int rows, int cols, int mines)
		{
			this.label = label;
			this.rows = rows;
			this.cols = cols;
			this.mines = mines;
		}

		private Difficulty next()
		{
			return values()[(ordinal() + 1) % values().length];
		}
	}
}
