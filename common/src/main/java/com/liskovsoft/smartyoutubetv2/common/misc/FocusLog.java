package com.liskovsoft.smartyoutubetv2.common.misc;

import android.util.Log;

final class FocusLog {
    private FocusLog() {
    }

    static void d(String tag, Object msg, Object... args) {
        Log.i(tag, format(msg, args));
    }

    static void e(String tag, Object msg, Object... args) {
        Log.e(tag, format(msg, args));
    }

    private static String format(Object msg, Object... args) {
        if (msg == null) {
            return "null";
        }

        if (args == null || args.length == 0) {
            return msg.toString();
        }

        try {
            return String.format(msg.toString(), args);
        } catch (RuntimeException e) {
            return msg + " (log format failed: " + e.getMessage() + ")";
        }
    }
}
