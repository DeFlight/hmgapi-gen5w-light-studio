package com.deflight.lightstudio.engine;

import android.os.IBinder;
import android.os.Parcel;

import com.deflight.lightstudio.Model;
import com.deflight.lightstudio.Pal;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Method;

/**
 * Light Studio's lamp engine: runs as root under app_process (svc/engine.sh), reads the
 * owner's choices once a second (the screen saves them to HMG-API's settings file,
 * /data/hmgapi/settings/gen5w-light-studio.json) and drives the ambient light: a See
 * preview, today's My Date or holiday, or the Everyday light, for the car's profile.
 *
 * A few lines (started, what it shows, errors) go to the service's log (Log).
 *
 * The car's calls, from its own vehicle settings app and automotive framework
 * (the numbers are the Ioniq 6 US 260507's; on the car each is looked up by name, lookUp):
 *   com.hkmc.micom.system  com.hkmc.micom.ISystemService
 *     76 requestSMLStatus()            the light reports itself in the log:
 *                                      SYS_SML_STATUS_M_RESP: rH rL gH gL bH bL bright dim drivemode
 *     77 setSMLConfiguration(mode,v)   5 brightness (level + 1), 8 dimmed in dark, 9 drive-mode colour (1 off, 2 on)
 *     78 setSMLRGB(byte[2] r,g,b)      the upper colour, each {value >> 8, value & 255}
 *   com.hkmc.micom.usm     com.hkmc.usm.IUSMService
 *     0x130 setDualLightALL(2, r, g, b)   the lower colour (the settings app: "nMLMBottomRed ...")
 *   The light takes the 64 colours of the car's own wheel as they are, and the car's presets
 *   (Polar White, Moon White, Orchid Green...) with 256 added to each value, as the settings
 *   app sends them. Any other colour is ignored by the car (tested: pure white, green, amber).
 *     0x12f setDualLight_Color(c)         2 custom dual colours, 1 one colour
 *     0x12e setDualLight_Speed(v)         brightness follows speed (the settings app's "usm_light_006")
 *     0x1c1 getDualLight_Color, 0x1c0 getDualLight_Speed, 0x1bd/0x1be/0x1bf getDualLight_BottomRed/Green/Blue
 *
 * Before it first changes the light it keeps the owner's settings in saved.txt (in the
 * service's folder); "restore" (Light Studio turned off, or the mod stopped) sends them back
 * and deletes the file. A saved.txt left over (the car switched off while Light Studio was on)
 * is kept, so the owner's own settings, not Light Studio's, are what come back.
 */
public final class Engine {
    static final int[] PAL = Pal.RGB;   // the screen's order (its stored indices)
    static final String MOD = "gen5w-light-studio";

    static final String SYS = "com.hkmc.micom.system", SYS_D = "com.hkmc.micom.ISystemService";
    static final String USM = "com.hkmc.micom.usm", USM_D = "com.hkmc.usm.IUSMService";

    // ---- the car's call numbers, looked up by name on the car at start (lookUp) ----
    // A number is a method's place in its interface, so another car's software can number the
    // same method differently. Each is read from the car's own Stub (TRANSACTION_<name>) and its
    // method checked for the arguments sent; nothing is ever sent by a guessed number.
    static int SML_STATUS = -1, SML_CONFIG = -1, SML_RGB = -1;               // the upper light: required
    static int DL_SPEED = -1, DL_COLOR = -1, DL_ALL = -1;                    // the lower light and its options: each optional
    static int DL_R = -1, DL_G = -1, DL_B = -1, DL_GSPEED = -1, DL_GCOLOR = -1, DL_GMODE = -1;

    /**
     * "Car has one light" (the screen's switch, from 1.0.2): no USM call is sent, only the upper
     * light's, as Light Vibes does on the EV6. restore() still puts the owner's USM settings back.
     */
    static boolean oneLight, restoring;

