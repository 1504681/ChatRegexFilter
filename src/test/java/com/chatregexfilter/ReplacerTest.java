package com.chatregexfilter;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class ReplacerTest
{
	@Test
	public void splitsOnLastComma()
	{
		assertArrayEquals(new String[]{"gz", ""}, Replacer.split("gz,"));
		assertArrayEquals(new String[]{"gz", ""}, Replacer.split("gz"));
		assertArrayEquals(new String[]{"gz", "grats"}, Replacer.split("gz, grats"));
		assertArrayEquals(new String[]{"a{1,3}", "b"}, Replacer.split("a{1,3}, b"));
		assertArrayEquals(new String[]{"a{1,3}", ""}, Replacer.split("a{1,3},"));
		assertArrayEquals(new String[]{"x", ""}, Replacer.split("x, \"\""));
		assertArrayEquals(new String[]{"x", ", "}, Replacer.split("x,\", \""));
		assertArrayEquals(new String[]{"x", " y "}, Replacer.split("x, \" y \""));
	}

	@Test
	public void removesMatchAndTidiesSpaces()
	{
		Replacer replacer = new Replacer("\\bgz\\b,", true);
		assertEquals("on 99", replacer.apply("GZ on 99", true));
		assertEquals("nice one", replacer.apply("nice gz one", true));
		assertNull(replacer.apply("no match here", true));
	}

	@Test
	public void dollarIsLiteralUnlessGroup()
	{
		assertEquals("500 $ coins", new Replacer("gp, $ coins", true).apply("500 gp", true));
		assertEquals("a\\b", new Replacer("x, \\", true).apply("axb", true));
		assertEquals(1, new Replacer("gp, $1 coins", true).getErrors().size());
	}

	@Test
	public void collapsesRepeatedCharacters()
	{
		Replacer replacer = new Replacer("(.)\\1{2,}, $1$1", true);
		assertEquals("Gzz@@", replacer.apply("Gzzzz@@@@@@@@@", true));
		assertNull(replacer.apply("gzz", true));
	}

	@Test
	public void groupFollowedByDigit()
	{
		assertEquals("ab2", new Replacer("(a)x, $1b2", true).apply("ax", true));
	}

	@Test
	public void rewritesNames()
	{
		Replacer replacer = new Replacer("\\b(iron|osrs|chunk)\\b,", true);
		assertEquals("<img=2>Bob:", replacer.applyToName("<img=2>Iron Bob:", true));
		assertEquals("Bob:", replacer.applyToName("Bob Chunk:", true));
		assertEquals("From Bob:", replacer.applyToName("From osrs Bob:", true));
		assertNull(replacer.applyToName("Zezima:", true));
		assertEquals("Bob:", new Replacer(" Iron$,", true).applyToName("Bob Iron:", true));
	}

	@Test
	public void rulesApplyInOrder()
	{
		Replacer replacer = new Replacer("cat, dog\ndog, bird", true);
		assertEquals("bird", replacer.apply("cat", true));
	}

	@Test
	public void keepsTagsAndTimestamp()
	{
		Replacer replacer = new Replacer("col,", true);
		assertEquals("<col=ff0000>red here</col>", replacer.apply("<col=ff0000>red col here</col>", true));
		assertEquals("<col=ff0000>red</col>", replacer.apply("<col=ff0000>red col</col>", true));
		assertEquals("[12:34] hi", new Replacer("^spam ", true).apply("[12:34] spam hi", true));
	}

	@Test
	public void wholeMessageRemovedIsBlank()
	{
		String result = new Replacer("^.*$,", true).apply("<col=ff0000>everything</col>", true);
		assertTrue(Replacer.isBlank(result));
	}

	@Test
	public void invalidRulesReported()
	{
		Replacer replacer = new Replacer("(bad,\n, x\ngood,", true);
		assertEquals(2, replacer.getErrors().size());
		assertEquals("ok", replacer.apply("good ok", true));
	}
}
