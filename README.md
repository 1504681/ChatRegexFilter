# Chat Regex Filter

RuneLite plugin that hides or rewrites chatbox lines matching regular expressions you write. It runs on every line the chatbox draws, whatever the source: public, private, clan, friends chat, group ironman chat, game messages, broadcasts, examines, trade and login notifications, all of it. If a line contains a match it simply isn't shown.

The built-in Chat Filter plugin only looks at player chat and a handful of game message types, and it skips friends and clan members. This one has no exceptions.

## Settings

**Patterns** is a text box with one regular expression per line. Java regex syntax. A line is hidden if any pattern is found anywhere in it, so `bank sale` hides "Huge BANK SALE at ge" and `^You have completed .* lap\.$` hides agility lap messages. Blank lines and lines starting with `#` are ignored. Invalid patterns are skipped and reported in the chatbox (and the client log) so a typo in one line never disables the rest.

**Replacements** rewrites text instead of hiding the line. One rule per line: `pattern, replacement`. Nothing after the comma (or `""`) removes the match, so `\bgz\b,` turns "gz on 99" into "on 99" and `lol, haha` swaps the word. `$1` to `$9` put back a captured group, so `(.)\1{2,}, $1$1` cuts any run of three or more of the same character down to two ("Gzzzz@@@@@@" becomes "Gzz@@"). Any other `$` is literal. The split is on the last comma, so commas inside the regex like `a{1,3},` still work; wrap the replacement in quotes to keep a comma or spaces in it (`x, ", "`). A line with no comma just removes the match. Rules run in order on the message, never the timestamp, and with **Strip formatting tags** on they skip over colour and icon tags so those survive. If a rule removes everything, the line is hidden. Hiding patterns are checked first, against the original text.

**Replace in names** (off by default) runs the same rules on the sender name in front of each line, so `\b(iron|osrs|chunk)\b,` turns "Iron Bob:" into "Bob:". It only changes what is drawn: right-click Add friend, Message and Report still use the real name, and **Include sender name** still matches the real name.

**Ignore case** is on by default and applies to both lists.

**Strip formatting tags** (on by default) removes `<col=...>` and `<img=...>` tags before matching, so patterns see the plain text you read on screen rather than the raw markup.

**Include sender name** (off by default) matches against `Name: message` instead of just the message, so `^SomePlayer: ` hides everything a particular player says while `Zezima: .*buying` only hides their buying spam. The name has chat icons stripped when tags are stripped.

The Debug section, collapsed by default, has tools for checking your patterns.

**Mark instead of hide** (off by default) is a debug mode. Matching lines stay in the chatbox but are rewritten as `X` followed by the message, all in one colour (red by default, set with **Mark colour**), so you can see exactly what your patterns would remove before you trust them. Turn it off again and the same lines disappear.

**Right-click copy** (off by default) adds two entries to the right-click menu on any chatbox line, game messages included. "Copy raw text" puts the message exactly as the game stores it on your clipboard, colour tags and all. "Copy filter text" copies what your patterns actually see with the current settings: tags stripped, timestamp gone, sender name in front if that option is on. Paste that into the pattern list, escape anything special like `.` `(` `[` `*` `?` with a backslash, and it hides that exact message.

Timestamps are never part of what your patterns see. RuneLite's Timestamp plugin adds them in a separate step, and if some other plugin has already glued a `[12:34]` or `[1:05 PM]` onto the front of the text it is removed before matching. So `^` always means the start of the actual message, with or without timestamps turned on.

Changing any setting redraws the chatbox, so lines already on screen appear or disappear straight away. Turning the plugin off shows everything again. Nothing is deleted; hidden lines are still in the chat history and come back the moment the pattern is removed.

## Changelog

1.2.0: `$1` to `$9` in replacements (e.g. collapse repeated characters), and **Replace in names** to apply the rules to sender names.

1.1.0: Replacements list: replace or remove the matched text instead of hiding the whole line.

1.0.0: first release. Regex hiding for every chat type, sender matching, a mark-instead-of-hide debug mode, and right-click copy of raw or filter text.

## License

BSD 2-Clause, see LICENSE.