    /** the car's numbers for every call; false (and the lights left alone) when the upper light's aren't all there */
    static boolean lookUp() {
        SML_STATUS = code(SYS_D, "requestSMLStatus", 76, void.class);
        SML_CONFIG = code(SYS_D, "setSMLConfiguration", 77, void.class, int.class, int.class);
        SML_RGB    = code(SYS_D, "setSMLRGB", 78, void.class, byte[].class, byte[].class, byte[].class);
        DL_SPEED   = code(USM_D, "setDualLight_Speed", 0x12e, void.class, int.class);
        DL_COLOR   = code(USM_D, "setDualLight_Color", 0x12f, void.class, int.class);
        DL_ALL     = code(USM_D, "setDualLightALL", 0x130, void.class, int.class, int.class, int.class, int.class);
        DL_R       = code(USM_D, "getDualLight_BottomRed", 0x1bd, int.class);
        DL_G       = code(USM_D, "getDualLight_BottomGreen", 0x1be, int.class);
        DL_B       = code(USM_D, "getDualLight_BottomBlue", 0x1bf, int.class);
        DL_GSPEED  = code(USM_D, "getDualLight_Speed", 0x1c0, int.class);
        DL_GCOLOR  = code(USM_D, "getDualLight_Color", 0x1c1, int.class);
        DL_GMODE   = code(USM_D, "getDualLight_Mode", 0x1c2, int.class);
        if (SML_STATUS < 0 || SML_CONFIG < 0 || SML_RGB < 0) {
            log("this car's software doesn't have the upper light's calls as Light Studio knows them: leaving the lights alone");
            return false;
        }
        if (DL_ALL < 0 || DL_COLOR < 0) log("no lower light calls on this car: upper light only");
        log("the car's dual light: mode " + usmGet(DL_GMODE) + ", color " + usmGet(DL_GCOLOR) + ", speed " + usmGet(DL_GSPEED)
            + ", lower (" + usmGet(DL_R) + "," + usmGet(DL_G) + "," + usmGet(DL_B) + ")");
        return true;
    }

    /** a call's number on this car, after checking the method takes what Light Studio sends; -1 if not */
    static int code(String iface, String name, int ioniq6, Class<?> ret, Class<?>... args) {
        try {
            Method m = Class.forName(iface).getMethod(name, args);
            if (m.getReturnType() != ret) { log("call " + name + ": returns " + m.getReturnType().getSimpleName() + ", not " + ret.getSimpleName() + ": not used"); return -1; }
            java.lang.reflect.Field f = Class.forName(iface + "$Stub").getDeclaredField("TRANSACTION_" + name);
            f.setAccessible(true);
            int c = f.getInt(null);
            log("call " + name + " = " + c + (c == ioniq6 ? "" : " (Ioniq 6: " + ioniq6 + ")"));
            return c;
        } catch (NoSuchMethodException e) {
            log("call " + name + ": not on this car with the arguments Light Studio sends: not used");
        } catch (Throwable e) {
            log("call " + name + ": not found (" + e.getClass().getSimpleName() + "): not used");
        }
        return -1;
    }

    static File saved() {
        String d = System.getenv("HMGAPI_DATA");
        return new File(d != null ? d : "/data/local/tmp", "saved.txt");
    }

    static void log(String s) { Log.line(s); }

    public static void main(String[] args) {
        String cmd = args.length > 0 ? args[0] : "run";
        Log.open(cmd);
        try {
            if ("restore".equals(cmd)) { restore(); return; }
            run();
        } catch (Throwable t) {
            log("engine stopped on an error: " + Log.trace(t));
            System.exit(1);
        }
    }

    // ---- the owner's choices ----
    /** what to show now: off (the car's own), or a light, and why (for the log) */
    /** one light (upper or lower): its two colours (screen indices; 64 White, 65 Off), style, speed; off: Rainbow's start */
    static final class Zone {
        int c1, c2, off; String style, speed;
        Zone(int c1, int c2, String style, String speed, int off) { this.c1 = c1; this.c2 = c2; this.style = style; this.speed = speed; this.off = off; }
        public String toString() {
            return "rainbow".equals(style) ? "rainbow " + speed + (off != 0 ? " (half way round)" : "")
                : style + " " + name(c1) + " " + rgb(PAL[c1]) + ("static".equals(style) || "pulse".equals(style) ? "" : " / " + name(c2) + " " + rgb(PAL[c2])) + ("static".equals(style) ? "" : " " + speed);
        }
    }

