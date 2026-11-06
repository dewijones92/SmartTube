package com.liskovsoft.smartyoutubetv2.common.misc;

import android.content.Context;

import com.liskovsoft.sharedutils.helpers.MessageHelpers;
import com.liskovsoft.smartyoutubetv2.common.R;
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.manager.PlayerEngine;

import java.util.Locale;

public final class AfrRestart {
    private static final String TAG = AfrRestart.class.getSimpleName();
    private static long sStartPositionMs = -1;

    private AfrRestart() {
    }

    public static void onModeStart(Context context, PlayerEngine player, float refreshRate, int pauseMs) {
        if (context == null || player == null || !context.getResources().getBoolean(R.bool.focus_mode_available)) {
            return;
        }

        if (pauseMs <= 0) {
            FocusLog.d(TAG, "dewidebug AFR switch to %s Hz without a pause setting: not rewinding", refreshRate);
            return;
        }

        if (sStartPositionMs < 0) {
            sStartPositionMs = Math.max(0, player.getPositionMs());
        }

        String message = String.format(Locale.UK, "Adjusting TV to %s Hz…", formatRate(refreshRate));
        FocusLog.d(TAG, "dewidebug AFR switch to %s Hz, start position=%s ms, pause=%s ms", refreshRate, sStartPositionMs, pauseMs);
        MessageHelpers.showLongMessage(context, message);
    }

    public static void onResume(Context context, PlayerEngine player, boolean play) {
        if (context == null || player == null || sStartPositionMs < 0) {
            return;
        }

        long start = sStartPositionMs;
        sStartPositionMs = -1;
        long current = player.getPositionMs();
        FocusLog.d(TAG, "dewidebug AFR resume: rewinding from %s ms to %s ms, play=%s", current, start, play);
        player.setPositionMs(start);
        player.setPlayWhenReady(play);
    }

    public static void reset() {
        sStartPositionMs = -1;
    }

    private static String formatRate(float rate) {
        float rounded = Math.round(rate * 100) / 100f;
        return rounded == Math.round(rounded) ? String.valueOf(Math.round(rounded)) : String.valueOf(rounded);
    }
}
