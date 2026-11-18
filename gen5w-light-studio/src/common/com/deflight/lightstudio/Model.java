package com.deflight.lightstudio;

import org.json.JSONObject;

import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * What the owner chose, shared by the screen (Studio) and the lamp engine. One HMG-API
 * setting, "config", holds it as JSON: per car profile (0 Guest, 1, 2) the Everyday light,
 * Holidays on/off and each holiday's and My Date's light. Anything missing takes the
 * defaults here, so an empty config is the out-of-the-box setup.
 */
public final class Model {
    private Model() {}

    /** the car's 64 ambient colours, in its own settings app's order (the light takes no others) */
    public static final int[] PAL = { 0xf7ffb6,0xe6feba,0xd2ffbc,0xaffebf,0x77fec4,0x3affc6,0x0affeb,0x00d9f5,0x00b4ff,0x00a1ff,0x008cff,0x007dff,0x0073fe,0x0063ff,0x0055ff,0x003eff,0x1900ff,0x3700ff,0x5100ff,0x6400ff,0x6d2aff,0x7a34ff,0x7d19ff,0x8000ff,0x8200ff,0x8f00ff,0x9c00ff,0xa700ff,0xb000ff,0xc300fe,0xcd00fe,0xe800fe,0xff00f1,0xff00c3,0xff009e,0xff0089,0xff0075,0xff006f,0xff006a,0xff0065,0xfe005f,0xff0048,0xfe003a,0xfe001e,0xff030a,0xfe2200,0xff3017,0xff3c2a,0xff4131,0xff4949,0xff5858,0xff606a,0xff6b74,0xff7171,0xff7d73,0xff896b,0xff9477,0xffa06f,0xffac71,0xffb16c,0xffbf7b,0xfecf8a,0xffd893,0xfeeda5 };

    public static final String[] STYLES = { "static", "alt", "fade", "pulse", "pulse2" };
    public static final String[] SPEEDS = { "slow", "med", "fast" };

    /** a light: Color 1 (upper), Color 2 (lower), style, speed, brightness 1-10 */
    public static final class Light {
        public int c1, c2, bright;
        public String style, speed, zones = "both";   // both, upper, lower

        public Light(int c1, int c2, String style, String speed, int bright) {
            this.c1 = c1; this.c2 = c2; this.style = style; this.speed = speed; this.bright = bright;
        }

        public Light copy() { Light l = new Light(c1, c2, style, speed, bright); l.zones = zones; return l; }

        static Light from(JSONObject o, Light d) {
            if (o == null) return d.copy();
            return new Light(clamp(o.optInt("c1", d.c1), 0, 63), clamp(o.optInt("c2", d.c2), 0, 63),
                    pick(o.optString("style", d.style), STYLES, d.style), pick(o.optString("speed", d.speed), SPEEDS, d.speed),
                    clamp(o.optInt("bright", d.bright), 1, 10));
        }

        void put(JSONObject o) throws Exception {
            o.put("c1", c1); o.put("c2", c2); o.put("style", style); o.put("speed", speed); o.put("bright", bright);
        }

        public String key() { return c1 + " " + c2 + " " + style + " " + speed + " " + bright + " " + zones; }
    }

    /** a holiday or one of My Dates */
    public static final class Item {
        public final String id;
        public final boolean custom;
        public String name;
        public boolean on;
        public int m, d;       // My Dates: the date (0: not set); holidays: worked out per year
        public Light light;

        Item(String id, boolean custom, String name, boolean on, int m, int d, Light light) {
            this.id = id; this.custom = custom; this.name = name; this.on = on; this.m = m; this.d = d; this.light = light;
        }

        public boolean set() { return !custom || (m >= 1 && d >= 1); }

        /** this item's {month, day} in the given year, or null (a My Date not set) */
        public int[] date(int year) {
            if (custom) return set() ? new int[] { m, d } : null;
            if ("eas".equals(id)) return easter(year);
            if ("thx".equals(id)) return nthWeekday(year, 11, Calendar.THURSDAY, 4);
            return new int[] { m, d };
        }
    }

