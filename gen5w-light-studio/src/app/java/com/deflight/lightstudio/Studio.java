package com.deflight.lightstudio;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Light Studio's screen, laid out like the approved mockup (Light Studio mockup, 1920 x 720):
 * every size here is the mockup's, in its pixels, scaled to the screen's width (px()), so on the
 * car's 1920 x 720 display it's the same picture. Colours and the profile pop-out are Button
 * Mapper's. This build is the screen only: the choices are kept in this app (per car profile)
 * and nothing is sent to the car yet.
 */
public final class Studio extends Activity {
    // ---- Button Mapper's colours (the mockup's night and day tokens) ----
    int BG_TOP, BG_BOTTOM, CARD, CARD_PRESSED, CARD_EDGE, ACCENT, ACCENT_PRESSED, ACCENT_DIM, ON_ACCENT,
        TEXT, TEXT_2, TEXT_3, CHIP, PRESS, FIELD, PANEL, DIM, BAD, LINE, WARN, WARN_DIM;

    void palette(boolean night) {
        if (night) {
            BG_TOP = 0xFF0E121A; BG_BOTTOM = 0xFF06080C;
            CARD = 0xFF161C27; CARD_PRESSED = 0xFF1F2736; CARD_EDGE = 0xFF263042;
            ACCENT = 0xFF35C9DC; ACCENT_PRESSED = 0xFF5FDCEB; ACCENT_DIM = 0x3335C9DC; ON_ACCENT = 0xFF061016;
            TEXT = 0xFFF2F5F8; TEXT_2 = 0xFF9AA4B2; TEXT_3 = 0xFF677180;
            CHIP = 0x14FFFFFF; PRESS = 0x14FFFFFF; FIELD = 0xFF0D1118; PANEL = 0xFF121822; DIM = 0xCC000000; BAD = 0xFFFF7A7A;
            LINE = 0xFF222A36; WARN = 0xFFF2A65A; WARN_DIM = 0x1AF2A65A;
        } else {
            BG_TOP = 0xFFF1F4F8; BG_BOTTOM = 0xFFE1E6ED;
            CARD = 0xFFFFFFFF; CARD_PRESSED = 0xFFE8EDF3; CARD_EDGE = 0xFFD2D9E2;
            ACCENT = 0xFF087D8F; ACCENT_PRESSED = 0xFF06697A; ACCENT_DIM = 0x55087D8F; ON_ACCENT = 0xFFFFFFFF;
            TEXT = 0xFF121821; TEXT_2 = 0xFF4A5563; TEXT_3 = 0xFF66707E;
            CHIP = 0x10000000; PRESS = 0x18000000; FIELD = 0xFFF3F5F8; PANEL = 0xFFF7F9FB; DIM = 0x99000000; BAD = 0xFFC62828;
            LINE = 0xFFE3E8EE; WARN = 0xFFB8650F; WARN_DIM = 0x1AB8650F;
        }
    }

    // no Fade: the car shows only its own 64 colours (a colour in between is ignored), so there's nothing to fade through
    static final String[] MODES = { "static", "alt", "pulse", "pulse2", "rainbow" },
        MODE_NAMES = { "Static", "Take turns", "Pulse", "Pulse both", "Rainbow" };
    /** Rainbow: once around the car's 64 colours, slow / medium / fast */
    static final int[] RAINBOW_MS = { 60000, 20000, 6000 };
    static final String[] MODE_HELP = { "Color 1, steady.", "The whole cabin switches between Color 1 and Color 2.",
        "Moves slowly between the two colors through the car's color wheel.", "Color 1 slowly brightens and dims.",
        "Brightens and dims, switching color each time." };
    static final String[] SPEEDS = { "slow", "med", "fast" }, SPEED_NAMES = { "Slow", "Medium", "Fast" };
    static final int[] SPEED_MS = { 6000, 3000, 1200 };
    static final String[] MON = { "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec" };

    /**
     * How the 64 colours look on screen, in the screen's order (Light Vibes' list). These are the
     * swatches as the car's own settings wheel draws them (sampled from a photo of it), not the
     * codes sent to the lamp (Pal.RGB): code (119,254,196), for one, is the wheel's green.
     */
    static final int[] HL_R = {94,110,120,158,179,193,197,194,201,195,202,203,206,207,212,215,232,232,230,233,235,238,218,216,217,220,222,243,247,248,247,242,239,251,255,255,243,229,212,177,178,93,86,70,47,20,7,0,0,0,0,0,0,0,35,30,37,45,78,74,74,81,96,90,255,255,40,3,183,24};
    static final int[] HL_G = {9,0,0,0,1,17,13,7,3,1,0,0,0,0,7,21,70,78,48,61,88,106,33,35,38,36,36,124,143,154,159,166,176,200,218,241,255,255,255,255,255,255,255,230,191,149,125,99,88,79,71,57,36,4,0,0,0,19,46,33,27,40,33,22,255,236,165,199,228,142};
    static final int[] HL_B = {188,205,207,217,204,148,108,80,58,50,48,37,23,7,0,0,59,59,29,50,58,58,0,0,0,0,12,73,77,70,67,62,71,90,105,122,145,146,153,153,159,170,211,229,235,246,245,244,246,245,241,239,232,227,219,202,178,186,198,203,203,186,201,187,255,214,40,192,252,253};

    // after the car's 64: its own presets (Pal): Polar White, Moon White, Orchid Green, Jade Green, Ice Blue, Ocean Blue
    static final int WHITE = Pal.WHITE, MOON = Pal.MOON, GREEN = Pal.GREEN, JADE = Pal.JADE, ICE = Pal.ICE, OCEAN = Pal.OCEAN, LAST = Pal.LAST;

    /** the grid's order: the car's wheel as a rainbow, red first (screen indices, Light Vibes' list) */
    static final int[] ORDER = Pal.ORDER;
    static int place(int c) { for (int k = 0; k < 64; k++) if (ORDER[k] == c) return k; return 0; }

    /** a colour's name: its place in the grid (#1 red ... #64 pink) */
    static String colorName(int c) {
        for (int k = 0; k < BASIC.length; k++) if (BASIC[k] == c) return BASIC_NAMES[k];   // a basic colour reads its name
        return "#" + (place(c) + 1) + " of 64";
    }

    /** the grid shows each colour as the car's wheel draws it */
    /** a thin edge round every swatch: light colours (White, the pale yellows) stay visible on the day theme */
    int swatchEdge() { return night() ? 0x33FFFFFF : 0x40000000; }

    static int gridHex(int i) { return hex(i); }

    /** how a colour looks on screen: the 64 wheel colours by the RGB the car is sent (so they read apart);
     *  the presets (White, Moon White, Green...) by their own swatches */
    static int hex(int i) { return i < 64 || Pal.isKia ? 0xFF000000 | Pal.RGB[i] : 0xFF000000 | HL_R[i] << 16 | HL_G[i] << 8 | HL_B[i]; }

    // ---- the mockup's state ----
    static final class Item {
        String id, name, rule; boolean custom, on; int m, d, c1, c2, bright; String mode, speed;
        // the lower lights: same as the upper, opposite (the upper's colours flipped), their own, or the car's own
        String lmode = "opp", lstyle, lspeed; int lc1, lc2;
        // the car's own options, per item: Dimmed in dark, Brightness with speed (the car sets the level)
        boolean dimDark = true, speedSync;
        Item(String id, String name, String rule, int m, int d, int c1, int c2, String mode, boolean custom) {
            c1 = Pal.near(c1); c2 = Pal.near(c2);   // a Kia: the defaults on its own wheel
            this.id = id; this.name = name; this.rule = rule; this.m = m; this.d = d; this.c1 = c1; this.c2 = c2;
            this.mode = mode; this.custom = custom; this.on = !custom; this.speed = "med"; this.bright = 10;
            this.lc1 = c2; this.lc2 = c1; this.lstyle = mode; this.lspeed = "med";
        }
    }

    static List<Item> fresh() {
        List<Item> l = new ArrayList<Item>();
        l.add(new Item("ny", "New Year's Day", "Jan 1", 1, 1, 32, 36, "alt", false));
        l.add(new Item("val", "Valentine's Day", "Feb 14", 2, 14, 15, 7, "alt", false));
        l.add(new Item("stp", "St. Patrick's Day", "Mar 17", 3, 17, 40, 33, "alt", false));
        l.add(new Item("eas", "Easter", "Changes each year", 4, 5, 17, 38, "alt", false));
        l.add(new Item("jul", "Independence Day", "Jul 4", 7, 4, 15, 51, "alt", false));
        l.add(new Item("hal", "Halloween", "Oct 31", 10, 31, 31, 36, "alt", false));   // the two oranges picked in the car
        l.add(new Item("thx", "Thanksgiving", "Fourth Thursday in Nov", 11, 26, 31, 24, "alt", false));
        l.add(new Item("xms", "Christmas", "Dec 25", 12, 25, 22, GREEN, "alt", false));
        l.add(new Item("nye", "New Year's Eve", "Dec 31", 12, 31, 33, 0, "alt", false));
        l.add(new Item("u1", "My date 1", "", 0, 0, 44, 3, "alt", true));
        l.add(new Item("u2", "My date 2", "", 0, 0, 44, 3, "alt", true));
        l.add(new Item("u3", "My date 3", "", 0, 0, 44, 3, "alt", true));
        return l;
    }

    /** the Everyday light (an Item with no date) */
    static Item freshEvery() {
        Item e = new Item("every", "Everyday", "", 0, 0, 44, 54, "static", false);
        e.speed = "slow"; e.bright = 7;
        return e;
    }

    boolean app = true, master = true, skipToday;
    List<Item> items = fresh();
    Item every = freshEvery();
    String page = "every";
    Item ed;            // the holiday or date open in the editor
    int tab = 1, etab = 1;
    String toast = "", seeing;

    // ---- profiles (Button Mapper's) ----
    int editing = 1, car = 1;
    final String[] pname = { "Guest", "Profile 1", "Profile 2" };
    final Drawable[] ppic = new Drawable[3];
    Dialog picker;

    final Handler ui = new Handler();
    Typeface light, regular, medium, bold;
    float S = 1f;
    FrameLayout root;
    final List<Preview> previews = new ArrayList<Preview>();