    /** what to show: the upper light, the lower light (null: the owner's usual colour), one brightness */
    static final class Want {
        boolean on = true, dimDark = true, speedSync; Zone up, low; int bright = 7; String why = "";
        String key() { return on ? up + " | " + low + " | " + bright + " dim " + dimDark + " speed " + speedSync + " (" + why + ")" : "off"; }
        String text() { return "upper: " + up + "; lower: " + (low == null ? "the usual color" : low.toString()) + "; brightness " + bright + " (" + why + ")"; }
    }

    static final String[] PNAME = { "Guest", "Profile 1", "Profile 2" };

    static File settingsFile() { return new File(System.getProperty("ls.settings", "/data/hmgapi/settings/" + MOD + ".json")); }
    static long settingsStamp = -1;
    static String settingsText = "{}";

    /** the screen's settings: HMG-API's file for this mod (read only when it changes) */
    static JSONObject settings() throws Exception {
        File f = settingsFile();
        String now = f.exists() ? read(f) : "{}";
        if (!now.equals(settingsText) || settingsStamp < 0) { settingsStamp = 0; settingsText = now; }
        return new JSONObject(settingsText.trim().length() == 0 ? "{}" : settingsText);
    }

    static int col(JSONObject x, String k, int d) { int c = x.optInt(k, d); return c > Pal.LAST || c < 0 ? 36 : c; }
    static String style(String s) { return "fade".equals(s) ? "alt" : s; }

    /**
     * An item from the screen's JSON (Studio.toJson): the upper light (c1, c2, mode, speed) and
     * the lower light by lmode: same as the upper, opposite (colours flipped; Rainbow half way
     * round), own (lc1, lc2, lstyle, lspeed), or car (the owner's usual colour).
     */
    static void light(Want w, JSONObject x) {
        String mode = style(x.optString("mode", "static")), speed = x.optString("speed", "med");
        int c1 = col(x, "c1", 44), c2 = col(x, "c2", 54);
        w.up = new Zone(c1, c2, mode, speed, 0);
        String lm = x.optString("lmode", "");
        if (lm.length() == 0) {   // saved by 0.4.x
            String z = x.optString("zones", "both");
            lm = "static".equals(mode) ? "opp" : "same";
        }
        if ("own".equals(lm)) w.low = new Zone(col(x, "lc1", c2), col(x, "lc2", c1), style(x.optString("lstyle", mode)), x.optString("lspeed", speed), 0);
        else if ("same".equals(lm)) w.low = new Zone(c1, c2, mode, speed, 0);
        else w.low = new Zone(c2, c1, mode, speed, 32);
        w.bright = clamp(x.optInt("bright", 7), 0, 10);   // 0: the lights off
        if (x.has("dimDark")) w.dimDark = x.optBoolean("dimDark", true);         // per item; settings from before kept the defaults
        if (x.has("speedSync")) w.speedSync = x.optBoolean("speedSync", false);
    }

    static int[] dateOf(String id, JSONObject x, int y) {
        if ("eas".equals(id)) return Model.easter(y);
        if ("thx".equals(id)) return Model.nthWeekday(y, 11, java.util.Calendar.THURSDAY, 4);
        return new int[] { x.optInt("m", 0), x.optInt("d", 0) };
    }

    static final String[] CUSTOM = { "u1", "u2", "u3" };