    public static final String[] HOL_IDS = { "ny", "val", "stp", "eas", "jul", "hal", "thx", "xms", "nye" };
    public static final String[] HOL_NAMES = { "New Year's Day", "Valentine's Day", "St. Patrick's Day", "Easter",
            "Independence Day", "Halloween", "Thanksgiving", "Christmas", "New Year's Eve" };
    public static final String[] HOL_RULES = { "Jan 1", "Feb 14", "Mar 17", "Changes each year", "Jul 4", "Oct 31",
            "Fourth Thursday in Nov", "Dec 25", "Dec 31" };
    static final int[][] HOL_DATE = { {1, 1}, {2, 14}, {3, 17}, {0, 0}, {7, 4}, {10, 31}, {0, 0}, {12, 25}, {12, 31} };
    /** default colours (indices into PAL) */
    static final int[][] HOL_COL = { {60, 0}, {43, 35}, {4, 61}, {52, 2}, {43, 15}, {46, 46}, {59, 46}, {43, 4}, {61, 28} };
    public static final String[] CUSTOM_IDS = { "u1", "u2", "u3" };
    public static final String[] MONTHS = { "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec" };

    public static Light defaultEveryday() { return new Light(8, 20, "static", "med", 7); }

    public static Light defaultLight(String id) {
        for (int i = 0; i < HOL_IDS.length; i++)
            if (HOL_IDS[i].equals(id)) return new Light(HOL_COL[i][0], HOL_COL[i][1], "hal".equals(id) ? "static" : "alt", "med", 10);
        return new Light(8, 32, "alt", "med", 10);
    }

    public static String defaultName(String id) {
        for (int i = 0; i < HOL_IDS.length; i++) if (HOL_IDS[i].equals(id)) return HOL_NAMES[i];
        for (int i = 0; i < CUSTOM_IDS.length; i++) if (CUSTOM_IDS[i].equals(id)) return "My date " + (i + 1);
        return id;
    }

    /** one profile's choices */
    public static final class Profile {
        public Light every;
        public boolean holidaysOn;
        public final LinkedHashMap<String, Item> items = new LinkedHashMap<String, Item>();

        public static Profile from(JSONObject o) {
            Profile p = new Profile();
            if (o == null) o = new JSONObject();
            p.every = Light.from(o.optJSONObject("every"), defaultEveryday());
            p.holidaysOn = o.optBoolean("holidays", true);
            JSONObject it = o.optJSONObject("items");
            if (it == null) it = new JSONObject();
            for (int i = 0; i < HOL_IDS.length; i++) {
                String id = HOL_IDS[i];
                JSONObject x = it.optJSONObject(id);
                p.items.put(id, new Item(id, false, HOL_NAMES[i], x == null || x.optBoolean("on", true),
                        HOL_DATE[i][0], HOL_DATE[i][1], Light.from(x, defaultLight(id))));
            }
            for (String id : CUSTOM_IDS) {
                JSONObject x = it.optJSONObject(id);
                int m = x == null ? 0 : clamp(x.optInt("m", 0), 0, 12), d = x == null ? 0 : clamp(x.optInt("d", 0), 0, 31);
                String name = x == null ? "" : x.optString("name", "").trim();
                p.items.put(id, new Item(id, true, name.length() == 0 ? defaultName(id) : name,
                        x != null && x.optBoolean("on", false) && m >= 1 && d >= 1, m, d, Light.from(x, defaultLight(id))));
            }
            return p;
        }

        public JSONObject toJson() {
            JSONObject o = new JSONObject();
            try {
                JSONObject e = new JSONObject();
                every.put(e);
                o.put("every", e);
                o.put("holidays", holidaysOn);
                JSONObject it = new JSONObject();
                for (Item x : items.values()) {
                    JSONObject j = new JSONObject();
                    x.light.put(j);
                    j.put("on", x.on);
                    if (x.custom) { j.put("name", x.name); j.put("m", x.m); j.put("d", x.d); }
                    it.put(x.id, j);
                }
                o.put("items", it);
            } catch (Exception ignored) {
            }
            return o;
        }

