# BattleTracker

A standalone plugin that tracks PVP & PVE statistics along with a suite of combat features to enhance server gameplay.

# Features
## PVP and PVE Records
One of the core features of BattleTracker is its robust tracking system for PVP and PVE statistics. BattleTracker tracks a variety of statistics including kills, deaths, killstreaks, and more. These statistics are saved in MySQL or SQLite, allowing them to be shared across Minecraft servers, or accessed by web applications.

## Leaderboards
Another feature BattleTracker includes is a leaderboard system that allows players to view the top players on the server based on various statistics.

## Death Messages
BattleTracker provides customizable death messages that can be displayed to players when they are killed in PVP or PVE combat. These are easily configurable for each tracker type, and can be customized to fit the theme of your server.

## Damage Recap
BattleTracker also includes a damage recap feature that displays a summary of the damage dealt and received by a player during combat. This can be summarized in multiple ways, such as which item dealt the most damage, a breakdown of players that dealt damage, or all the types of damage a player received.

## Combat Logging
BattleTracker includes a combat logging feature that places players "in combat" when they attack another player. If they log out, they will be killed and their attacker will receive credit for the kill.

## Damage Indicators
Inside BattleTracker, there is also support for damage indicators, which show to a player how much damage they inflicted on another player or entity.

# PlaceholderAPI
BattleTracker registers the PlaceholderAPI identifier `bt`. Placeholders use the tracker name as the first segment. The default trackers are `pvp` and `pve`.

## Player Stats
- `%bt_<tracker>_<stat>%`

Examples:
- `%bt_pvp_kills%`
- `%bt_pve_deaths%`
- `%bt_pvp_max_streak%`

## Leaderboards
- `%bt_<tracker>_top_<stat>_<place>%` - value for the player in that leaderboard position
- `%bt_<tracker>_top_<stat>_<place>_name%` - player name for that leaderboard position
- `%bt_<tracker>_top_<stat>_<place>_uuid%` - player UUID for that leaderboard position

Examples:
- `%bt_pvp_top_kills_1%`
- `%bt_pvp_top_kills_1_name%`
- `%bt_pve_top_deaths_10_uuid%`

## Built-in Stats
- `kills`
- `deaths`
- `ties`
- `streak`
- `max_streak`
- `ranking`
- `max_ranking`
- `rating`
- `max_rating`
- `kd_ratio`
- `max_kd_ratio`

# Links
- Website: https://www.battleplugins.org
- Download: https://modrinth.com/plugin/battletracker
- Discord: [BattlePlugins Discord](https://discord.com/invite/J3Hjjb8)