    // ---- life ----
    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        light = Typeface.create("sans-serif-light", Typeface.NORMAL);
        regular = Typeface.create("sans-serif", Typeface.NORMAL);
        medium = Typeface.create("sans-serif-medium", Typeface.NORMAL);
        bold = Typeface.create("sans-serif", Typeface.BOLD);
        S = getResources().getDisplayMetrics().density;   // Button Mapper's sizes: the mockup's numbers in dp
        int c = Profiles.carUser(this);
        car = c >= 0 ? c : 1;
        editing = car;
        Object m = Profiles.manager(this);
        for (int n = 0; n < 3; n++) { pname[n] = Profiles.name(this, m, n); ppic[n] = Profiles.picture(this, m, n); }
        app = prefs().getBoolean("app", true);
        kiaBasics();
        all64 = prefs().getBoolean("all64", false);
        oneLight = prefs().getBoolean("oneLight", false);
        restore();
        build();
        push();
        ui.post(tick);
    }

    @Override
    protected void onResume() {
        super.onResume();
        int c = Profiles.carUser(this);
        if (c >= 0 && c != car) {
            keep();
            if (editing == car) editing = c;
            car = c;
            restore();
            build();
        }
    }

    @Override
    protected void onPause() { super.onPause(); keep(); }

    @Override
    protected void onDestroy() { ui.removeCallbacks(tick); super.onDestroy(); }

    @Override
    public void onConfigurationChanged(android.content.res.Configuration c) {
        super.onConfigurationChanged(c);
        build();
    }

    /** always follows the car's display, like Button Mapper */
    boolean night() {
        return (getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK)
                == android.content.res.Configuration.UI_MODE_NIGHT_YES;
    }

    SharedPreferences prefs() { return getSharedPreferences("studio", MODE_PRIVATE); }

    // ---- kept in this app, per profile ----
    static JSONObject toJson(Item x) throws Exception {
        JSONObject o = new JSONObject();
        o.put("name", x.name); o.put("on", x.on); o.put("m", x.m); o.put("d", x.d); o.put("c1", x.c1); o.put("c2", x.c2);
        o.put("mode", x.mode); o.put("speed", x.speed); o.put("bright", x.bright);
        o.put("lmode", x.lmode); o.put("lc1", x.lc1); o.put("lc2", x.lc2); o.put("lstyle", x.lstyle); o.put("lspeed", x.lspeed);
        o.put("dimDark", x.dimDark); o.put("speedSync", x.speedSync);
        return o;
    }

    static void fromJson(Item x, JSONObject o) {
        if (o == null) return;
        x.name = o.optString("name", x.name); x.on = o.optBoolean("on", x.on); x.m = o.optInt("m", x.m); x.d = o.optInt("d", x.d);
        x.c1 = o.optInt("c1", x.c1); x.c2 = o.optInt("c2", x.c2); x.mode = o.optString("mode", x.mode);
        x.speed = o.optString("speed", x.speed); x.bright = Math.max(0, Math.min(10, o.optInt("bright", x.bright)));
        x.dimDark = o.optBoolean("dimDark", x.dimDark); x.speedSync = o.optBoolean("speedSync", x.speedSync);
        if ("fade".equals(x.mode)) x.mode = "alt";
        if (x.c1 > LAST) x.c1 = 36;
        if (x.c2 > LAST) x.c2 = 36;
        if (o.has("lmode")) {
            x.lmode = o.optString("lmode", x.lmode);
            if ("car".equals(x.lmode)) x.lmode = "same";   // "My usual color" is gone: Light Studio drives both lights
            x.lc1 = o.optInt("lc1", x.lc1); x.lc2 = o.optInt("lc2", x.lc2);
            x.lstyle = o.optString("lstyle", x.lstyle); x.lspeed = o.optString("lspeed", x.lspeed);
            if ("fade".equals(x.lstyle)) x.lstyle = "alt";
            if (x.lc1 > LAST) x.lc1 = 36;
            if (x.lc2 > LAST) x.lc2 = 36;
        } else {   // saved by 0.4.x: its Lights row and two-tone Static
            String z = o.optString("zones", "both");
            x.lmode = "static".equals(x.mode) ? "opp" : "same";
        }
    }

    void keep() {
        try {
            JSONObject o = new JSONObject();
            o.put("master", master);
            o.put("skip", skipToday ? today() : "");
            o.put("every", toJson(every));
            JSONObject it = new JSONObject();
            for (Item x : items) it.put(x.id, toJson(x));
            o.put("items", it);
            prefs().edit().putString("p" + editing, o.toString()).putBoolean("app", app).commit();
        } catch (Exception ignored) {
        }
        push();
    }

    // ---- to the lamp engine, through HMG-API's settings ----
    long seeUntil;
    final Object pushLock = new Object();
    String pending;
    boolean pushing;

    /** everything the engine needs: on, every profile's lights (defaults where never set), See */
    String engineSettings() {
        try {
            JSONObject cfg = new JSONObject();
            for (int n = 0; n < 3; n++) {
                String s = prefs().getString("p" + n, null);
                JSONObject o = s != null ? new JSONObject(s) : null;
                if (o == null) {
                    o = new JSONObject();
                    o.put("master", true); o.put("skip", "");
                    o.put("every", toJson(freshEvery()));
                    JSONObject it = new JSONObject();
                    for (Item x : fresh()) it.put(x.id, toJson(x));
                    o.put("items", it);
                }
                cfg.put("p" + n, o);
            }
            JSONObject all = new JSONObject();
            all.put("on", app ? "1" : "0");
            all.put("oneLight", oneLight ? "1" : "0");
            all.put("config", cfg.toString());
            all.put("see", seeing != null && app ? editing + ":" + seeing + ":" + seeUntil : "");
            return all.toString();
        } catch (Exception e) {
            return null;
        }
    }

    /** sends the settings off the screen's thread; the newest wins */
    void push() {
        String js = engineSettings();
        if (js == null) return;
        synchronized (pushLock) {
            pending = js;
            if (pushing) return;
            pushing = true;
        }
        new Thread() {
            @Override public void run() {
                while (true) {
                    String j;
                    synchronized (pushLock) {
                        j = pending; pending = null;
                        if (j == null) { pushing = false; return; }
                    }
                    try {
                        Daemon.settingSet(j);
                    } catch (final Exception e) {
                        ui.post(new Runnable() { @Override public void run() { pushFailed(e.getMessage()); } });
                    }
                }
            }
        }.start();
    }

    void pushFailed(String why) {
        android.widget.Toast.makeText(this, "Light Studio couldn't save to HMG-API: " + why, android.widget.Toast.LENGTH_LONG).show();
    }

    void restore() {
        items = fresh();
        every = freshEvery();
        master = true;
        skipToday = false;
        try {
            String s = prefs().getString("p" + editing, null);
            if (s == null) return;
            JSONObject o = new JSONObject(s);
            master = o.optBoolean("master", true);
            skipToday = today().equals(o.optString("skip", ""));
            fromJson(every, o.optJSONObject("every"));
            JSONObject it = o.optJSONObject("items");
            if (it != null) for (Item x : items) fromJson(x, it.optJSONObject(x.id));
        } catch (Exception ignored) {
        }
    }

    void changed() { keep(); build(); }

    static String today() {
        Calendar c = Calendar.getInstance();
        return c.get(Calendar.YEAR) + "-" + (c.get(Calendar.MONTH) + 1) + "-" + c.get(Calendar.DAY_OF_MONTH);
    }

    // ---- the mockup's logic ----
    /** this year's date of a holiday whose date moves (Easter, Thanksgiving); the others keep theirs */
    static int[] date(Item x, int y) {
        if ("eas".equals(x.id)) return Model.easter(y);
        if ("thx".equals(x.id)) return Model.nthWeekday(y, 11, Calendar.THURSDAY, 4);
        return new int[] { x.m, x.d };
    }

    String when(Item x) {
        if (x.custom) return x.m > 0 && x.d > 0 ? MON[x.m - 1] + " " + x.d + "  ·  every year" : "Tap to pick a date";
        int[] d = date(x, Calendar.getInstance().get(Calendar.YEAR));
        String date = MON[d[0] - 1] + " " + d[1];
        return x.rule.equals(date) ? date : x.rule + "  ·  " + date + ", " + Calendar.getInstance().get(Calendar.YEAR);
    }

    Item clash(Item x) {
        int y = Calendar.getInstance().get(Calendar.YEAR);
        if (x.custom) {
            for (Item h : items) if (!h.custom) { int[] d = date(h, y); if (d[0] == x.m && d[1] == x.d) return h; }
            return null;
        }
        int[] d = date(x, y);
        for (Item u : items) if (u.custom && u.on && u.m > 0 && u.d > 0 && u.m == d[0] && u.d == d[1]) return u;
        return null;
    }

    /** today's holiday or date (dates win), or null */
    Item act() {
        if (!app || !master) return null;
        Calendar c = Calendar.getInstance();
        int y = c.get(Calendar.YEAR), m = c.get(Calendar.MONTH) + 1, d = c.get(Calendar.DAY_OF_MONTH);
        Item found = null;
        for (Item x : items) {
            if (!x.on) continue;
            if (x.custom) { if (x.m == m && x.d == d) return x; continue; }
            int[] dt = date(x, y);
            if (dt[0] == m && dt[1] == d && found == null) found = x;
        }
        return found;
    }

    String nextText() {
        if (!app) return "Using car's colors";
        if (!master) return "Holidays are off";
        Calendar t = Calendar.getInstance();
        for (int add = 1; add <= 366; add++) {
            Calendar c = (Calendar) t.clone();
            c.add(Calendar.DAY_OF_MONTH, add);
            int y = c.get(Calendar.YEAR), m = c.get(Calendar.MONTH) + 1, d = c.get(Calendar.DAY_OF_MONTH);
            Item best = null;
            for (Item x : items) {
                if (!x.on || (x.custom && (x.m == 0 || x.d == 0))) continue;
                int[] dt = x.custom ? new int[] { x.m, x.d } : date(x, y);
                if (dt[0] == m && dt[1] == d && (best == null || x.custom)) best = x;
            }
            if (best != null) return "Next: " + best.name + ", " + MON[m - 1] + " " + d + " (in " + add + " days)";
        }
        return "No holidays on";
    }

    /** a style that uses only Color 1 */
    static boolean single(String style) { return "static".equals(style) || "pulse".equals(style); }
    boolean one(Item x) { return single(x.mode); }

    static final String[] LMODES = { "same", "opp", "own" }, LMODE_NAMES = { "Same as upper", "Opposite", "Custom" };

    /** what each light shows: {colour 1, colour 2, style index, speed index}; null = the car's own */
    static int[] upper(Item x) { return new int[] { x.c1, x.c2, idx(MODES, x.mode), idx(SPEEDS, x.speed) }; }
    static int[] lower(Item x) {
        if ("car".equals(x.lmode)) return null;
        if ("own".equals(x.lmode)) return new int[] { x.lc1, x.lc2, idx(MODES, x.lstyle), idx(SPEEDS, x.lspeed) };
        if ("same".equals(x.lmode)) return upper(x);
        return new int[] { x.c2, x.c1, idx(MODES, x.mode), idx(SPEEDS, x.speed), 32 };   // opposite: the colours flipped (Rainbow: half way round)
    }

    /** the lower lights' first colour on cards (their main colour), or the upper's when they're the car's own */
    static int lowerShown(Item x) { int[] l = lower(x); return l == null ? x.c1 : l[0]; }

    /** the editor's light tabs: 1, 2 upper Color 1 / 2; 3, 4 lower */
    static boolean isLower(int tb) { return tb >= 3; }
    static int colorAt(Item x, int tb) {
        if (!isLower(tb)) return tb == 1 ? x.c1 : x.c2;
        int[] l = lower(x);
        return l == null ? -1 : tb == 3 ? l[0] : l[1];
    }
    /** a colour picked for a light; picking for linked lower lights gives them their own settings first */
    void setColor(Item x, int tb, int c) {
        if (!isLower(tb)) { if (tb == 1) x.c1 = c; else x.c2 = c; return; }
        ownLower(x);
        if (tb == 3) x.lc1 = c; else x.lc2 = c;
    }
    /** the lower lights take their own settings, starting from what they show now */
    void ownLower(Item x) {
        if ("own".equals(x.lmode)) return;
        int[] l = lower(x);
        if (l == null) l = new int[] { x.c1, x.c2, idx(MODES, x.mode), idx(SPEEDS, x.speed) };
        x.lc1 = l[0]; x.lc2 = l[1]; x.lstyle = MODES[l[2]]; x.lspeed = SPEEDS[l[3]]; x.lmode = "own";
    }

    /** a rainbow swatch: the car's colours from red round to purple */
    Drawable rainbow(int radius) {
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
            new int[] { hex(22), hex(31), hex(36), hex(41), hex(45), hex(52), hex(0), hex(8) });
        g.setCornerRadius(px(radius));
        return g;
    }

    /** a light's swatch: its colour, or a rainbow when it runs Rainbow */
    Drawable swatch(Item x, boolean lowerLight, int radius) {
        int[] v = lowerLight ? lower(x) : upper(x);
        if (v != null && "rainbow".equals(MODES[v[2]])) return rainbow(radius);
        return rr(hex(lowerLight ? lowerShown(x) : x.c1), 0, 0, px(radius));
    }

    static String styleText(String style) {
        return "alt".equals(style) ? "Color 1 and Color 2 take turns" : "pulse".equals(style) ? "Color 1 pulses (whole cabin)"
            : "pulse2".equals(style) ? "pulses, switching color each time (whole cabin)" : "rainbow".equals(style) ? "runs through every color" : "steady Color 1";
    }

    /** the line under the preview: both lights */
    String help(Item x) {
        String up = "Upper: " + styleText(x.mode) + ". ";
        if ("same".equals(x.lmode)) return up + "Lower: the same.";
        if ("opp".equals(x.lmode)) return up + ("rainbow".equals(x.mode) ? "Lower: the same rainbow, half way round."
            : "static".equals(x.mode) ? "Lower: Color 2, so top and bottom differ." : "Lower: always the other color, so top and bottom trade places.");
        return up + "Lower: " + styleText(x.lstyle) + ".";
    }

    String modeText(Item x) {
        int i = idx(MODES, x.mode);
        return i == 0 ? MODE_NAMES[0] : MODE_NAMES[i] + "  ·  " + SPEED_NAMES[idx(SPEEDS, x.speed)];
    }

    static int idx(String[] a, String v) { for (int i = 0; i < a.length; i++) if (a[i].equals(v)) return i; return 0; }

    final Runnable tick = new Runnable() {
        @Override public void run() {
            for (Preview p : previews) p.invalidate();
            ui.postDelayed(this, 40);
        }
    };

    final Runnable seeEnd = new Runnable() {
        @Override public void run() { seeing = null; toast = ""; push(); build(); }
    };

    void see(String id) {
        ui.removeCallbacks(seeEnd);
        if (id == null || id.equals(seeing)) { seeing = null; toast = ""; }
        else { seeing = id; seeUntil = System.currentTimeMillis() + 30000; ui.postDelayed(seeEnd, 30000); }
        push();
        build();
    }

    Item byId(String id) { for (Item x : items) if (x.id.equals(id)) return x; return null; }

    // ---- the screen ----
    void build() {
        palette(night());
        previews.clear();
        getWindow().setBackgroundDrawable(new ColorDrawable(BG_BOTTOM));
        if (picker != null) { picker.dismiss(); picker = null; }
        root = new FrameLayout(this);
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(px(24), px(10), px(24), px(8));
        page.setBackgroundDrawable(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[] { BG_TOP, BG_BOTTOM }));
        page.addView(header());
        Item a = act();
        if (a != null) page.addView(todayBar(a), top(10));
        if (!app && "hol".equals(this.page)) page.addView(offBar(), top(10));   // Everyday says it in its note (the banner would push Brightness off the screen)
        View body = "hol".equals(this.page) ? holidays() : everydayPage();
        body.setAlpha(app ? 1f : 0.45f);
        LinearLayout.LayoutParams bl = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1);
        bl.topMargin = px("hol".equals(this.page) ? 2 : 10);
        page.addView(body, bl);
        root.addView(page, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        if (ed != null) root.addView(editor(), new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        setContentView(root);
    }

    View header() {
        LinearLayout head = row(16);
        LinearLayout titles = new LinearLayout(this);
        titles.setOrientation(LinearLayout.VERTICAL);
        titles.addView(t("Light Studio", 26, TEXT, light));
        titles.addView(t("Everyday lights, holidays and your dates", 14, TEXT_2, regular));
        head.addView(titles);
        head.addView(profileChip(), gap(24));
        head.addView(segment(new String[] { "every", "hol" }, new String[] { "Everyday", "Holidays" }, page, 17, 10, 26, 20, 24, new Pick() {
            @Override public void pick(String v) { page = v; build(); }
        }), gap(24));
        head.addView(new View(this), new LinearLayout.LayoutParams(0, 1, 1));
        if (seeing != null && byId(seeing) != null) {
            final Item x = byId(seeing);
            LinearLayout s = row(12);
            s.setPadding(px(16), px(4), px(4), px(4));
            s.setBackgroundDrawable(rr(ACCENT_DIM, 0, 0, px(22)));
            View dot = new View(this);
            dot.setBackgroundDrawable(rr(hex(x.c1), 0, 0, px(7)));
            s.addView(dot, new LinearLayout.LayoutParams(px(14), px(14)));
            s.addView(t("Showing " + x.name + " on the car for 30 seconds", 15, TEXT, regular), gap(12));
            TextView stop = t("Stop", 15, ON_ACCENT, bold);
            stop.setGravity(Gravity.CENTER);
            stop.setPadding(px(16), 0, px(16), 0);
            stop.setBackgroundDrawable(pressable(ACCENT, ACCENT_PRESSED, 0, 0, px(18)));
            stop.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) { see(null); } });
            s.addView(stop, size(-2, 36, 12));
            head.addView(s, gap(16));
        }
        head.addView(pill(app, app ? "Light Studio on" : "Light Studio off", 16, medium, 8, 18, new View.OnClickListener() {
            @Override public void onClick(View v) { app = !app; if (!app) { seeing = null; } changed(); }
        }), gap(16));
        TextView close = t("✕", 26, TEXT_2, light);
        close.setGravity(Gravity.CENTER);
        close.setBackgroundDrawable(pressable(Color.TRANSPARENT, PRESS, 0, 0, px(28)));
        close.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) { finish(); } });
        head.addView(close, size(56, 56, 16));
        return head;
    }

    View todayBar(final Item a) {
        LinearLayout b = row(16);
        b.setPadding(px(18), px(10), px(10), px(10));
        b.setBackgroundDrawable(skipToday ? rr(CARD, CARD_EDGE, 1, px(16)) : rr(ACCENT_DIM, ACCENT, 1, px(16)));
        LinearLayout ch = row(6);
        View c1 = new View(this); c1.setBackgroundDrawable(skipToday ? rr(TEXT_2, 0, 0, px(5)) : swatch(a, false, 5));
        View c2 = new View(this); c2.setBackgroundDrawable(skipToday ? rr(TEXT_2, 0, 0, px(5)) : swatch(a, true, 5));
        ch.addView(c1, size(28, 18, 0)); ch.addView(c2, size(28, 18, 6));
        b.addView(ch);
        b.addView(t(skipToday ? "Today is " + a.name + ". Your everyday lights are back until tomorrow." : "Today is " + a.name + ". Its colors are on.", 17, TEXT, regular),
            flex(16));
        TextView btn = t(skipToday ? "Turn " + a.name + " back on" : "Back to everyday lights today", 16, skipToday ? TEXT : ON_ACCENT, bold);
        btn.setGravity(Gravity.CENTER);
        btn.setPadding(px(24), 0, px(24), 0);
        btn.setBackgroundDrawable(skipToday ? pressable(CHIP, PRESS, 0, 0, px(24)) : pressable(ACCENT, ACCENT_PRESSED, 0, 0, px(24)));
        btn.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) { skipToday = !skipToday; changed(); } });
        b.addView(btn, size(-2, 48, 16));
        return b;
    }

    View offBar() {
        LinearLayout b = row(16);
        b.setPadding(px(20), px(12), px(12), px(12));
        b.setBackgroundDrawable(rr(CARD, CARD_EDGE, 1, px(16)));
        LinearLayout tx = new LinearLayout(this);
        tx.setOrientation(LinearLayout.VERTICAL);
        tx.addView(t("Using your car's colors", 18, TEXT, bold));
        tx.addView(t("Light Studio is off: the car's own light settings are in charge. Your Light Studio settings below are kept.", 15, TEXT_2, regular), top(2));
        b.addView(tx, flex(0));
        TextView btn = t("Turn on", 16, ON_ACCENT, bold);
        btn.setGravity(Gravity.CENTER);
        btn.setPadding(px(24), 0, px(24), 0);
        btn.setBackgroundDrawable(pressable(ACCENT, ACCENT_PRESSED, 0, 0, px(24)));
        btn.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) { app = true; changed(); } });
        b.addView(btn, size(-2, 48, 16));
        return b;
    }

    // ---- Everyday ----
    View everydayPage() {
        LinearLayout panel = new LinearLayout(this);
        panel.setPadding(px(20), px(14), px(20), px(14));
        panel.setBackgroundDrawable(rr(PANEL, CARD_EDGE, 1, px(18)));
        Item a = act();
        String note = !app ? "Light Studio is off: using your car's own light settings."
            : a != null && !skipToday
            ? "Today is " + a.name + ", so these show tomorrow, or now if you tap Back to everyday lights today."
            : "Changes show on the car right away. Holidays and your dates take over on their day.";
        editorBody(panel, every, false, note);
        return panel;
    }

    /** the mockup's editor: Color 1 / Color 2 and the 64 colours (540 wide), then preview, Style, Speed, Brightness */
    void editorBody(LinearLayout panel, final Item L, final boolean inEd, String note) {
        final int tb = inEd ? etab : tab;
        LinearLayout left = new LinearLayout(this);
        left.setOrientation(LinearLayout.VERTICAL);
        LinearLayout tabs = row(8);
        for (int n = 1; n <= 4; n++) {
            final int k = n;
            boolean on = tb == n;
            int c = colorAt(L, n);
            boolean low = isLower(n);
            String st = low ? (lower(L) == null ? "static" : MODES[lower(L)[2]]) : L.mode;
            LinearLayout b = row(10);
            b.setPadding(px(12), 0, px(10), 0);
            b.setBackgroundDrawable(on ? rr(ACCENT_DIM, ACCENT, 2, px(14)) : pressable(CARD, CARD_PRESSED, CARD_EDGE, 1, px(14)));
            View sw = new View(this);
            if ("rainbow".equals(st)) {
                sw.setBackgroundDrawable(rainbow(7));
            } else sw.setBackgroundDrawable(c < 0 ? rr(0, TEXT_3, 1, px(7)) : rr(hex(c), swatchEdge(), 2, px(7)));
            b.addView(sw, size(30, 24, 0));
            LinearLayout tt = new LinearLayout(this);
            tt.setOrientation(LinearLayout.VERTICAL);
            tt.addView(t((low ? "Lower " : "Upper ") + (n % 2 == 1 ? "1" : "2"), 16, on ? TEXT : TEXT_2, medium));
            String sub = low && "car".equals(L.lmode) ? "Usual color" : "rainbow".equals(st) ? (low && "opp".equals(L.lmode) ? "Opposite" : "Rainbow") : (n % 2 == 0) && single(st) && (low || !"opp".equals(L.lmode)) ? "Not used"
                : low && "same".equals(L.lmode) ? "Same" : low && "opp".equals(L.lmode) ? "Opposite" : colorName(c);
            TextView sv = t(sub, 13, on ? TEXT : TEXT_2, regular);
            sv.setSingleLine(true);
            sv.setAlpha(0.8f);
            tt.addView(sv);
            b.addView(tt, gap(10));
            b.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) { if (inEd) etab = k; else tab = k; build(); } });
            LinearLayout.LayoutParams l = new LinearLayout.LayoutParams(0, px(52), 1);
            if (n > 1) l.leftMargin = px(n == 3 ? 14 : 6);
            tabs.addView(b, l);
        }
        left.addView(tabs);
        LinearLayout grid = new LinearLayout(this);
        grid.setOrientation(LinearLayout.VERTICAL);
        grid.setPadding(px(12), px(12), px(12), px(12));
        grid.setBackgroundDrawable(rr(FIELD, CARD_EDGE, 1, px(16)));
        final int cur = colorAt(L, tb);
        if (!all64) basics(grid, L, tb, cur);
        else for (int r = 0; r < 4; r++) {
            LinearLayout line = new LinearLayout(this);
            for (int c = 0; c < 16; c++) {
                final int i = ORDER[r * 16 + c];
                View s = new View(this);
                s.setBackgroundDrawable(i == cur ? ring(gridHex(i)) : pressable(gridHex(i), gridHex(i) & 0xB0FFFFFF, swatchEdge(), 2, px(8)));
                s.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View v) { setColor(L, tb, i); changed(); }
                });
                LinearLayout.LayoutParams l = new LinearLayout.LayoutParams(0, px(32), 1);
                if (c > 0) l.leftMargin = px(5);
                line.addView(s, l);
            }
            LinearLayout.LayoutParams ll = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            if (r > 0) ll.topMargin = px(5);
            grid.addView(line, ll);
        }
        String editing = isLower(tb) ? (lower(L) == null ? "static" : MODES[lower(L)[2]]) : L.mode;
        if ("rainbow".equals(editing)) grid.setAlpha(0.3f);   // Rainbow: no colours to pick
        left.addView(grid, top(10));
        if (inEd) edExtras(left, L, tb);
        else {
            LinearLayout gs = row(10);
            gs.setGravity(Gravity.CENTER_VERTICAL);
            gs.addView(gridSwitch(), new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, px(44)));
            // the car's, not a light setting's: the EV6 and others have one ambient light
            if (app) gs.addView(check(oneLight, "Car has one light", new View.OnClickListener() { @Override public void onClick(View v) {
                oneLight = !oneLight; prefs().edit().putBoolean("oneLight", oneLight).commit(); changed(); } }), gap(24));
            left.addView(gs, top(10));
        }
        panel.addView(left, new LinearLayout.LayoutParams(px(560), ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout right = new LinearLayout(this);
        right.setOrientation(LinearLayout.VERTICAL);
        LinearLayout pv = new LinearLayout(this);
        pv.setOrientation(LinearLayout.VERTICAL);
        pv.setPadding(px(18), px(10), px(18), px(10));
        pv.setBackgroundDrawable(rr(CARD, CARD_EDGE, 1, px(16)));
        pv.addView(t("PREVIEW", 15, TEXT_2, regular));
        FrameLayout box = new FrameLayout(this);
        box.setBackgroundDrawable(rr(0xFF05070A, 0, 0, px(16)));
        Preview p = new Preview(this, L);
        previews.add(p);
        FrameLayout.LayoutParams pl = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        box.addView(p, pl);
        pv.addView(box, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(52)) {{ topMargin = px(6); }});
        pv.addView(t(help(L), 14, TEXT_2, regular), top(6));
        right.addView(pv);
        if (isLower(tb)) right.addView(labelled("Lower", segment(LMODES, LMODE_NAMES, L.lmode, 15, 10, 16, 18, 22, new Pick() {
            @Override public void pick(String v) { if ("own".equals(v)) ownLower(L); else L.lmode = v; changed(); }
        })), top(10));
        // Style and Speed are the light being edited: the upper, or the lower when they have their own settings
        final boolean low = isLower(tb);
        final boolean linked = low && !"own".equals(L.lmode);
        final String style = low ? ("own".equals(L.lmode) ? L.lstyle : lower(L) == null ? "static" : MODES[lower(L)[2]]) : L.mode;
        final String speed = low ? ("own".equals(L.lmode) ? L.lspeed : lower(L) == null ? "med" : SPEEDS[lower(L)[3]]) : L.speed;
        View sy = labelled(low ? "Lower style" : "Upper style", segment(MODES, MODE_NAMES, style, 15, 10, 16, 18, 22, new Pick() {
            @Override public void pick(String v) { if (low) { ownLower(L); L.lstyle = v; } else L.mode = v; changed(); }
        }));
        sy.setAlpha(linked ? 0.45f : 1f);
        right.addView(sy, top(10));
        String[] sn = "rainbow".equals(style) ? new String[] { "Slow · 60 s", "Medium · 20 s", "Fast · 6 s" }
            : new String[] { "Slow · 6 s", "Medium · 3 s", "Fast · 1.2 s" };
        View sp = labelled("Speed", segment(SPEEDS, sn, speed, 15, 10, 14, 18, 22, new Pick() {
            @Override public void pick(String v) {
                if ("static".equals(style)) return;
                if (low) { ownLower(L); L.lspeed = v; } else L.speed = v;
                changed();
            }
        }));
        sp.setAlpha("static".equals(style) || linked ? 0.4f : 1f);
        right.addView(sp, top(10));
        // Brightness and the car's two options: one setting for the whole cabin (the car has only one),
        // so they show on the Upper tabs only
        if (!isLower(tb)) {
            LinearLayout br = row(16);
            br.addView(t("Brightness", 16, TEXT_2, regular), size(100, -2, 0));
            Slider sl = new Slider(this, L.bright, new Pick() { @Override public void pick(String v) { L.bright = Integer.parseInt(v); changed(); } });
            br.addView(sl, size(320, 32, 16));
            // Brightness with speed: the car sets the level, so the slider is greyed and does nothing
            boolean carLevel = L.speedSync && L.bright > 0;
            TextView lv = t(carLevel ? "Car sets it" : L.bright == 0 ? "Off" : L.bright + " of 10", 16, L.bright == 0 ? WARN : TEXT, L.bright == 0 ? bold : regular);
            br.addView(lv, gap(16));
            if (carLevel) { sl.setEnabled(false); sl.setAlpha(0.35f); lv.setAlpha(0.5f); }
            right.addView(br, top(8));
            if (app || inEd) {
                LinearLayout opts = row(12);
                opts.addView(t("Car options", 16, TEXT_2, regular), size(100, -2, 0));
                opts.addView(check(L.dimDark, "Dimmed in dark", new View.OnClickListener() { @Override public void onClick(View v) {
                    L.dimDark = !L.dimDark; changed(); } }), gap(16));
                opts.addView(check(L.speedSync, "Brightness with speed", new View.OnClickListener() { @Override public void onClick(View v) {
                    L.speedSync = !L.speedSync; changed(); } }), gap(12));
                right.addView(opts, top(4));
            }
        } else {   // a Lower tab: no slider, the car has one brightness for both lights
            LinearLayout br = row(16);
            br.addView(t("Brightness", 16, TEXT_2, regular), size(100, -2, 0));
            br.addView(t("Same as upper  \u00b7  one brightness for both lights", 16, TEXT_3, regular), gap(16));
            right.addView(br, top(14));
        }
        // off (the car's own settings in charge): the note says so
        if (!app && !inEd) right.addView(t(note, 16, WARN, bold), top(10));
        LinearLayout.LayoutParams rl = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1);
        rl.leftMargin = px(28);
        panel.addView(right, rl);
    }

    /** a tick box with its label, like the car's own settings */
    View check(boolean on, String label, View.OnClickListener click) {
        LinearLayout b = row(10);
        b.setPadding(px(10), px(6), px(14), px(6));
        b.setBackgroundDrawable(pressable(Color.TRANSPARENT, PRESS, 0, 0, px(10)));
        TextView box = t(on ? "\u2713" : "", 16, ON_ACCENT, bold);
        box.setGravity(Gravity.CENTER);
        box.setBackgroundDrawable(on ? rr(ACCENT, 0, 0, px(6)) : rr(Color.TRANSPARENT, TEXT_3, 2, px(6)));
        b.addView(box, size(26, 26, 0));
        b.addView(t(label, 15, on ? TEXT : TEXT_2, regular), gap(10));
        b.setOnClickListener(click);
        return b;
    }

    View labelled(String label, View v) {
        LinearLayout r = row(16);
        r.addView(t(label, 16, TEXT_2, regular), size(100, -2, 0));
        r.addView(v, gap(16));
        return r;
    }

    /**
     * Reset all colors: the holidays' own colours, style, speed and brightness go back to the
     * defaults. My Dates are left alone, and so are a holiday's Custom lower lights.
     */
    void resetHolidays() {
        for (Item d : fresh()) {
            if (d.custom) continue;
            Item x = byId(d.id);
            if (x == null) continue;
            x.c1 = d.c1; x.c2 = d.c2; x.mode = d.mode; x.speed = d.speed; x.bright = d.bright;
            if (!"own".equals(x.lmode)) x.lmode = d.lmode;
        }
    }

    // ---- Holidays ----
    View holidays() {
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        LinearLayout hr = row(16);
        hr.setPadding(px(4), px(10), px(4), px(8));
        TextView h = t(spaced("HOLIDAYS"), 15, TEXT_2, regular);
        hr.addView(h);
        hr.addView(new View(this), new LinearLayout.LayoutParams(0, 1, 1));
        TextView nx = t(nextText(), 15, TEXT_2, regular);
        nx.setPadding(px(16), px(6), px(16), px(6));
        nx.setBackgroundDrawable(rr(CHIP, 0, 0, px(16)));
        hr.addView(nx);
        hr.addView(pill(master, master ? "Holidays on" : "Holidays off", 15, regular, 6, 16, new View.OnClickListener() {
            @Override public void onClick(View v) { master = !master; changed(); }
        }), gap(16));
        TextView reset = t("Reset all colors", 15, TEXT_2, regular);
        reset.setPadding(px(18), px(8), px(18), px(8));
        reset.setBackgroundDrawable(pressable(Color.TRANSPARENT, PRESS, CARD_EDGE, 1, px(20)));
        reset.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) { resetHolidays(); changed(); } });
        hr.addView(reset, gap(16));
        col.addView(hr);
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setAlpha(master ? 1f : 0.55f);
        grid(body, false);
        TextView mh = t(spaced("MY DATES"), 15, TEXT_2, regular);
        mh.setPadding(px(4), px(18), px(4), px(8));
        body.addView(mh);
        grid(body, true);
        TextView n = t("On the day, the light changes at start-up and stays all day. The next day your everyday lights come back. Your dates win over a holiday on the same day.", 14, TEXT_2, regular);
        n.setPadding(px(4), px(12), px(4), px(8));
        body.addView(n);
        final ScrollView sv = new ScrollView(this);
        sv.setOverScrollMode(View.OVER_SCROLL_NEVER);
        sv.setVerticalScrollBarEnabled(false);
        sv.addView(body);
        // our own scroll bar (Android 4.4's is thin and fades): a track with a thumb, always shown
        FrameLayout wrap = new FrameLayout(this);
        FrameLayout.LayoutParams svl = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        svl.rightMargin = px(22);
        wrap.addView(sv, svl);
        final View track = new View(this);
        track.setBackgroundDrawable(rr(CHIP, 0, 0, px(4)));
        wrap.addView(track, new FrameLayout.LayoutParams(px(8), ViewGroup.LayoutParams.MATCH_PARENT, Gravity.RIGHT));
        final View thumb = new View(this);
        thumb.setBackgroundDrawable(rr(ACCENT, 0, 0, px(4)));
        wrap.addView(thumb, new FrameLayout.LayoutParams(px(8), px(60), Gravity.RIGHT | Gravity.TOP));
        final Runnable place = new Runnable() {
            @Override public void run() {
                int h = sv.getHeight(), ch = sv.getChildCount() > 0 ? sv.getChildAt(0).getHeight() : 0;
                if (h <= 0 || ch <= h) { thumb.setVisibility(View.INVISIBLE); track.setVisibility(View.INVISIBLE); return; }
                thumb.setVisibility(View.VISIBLE); track.setVisibility(View.VISIBLE);
                int th = Math.max(px(40), h * h / ch);
                FrameLayout.LayoutParams tl = (FrameLayout.LayoutParams) thumb.getLayoutParams();
                tl.height = th;
                tl.topMargin = (int) ((h - th) * (sv.getScrollY() / (float) (ch - h)));
                thumb.setLayoutParams(tl);
            }
        };
        sv.getViewTreeObserver().addOnScrollChangedListener(new android.view.ViewTreeObserver.OnScrollChangedListener() {
            @Override public void onScrollChanged() { place.run(); }
        });
        sv.getViewTreeObserver().addOnGlobalLayoutListener(new android.view.ViewTreeObserver.OnGlobalLayoutListener() {
            @Override public void onGlobalLayout() { place.run(); }
        });
        col.addView(wrap, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
        return col;
    }

    void grid(LinearLayout body, boolean custom) {
        List<Item> l = new ArrayList<Item>();
        for (Item x : items) if (x.custom == custom) l.add(x);
        for (int k = 0; k < l.size(); k += 2) {
            LinearLayout line = new LinearLayout(this);
            for (int j = k; j < k + 2; j++) {
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
                if (j > k) lp.leftMargin = px(12);
                line.addView(j < l.size() ? card(l.get(j)) : new View(this), lp);
            }
            LinearLayout.LayoutParams ll = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            if (k > 0) ll.topMargin = px(12);
            body.addView(line, ll);
        }
    }

    View card(final Item x) {
        Item c = x.on ? clash(x) : null;
        boolean beaten = c != null && !x.custom;
        boolean lit = x.on && master && !beaten;
        LinearLayout card = row(14);
        card.setPadding(px(18), px(12), px(16), px(12));
        card.setBackgroundDrawable(lit ? rr(ACCENT_DIM, ACCENT, 1, px(16)) : rr(CARD, CARD_EDGE, 1, px(16)));
        TextView box = t(x.on ? "✓" : "", 18, ON_ACCENT, bold);
        box.setGravity(Gravity.CENTER);
        box.setBackgroundDrawable(x.on ? rr(ACCENT, 0, 0, px(6)) : rr(Color.TRANSPARENT, TEXT_2, 2, px(6)));
        box.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) {
            if (x.custom && x.m == 0) { open(x); return; }
            x.on = !x.on; changed();
        } });
        card.addView(box, size(30, 30, 0));
        LinearLayout mid = row(14);
        mid.setBackgroundDrawable(pressable(Color.TRANSPARENT, PRESS, 0, 0, px(10)));
        mid.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) { open(x); } });
        LinearLayout tx = new LinearLayout(this);
        tx.setOrientation(LinearLayout.VERTICAL);
        TextView nm = t(x.name, 19, TEXT, medium);
        nm.setSingleLine(true);
        nm.setEllipsize(TextUtils.TruncateAt.END);
        tx.addView(nm);
        String w = when(x) + (c != null ? (beaten ? "  ·  " + c.name + " replaces it" : "  ·  replaces " + c.name) : "");
        TextView wt = t(w, 14, (x.custom && x.m == 0) || (c != null && !beaten) ? ACCENT : TEXT_2, regular);
        wt.setSingleLine(true);
        wt.setEllipsize(TextUtils.TruncateAt.END);
        tx.addView(wt, top(2));
        mid.addView(tx, flex(0));
        LinearLayout rc = new LinearLayout(this);
        rc.setOrientation(LinearLayout.VERTICAL);
        rc.setGravity(Gravity.LEFT);
        // the two lights as two labelled bars, upper over lower, like the preview
        for (int k = 0; k < 2; k++) {
            LinearLayout ln = row(8);
            TextView lb = t(k == 0 ? "UPPER" : "LOWER", 11, TEXT_3, medium);
            ln.addView(lb, size(46, -2, 0));
            // the light's two colours side by side (one when its style uses only Color 1; a rainbow for Rainbow)
            int[] v = k == 0 ? upper(x) : lower(x);
            String st = MODES[v[2]];
            if ("rainbow".equals(st)) {
                View bar = new View(this); bar.setBackgroundDrawable(rainbow(5)); ln.addView(bar, size(80, 14, 4));
            } else {
                View c1 = new View(this); c1.setBackgroundDrawable(rr(hex(v[0]), 0, 0, px(5))); ln.addView(c1, size(37, 14, 4));
                if (!single(st)) { View c2 = new View(this); c2.setBackgroundDrawable(rr(hex(v[1]), 0, 0, px(5))); ln.addView(c2, size(37, 14, 6)); }
            }
            rc.addView(ln, k == 0 ? new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT) : top(5));
        }
        rc.addView(t(modeText(x), 13, TEXT_2, regular), top(5));
        LinearLayout.LayoutParams rcl = new LinearLayout.LayoutParams(px(140), ViewGroup.LayoutParams.WRAP_CONTENT);
        rcl.leftMargin = px(14);
        mid.addView(rc, rcl);   // fixed width, left-aligned: every card's colors line up
        card.addView(mid, flex(14));
        final boolean showing = x.id.equals(seeing);
        LinearLayout see = row(8);
        see.setPadding(px(16), 0, px(16), 0);
        see.setBackgroundDrawable(showing ? pressable(ACCENT, ACCENT_PRESSED, 0, 0, px(22)) : pressable(CHIP, PRESS, 0, 0, px(22)));
        see.addView(new Eye(this, showing ? ON_ACCENT : TEXT), size(20, 20, 0));
        see.addView(t(showing ? "Stop" : "See", 15, showing ? ON_ACCENT : TEXT, showing ? bold : regular), gap(8));
        see.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) { if (app) see(x.id); } });
        card.addView(see, size(-2, 44, 14));
        return card;
    }

    // ---- the editor (the mockup's dialog over the screen) ----
    void open(Item x) { ed = x; etab = 1; toast = ""; build(); }

    View editor() {
        FrameLayout dim = new FrameLayout(this);
        dim.setBackgroundColor(DIM);
        dim.setClickable(true);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(px(20), px(12), px(20), px(14));
        box.setBackgroundDrawable(rr(PANEL, CARD_EDGE, 1, px(22)));
        box.setClickable(true);
        LinearLayout head = row(16);
        LinearLayout tt = new LinearLayout(this);
        tt.setOrientation(LinearLayout.VERTICAL);
        tt.addView(t(ed.name, 26, TEXT, light));
        tt.addView(t(ed.custom ? "Your own date, every year" : when(ed), 14, TEXT_2, regular));
        head.addView(tt, flex(0));
        if (ed.custom) head.addView(edDate(), gap(16));
        TextView close = t("✕", 24, TEXT_2, light);
        close.setGravity(Gravity.CENTER);
        close.setBackgroundDrawable(pressable(Color.TRANSPARENT, PRESS, 0, 0, px(24)));
        close.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) { closeEd(); } });
        head.addView(close, size(48, 48, 16));
        box.addView(head);
        LinearLayout body = new LinearLayout(this);
        editorBody(body, ed, true, null);
        box.addView(body, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1) {{ topMargin = px(8); }});
        FrameLayout.LayoutParams bl2 = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        bl2.setMargins(px(16), px(10), px(16), px(10));
        dim.addView(box, bl2);
        return dim;
    }

    void closeEd() {
        View f = getCurrentFocus();
        if (f != null) ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(f.getWindowToken(), 0);
        ed = null; toast = ""; changed();
    }


    /** basic colours first; the full 64 a tap away (kept between visits) */
    boolean all64;
    /** the car has one ambient light (EV6 and others): the engine sends no USM (dual light) calls */
    boolean oneLight;

    /**
     * The basic colours: the car's own White preset and eight from its wheel. These wheel picks
     * stand in for the car's named presets until their own codes are read from the car.
     */
    static final int BLACK = -2;   // not a colour: Black turns the lights off (brightness 0, the whole cabin)
    static final int[] BASIC = { WHITE, MOON, BLACK, 22, 31, 36, GREEN, JADE, 42, 51, 59, 4 };
    static final String[] BASIC_NAMES = { "White", "Moon White", "Off", "Red", "Orange", "Yellow", "Green", "Jade", "Cyan", "Blue", "Violet", "Magenta" };

    /** a Kia: its own wheel (Pal.kia), the basics on it, Moon White's slot as the Kia wheel's warm white */
    static void kiaBasics() {
        if (!Pal.kiaProduct(Pal.product())) return;
        Pal.kia();
        int[] k = Pal.KIA_BASIC;
        BASIC[3] = k[0]; BASIC[4] = k[1]; BASIC[5] = k[2]; BASIC[8] = k[3]; BASIC[9] = k[4]; BASIC[10] = k[5]; BASIC[11] = k[6];
        BASIC_NAMES[1] = "Warm White";
    }
    /** how a basic's swatch looks */
    static int basicSwatch(int k) {
        int c = BASIC[k];
        return c == BLACK ? 0xFF000000 : hex(c);
    }

    void basics(LinearLayout into, final Item L, final int tb, int cur) {
        for (int r = 0; r < 2; r++) {
            LinearLayout line = new LinearLayout(this);
            for (int c = 0; c < 6; c++) {
                final int k = r * 6 + c, col = BASIC[k];
                boolean on = col == BLACK ? L.bright == 0 : col == cur && L.bright > 0;
                LinearLayout b = new LinearLayout(this);
                b.setOrientation(LinearLayout.VERTICAL);
                b.setGravity(Gravity.CENTER);
                b.setBackgroundDrawable(on ? rr(ACCENT_DIM, ACCENT, 2, px(12)) : pressable(CARD, CARD_PRESSED, CARD_EDGE, 1, px(12)));
                View sw = new View(this);
                sw.setBackgroundDrawable(rr(basicSwatch(k), swatchEdge(), 2, px(8)));
                b.addView(sw, size(50, 30, 0));
                TextView nm = t(BASIC_NAMES[k], 14, on ? TEXT : TEXT_2, on ? bold : regular);
                nm.setMaxLines(1);   // setSingleLine hides the text here (as on the segments)
                nm.setGravity(Gravity.CENTER);
                b.addView(nm, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT) {{ topMargin = px(6); }});
                b.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) {
                    if (col == BLACK) L.bright = 0;                      // the lights off; the colours stay as they were
                    else { setColor(L, tb, col); if (L.bright == 0) L.bright = 10; }
                    changed();
                } });
                LinearLayout.LayoutParams l = new LinearLayout.LayoutParams(0, px(76), 1);
                if (c > 0) l.leftMargin = px(6);
                line.addView(b, l);
            }
            LinearLayout.LayoutParams ll = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            if (r > 0) ll.topMargin = px(8);
            into.addView(line, ll);
        }
    }

    /** All 64 colors / Basic colors */
    View gridSwitch() {
        TextView b = t(all64 ? "\u2039  Basic colors" : "All 64 colors  \u203A", 16, TEXT, medium);
        b.setGravity(Gravity.CENTER);
        b.setPadding(px(18), 0, px(18), 0);
        b.setBackgroundDrawable(pressable(CHIP, PRESS, 0, 0, px(22)));
        b.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) {
            all64 = !all64; prefs().edit().putBoolean("all64", all64).commit(); build();
        } });
        return b;
    }


    void edExtras(LinearLayout right, Item L, int tb) {
        final Item x = ed;
        right.addView(new View(this), new LinearLayout.LayoutParams(1, 0, 1));
        LinearLayout btns = row(10);
        btns.setGravity(Gravity.CENTER_VERTICAL);
        btns.addView(gridSwitch(), new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, px(44)));
        btns.addView(new View(this), new LinearLayout.LayoutParams(0, 1, 1));
        TextView reset = t(x.custom ? "Clear date" : "Reset colors", 16, TEXT_2, regular);
        reset.setGravity(Gravity.CENTER);
        reset.setPadding(px(16), 0, px(16), 0);
        reset.setBackgroundDrawable(pressable(Color.TRANSPARENT, PRESS, CARD_EDGE, 1, px(26)));
        reset.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) {
            for (Item d : fresh()) if (d.id.equals(x.id)) {
                x.c1 = d.c1; x.c2 = d.c2; x.mode = d.mode; x.speed = "med"; x.bright = 10;
                if (x.custom || !"own".equals(x.lmode)) { x.lmode = d.lmode; x.lc1 = d.lc1; x.lc2 = d.lc2; x.lstyle = d.lstyle; x.lspeed = d.lspeed; }   // a holiday keeps its Custom lower lights
                if (x.custom) { x.name = d.name; x.m = d.m; x.d = d.d; x.on = false; }
            }
            toast = ""; changed();
        } });
        btns.addView(reset, size(-2, 48, 10));
        // the See state lives on the button itself (a note line here pushed the buttons off the screen)
        final boolean showing = x.id.equals(seeing);
        TextView seeb = t(showing ? "Showing (30 s)" : "See on the car", 16, showing ? ACCENT : TEXT, showing ? bold : regular);
        seeb.setGravity(Gravity.CENTER);
        seeb.setPadding(px(16), 0, px(16), 0);
        seeb.setBackgroundDrawable(showing ? pressable(ACCENT_DIM, PRESS, ACCENT, 1, px(26)) : pressable(CHIP, PRESS, 0, 0, px(26)));
        seeb.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) {
            if (!app) return;
            toast = "";
            if (showing) { see(null); return; }
            ui.removeCallbacks(seeEnd); seeing = x.id; seeUntil = System.currentTimeMillis() + 30000; ui.postDelayed(seeEnd, 30000); push(); build();
        } });
        btns.addView(seeb, size(-2, 48, 10));
        TextView done = t("Done", 16, ON_ACCENT, bold);
        done.setGravity(Gravity.CENTER);
        done.setPadding(px(22), 0, px(22), 0);
        done.setBackgroundDrawable(pressable(ACCENT, ACCENT_PRESSED, 0, 0, px(26)));
        done.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) { closeEd(); } });
        btns.addView(done, size(-2, 48, 10));
        right.addView(btns, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT) {{ topMargin = px(10); }});
    }

    /** a My Date's Name and Date, on one row in the editor's title bar */
    View edDate() {
        final Item x = ed;
        LinearLayout nr = row(12);
        EditText e = new EditText(this);
        e.setSingleLine(true);
        e.setText(x.name);
        e.setHint("Name");
        e.setHintTextColor(TEXT_3);
        e.setTextSize(TypedValue.COMPLEX_UNIT_PX, px(17));
        e.setTextColor(TEXT);
        e.setTypeface(regular);
        e.setPadding(px(14), 0, px(14), 0);
        e.setBackgroundDrawable(rr(FIELD, ACCENT_DIM, 1, px(12)));
        e.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(Editable t) { x.name = t.toString(); keep(); }
        });
        nr.addView(e, new LinearLayout.LayoutParams(px(300), px(44)));
        String[] ms = new String[13]; ms[0] = "Month"; for (int i = 0; i < 12; i++) ms[i + 1] = MON[i];
        nr.addView(select(ms, x.m, new Pick() { @Override public void pick(String v) { x.m = Integer.parseInt(v); x.on = x.m > 0 && x.d > 0; changed(); } }), gap(10));
        String[] ds = new String[32]; ds[0] = "Day"; for (int i = 1; i <= 31; i++) ds[i] = String.valueOf(i);
        nr.addView(select(ds, x.d, new Pick() { @Override public void pick(String v) { x.d = Integer.parseInt(v); x.on = x.m > 0 && x.d > 0; changed(); } }), gap(10));
        return nr;
    }

    /** the mockup's select box; a tap opens the choices */
    View select(final String[] options, int now, final Pick pick) {
        TextView s = t(options[Math.max(0, Math.min(now, options.length - 1))] + "  ▾", 17, TEXT, regular);
        s.setGravity(Gravity.CENTER_VERTICAL);
        s.setPadding(px(12), 0, px(12), 0);
        s.setBackgroundDrawable(pressable(FIELD, CARD_PRESSED, ACCENT_DIM, 1, px(12)));
        s.setMinimumHeight(px(44));
        s.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) { choose(options, pick); } });
        return s;
    }

    void choose(final String[] options, final Pick pick) {
        final Dialog d = new Dialog(this, android.R.style.Theme_Translucent_NoTitleBar);
        picker = d;
        d.requestWindowFeature(Window.FEATURE_NO_TITLE);
        FrameLayout dim = new FrameLayout(this);
        dim.setBackgroundColor(0x73000000);
        dim.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) { d.dismiss(); } });
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(px(16), px(16), px(16), px(16));
        box.setBackgroundDrawable(rr(PANEL, CARD_EDGE, 1, px(18)));
        box.setClickable(true);
        int cols = options.length > 13 ? 8 : 4;
        for (int r = 1; r < options.length; r += cols) {
            LinearLayout line = new LinearLayout(this);
            for (int k = r; k < r + cols; k++) {
                LinearLayout.LayoutParams l = new LinearLayout.LayoutParams(0, px(56), 1);
                l.setMargins(px(5), px(5), px(5), px(5));
                if (k >= options.length) { line.addView(new View(this), l); continue; }
                final int at = k;
                TextView tl = t(options[k], 18, TEXT, regular);
                tl.setGravity(Gravity.CENTER);
                tl.setBackgroundDrawable(pressable(CARD, CARD_PRESSED, CARD_EDGE, 1, px(14)));
                tl.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) { d.dismiss(); pick.pick(String.valueOf(at)); } });
                line.addView(tl, l);
            }
            box.addView(line);
        }
        dim.addView(box, new FrameLayout.LayoutParams(px(cols == 8 ? 900 : 560), ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER));
        d.setContentView(dim);
        d.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        d.getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
        d.show();
    }

    // ---- the mockup's controls ----
    interface Pick { void pick(String v); }

    /** a segmented switch: chip track (pad 4, radius r), buttons (padding v/h, radius br); the picked one accent and bold */
    View segment(final String[] values, String[] titles, String now, int font, int pv, int ph, int br, int r, final Pick pick) {
        LinearLayout seg = new LinearLayout(this);
        seg.setPadding(px(4), px(4), px(4), px(4));
        seg.setBackgroundDrawable(rr(CHIP, 0, 0, px(r)));
        for (int k = 0; k < values.length; k++) {
            final String v = values[k];
            boolean on = v.equals(now);
            TextView t = t(titles[k], font, on ? ON_ACCENT : TEXT_2, on ? bold : regular);
            t.setGravity(Gravity.CENTER);
            t.setMaxLines(1);
            t.setPadding(px(ph), px(pv), px(ph), px(pv));
            t.setBackgroundDrawable(on ? rr(ACCENT, 0, 0, px(br)) : pressable(0, PRESS, 0, 0, px(br)));
            t.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View x) { pick.pick(v); } });
            seg.addView(t);
        }
        return seg;
    }

    /** the mockup's on/off pill: a 44 x 26 track with a 20 knob, then the label */
    View pill(boolean on, String label, int font, Typeface face, int pv, int pr, View.OnClickListener click) {
        LinearLayout p = row(10);
        p.setPadding(px(pv), px(pv), px(pr), px(pv));
        p.setBackgroundDrawable(pressable(CHIP, PRESS, 0, 0, px(22)));
        LinearLayout track = new LinearLayout(this);
        track.setGravity((on ? Gravity.RIGHT : Gravity.LEFT) | Gravity.CENTER_VERTICAL);
        track.setPadding(px(3), px(3), px(3), px(3));
        track.setBackgroundDrawable(rr(on ? ACCENT : TEXT_2, 0, 0, px(13)));
        View knob = new View(this);
        knob.setBackgroundDrawable(rr(on ? ON_ACCENT : BG_TOP, 0, 0, px(10)));
        track.addView(knob, size(20, 20, 0));
        p.addView(track, size(44, 26, 0));
        p.addView(t(label, font, TEXT, face), gap(10));
        p.setOnClickListener(click);
        return p;
    }

    /** the preview: the mockup's glowing line, animated by style (40 ms frames) */
    final class Preview extends View {
        final Item L;
        final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        final RectF r = new RectF();
        final long t0 = System.currentTimeMillis();

        Preview(Context c, Item L) { super(c); this.L = L; setLayerType(View.LAYER_TYPE_SOFTWARE, null); }

        float alpha, glow;

        /** one light at time t: its colour (alpha, glow set alongside) */
        int at(int[] v, long t) {
            int d = SPEED_MS[v[3]], c1 = hex(v[0]), c2 = hex(v[1]), col = c1;
            alpha = 1f; glow = 20;
            String m = MODES[v[2]];
            if ("rainbow".equals(m)) {
                int cyc = RAINBOW_MS[v[3]];
                int i = (int) (((t % cyc) * 64L / cyc + (v.length > 4 ? v[4] : 0)) % 64);
                return hex(ORDER[i]);
            }
            if ("alt".equals(m)) col = (t % d) < d / 2 ? c1 : c2;
            else if ("pulse".equals(m)) { float f = (t % d) / (float) d; float e = (float) (0.5 - 0.5 * Math.cos(f * 2 * Math.PI)); alpha = 1 - e; glow = 20 - 18 * e; }   // down to off, as in the car
            else if ("pulse2".equals(m)) {
                long d2 = d * 2L; float f = (t % d2) / (float) d2;
                if (f < 0.24f) { col = c1; alpha = 1 - 0.8f * (f / 0.24f); }
                else if (f < 0.25f) { col = c2; alpha = 0.2f; }
                else if (f < 0.5f) { col = c2; alpha = 0.2f + 0.8f * ((f - 0.25f) / 0.25f); }
                else if (f < 0.74f) { col = c2; alpha = 1 - 0.8f * ((f - 0.5f) / 0.24f); }
                else if (f < 0.75f) { col = c1; alpha = 0.2f; }
                else { col = c1; alpha = 0.2f + 0.8f * ((f - 0.75f) / 0.25f); }
            }
            return col;
        }

        final Paint label = new Paint(Paint.ANTI_ALIAS_FLAG);

        @Override protected void onDraw(Canvas c) {
            long t = System.currentTimeMillis() - t0;
            float op = L.bright == 0 ? 0.08f : 0.25f + 0.075f * L.bright, h = px(6), pad = px(80), gapY = px(14);
            label.setColor(0xFFB8C2CC);
            label.setTextSize(px(12));
            label.setTypeface(medium);
            c.drawText("UPPER", px(16), getHeight() / 2f - gapY / 2f - h / 2f + px(4), label);
            c.drawText("LOWER", px(16), getHeight() / 2f + gapY / 2f + h / 2f + px(4), label);
            int up = at(upper(L), t);
            bar(c, up, alpha * op, glow, pad, getHeight() / 2f - gapY / 2f - h, h);
            int[] lo = lower(L);
            if (lo == null) bar(c, 0, 0, 0, pad, getHeight() / 2f + gapY / 2f, h);
            else { int col = at(lo, t); bar(c, col, alpha * op, glow, pad, getHeight() / 2f + gapY / 2f, h); }
        }

        /** one light strip; colour 0: the car's own (drawn as a faint grey line) */
        void bar(Canvas c, int col, float a, float glow, float pad, float y, float h) {
            if (col == 0) { paint.setColor(0x33FFFFFF); paint.clearShadowLayer(); }
            else {
                paint.setColor(col);
                paint.setAlpha((int) (255 * a));
                paint.setShadowLayer(px(Math.max(1, (int) glow)), 0, 0, (col & 0x00FFFFFF) | ((int) (255 * a) << 24));
            }
            r.set(pad, y, getWidth() - px(24), y + h);
            c.drawRoundRect(r, h / 2, h / 2, paint);
        }
    }

    static int mix(int a, int b, float f) {
        int ar = a >> 16 & 255, ag = a >> 8 & 255, ab = a & 255, br = b >> 16 & 255, bg = b >> 8 & 255, bb = b & 255;
        return 0xFF000000 | (int) (ar + (br - ar) * f) << 16 | (int) (ag + (bg - ag) * f) << 8 | (int) (ab + (bb - ab) * f);
    }

    /** the mockup's eye icon (stroke 2 on a 24 grid) */
    final class Eye extends View {
        final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        Eye(Context c, int colour) { super(c); p.setColor(colour); p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(px(2) * 20f / 24f); p.setStrokeCap(Paint.Cap.ROUND); p.setStrokeJoin(Paint.Join.ROUND); }
        @Override protected void onDraw(Canvas c) {
            float k = getWidth() / 24f;
            Path e = new Path();
            e.moveTo(2 * k, 12 * k);
            e.cubicTo(2 * k, 12 * k, 5.5f * k, 5 * k, 12 * k, 5 * k);
            e.cubicTo(18.5f * k, 5 * k, 22 * k, 12 * k, 22 * k, 12 * k);
            e.cubicTo(22 * k, 12 * k, 18.5f * k, 19 * k, 12 * k, 19 * k);
            e.cubicTo(5.5f * k, 19 * k, 2 * k, 12 * k, 2 * k, 12 * k);
            c.drawPath(e, p);
            c.drawCircle(12 * k, 12 * k, 3 * k, p);
        }
    }

    /** the brightness slider, 0-10 (0: the lights off), in the accent colour */
    final class Slider extends View {
        int v; final Pick pick;
        final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        Slider(Context c, int v, Pick pick) { super(c); this.v = v; this.pick = pick; }
        @Override protected void onDraw(Canvas c) {
            float w = getWidth(), h = getHeight(), th = px(4), knob = px(9), x0 = knob, x1 = w - knob;
            float x = x0 + (x1 - x0) * v / 10f;
            p.setColor(CARD_EDGE);
            c.drawRoundRect(new RectF(x0, h / 2 - th / 2, x1, h / 2 + th / 2), th / 2, th / 2, p);
            p.setColor(ACCENT);
            c.drawRoundRect(new RectF(x0, h / 2 - th / 2, x, h / 2 + th / 2), th / 2, th / 2, p);
            c.drawCircle(x, h / 2, knob, p);
        }
        @Override public boolean onTouchEvent(MotionEvent e) {
            if (!isEnabled()) return false;   // greyed: the car sets the level
            float knob = px(9), x0 = knob, x1 = getWidth() - knob;
            int n = Math.max(0, Math.min(10, Math.round(10 * (e.getX() - x0) / (x1 - x0))));
            if (n != v) { v = n; invalidate(); }
            if (e.getAction() == MotionEvent.ACTION_UP) pick.pick(String.valueOf(v));
            return true;
        }
    }

    /** a selected swatch: the mockup's 3 px panel gap and 3 px ring around the colour */
    Drawable ring(int colour) {
        GradientDrawable outer = rr(TEXT, 0, 0, px(10));
        GradientDrawable gapd = rr(PANEL, 0, 0, px(8));
        GradientDrawable fill = rr(colour, 0, 0, px(6));
        LayerDrawable l = new LayerDrawable(new Drawable[] { outer, gapd, fill });
        l.setLayerInset(1, px(3), px(3), px(3), px(3));
        l.setLayerInset(2, px(6), px(6), px(6), px(6));
        return l;
    }

    /** the mockup's letter-spacing 0.06em (API 19 has no setLetterSpacing): a hair space between letters */
    static String spaced(String s) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < s.length(); i++) { if (i > 0) b.append('\u200a'); b.append(s.charAt(i)); }
        return b.toString();
    }

    // ---- the mockup's sizes (its pixels, scaled to this screen) ----
    int px(int v) { return Math.round(v * S); }

    TextView t(String s, int size, int colour, Typeface face) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(TypedValue.COMPLEX_UNIT_PX, size * S);
        t.setTextColor(colour);
        t.setTypeface(face);
        t.setIncludeFontPadding(false);
        return t;
    }

    LinearLayout row(int gapIgnored) {
        LinearLayout l = new LinearLayout(this);
        l.setGravity(Gravity.CENTER_VERTICAL);
        return l;
    }

    LinearLayout.LayoutParams gap(int left) {
        LinearLayout.LayoutParams l = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        l.leftMargin = px(left);
        return l;
    }

    LinearLayout.LayoutParams size(int w, int h, int left) {
        LinearLayout.LayoutParams l = new LinearLayout.LayoutParams(w == -2 ? ViewGroup.LayoutParams.WRAP_CONTENT : px(w), h == -2 ? ViewGroup.LayoutParams.WRAP_CONTENT : px(h));
        l.leftMargin = px(left);
        return l;
    }

    LinearLayout.LayoutParams flex(int left) {
        LinearLayout.LayoutParams l = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        l.leftMargin = px(left);
        return l;
    }

    LinearLayout.LayoutParams top(int t) {
        LinearLayout.LayoutParams l = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        l.topMargin = px(t);
        return l;
    }

    /** a rounded box: fill, border colour and width in the mockup's px (0: none), radius in screen px */
    GradientDrawable rr(int fill, int edge, int edgePx, int radius) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(fill);
        g.setCornerRadius(radius);
        if (edge != 0 && edgePx > 0) g.setStroke(Math.max(1, px(edgePx)), edge);
        return g;
    }

    Drawable pressable(int fill, int pressed, int edge, int edgePx, int radius) {
        StateListDrawable s = new StateListDrawable();
        s.addState(new int[] { android.R.attr.state_pressed }, rr(pressed, edge, edgePx, radius));
        s.addState(new int[0], rr(fill, edge, edgePx, radius));
        return s;
    }

    // ---- Button Mapper's helpers (for its profile pop-out) ----
    TextView text(String s, int sp, int colour, Typeface face) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setTextColor(colour);
        t.setTypeface(face);
        return t;
    }

    int dp(int v) { return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, getResources().getDisplayMetrics()); }

    static GradientDrawable round(int fill, int edge, int radius) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(fill);
        g.setCornerRadius(radius);
        if (edge != 0) g.setStroke(2, edge);
        return g;
    }

    static Drawable pressable(int fill, int pressed, int edge, int radius) {
        StateListDrawable s = new StateListDrawable();
        s.addState(new int[] { android.R.attr.state_pressed }, round(pressed, edge, radius));
        s.addState(new int[0], round(fill, edge, radius));
        return s;
    }

    TextView inUseTag() {
        TextView b = text("IN USE", 11, ON_ACCENT, bold);
        b.setPadding(dp(8), dp(2), dp(8), dp(2));
        b.setBackgroundDrawable(round(ACCENT, 0, dp(10)));
        return b;
    }

    LinearLayout.LayoutParams tagParams() {
        LinearLayout.LayoutParams bl = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        bl.leftMargin = dp(10);
        return bl;
    }

    /** Button Mapper's profile chip: picture, name, IN USE; tap for the profiles */
    View profileChip() {
        LinearLayout t = new LinearLayout(this);
        t.setGravity(Gravity.CENTER_VERTICAL);
        t.setPadding(dp(6), dp(6), dp(16), dp(6));
        t.setBackgroundDrawable(pressable(CARD, CARD_PRESSED, CARD_EDGE, 1, dp(26)));
        t.addView(avatar(editing, 38), new LinearLayout.LayoutParams(dp(38), dp(38)));
        TextView nm = text(pname[editing], 18, TEXT, bold);
        nm.setSingleLine(true);
        nm.setPadding(dp(12), 0, 0, 0);
        t.addView(nm);
        if (editing == car) t.addView(inUseTag(), tagParams());
        TextView arrow = text("\u25BE", 18, TEXT_2, regular);
        arrow.setPadding(dp(10), 0, 0, 0);
        t.addView(arrow);
        t.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) { profiles(); } });
        return t;
    }

    /** the profiles pop-out under the chip: Profile 1, Profile 2, Guest (tap one to edit its lights) */
    void profiles() {
        final Dialog d = new Dialog(this, android.R.style.Theme_Translucent_NoTitleBar);
        picker = d;
        d.requestWindowFeature(Window.FEATURE_NO_TITLE);
        FrameLayout dim = new FrameLayout(this);
        dim.setBackgroundColor(0x73000000);
        dim.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { d.dismiss(); }
        });
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(20), dp(18), dp(20), dp(18));
        panel.setBackgroundDrawable(round(PANEL, CARD_EDGE, dp(20)));
        panel.setClickable(true);
        panel.addView(text("EDIT LIGHTS FOR", 13, TEXT_3, bold));
        int inUse = car;
        for (final int n : Profiles.ORDER) {
            boolean ed = n == editing;
            LinearLayout t = new LinearLayout(this);
            t.setGravity(Gravity.CENTER_VERTICAL);
            t.setPadding(dp(16), dp(10), dp(16), dp(10));
            t.setBackgroundDrawable(ed ? round(ACCENT_DIM, ACCENT, dp(14)) : pressable(CARD, CARD_PRESSED, CARD_EDGE, dp(14)));
            t.addView(avatar(n, 44), new LinearLayout.LayoutParams(dp(44), dp(44)));
            LinearLayout names = new LinearLayout(this);
            names.setOrientation(LinearLayout.VERTICAL);
            names.setPadding(dp(14), 0, 0, 0);
            LinearLayout top = new LinearLayout(this);
            top.setGravity(Gravity.CENTER_VERTICAL);
            TextView nm = text(pname[n], 19, TEXT, ed ? bold : regular);
            nm.setSingleLine(true);
            top.addView(nm);
            if (n == inUse) top.addView(inUseTag(), tagParams());
            names.addView(top);
            names.addView(text(Profiles.defaultName(n), 13, TEXT_2, regular));
            t.addView(names, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
            if (ed) t.addView(text("✓", 22, ACCENT, bold));
            t.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { d.dismiss(); if (n != editing) { keep(); editing = n; restore(); build(); } }
            });
            LinearLayout.LayoutParams tl = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            tl.topMargin = dp(10);
            panel.addView(t, tl);
        }
        TextView fl = text("Each profile has its own lights; the car's profile in use gets its own automatically.", 13, TEXT_2, regular);
        fl.setPadding(dp(4), dp(14), dp(4), 0);
        panel.addView(fl);

        FrameLayout.LayoutParams pp = new FrameLayout.LayoutParams(dp(520), ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP | Gravity.LEFT);
        pp.leftMargin = dp(300);
        pp.topMargin = dp(70);
        dim.addView(panel, pp);
        d.setContentView(dim);
        Window w = d.getWindow();
        w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        w.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
        d.show();
    }

    /** a profile's picture (from the phone app) in a circle, else its first letter */
    View avatar(int n, int sizeDp) {
        Drawable pic = ppic[n];
        if (pic instanceof android.graphics.drawable.BitmapDrawable) {
            android.graphics.Bitmap src = ((android.graphics.drawable.BitmapDrawable) pic).getBitmap();
            int size = dp(sizeDp);
            android.graphics.Bitmap out = android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.ARGB_8888);
            android.graphics.Canvas cv = new android.graphics.Canvas(out);
            android.graphics.Paint pt = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
            android.graphics.BitmapShader sh = new android.graphics.BitmapShader(src, android.graphics.Shader.TileMode.CLAMP, android.graphics.Shader.TileMode.CLAMP);
            float sc = (float) size / Math.min(src.getWidth(), src.getHeight());
            android.graphics.Matrix mx = new android.graphics.Matrix();
            mx.setScale(sc, sc);
            mx.postTranslate((size - src.getWidth() * sc) / 2f, (size - src.getHeight() * sc) / 2f);
            sh.setLocalMatrix(mx);
            pt.setShader(sh);
            cv.drawCircle(size / 2f, size / 2f, size / 2f, pt);
            android.widget.ImageView iv = new android.widget.ImageView(this);
            iv.setImageBitmap(out);
            return iv;
        }
        String nm = pname[n];
        TextView t = text(nm.length() == 0 ? "?" : nm.substring(0, 1).toUpperCase(), sizeDp / 2, 0xFF061016, bold);
        t.setGravity(Gravity.CENTER);
        GradientDrawable g = new GradientDrawable();
        g.setShape(GradientDrawable.OVAL);
        g.setColor(n == 1 ? 0xFF6FA8DC : n == 2 ? 0xFFF2A65A : 0xFF9AA4B2);
        t.setBackgroundDrawable(g);
        return t;
    }
}