    /**
     * Light Studio off: the car's own. Else, for the car's profile: a See preview (30 s), else
     * today's My Date or holiday (unless the owner chose Back to everyday lights today), else Everyday.
     * The same rules as the screen (Studio.act).
     */
    static Want want() {
        Want w = new Want();
        try {
            JSONObject o = settings();
            w.on = !"0".equals(o.optString("on", "1"));
            boolean one = "1".equals(o.optString("oneLight", "0"));
            if (one != oneLight) {
                oneLight = one;
                log(one ? "Car has one light: on, no USM calls" : "Car has one light: off");
                if (one) usmBack();
            }
            if (!w.on) return w;
            w.dimDark = !"0".equals(o.optString("dimDark", "1"));
            w.speedSync = "1".equals(o.optString("speedSync", "0"));
            JSONObject cfg = new JSONObject(o.optString("config", "{}"));
            int prof = carProfile();
            String pn = PNAME[prof];
            String[] see = o.optString("see", "").split(":");
            if (see.length == 3 && System.currentTimeMillis() < num64(see[2])) {   // a preview, of any profile's item
                JSONObject sp = cfg.optJSONObject("p" + num(see[0], 1));
                JSONObject x = sp == null ? null : "every".equals(see[1]) ? sp.optJSONObject("every") : sp.optJSONObject("items") == null ? null : sp.optJSONObject("items").optJSONObject(see[1]);
                if (x != null) { light(w, x); w.why = "See " + x.optString("name", see[1]); return w; }
            }
            JSONObject p = cfg.optJSONObject("p" + prof);
            if (p == null) { log("no lights saved for " + pn + " yet"); return null; }
            JSONObject every = p.optJSONObject("every");
            if (every == null) every = new JSONObject();
            java.util.Calendar c = java.util.Calendar.getInstance();
            int y = c.get(java.util.Calendar.YEAR), m = c.get(java.util.Calendar.MONTH) + 1, d = c.get(java.util.Calendar.DAY_OF_MONTH);
            String today = y + "-" + m + "-" + d;
            JSONObject it = p.optJSONObject("items");
            JSONObject found = null; String fid = null;
            if (p.optBoolean("master", true) && it != null) {
                java.util.Iterator<String> ks = it.keys();
                while (ks.hasNext()) {
                    String id = ks.next();
                    JSONObject x = it.optJSONObject(id);
                    if (x == null || !x.optBoolean("on", false)) continue;
                    boolean custom = java.util.Arrays.asList(CUSTOM).contains(id);
                    int[] dt = dateOf(id, x, y);
                    if (dt[0] != m || dt[1] != d) continue;
                    if (custom) { found = x; fid = id; break; }   // My Dates win
                    if (found == null) { found = x; fid = id; }
                }
            }
            if (found != null && !today.equals(p.optString("skip", ""))) {
                light(w, found); w.why = pn + ", today is " + found.optString("name", fid); return w;
            }
            light(w, every);
            w.why = pn + ", everyday" + (found != null ? " (" + found.optString("name", fid) + " skipped today)" : "");
        } catch (Exception e) {
            log("settings: " + Log.trace(e));
            return null;
        }
        return w;
    }

    static long num64(String s) { try { return Long.parseLong(s.trim()); } catch (Exception e) { return 0; } }

    /** the car's profile: 0 Guest, 1, 2 (com.hkmc.userprofile getUserId); 1 if the car doesn't say */
    static IBinder profB;
    static int lastProfRaw = -98;
    static int carProfile() {
        try {
            if (profB == null || !profB.isBinderAlive()) profB = binder("com.hkmc.userprofile");
            int u = call(profB, "com.hkmc.userprofile.IUserProfileService", 4, true);
            lastProfRaw = u;
            return u >= 0 && u <= 2 ? u : 1;
        } catch (Exception e) {
            if (lastProfRaw != -99) { log("car profile: can't read it (" + e + "), using Profile 1"); lastProfRaw = -99; }
            return 1;
        }
    }

    static int num(String s, int d) { try { return Integer.parseInt(s.trim()); } catch (Exception e) { return d; } }
    static int clamp(int v, int lo, int hi) { return v < lo ? lo : v > hi ? hi : v; }

    // ---- the loop ----
    static int lastTop = -2, lastBottom = -2, lastBright = -1;   // RGB sent last; -2: nothing yet
    static long lastAll;
    static boolean lastSingle;

    static void run() throws Exception {
        log("Light Studio engine started");
        if (!lookUp()) { while (true) Thread.sleep(3600000); }   // stays up, touches nothing
        Want w = null; String wk = "";
        long nextRead = 0, t0 = System.currentTimeMillis(), lastFrame = 0;
        boolean holding = false;   // take() runs once at start: it keeps a saved.txt left from before and sends the links off again
        if (saved().exists()) log("found the owner's saved light settings from before: keeping them");
        while (true) {
            long now = System.currentTimeMillis();
            if (now >= nextRead) {
                Want n = want();
                if (n != null) {
                    if (!n.key().equals(wk)) {
                        log("now showing: " + (n.on ? n.text() : "Light Studio is off: the car's own settings"));
                        t0 = now; lastAll = 0;
                    }
                    w = n; wk = n.key();
                }
                nextRead = now + 1000;
            }
            if (w != null && !w.on) {
                if (saved().exists()) restore();
                holding = false;
                lastTop = lastBottom = -2; lastBright = -1;
            } else if (w != null) {
                if (!holding || !saved().exists()) { take(); holding = true; lastAll = 0; }
                if (now - lastFrame >= 200) { frame(w, now - t0, now); lastFrame = now; }
            }
            Thread.sleep(100);
        }
    }

