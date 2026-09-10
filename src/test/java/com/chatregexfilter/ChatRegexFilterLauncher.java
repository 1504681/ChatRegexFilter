package com.chatregexfilter;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

// ./gradlew run, or run this from the IDE with -ea
public class ChatRegexFilterLauncher
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(ChatRegexFilterPlugin.class);
		RuneLite.main(args);
	}
}
