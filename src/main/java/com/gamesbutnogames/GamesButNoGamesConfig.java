package com.gamesbutnogames;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("games-but-no-games")
public interface GamesButNoGamesConfig extends Config
{
	@ConfigItem(
		keyName = "snakeHighScore",
		name = "Snake high score",
		description = "Best Snake score saved on this RuneLite profile",
		hidden = true
	)
	default int snakeHighScore()
	{
		return 0;
	}

	@ConfigItem(
		keyName = "snakeMediumHighScore",
		name = "Snake medium high score",
		description = "Best score on the medium Snake board saved on this RuneLite profile",
		hidden = true
	)
	default int snakeMediumHighScore()
	{
		return 0;
	}

	@ConfigItem(
		keyName = "snakeSmallHighScore",
		name = "Snake small high score",
		description = "Best score on the small Snake board saved on this RuneLite profile",
		hidden = true
	)
	default int snakeSmallHighScore()
	{
		return 0;
	}

	@ConfigItem(
		keyName = "minesweeperBestTime",
		name = "Minesweeper Easy best time",
		description = "Fastest Easy Minesweeper clear in seconds saved on this RuneLite profile",
		hidden = true
	)
	default int minesweeperBestTime()
	{
		return 0;
	}

	@ConfigItem(
		keyName = "minesweeperHardBestTime",
		name = "Legacy Minesweeper hard best time",
		description = "Retained record from the previous Hard mode",
		hidden = true
	)
	default int minesweeperHardBestTime()
	{
		return 0;
	}

	@ConfigItem(
		keyName = "minesweeperIntermediateBestTime",
		name = "Minesweeper Hard best time",
		description = "Fastest Hard Minesweeper clear in seconds saved on this RuneLite profile",
		hidden = true
	)
	default int minesweeperIntermediateBestTime()
	{
		return 0;
	}

	@ConfigItem(
		keyName = "tunnelFlightHighScore",
		name = "Tunnel Flight high score",
		description = "Best Tunnel Flight score saved on this RuneLite profile",
		hidden = true
	)
	default int tunnelFlightHighScore()
	{
		return 0;
	}

	@ConfigItem(
		keyName = "tunnelFlightHardHighScore",
		name = "Tunnel Flight Hard high score",
		description = "Best Tunnel Flight Hard score saved on this RuneLite profile",
		hidden = true
	)
	default int tunnelFlightHardHighScore()
	{
		return 0;
	}

	@ConfigItem(
		keyName = "tunnelFlightTriangleColor",
		name = "Tunnel Flight triangle color",
		description = "Triangle color in Tunnel Flight (Red, Yellow, Green, Blue, or Orange)",
		hidden = true
	)
	default String tunnelFlightTriangleColor()
	{
		return "Yellow";
	}

}
