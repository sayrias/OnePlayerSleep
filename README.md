# OneSleepPlus

OneSleepPlus is a Bukkit-compatible **One Player Sleep / Single Player Sleep** plugin focused on Minecraft 26.x while retaining
compatibility with older Spigot/Paper releases. A world can require either a fixed number of
sleepers or a percentage of its eligible players.

## Threshold safety

`sleep.threshold` accepts `2`, `"50%"`, or `"%50"`. Fixed thresholds are always clamped to the
eligible population. If a world has three eligible players and the configured value is five,
three sleepers are required; when more players join, the requirement automatically moves back
toward five. Percentage results support `CEIL`, `FLOOR`, and `NEAREST` rounding and are always
clamped between one and the eligible population.

Players are counted per world. Spectators and players with `onesleepplus.ignore` are excluded.
Creative players can be included or excluded in `config.yml`.

## Platforms and integrations

- CraftBukkit, Spigot, Paper, and Paper-derived servers
- Minecraft 26.x is the primary target; the shared API surface is compiled for 1.16.5+
- Folia-aware global, region, and entity scheduling
- Optional PlaceholderAPI expansion
- Optional Geyser/Floodgate detection and simplified Bedrock action-bar messages

Geyser and Floodgate are not required. Bedrock players still participate as normal Bukkit players
when the integrations are absent or installed only on a proxy.

## Commands

```text
/onesleepplus status [world]
/onesleepplus set <number|percentage> [world]
/onesleepplus reload
/onesleepplus test
```

The search-friendly aliases `/onesleep`, `/oneplayersleep`, `/singleplayersleep`, and `/opsleep`
run the same command. The main permissions are `onesleepplus.admin`, `onesleepplus.status`,
`onesleepplus.reload`, `onesleepplus.set`, `onesleepplus.test`, and `onesleepplus.ignore`.

## Placeholders

```text
%onesleepplus_sleeping%
%onesleepplus_required%
%onesleepplus_remaining%
%onesleepplus_eligible%
%onesleepplus_threshold%
%onesleepplus_progress%
%onesleepplus_countdown%
%onesleepplus_world_enabled%
```

The legacy `%oneplayersleep_*%` identifier is also registered for search-friendly compatibility.

## Furnace catch-up

When a night is skipped, active furnaces, smokers, and blast furnaces in already loaded chunks can
advance by the number of ticks that were actually skipped. The simulation respects recipes, fuel,
output capacity, Bukkit furnace events, experience, and configurable safety caps. Unloaded chunks
are never loaded. Unknown custom fuels are not guessed; add their burn duration under
`furnace-catch-up.custom-fuel-burn-times`.

## Languages

English is the default language. On first start, `lang/messages_en.yml` and
`lang/messages_tr.yml` are created inside the plugin data folder. Set `language: "tr"` in
`config.yml` and run `/osp reload` to switch to Turkish. Additional languages can be added as
`lang/messages_<code>.yml`; missing keys safely fall back to English.

## Developer API

Retrieve `OneSleepPlusApi` (or the descriptive `OnePlayerSleepApi` alias) from Bukkit's
`ServicesManager`. The plugin also exposes
`SleepProgressEvent`, cancellable `SleepThresholdReachedEvent`, cancellable `NightSkipEvent`, and
`NightSkippedEvent`.

## Build

```bash
./gradlew clean test build
```

The resulting plugin is written to `build/libs/OneSleepPlus-1.0.0.jar`.

## License

OneSleepPlus is distributed under the proprietary
`OneSleepPlus Free Use License 1.0`. Unmodified official builds may be used on
personal or commercial Minecraft servers, but redistribution, resale, public
modified builds, and bundling require prior written permission. See `LICENSE`
for the complete terms. The license is also included in release JARs at
`META-INF/LICENSE`.
