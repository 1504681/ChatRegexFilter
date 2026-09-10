package com.chatregexfilter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class LineFilterTest
{
	@Test
	public void parsesLinesSkippingBlanksAndComments()
	{
		LineFilter filter = new LineFilter("drop party\n\n# a comment\n  ^Welcome to  \r\n", true);
		assertEquals(2, filter.getPatterns().size());
		assertTrue(filter.getErrors().isEmpty());
	}

	@Test
	public void findsAnywhereInLine()
	{
		LineFilter filter = new LineFilter("bank sale", true);
		assertTrue(filter.matches("Huge BANK SALE at ge w302"));
		assertFalse(filter.matches("selling bank"));
	}

	@Test
	public void caseSensitiveWhenAsked()
	{
		LineFilter filter = new LineFilter("Bank", false);
		assertTrue(filter.matches("Bank sale"));
		assertFalse(filter.matches("bank sale"));
	}

	@Test
	public void anchorsAndGroupsWork()
	{
		LineFilter filter = new LineFilter("^You have completed (a|an) .* lap\\.$", true);
		assertTrue(filter.matches("You have completed a Seers' Village agility lap."));
		assertFalse(filter.matches("You have completed a lap and something else"));
	}

	@Test
	public void invalidPatternsAreReportedNotFatal()
	{
		LineFilter filter = new LineFilter("good\n(unclosed\nalso good", true);
		assertEquals(2, filter.getPatterns().size());
		assertEquals(1, filter.getErrors().size());
		assertTrue(filter.getErrors().get(0).startsWith("line 2:"));
		assertTrue(filter.matches("also good"));
	}

	@Test
	public void emptyAndNullNeverMatch()
	{
		assertTrue(new LineFilter("", true).isEmpty());
		assertTrue(new LineFilter(null, true).isEmpty());
		assertFalse(new LineFilter("", true).matches("anything"));
		assertFalse(new LineFilter("x", true).matches(null));
	}

	@Test
	public void stripsLeadingTimestamps()
	{
		assertEquals("hello", LineFilter.stripTimestamp("[12:34] hello"));
		assertEquals("hello", LineFilter.stripTimestamp("[12:34:56] hello"));
		assertEquals("hello", LineFilter.stripTimestamp("[1:05 PM] hello"));
		assertEquals("hello", LineFilter.stripTimestamp("<col=808080>[12:34]</col> hello"));
		assertEquals("[Clan] hello", LineFilter.stripTimestamp("[Clan] hello"));
		assertEquals("hello [12:34]", LineFilter.stripTimestamp("hello [12:34]"));
		assertEquals("", LineFilter.stripTimestamp(null));
	}

	@Test
	public void normalizeStripsTagsAndAddsSender()
	{
		assertEquals("You have been poisoned!",
			LineFilter.normalize(null, "<col=ff0000>You have been poisoned!</col>", true));
		assertEquals("<col=ff0000>You have been poisoned!</col>",
			LineFilter.normalize(null, "<col=ff0000>You have been poisoned!</col>", false));
		assertEquals("Zezima: buying gf",
			LineFilter.normalize("<img=2>Zezima", "[12:34] buying gf", true));
		assertEquals("", LineFilter.normalize(null, null, true));
	}

	@Test
	public void anchoredPatternWorksWithTimestampsOn()
	{
		LineFilter filter = new LineFilter("^Zezima: ", true);
		assertTrue(filter.matches(LineFilter.normalize("Zezima", "[12:34] hi", true)));
		assertFalse(filter.matches(LineFilter.normalize("Zezima", "hi", false).replace("Zezima: ", "")));
	}
}
