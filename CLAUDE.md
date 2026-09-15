<!-- managed by ~/homelab/bin/claude-md; hand-written notes go below the line -->
# ChatRegexFilter

RuneLite plugin that hides chatbox lines matching regular expressions you write. It runs on every line the chatbox draws, whatever the source: public, private, clan, friends chat, group ironman chat, game messages, broadcasts, examines, trade and login notifications, all of it.

## Run
- `./gradlew run` launches a RuneLite client with the plugin loaded; `./gradlew build` must pass before any hub PR (the hub builds your exact commit).

## Test
- `./gradlew test`

## Where things live
- Git: https://github.com/1504681/ChatRegexFilter.git (PUBLIC).

## Rules for this repo
- RuneLite plugin. Process, hub submission and the auto-merge update path: ~/homelab/runbooks/runelite-plugin.md.
- Keep README and Plugin Hub PR bodies short: what it does, settings, changelog. No gradle/run boilerplate in the README.
- Keep `runelite-plugin.properties` and `runelite_plugin.json` in sync; bump the version constant + changelog with every hub update.
- Verify RuneLite API symbols offline with `javap -cp ~/.gradle/caches/modules-2/files-2.1/net.runelite/<jar>` before coding against them.

## Conventions
Box-wide conventions and the port map: ~/homelab (BOX.md is imported into every session). Registry: add/edit services.toml then bin/apply. No Claude attribution or session links in anything pushed.

<!-- hand-written notes below this line are preserved by bin/claude-md -->
---
