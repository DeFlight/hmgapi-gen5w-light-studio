package com.deflight.lightstudio;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;


/**
 * The car's user profiles (Profile 1, Profile 2, Guest), each with its own lights (from Button Mapper).
 *
 * The car's profile service is the framework's com.hkmc.userprofile.UserProfileManager
 * (automotive.jar): getUserType() 0 Guest, 1 Profile 1, 2 Profile 2 (-1 none);
 * getUserName(n); getUserImg(n), 4 = a picture from the phone app, read with
 * getTmsProfileImage(n). Reached by reflection: it's on the car, not in the SDK.
 *
 */
final class Profiles {
    private Profiles() {}

    static final int GUEST = 0;
    /** the order on the setup screen */
    static final int[] ORDER = { 1, 2, GUEST };

    static String defaultName(int n) {
        return n == GUEST ? "Guest" : "Profile " + n;
    }

    static Object manager(Context c) {
        try {
            return Class.forName("com.hkmc.userprofile.UserProfileManager").getConstructor(Context.class).newInstance(c);
        } catch (Throwable t) {
            return null;
        }
    }

    /** the car's profile now: 0 Guest, 1, 2, or -1 if the car doesn't say */
    static int carUser(Context c) {
        Object m = manager(c);
        if (m == null) return -1;
        try {
            int u = (Integer) m.getClass().getMethod("getUserType").invoke(m);
            return u >= 0 && u <= 2 ? u : -1;
        } catch (Throwable t) {
            return -1;
        }
    }

    /** the name the owner gave the profile in the car, else Profile 1 / Profile 2 / Guest */
    static String name(Context c, Object m, int n) {
        if (n != GUEST && m != null) {
            try {
                String s = (String) m.getClass().getMethod("getUserName", int.class).invoke(m, n);
                if (s != null && s.trim().length() > 0) return s.trim();
            } catch (Throwable t) { /* the default */ }
        }
        return defaultName(n);
    }

    /** the profile's picture when it's one from the phone app (the car's own pictures aren't readable here), else null */
    static Drawable picture(Context c, Object m, int n) {
        if (m == null || n == GUEST) return null;
        try {
            int img = (Integer) m.getClass().getMethod("getUserImg", int.class).invoke(m, n);
            if (img != 4) return null;   // ProfileImageType.IMAGE_TMS
            byte[] b = (byte[]) m.getClass().getMethod("getTmsProfileImage", int.class).invoke(m, n);
            if (b == null || b.length == 0) return null;
            android.graphics.Bitmap bm = BitmapFactory.decodeByteArray(b, 0, b.length);
            return bm == null ? null : new BitmapDrawable(c.getResources(), bm);
        } catch (Throwable t) {
            return null;
        }
    }

}
