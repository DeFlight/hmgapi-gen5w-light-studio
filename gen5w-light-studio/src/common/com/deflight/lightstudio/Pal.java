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
    /** the wheel's order as a rainbow, red first (indices into RGB): the screen's grid and Rainbow follow it */
    public static final int[] ORDER = { 22, 23, 24, 25, 26, 18, 19, 16, 17, 20, 21, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47,
        48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15 };
}