    static int period(String speed) { return "slow".equals(speed) ? 6000 : "fast".equals(speed) ? 1200 : 3000; }

    /** Rainbow: once around the 64 colours */
    static int rainbowPeriod(String speed) { return "slow".equals(speed) ? 60000 : "fast".equals(speed) ? 6000 : 20000; }

    static int mix(int a, int b, double f) {
        int ar = a >> 16 & 255, ag = a >> 8 & 255, ab = a & 255;
        return (int) Math.round(ar + ((b >> 16 & 255) - ar) * f) << 16 | (int) Math.round(ag + ((b >> 8 & 255) - ag) * f) << 8 | (int) Math.round(ab + ((b & 255) - ab) * f);
    }

    /** 0..1..0 over one period */
    static double tri(long t, int p) { double f = (t % p) / (double) p; return f < 0.5 ? f * 2 : 2 - f * 2; }

    /**
     * One light's colour (RGB) at time t, the same as the screen's preview: Static Color 1; Take
     * turns switches each half period; Fade blends straight between the two (RGB in between);
     * Pulse Color 1; Pulse both switches at the dim point; Rainbow walks the 64 colours in order.
     */
    static int colorAt(Zone z, long t) {
        int p = period(z.speed);
        if ("alt".equals(z.style)) return PAL[(t / (p / 2)) % 2 == 1 ? z.c2 : z.c1];
        if ("pulse2".equals(z.style)) return PAL[((t + p / 2) / p) % 2 == 1 ? z.c2 : z.c1];
        if ("rainbow".equals(z.style)) { int rp = rainbowPeriod(z.speed); return PAL[Pal.ORDER[(int) (((t % rp) * 64L / rp + z.off) % 64)]]; }
        return PAL[z.c1];
    }

    static boolean pulses(Zone z) { return z != null && ("pulse".equals(z.style) || "pulse2".equals(z.style)); }

    /**
     * One step: each light by its own settings, the lower one the owner's usual colour when
     * picked so. Brightness is one setting for the whole cabin (the car has only one): it
     * follows a pulsing light (the upper first), dimming from the owner's level to 1 and back.
     * Only what changed is sent; everything again every 10 s.
     */
    static void frame(Want w, long t, long now) {
        int top = colorAt(w.up, t);
        int bottom = w.low == null ? ownerBottom() : colorAt(w.low, t);
        Zone pz = pulses(w.up) ? w.up : pulses(w.low) ? w.low : null;
        int bright = pz == null ? w.bright : (int) Math.round(w.bright * (1 - tri(t, period(pz.speed))));   // Pulse: down to 0 (off) and back
        boolean single = bottom == top && presetRgb(top);   // a preset (White, Orchid Green...) on both lights: single-colour mode
        boolean all = now - lastAll > 10000 || single != lastSingle;
        if (all) lastAll = now;
        lastSingle = single;
        try {
            if (all) {   // the car can put its links back on its own
                config(9, 1); usm(DL_SPEED, w.speedSync ? 2 : 1); usm(DL_COLOR, single ? 1 : 2); config(8, w.dimDark ? 2 : 1);
            }
            if (all || top != lastTop) { top(top); lastTop = top; }
            if (bottom >= 0 && (all || bottom != lastBottom)) {   // sent in one-colour mode too, as the car does
                bottom(bottom); lastBottom = bottom;
            }
            // Brightness with speed: the car sets the level; only Off, back from Off, the first frame and Pulse are sent
            boolean sendBright = !w.speedSync ? all || bright != lastBright
                : bright != lastBright && (pz != null || lastBright <= 0 || bright == 0);
            if (sendBright) { config(5, bright + 1); lastBright = bright; }
        } catch (Exception e) {
            log("lamp call failed: " + Log.trace(e));
            lastAll = 0;
        }
    }

    /** keep the owner's settings (once), then take the light: one-colour links off, dual colours on */
    static void take() throws Exception {
        if (!saved().exists()) {
            int[] o = readOwner();
            StringBuilder s = new StringBuilder();
            for (int v : o) s.append(v).append(' ');
            write(saved(), s.toString().trim());
            log("kept the car's own light settings, to put back later");
        }
        config(9, 1);              // drive-mode colour off: custom colours only show with it off
        usm(DL_SPEED, 1);             // brightness by speed off
        usm(DL_COLOR, 2);             // two colours
    }

