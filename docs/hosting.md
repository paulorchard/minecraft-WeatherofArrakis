# Hosting setup

The mod never restarts the server. A host's scheduler runs the Major storm and then restarts the server.

For a daily restart at time T:

1. At T: run `coriolis major 10` from the console (or rcon). The Major Coriolis storm is announced and hits in 10 minutes; the last 5 minutes use the staged sky and chat warnings.
2. At T + 10 min the storm arrives; it rages for `major_storm_seconds` (default 120), then everyone is disconnected ("The Coriolis storm has swept you from Arrakis.") and logins are refused.
3. While the world is empty, abandoned altered chunks that nobody has visited for `reset_after_major_storms` (3) Major storms are reset, within `major_reset_window_seconds` (180).
4. At T + 15 minutes: restart the server.

If no restart comes, logins reopen `major_login_reopen_seconds` (600) after the disconnect. The refusal flag lives only in memory, so a restart always clears it.

`coriolis major cancel` calls the storm off, only before players are disconnected.

Test runs: set `major_storm_disconnects = false` to keep players online. Chunks that are loaded (a player is near) are never reset.

Server setting for headless tests: `pause-when-empty-seconds=-1` in `server.properties`, otherwise an empty server stops ticking after 60 s and no phase advances.
