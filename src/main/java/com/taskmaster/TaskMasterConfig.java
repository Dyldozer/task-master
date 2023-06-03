package com.taskmaster;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("example")
public interface TaskMasterConfig extends Config
{
	@ConfigItem(
		keyName = "server",
		name = "Server Address",
		description = "The address of the task server"
	)
	default String server()
	{
		return "";
	}
}
