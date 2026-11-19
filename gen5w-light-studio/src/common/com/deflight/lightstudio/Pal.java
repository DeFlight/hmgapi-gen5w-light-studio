package com.deflight.lightstudio;

/** The car's 64 ambient colours as RGB codes, in Light Vibes' list order, then the car's six presets (64 Polar White to 69 Ocean Blue); the screen stores these indices. */
public final class Pal {
    private Pal() {}
    public static final int[] RGB = { 0xb000ff,0xc300fe,0xcd00fe,0xe800fe,0xff00f1,0xff00c3,0xff009e,0xff0089,0xff0075,0xff006f,0xff006a,0xff0065,0xfe005f,0xff0048,0xfe003a,0xfe001e,0xff606a,0xff6b74,0xff4949,0xff5858,0xff7171,0xff7d73,0xff030a,0xfe2200,0xff3017,0xff3c2a,0xff4131,0xff896b,0xff9477,0xffa06f,0xffac71,0xffb16c,0xffbf7b,0xfecf8a,0xffd893,0xfeeda5,0xf7ffb6,0xe6feba,0xd2ffbc,0xaffebf,0x77fec4,0x3affc6,0x0affeb,0x00d9f5,0x00b4ff,0x00a1ff,0x008cff,0x007dff,0x0073fe,0x0063ff,0x0055ff,0x003eff,0x1900ff,0x3700ff,0x5100ff,0x6400ff,0x6d2aff,0x7a34ff,0x7d19ff,0x8000ff,0x8200ff,0x8f00ff,0x9c00ff,0xa700ff, 0xe5f2fa, 0xf4e9df, 0x7ed715, 0x03c7c0, 0xb7e4fc, 0x188efd };
    /**
     * 64-69: the car's own presets, which aren't on its wheel (read from the car, Nov 2026): Polar White,
     * Moon White, Orchid Green, Jade Green, Ice Blue, Ocean Blue. The car shows these only in its
     * single-colour mode (both lights the same colour), the way its own settings send them.
     */
    public static final int WHITE = 64, MOON = 65, GREEN = 66, JADE = 67, ICE = 68, OCEAN = 69, LAST = 69;
    public static final int OFF = -1;   // no colour is "off": Off is brightness 0
    public static boolean preset(int c) { return c > 63 && c <= LAST; }

    /**
     * Kia cars have their own 64-colour wheel (the EV6's vehicle settings app, as Light Vibes lists it):
     * a Kia ignores the Hyundai values. kia() swaps RGB and ORDER in place to the Kia wheel, the six
     * preset slots to the nearest Kia wheel colours (no Kia presets are known), and BASIC_KIA's colours
     * become the screen's basics.
     */
    public static final int[] KIA = { 0x1043f4,0x102ef2,0x1020ef,0x211cef,0x2f1cef,0x401cef,0x521cef,0x641cef,0x781cef,0x881def,0x9b21ef,0xa923ef,0xb723ea,0xc121e0,0xcc1fd2,0xd51bc0,0xdd17ae,0xe41399,0xeb0e99,0xef0a74,0xf60660,0xfa0451,0xfe0145,0xff321d,0xff4423,0xff5027,0xff6930,0xff7835,0xff8f3e,0xffa646,0xffc358,0xffcd76,0xffda9e,0xffe8c8,0xfef3e6,0xffffff,0xf6fff6,0xe9ffdd,0xdcffbc,0xcafc93,0xa6e25b,0xa8f362,0x95eb62,0x84e562,0x75df62,0x68dd62,0x5dde69,0x51e176,0x47e785,0x3eee98,0x35f5a9,0x2cfabd,0x25facf,0x1efae1,0x19faed,0x14faf9,0x10f4fc,0x10e6fc,0x10d2fc,0x10bbfc,0x10a3fc,0x108afc,0x106ffc,0x1059f8 };
    /** Kia preset slots 64-69: White, Warm White, Green, Jade, Ice Blue, Ocean Blue on the Kia wheel */
    static final int[] KIA_PRESET = { 35, 34, 45, 51, 58, 61 };
    /** the basics' wheel colours on a Kia: Red, Orange, Yellow, Cyan, Blue, Violet, Magenta */
    public static final int[] KIA_BASIC = { 23, 27, 30, 55, 2, 9, 14 };
    /** model codes (the product's first part) of Kia cars, from HMG-API's maps/kia */
    static final String[] KIA_MODELS = { "BDPE", "CKPE", "CKPE22", "CKPE23", "CVEV", "DE20EV", "DE22EV", "DE22HEV", "DE22PHEV", "DEEVPBV", "DEPEHEV", "DEPEPHEV", "DL3", "DL322", "DL3HEV", "DL3HEV22", "HMPE2", "JAPE", "JAPE22", "KA4", "KA423", "MQ4", "MQ422", "MQ422HEV", "MQ4HEV", "MQ4PHEV", "NQ5", "NQ5HEV", "NQ5PHEV", "ON", "ON22", "ONPE", "SG225EV", "SG2EV", "SG2HEV", "SG2PHEV", "SK3", "SK321", "SK322", "SK3EPE", "SK3EV", "SK3EV21", "SK3EV22", "SK3PE", "SP2", "SP221", "SP222", "SP2PE", "TAM", "TAM22", "YGPE", "YGPEHEV" };
    public static boolean isKia;

    /** a product like "CVEV.USA.0000.V015.012.260507": Kia or not */
    public static boolean kiaProduct(String product) {
        if (product == null) return false;
        String m = product.trim().toUpperCase(java.util.Locale.US);
        int d = m.indexOf('.');
        if (d > 0) m = m.substring(0, d);
        for (String k : KIA_MODELS) if (k.equals(m)) return true;
        return false;
    }

    static final int[] HYUNDAI = RGB.clone();
    /** a Hyundai wheel position as the nearest Kia one (the default colours are Hyundai positions) */
    public static int near(int c) {
        if (!isKia || c < 0 || c > 63) return c;
        int h = HYUNDAI[c], best = 0; long bd = Long.MAX_VALUE;
        for (int i = 0; i < 64; i++) {
            int k = KIA[i];
            long dr = (h >> 16 & 255) - (k >> 16 & 255), dg = (h >> 8 & 255) - (k >> 8 & 255), db = (h & 255) - (k & 255);
            long d = dr * dr + dg * dg + db * db;
            if (d < bd) { bd = d; best = i; }
        }
        return best;
    }

    /** the Kia wheel, in place (call once, at start) */
    public static void kia() {
        if (isKia) return;
        isKia = true;
        for (int i = 0; i < 64; i++) RGB[i] = KIA[i];
        for (int i = 0; i < KIA_PRESET.length; i++) RGB[64 + i] = KIA[KIA_PRESET[i]];
        for (int i = 0; i < 64; i++) ORDER[i] = (23 + i) % 64;   // the Kia wheel runs round from its red
    }

    /** the car's product code (ro.build.product) */
    public static String product() {
        try {
            return (String) Class.forName("android.os.SystemProperties").getMethod("get", String.class).invoke(null, "ro.build.product");
        } catch (Throwable e) {
            return null;
        }
    }
    /** the wheel's order as a rainbow, red first (indices into RGB): the screen's grid and Rainbow follow it */
    public static final int[] ORDER = { 22, 23, 24, 25, 26, 18, 19, 16, 17, 20, 21, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47,
        48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15 };
}
