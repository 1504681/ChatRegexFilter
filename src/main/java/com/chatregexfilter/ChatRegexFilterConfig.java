package com.chatregexfilter;

import java.awt.Color;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup(ChatRegexFilterConfig.GROUP)
public interface ChatRegexFilterConfig extends Config
{
	String GROUP = "chatregexfilter";

	@ConfigItem(
		keyName = "patterns",
		name = "Patterns",
		description = "Regular expressions, one per line. Any chatbox line containing a match is hidden. Lines starting with # are ignored",
		position = 0
	)
	default String patterns()
	{
		return "";
	}

	@ConfigItem(
		keyName = "caseInsensitive",
		name = "Ignore case",
		description = "Match regardless of upper or lower case",
		position = 1
	)
	default boolean caseInsensitive()
	{
		return true;
	}

	@ConfigItem(
		keyName = "stripTags",
		name = "Strip formatting tags",
		description = "Remove <col=...> and <img=...> tags before matching, so patterns see the plain text you read in the chatbox",
		position = 2
	)
	default boolean stripTags()
	{
		return true;
	}

	@ConfigItem(
		keyName = "includeSender",
		name = "Include sender name",
		description = "Match against \"Name: message\" instead of just the message, so a pattern can target a particular player",
		position = 3
	)
	default boolean includeSender()
	{
		return false;
	}

	@ConfigItem(
		keyName = "reportInvalid",
		name = "Report invalid patterns",
		description = "Print a chatbox message when a line in the pattern list is not a valid regular expression",
		position = 4
	)
	default boolean reportInvalid()
	{
		return true;
	}

	@ConfigItem(
		keyName = "debugMode",
		name = "Mark instead of hide",
		description = "Debug: leave matching lines in the chatbox, prefixed with an X and coloured, so you can see what would be hidden",
		position = 5
	)
	default boolean debugMode()
	{
		return false;
	}

	@ConfigItem(
		keyName = "debugColor",
		name = "Mark colour",
		description = "Colour used for marked lines when Mark instead of hide is on",
		position = 6
	)
	default Color debugColor()
	{
		return Color.RED;
	}
}
