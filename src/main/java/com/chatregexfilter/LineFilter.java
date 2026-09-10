package com.chatregexfilter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import net.runelite.client.util.Text;

/**
 * Compiles the configured regex list and decides whether a chat line should be hidden.
 * Pure Java, no client access, so it can be unit tested.
 */
public class LineFilter
{
	// a leading timestamp such as [12:34], [12:34:56] or [1:23 PM], optionally wrapped in a colour tag
	private static final Pattern LEADING_TIMESTAMP = Pattern.compile(
		"^\\s*(?:<col=[0-9a-fA-F]+>)?\\[\\d{1,2}:\\d{2}(?::\\d{2})?(?:\\s*[AaPp][Mm])?\\](?:</col>)?\\s*");

	private final List<Pattern> patterns;
	private final List<String> errors;

	public LineFilter(String text, boolean caseInsensitive)
	{
		List<Pattern> compiled = new ArrayList<>();
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
				int flags = caseInsensitive ? Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE : 0;
				try
				{
					compiled.add(Pattern.compile(line, flags));
				}
				catch (PatternSyntaxException e)
				{
					problems.add("line " + lineNumber + ": " + e.getDescription());
				}
			}
		}
		patterns = Collections.unmodifiableList(compiled);
		errors = Collections.unmodifiableList(problems);
	}

	public List<Pattern> getPatterns()
	{
		return patterns;
	}

	// human readable description of every line that failed to compile
	public List<String> getErrors()
	{
		return errors;
	}

	public boolean isEmpty()
	{
		return patterns.isEmpty();
	}

	// true when any pattern is found anywhere in the line
	public boolean matches(String line)
	{
		if (line == null || patterns.isEmpty())
		{
			return false;
		}
		for (Pattern pattern : patterns)
		{
			if (pattern.matcher(line).find())
			{
				return true;
			}
		}
		return false;
	}

	// the text a regex is run against: optional "Sender: " prefix, no timestamp, and optionally no <col>/<img> tags
	public static String normalize(String sender, String message, boolean stripTags)
	{
		if (message == null)
		{
			message = "";
		}
		String text = stripTimestamp(message);
		if (stripTags)
		{
			text = Text.removeTags(text);
		}
		if (sender != null && !sender.isEmpty())
		{
			String name = stripTags ? Text.removeTags(sender) : sender;
			text = name + ": " + text;
		}
		return text;
	}

	public static String stripTimestamp(String message)
	{
		if (message == null)
		{
			return "";
		}
		return LEADING_TIMESTAMP.matcher(message).replaceFirst("");
	}
}
