package com.chatregexfilter;

import static org.junit.Assert.assertEquals;
import java.awt.Color;
import org.junit.Test;

public class MarkLineTest
{
	@Test
	public void wrapsWholeLineInOneColour()
	{
		assertEquals("<col=ff0000>X hello</col>", ChatRegexFilterPlugin.markLine("hello", Color.RED));
	}

	@Test
	public void innerTagsAreRemovedSoColourHolds()
	{
		assertEquals("<col=ff0000>X You have been poisoned! run</col>",
			ChatRegexFilterPlugin.markLine("<col=00ff00>You have been poisoned!</col> run", Color.RED));
	}

	@Test
	public void nullIsSafe()
	{
		assertEquals("<col=ff0000>X </col>", ChatRegexFilterPlugin.markLine(null, Color.RED));
	}
}
