package com.chatregexfilter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import net.runelite.client.util.Text;

/**
 * Parses "pattern, replacement" rules and rewrites the text of a chat line.
 * Pure Java, no client access, so it can be unit tested.
 */
public class Replacer
{
	private static final Pattern TAG = Pattern.compile("<[^>]*>");
	private static final Pattern QUOTED_REPLACEMENT = Pattern.compile(",\\s*\"(.*)\"$");
	private static final Pattern EXTRA_SPACES = Pattern.compile(" {2,}");

	static final class Rule
	{
		final Pattern pattern;
		final String replacement;

		Rule(Pattern pattern, String replacement)
		{
			this.pattern = pattern;
			this.replacement = replacement;
		}
	}

	private final List<Rule> rules;
	private final List<String> errors;

	public Replacer(String text, boolean caseInsensitive)
	{
		List<Rule> parsed = new ArrayList<>();
		List<String> problems = new ArrayList<>();
		if (text != null)
		{
			int lineNumber = 0;
			for (String raw : text.split("\\r?\\n"))
			{
				lineNumber++;
				String line = raw.trim();
				if (line.isEmpty() || line.startsWith("#"))
				{
					continue;
				}
				String[] parts = split(line);
				if (parts[0].isEmpty())
				{
					problems.add("replacement line " + lineNumber + ": nothing before the comma");
					continue;
				}
				int flags = caseInsensitive ? Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE : 0;
				try
				{
					parsed.add(new Rule(Pattern.compile(parts[0], flags), parts[1]));
				}
				catch (PatternSyntaxException e)
				{
					problems.add("replacement line " + lineNumber + ": " + e.getDescription());
				}
			}
		}
		rules = Collections.unmodifiableList(parsed);
		errors = Collections.unmodifiableList(problems);
	}

	// pattern and replacement. Splits on the last comma so commas inside the regex ({1,3}, [,.]) still work.
	// A quoted replacement is taken as-is ("" is nothing, ", " keeps its comma); no comma at all means remove
	static String[] split(String line)
	{
		Matcher quoted = QUOTED_REPLACEMENT.matcher(line);
		if (quoted.find())
		{
			return new String[]{line.substring(0, quoted.start()).trim(), quoted.group(1)};
		}
		int comma = line.lastIndexOf(',');
		if (comma < 0)
		{
			return new String[]{line, ""};
		}
		return new String[]{line.substring(0, comma).trim(), line.substring(comma + 1).trim()};
	}

	public List<String> getErrors()
	{
		return errors;
	}

	public boolean isEmpty()
	{
		return rules.isEmpty();
	}

	/**
	 * Applies every rule in order. Returns null when nothing changed. A leading timestamp is left alone so ^ still
	 * means the start of the message; with skipTags the rules only see the text between tags, so colours and icons survive.
	 */
	public String apply(String message, boolean skipTags)
	{
		if (message == null || rules.isEmpty())
		{
			return null;
		}
		String body = LineFilter.stripTimestamp(message);
		String prefix = message.substring(0, message.length() - body.length());

		String result = skipTags ? replaceOutsideTags(body) : replaceAll(body);
		if (result.equals(body))
		{
			return null;
		}
		return prefix + EXTRA_SPACES.matcher(result).replaceAll(" ").trim();
	}

	// true when a rewritten line has no visible text left and should be hidden instead
	public static boolean isBlank(String message)
	{
		return message == null || Text.removeTags(message).trim().isEmpty();
	}

	private String replaceAll(String text)
	{
		for (Rule rule : rules)
		{
			text = rule.pattern.matcher(text).replaceAll(Matcher.quoteReplacement(rule.replacement));
		}
		return text;
	}

	private String replaceOutsideTags(String text)
	{
		StringBuilder out = new StringBuilder();
		Matcher tag = TAG.matcher(text);
		int last = 0;
		while (tag.find())
		{
			out.append(replaceAll(text.substring(last, tag.start())));
			out.append(tag.group());
			last = tag.end();
		}
		out.append(replaceAll(text.substring(last)));
		return out.toString();
	}
}
