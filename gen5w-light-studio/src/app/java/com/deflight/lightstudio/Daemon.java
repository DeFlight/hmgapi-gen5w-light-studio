package com.deflight.lightstudio;

import android.net.LocalSocket;
import android.net.LocalSocketAddress;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * The two calls this app makes to hmgapid, HMG-API's daemon (abstract socket @hmgapid),
 * which answers a mod's own app for its own mod only:
 *   setting-get          the owner's saved values (a JSON object; not the defaults)
 *   setting-set <json>   replaces them; HMG-API re-reads the button bindings at once
 * Wire format: "LFD1", argument count, each argument as length + UTF-8 (lengths are
 * 32-bit little-endian); answers are frames (type byte, length, data): 'O' output,
 * 'X' the exit code (0 = ok). Calls block: keep them off the UI thread.
 */
final class Daemon {
    private Daemon() {}

    static final int TIMEOUT_MS = 3000;

    static String settingGet() throws IOException {
        return ok(call("setting-get"));
    }

    static void settingSet(String json) throws IOException {
        ok(call("setting-set", json));
    }

    private static String ok(String[] r) throws IOException {
        if (!"0".equals(r[0])) throw new IOException("HMG-API said no (" + r[0] + "): " + r[1].trim());
        return r[1];
    }

    /** {exit code, output} */
    private static String[] call(String... args) throws IOException {
        LocalSocket s = new LocalSocket();
        try {
            try {
                s.connect(new LocalSocketAddress("hmgapid", LocalSocketAddress.Namespace.ABSTRACT));
            } catch (IOException e) {
                throw new IOException("HMG-API isn't running");
            }
            s.setSoTimeout(TIMEOUT_MS);
            ByteArrayOutputStream req = new ByteArrayOutputStream();
            req.write(new byte[] { 'L', 'F', 'D', '1' });
            u32(req, args.length);
            for (String a : args) {
                byte[] b = a.getBytes("UTF-8");
                u32(req, b.length);
                req.write(b);
            }
            OutputStream out = s.getOutputStream();
            out.write(req.toByteArray());
            out.flush();
            DataInputStream in = new DataInputStream(s.getInputStream());
            ByteArrayOutputStream text = new ByteArrayOutputStream();
            int code = -1;
            while (true) {
                int type = in.read();
                if (type < 0) break;
                byte[] n = new byte[4];
                in.readFully(n);
                int len = (n[0] & 0xff) | (n[1] & 0xff) << 8 | (n[2] & 0xff) << 16 | (n[3] & 0xff) << 24;
                if (len < 0 || len > (1 << 20)) throw new IOException("HMG-API: bad answer");
                byte[] data = new byte[len];
                in.readFully(data);
                if (type == 'O') text.write(data);
                else if (type == 'X' && len == 4) {
                    code = (data[0] & 0xff) | (data[1] & 0xff) << 8 | (data[2] & 0xff) << 16 | (data[3] & 0xff) << 24;
                    break;
                }
            }
            return new String[] { String.valueOf(code), text.toString("UTF-8") };
        } finally {
            s.close();
        }
    }

    private static void u32(ByteArrayOutputStream o, int v) {
        o.write(v & 0xff);
        o.write(v >> 8 & 0xff);
        o.write(v >> 16 & 0xff);
        o.write(v >> 24 & 0xff);
    }
}
