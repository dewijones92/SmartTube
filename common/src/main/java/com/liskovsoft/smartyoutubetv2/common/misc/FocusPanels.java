package com.liskovsoft.smartyoutubetv2.common.misc;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

import com.liskovsoft.smartyoutubetv2.common.R;
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.manager.PlayerEngine;

public final class FocusPanels {
    private static final String TAG = FocusPanels.class.getSimpleName();
    private static final long TICK_MS = 500;
    private static final long TRACE_MS = 8_000;
    private static final Handler sHandler = new Handler(Looper.getMainLooper());
    private static PlayerEngine sPlayer;
    private static String sLastEvent;
    private static long sTraceUntil;
    private static long sLastPositionMs = -1;
    private static long sStalledSinceMs = -1;

    private static final Runnable sTick = FocusPanels::tick;

    private FocusPanels() {
    }

    public static void onPanel(Context context, PlayerEngine player, String event) {
        if (context == null || player == null || !context.getResources().getBoolean(R.bool.focus_mode_available)) {
            return;
        }

        sPlayer = player;
        sLastEvent = event;
        sTraceUntil = SystemClock.uptimeMillis() + TRACE_MS;
        sLastPositionMs = -1;
        sStalledSinceMs = -1;
        FocusLog.d(TAG, "dewidebug panel %s: %s", event, describe(player));
        sHandler.removeCallbacks(sTick);
        sHandler.postDelayed(sTick, TICK_MS);
    }

    private static void tick() {
        PlayerEngine player = sPlayer;

        if (player == null) {
            return;
        }

        long now = SystemClock.uptimeMillis();
        long position = player.getPositionMs();

        if (sLastPositionMs >= 0 && position == sLastPositionMs && player.getPlayWhenReady()) {
            if (sStalledSinceMs < 0) {
                sStalledSinceMs = now - TICK_MS;
            }
        } else if (sStalledSinceMs >= 0) {
            FocusLog.d(TAG, "dewidebug panel %s: playback stood still for %s ms, now moving again", sLastEvent, now - sStalledSinceMs);
            sStalledSinceMs = -1;
        }

        sLastPositionMs = position;
        FocusLog.d(TAG, "dewidebug panel %s tick: %s", sLastEvent, describe(player));

        if (now < sTraceUntil) {
            sHandler.postDelayed(sTick, TICK_MS);
        } else {
            if (sStalledSinceMs >= 0) {
                FocusLog.d(TAG, "dewidebug panel %s: still standing still after %s ms at trace end", sLastEvent, now - sStalledSinceMs);
            }
            sPlayer = null;
        }
    }

    private static String describe(PlayerEngine player) {
        return String.format("position=%s ms playWhenReady=%s playing=%s loading=%s engineBlocked=%s",
                player.getPositionMs(), player.getPlayWhenReady(), player.isPlaying(), player.isLoading(), player.isEngineBlocked());
    }
}