    /** Car has one light turned on: the owner's USM settings back, as kept (only those) */
    static void usmBack() {
        int[] o = owner();
        if (o == null || o.length < 11) return;
        restoring = true;
        try {
            if (o[6] == 2 && o[8] >= 0) usm(DL_ALL, 2, o[8], o[9], o[10]);
            if (o[6] >= 0) usm(DL_COLOR, o[6]);
            if (o[7] >= 0) usm(DL_SPEED, o[7]);
            log("put the car's own dual light settings back (color " + o[6] + ", speed " + o[7] + ")");
        } catch (Exception e) {
            log("dual light settings back: " + e);
        } finally {
            restoring = false;
        }
    }

    /**
     * the owner's light: { r, g, b, bright raw, dim, drive mode, usm colour, usm speed, bottom r, g, b },
     * -1 for what the car didn't say
     */
    static int[] readOwner() throws Exception {
        int[] o = new int[11];
        java.util.Arrays.fill(o, -1);
        sys(SML_STATUS);
        Thread.sleep(500);
        int[] st = lastStatus();
        if (st != null) {
            o[0] = st[1]; o[1] = st[3]; o[2] = st[5]; o[3] = st[6]; o[4] = st[7]; o[5] = st[8];
        }
        o[6] = usmGet(DL_GCOLOR); o[7] = usmGet(DL_GSPEED);
        o[8] = usmGet(DL_R); o[9] = usmGet(DL_G); o[10] = usmGet(DL_B);
        return o;
    }

    /** the owner's own upper / lower colour, from what take() kept (-1: not known) */
    static int ownerTop() { int[] o = owner(); return o == null || o[0] < 0 ? -1 : o[0] << 16 | o[1] << 8 | o[2]; }
    static int ownerBottom() { int[] o = owner(); return o == null || o.length < 11 || o[8] < 0 ? -1 : o[8] << 16 | o[9] << 8 | o[10]; }
    static int[] owner() {
        try {
            String[] f = read(saved()).trim().split(" +");
            int[] o = new int[f.length];
            for (int i = 0; i < f.length; i++) o[i] = Integer.parseInt(f[i]);
            return o;
        } catch (Exception e) {
            return null;
        }
    }

    static final String[] PRESET = { "Polar White", "Moon White", "Orchid Green", "Jade Green", "Ice Blue", "Ocean Blue" };
    static String name(int i) { return Pal.preset(i) ? PRESET[i - 64] : "#" + i; }

    /** a preset's code (not on the wheel)? */
    static boolean presetRgb(int rgb) { for (int i = 64; i <= Pal.LAST; i++) if (PAL[i] == rgb) return true; return false; }

    static String rgb(int c) { return "(" + (c >> 16 & 255) + "," + (c >> 8 & 255) + "," + (c & 255) + ")"; }

    static void restore() throws Exception {
        restoring = true;   // the owner's USM settings go back even with Car has one light on
        try { restoreKept(); } finally { restoring = false; }
    }

    static void restoreKept() throws Exception {
        File f = saved();
        if (!f.exists()) { log("restore: nothing kept, nothing to put back"); return; }
        if (!lookUp()) return;
        String[] p = read(f).trim().split("\\s+");
        int[] o = new int[p.length];
        for (int i = 0; i < p.length; i++) o[i] = num(p[i], -1);
        if (o.length >= 11) {
            config(9, 1);
            if (o[0] >= 0) { int c = o[0] << 16 | o[1] << 8 | o[2]; top(c); }   // a preset goes back with its +256
            if (o[6] == 2 && o[8] >= 0) usm(DL_ALL, 2, o[8], o[9], o[10]);
            if (o[6] >= 0) usm(DL_COLOR, o[6]);
            if (o[7] >= 0) usm(DL_SPEED, o[7]);
            if (o[3] >= 1) config(5, o[3]);
            if (o[4] >= 1) config(8, o[4]);
            if (o[5] >= 1) config(9, o[5]);
        }
        f.delete();
        log("put the car's own light settings back");
    }

