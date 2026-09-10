# Chat Regex Filter

RuneLite plugin that hides chatbox lines matching regular expressions you write. It runs on every line the chatbox draws, whatever the source: public, private, clan, friends chat, group ironman chat, game messages, broadcasts, examines, trade and login notifications, all of it. If a line contains a match it simply isn't shown.

The built-in Chat Filter plugin only looks at player chat and a handful of game message types, and it skips friends and clan members. This one has no exceptions.

## Settings

**Patterns** is a text box with one regular expression per line. Java regex syntax. A line is hidden if any pattern is found anywhere in it, so `bank sale` hides "Huge BANK SALE at ge" and `^You have completed .* lap\.$` hides agility lap messages. Blank lines and lines starting with `#` are ignored. Invalid patterns are skipped and reported in the chatbox (and the client log) so a typo in one line never disables the rest.

**Ignore case** is on by default.

**Strip formatting tags** (on by default) removes `<col=...>` and `<img=...>` tags before matching, so patterns see the plain text you read on screen rather than the raw markup.

**Include sender name** (off by default) matches against `Name: message` instead of just the message, so `^SomePlayer: ` hides everything a particular player says while `Zezima: .*buying` only hides their buying spam. The name has chat icons stripped when tags are stripped.

**Mark instead of hide** (off by default) is a debug mode. Matching lines stay in the chatbox but are rewritten as `X` followed by the message, all in one colour (red by default, set with **Mark colour**), so you can see exactly what your patterns would remove before you trust them. Turn it off again and the same lines disappear.

Timestamps are never part of what your patterns see. RuneLite's Timestamp plugin adds them in a separate step, and if some other plugin has already glued a `[12:34]` or `[1:05 PM]` onto the front of the text it is removed before matching. So `^` always means the start of the actual message, with or without timestamps turned on.

Changing any setting redraws the chatbox, so lines already on screen appear or disappear straight away. Turning the plugin off shows everything again. Nothing is deleted; hidden lines are still in the chat history and come back the moment the pattern is removed.

## Running it locally

You need a JDK, 11 or newer. Gradle comes with the wrapper.

```
git clone https://github.com/1504681/ChatRegexFilter.git
cd ChatRegexFilter
./gradlew run
```

On Windows use `.\gradlew.bat run`. That starts a normal RuneLite client in developer mode with the plugin already loaded. Log in, search the plugin list for Regex and add a pattern.

`./gradlew build` compiles and runs the unit tests, which is what the Plugin Hub CI does. `./gradlew installPlugin` puts a jar in `~/.runelite/externalPlugins` if you'd rather sideload. From an IDE, run `ChatRegexFilterLauncher` with `-ea`.

## Changelog

1.0.0: first release. Regex hiding for every chat type, sender matching, and a mark-instead-of-hide debug mode.

## License

BSD 2-Clause, see LICENSE.
