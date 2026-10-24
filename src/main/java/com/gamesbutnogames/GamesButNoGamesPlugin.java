package com.gamesbutnogames;

import com.google.inject.Provides;
import javax.inject.Inject;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import java.awt.image.BufferedImage;

@PluginDescriptor(
	name = "Games But No Games"
)
public class GamesButNoGamesPlugin extends Plugin
{
	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private GamesButNoGamesConfig config;

	@Inject
	private ConfigManager configManager;

	private GamesPanel panel;
	private NavigationButton navigationButton;

	@Override
	protected void startUp()
	{
		panel = new GamesPanel(config, configManager);
		BufferedImage icon = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
		java.awt.Graphics2D graphics = icon.createGraphics();
		graphics.setColor(new java.awt.Color(92, 190, 115));
		graphics.fillRoundRect(2, 2, 12, 12, 4, 4);
		graphics.setColor(java.awt.Color.WHITE);
		graphics.fillOval(9, 4, 3, 3);
		graphics.dispose();
		navigationButton = NavigationButton.builder()
			.tooltip("Games But No Games")
			.icon(icon)
			.panel(panel)
			.build();
		clientToolbar.addNavigation(navigationButton);
	}

	@Override
	protected void shutDown()
	{
		clientToolbar.removeNavigation(navigationButton);
		panel.stop();
		panel = null;
		navigationButton = null;
	}

	@Provides
	GamesButNoGamesConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(GamesButNoGamesConfig.class);
	}
}
