package com.gamesbutnogames;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.event.HierarchyEvent;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.SwingConstants;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.ui.PluginPanel;

final class GamesPanel extends PluginPanel
{
	private final SnakePanel snakePanel;
	private final MinesweeperPanel minesweeperPanel;
	private final TunnelFlightPanel tunnelFlightPanel;
	private final String[] gameNames = {"Snake", "Minesweeper", "Tunnel Flight"};
	private final JPanel gameCards = new JPanel(new CardLayout());
	private final JLabel currentGameLabel = new JLabel(gameNames[0], SwingConstants.CENTER);
	private int currentGame;

	GamesPanel(GamesButNoGamesConfig config, ConfigManager configManager)
	{
		setLayout(new BorderLayout());
		snakePanel = new SnakePanel(config, configManager);
		minesweeperPanel = new MinesweeperPanel(config, configManager);
		tunnelFlightPanel = new TunnelFlightPanel(config, configManager);
		gameCards.add(snakePanel, gameNames[0]);
		gameCards.add(minesweeperPanel, gameNames[1]);
		gameCards.add(tunnelFlightPanel, gameNames[2]);
		JButton previousButton = new JButton("<");
		JButton nextButton = new JButton(">");
		previousButton.setToolTipText("Previous game");
		nextButton.setToolTipText("Next game");
		previousButton.addActionListener(event -> showGame((currentGame + gameNames.length - 1) % gameNames.length));
		nextButton.addActionListener(event -> showGame((currentGame + 1) % gameNames.length));
		JPanel gameSwitcher = new JPanel(new BorderLayout(6, 0));
		gameSwitcher.add(previousButton, BorderLayout.WEST);
		gameSwitcher.add(currentGameLabel, BorderLayout.CENTER);
		gameSwitcher.add(nextButton, BorderLayout.EAST);
		add(gameSwitcher, BorderLayout.NORTH);
		add(gameCards, BorderLayout.CENTER);
		addHierarchyListener(event ->
		{
			if ((event.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0)
			{
				if (isShowing())
				{
					showCurrentGame();
					SwingUtilities.invokeLater(this::focusCurrentGame);
				}
				else
				{
					hideCurrentGame();
				}
			}
		});
	}

	private void showGame(int index)
	{
		hideCurrentGame();
		currentGame = index;
		currentGameLabel.setText(gameNames[index]);
		((CardLayout) gameCards.getLayout()).show(gameCards, gameNames[index]);
		showCurrentGame();
		focusCurrentGame();
	}

	private void hideCurrentGame()
	{
		if (currentGame == 0) snakePanel.onGameHidden();
		else if (currentGame == 1) minesweeperPanel.onGameHidden();
		else tunnelFlightPanel.onGameHidden();
	}

	private void showCurrentGame()
	{
		if (currentGame == 0) snakePanel.onGameShown();
		else if (currentGame == 1) minesweeperPanel.onGameShown();
		else tunnelFlightPanel.onGameShown();
	}

	private void focusCurrentGame()
	{
		if (!isShowing())
		{
			return;
		}
		if (currentGame == 0)
		{
			snakePanel.requestKeyboardFocus();
		}
		else if (currentGame == 2)
		{
			tunnelFlightPanel.requestKeyboardFocus();
		}
	}

	void stop()
	{
		snakePanel.stop();
		minesweeperPanel.stop();
		tunnelFlightPanel.stop();
	}
}