        /** the holiday or My Date for this day (My Dates win), or null; holidays off: null */
        public Item on(int year, int month, int day) {
            if (!holidaysOn) return null;
            Item found = null;
            for (Item x : items.values()) {
                if (!x.on || !x.set()) continue;
                int[] dt = x.date(year);
                if (dt == null || dt[0] != month || dt[1] != day) continue;
                if (x.custom) return x;
                if (found == null) found = x;
            }
            return found;
        }

        /** the My Date that takes over this holiday's day, or null */
        public Item replacedBy(Item h, int year) {
            if (h.custom) return null;
            int[] dt = h.date(year);
            for (Item x : items.values())
                if (x.custom && x.on && x.set() && x.m == dt[0] && x.d == dt[1]) return x;
            return null;
        }

        /** the holiday a My Date takes over, or null */
        public Item replaces(Item c, int year) {
            if (!c.custom || !c.set()) return null;
            for (Item x : items.values()) {
                if (x.custom) continue;
                int[] dt = x.date(year);
                if (dt[0] == c.m && dt[1] == c.d) return x;
            }
            return null;
        }
    }

    /** the whole config: profile number -> its choices */
    public static Map<Integer, Profile> profiles(String config) {
        Map<Integer, Profile> out = new LinkedHashMap<Integer, Profile>();
        JSONObject o;
        try { o = new JSONObject(config == null || config.trim().length() == 0 ? "{}" : config); } catch (Exception e) { o = new JSONObject(); }
        JSONObject ps = o.optJSONObject("p");
        for (int n : new int[] { 1, 2, 0 }) out.put(n, Profile.from(ps == null ? null : ps.optJSONObject(String.valueOf(n))));
        return out;
    }

    public static String config(Map<Integer, Profile> ps) {
        JSONObject o = new JSONObject(), p = new JSONObject();
        try {
            for (Map.Entry<Integer, Profile> e : ps.entrySet()) p.put(String.valueOf(e.getKey()), e.getValue().toJson());
            o.put("v", 1);
            o.put("p", p);
        } catch (Exception ignored) {
        }
        return o.toString();
    }

    public static String today(Calendar c) {
        return c.get(Calendar.YEAR) + "-" + (c.get(Calendar.MONTH) + 1) + "-" + c.get(Calendar.DAY_OF_MONTH);
    }

    /** Western Easter (Anonymous Gregorian algorithm): {month, day} */
    public static int[] easter(int y) {
        int a = y % 19, b = y / 100, c = y % 100, d = b / 4, e = b % 4, f = (b + 8) / 25, g = (b - f + 1) / 3;
        int h = (19 * a + b - d - g + 15) % 30, i = c / 4, k = c % 4, l = (32 + 2 * e + 2 * i - h - k) % 7;
        int m = (a + 11 * h + 22 * l) / 451, month = (h + l - 7 * m + 114) / 31, day = (h + l - 7 * m + 114) % 31 + 1;
        return new int[] { month, day };
    }

    /** the n-th given weekday of a month: {month, day} */
    public static int[] nthWeekday(int y, int month, int weekday, int n) {
        Calendar c = Calendar.getInstance();
        c.clear();
        c.set(y, month - 1, 1);
        int first = c.get(Calendar.DAY_OF_WEEK);
        int day = 1 + (weekday - first + 7) % 7 + 7 * (n - 1);
        return new int[] { month, day };
    }

    public static int clamp(int v, int lo, int hi) { return v < lo ? lo : v > hi ? hi : v; }

    static String pick(String v, String[] ok, String d) {
        for (String s : ok) if (s.equals(v)) return v;
        return d;
    }
}
