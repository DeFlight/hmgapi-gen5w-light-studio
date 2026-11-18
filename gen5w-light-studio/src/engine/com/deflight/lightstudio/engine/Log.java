package com.deflight.lightstudio.engine;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

/** The engine's log: a few lines on the service's output, which HMG-API keeps as the service's log. */
final class Log {
    private Log() {}

    static final SimpleDateFormat T = new SimpleDateFormat("MM-dd HH:mm:ss");
    static String who = "";

    static void open(String cmd) { who = "restore".equals(cmd) ? "[restore] " : ""; }

    static synchronized void line(String s) {
        System.out.println(T.format(new Date()) + " " + who + s);
        System.out.flush();
    }

    static String trace(Throwable t) {
        StringWriter w = new StringWriter();
        t.printStackTrace(new PrintWriter(w));
        return w.toString().trim();
    }
}