    // ---- the car's calls ----
    /**
     * The car's presets (Polar White, Moon White, Orchid Green...) go with 256 added to each value, as the
     * car's own settings send them (its preset table holds RGB + 0x100; setSMLRGB takes each value as
     * two bytes, high then low). The 64 wheel colours go as they are.
     */
    static int flag(int rgb) { return presetRgb(rgb) ? 256 : 0; }

    static void top(int rgb) throws Exception {
        int f = flag(rgb) >> 8;
        sys(SML_RGB, new byte[] { (byte) f, (byte) (rgb >> 16) }, new byte[] { (byte) f, (byte) (rgb >> 8) }, new byte[] { (byte) f, (byte) rgb });
    }

    static void bottom(int rgb) throws Exception { int f = flag(rgb); usm(DL_ALL, 2, (rgb >> 16 & 255) + f, (rgb >> 8 & 255) + f, (rgb & 255) + f); }

    static void config(int mode, int v) throws Exception { sys(SML_CONFIG, mode, v); }

    static IBinder sysB, usmB;
    static Method getService;

    static IBinder binder(String name) throws Exception {
        if (getService == null) getService = Class.forName("android.os.ServiceManager").getMethod("getService", String.class);
        return (IBinder) getService.invoke(null, name);
    }

    static void sys(int code, Object... args) throws Exception {
        if (code < 0) throw new IOException("call not on this car");
        if (sysB == null || !sysB.isBinderAlive()) sysB = binder(SYS);
        call(sysB, SYS_D, code, false, args);
    }

    static void usm(int code, Object... args) throws Exception {
        if (code < 0) return;   // not on this car
        if (oneLight && !restoring) return;   // Car has one light: no USM calls
        if (usmB == null || !usmB.isBinderAlive()) usmB = binder(USM);
        call(usmB, USM_D, code, false, args);
    }

    static int usmGet(int code) {
        if (code < 0) return -1;   // not on this car
        try {
            if (usmB == null || !usmB.isBinderAlive()) usmB = binder(USM);
            return call(usmB, USM_D, code, true);
        } catch (Exception e) {
            log("usm read 0x" + Integer.toHexString(code) + ": " + e);
            return -1;
        }
    }

    static int call(IBinder b, String descriptor, int code, boolean reads, Object... args) throws Exception {
        if (b == null) throw new IOException(descriptor + " isn't running");
        Parcel d = Parcel.obtain(), r = Parcel.obtain();
        try {
            d.writeInterfaceToken(descriptor);
            for (Object a : args) {
                if (a instanceof byte[]) d.writeByteArray((byte[]) a);
                else d.writeInt((Integer) a);
            }
            b.transact(code, d, r, 0);
            r.readException();
            return reads ? r.readInt() : 0;
        } finally {
            d.recycle();
            r.recycle();
        }
    }

    /** the light's last report in the log: 9 values (rH rL gH gL bH bL bright dim drive), or null */
    static int[] lastStatus() {
        String last = null; int lines = 0;
        try {
            Process p = new ProcessBuilder("logcat", "-d", "-v", "brief", "-s", "SystemService:D").redirectErrorStream(true).start();
            BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
            for (String l; (l = in.readLine()) != null;) { lines++; if (l.contains("SYS_SML_STATUS_M_RESP:") && !l.contains("handleMessage")) last = l; }
            p.waitFor();
        } catch (Exception e) {
        }
        if (last == null) return null;
        String[] v = last.substring(last.indexOf("SYS_SML_STATUS_M_RESP:") + 22).trim().split("\\s+");
        if (v.length < 9) return null;
        int[] out = new int[9];
        for (int i = 0; i < 9; i++) out[i] = Integer.parseInt(v[i].replace("0x", ""), 16);
        return out;
    }

    // ---- files ----
    static void write(File f, String s) throws IOException {
        FileOutputStream o = new FileOutputStream(f);
        try { o.write(s.getBytes("UTF-8")); o.getFD().sync(); } finally { o.close(); }
    }

    static String read(File f) throws IOException {
        FileInputStream in = new FileInputStream(f);
        try {
            ByteArrayOutputStream b = new ByteArrayOutputStream();
            byte[] buf = new byte[256];
            for (int n; (n = in.read(buf)) > 0;) b.write(buf, 0, n);
            return b.toString("UTF-8");
        } finally { in.close(); }
    }


}
