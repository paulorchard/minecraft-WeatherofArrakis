package com.paulorchard.islandcraft.weatherofarrakis.client;

import com.paulorchard.islandcraft.weatherofarrakis.WeatherOfArrakis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;

/** Debug screenshots: /storm_debug shot [name], or automatically with -Dislandcraft.weather.autoshot=name once the storm look is at full strength for 2 s. */
public final class StormShot {
    private static final String AUTO = System.getProperty("islandcraft.weather.autoshot");
    private static long due = -1;
    private static String name = "storm";
    private static boolean wasHidden;
    private static boolean autoDone;
    private static long fullSince = -1;

    private StormShot() {}

    public static void request(String n, float seconds) {
        name = n;
        due = net.minecraft.util.Util.getMillis() + (long) (seconds * 1000);
    }

    public static void tick() {
        runAutoCommands();
        Minecraft mc = Minecraft.getInstance();
        long now = net.minecraft.util.Util.getMillis();
        if (AUTO != null && !autoDone && mc.level != null) {
            boolean full = ClientDebug.rainGradient >= 0.99f;
            if (full) {
                if (fullSince < 0) fullSince = now;
                if (now - fullSince > 2000 && due < 0) {
                    autoDone = true;
                    request(AUTO, 0);
                }
            } else fullSince = -1;
        }
        if (due > 0 && now >= due) {
            due = -1;
            wasHidden = mc.options.hideGui;
            // GUI stays visible so the overlay layer shows
            // one frame later the GUI is gone; grab on the next tick
            pendingGrab = 3;
        }
        if (pendingGrab > 0 && --pendingGrab == 0) {
            Screenshot.grab(mc.gameDirectory, name + ".png", mc.getMainRenderTarget(), 1,
                    msg -> WeatherOfArrakis.LOGGER.info("[storm-debug] screenshot: {} / {}", msg.getString(), mc.gameDirectory + "/screenshots/" + name + ".png"));
            
        }
    }

    private static int pendingGrab;

    /** Dev only: -Pautocmd="5:storm_sound foo;20:storm_sound stop" runs client or server commands N seconds after the player is in the world. */
    private static final String AUTOCMD = System.getProperty("islandcraft.weather.autocmd");
    private static long cmdStart = -1;
    private static int cmdNext;

    public static void runAutoCommands() {
        if (AUTOCMD == null) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) { cmdStart = -1; cmdNext = 0; return; }
        if (cmdStart < 0) cmdStart = net.minecraft.util.Util.getMillis();
        String[] items = AUTOCMD.split(";");
        while (cmdNext < items.length) {
            int colon = items[cmdNext].indexOf(':');
            double at = Double.parseDouble(items[cmdNext].substring(0, colon));
            if ((net.minecraft.util.Util.getMillis() - cmdStart) / 1000.0 < at) return;
            String cmd = items[cmdNext].substring(colon + 1).trim();
            WeatherOfArrakis.LOGGER.info("[storm-debug] autocmd: {}", cmd);
            mc.player.connection.sendCommand(cmd);
            cmdNext++;
        }
    }
}
