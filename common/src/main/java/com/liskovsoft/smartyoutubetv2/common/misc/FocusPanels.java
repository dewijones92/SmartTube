package com.liskovsoft.smartyoutubetv2.common.misc;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.KeyEvent;

import com.liskovsoft.smartyoutubetv2.common.R;
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.manager.PlayerEngine;

public final class FocusPanels {
    private static final String TAG = FocusPanels.class.getSimpleName();
    private static final String CLOSED = "closed";
    private static final long TICK_MS = 500;
    private static final long TRACE_MS = 8_000;
    private static final long RESUME_WINDOW_MS = 3_000;
    private static final Handler sHandler = new Handler(Looper.getMainLooper());
    private static PlayerEngine sPlayer;
    private static String sLastEvent;
    private static long sTraceUntil;
    private static long sLastPositionMs = -1;
    private static long sStalledSinceMs = -1;
    private static long sClosedAtMs = -1;
    private static boolean sPlayingAtClose;
    private static boolean sUserKeySinceClose;
    private static boolean sResumeDecided;

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

        if (CLOSED.equals(event)) {
            sClosedAtMs = SystemClock.uptimeMillis();
            sPlayingAtClose = player.getPlayWhenReady();
            sUserKeySinceClose = false;
            sResumeDecided = false;
        } else {
            sClosedAtMs = -1;
        }

        FocusLog.d(TAG, "dewidebug panel %s: %s", event, describe(player));
        sHandler.removeCallbacks(sTick);
        sHandler.postDelayed(sTick, TICK_MS);
    }

    static void onKey(KeyEvent event) {
        if (sClosedAtMs < 0 || event.getAction() != KeyEvent.ACTION_DOWN) {
            return;
        }

        int code = event.getKeyCode();

        if (code == KeyEvent.KEYCODE_BACK || code == KeyEvent.KEYCODE_ESCAPE) {
            return;
        }

        if (!sUserKeySinceClose) {
            FocusLog.d(TAG, "dewidebug panel closed: user pressed %s %s ms after the close, so any pause now is theirs",
                    KeyEvent.keyCodeToString(code), SystemClock.uptimeMillis() - sClosedAtMs);
        }

        sUserKeySinceClose = true;
    }

    private static void tick() {
        PlayerEngine player = sPlayer;

        if (player == null) {
            return;
        }

        long now = SystemClock.uptimeMillis();
        long position = player.getPositionMs();

        boolean ended = player.getDurationMs() > 0 && position >= player.getDurationMs() - 1_000;

        if (sLastPositionMs >= 0 && position == sLastPositionMs && player.getPlayWhenReady() && !ended) {
            if (sStalledSinceMs < 0) {
                sStalledSinceMs = now - TICK_MS;
            }
        } else if (sStalledSinceMs >= 0) {
            FocusLog.d(TAG, "dewidebug panel %s: playback stood still for %s ms, now moving again", sLastEvent, now - sStalledSinceMs);
            sStalledSinceMs = -1;
        }

        sLastPositionMs = position;
        FocusLog.d(TAG, "dewidebug panel %s tick: %s", sLastEvent, describe(player));
        maybeUndoPauseAfterClose(player, now, ended);

        if (now < sTraceUntil) {
            sHandler.postDelayed(sTick, TICK_MS);
        } else {
            if (sStalledSinceMs >= 0) {
                FocusLog.d(TAG, "dewidebug panel %s: still standing still after %s ms at trace end", sLastEvent, now - sStalledSinceMs);
            }
            sPlayer = null;
            sClosedAtMs = -1;
        }
    }

    private static void maybeUndoPauseAfterClose(PlayerEngine player, long now, boolean ended) {
        if (sClosedAtMs < 0 || sResumeDecided || player.getPlayWhenReady()) {
            return;
        }

        long sinceClose = now - sClosedAtMs;
        long pauseAt = FocusTrace.lastPauseAtMs();
        long pauseAfterClose = pauseAt - sClosedAtMs;
        String caller = FocusTrace.lastPauseCaller();
        String skip = null;

        if (!sPlayingAtClose) {
            skip = "it was already paused when the panel closed";
        } else if (sUserKeySinceClose) {
            skip = "the user pressed a key after the close";
        } else if (ended) {
            skip = "the video has ended";
        } else if (sinceClose > RESUME_WINDOW_MS) {
            skip = "the pause was noticed " + sinceClose + " ms after the close, outside the " + RESUME_WINDOW_MS + " ms window";
        } else if (pauseAt < sClosedAtMs) {
            skip = "no pause event was seen after the close (player replaced or released?)";
        } else if (player.isEngineBlocked()) {
            skip = "the engine is blocked (background, PIP or closing)";
        } else if (!"PlaybackActivity".equals(FocusTrace.resumedActivity())) {
            skip = "the player is not the resumed screen (resumed=" + FocusTrace.resumedActivity() + ")";
        } else if (caller.contains("MediaSession") || caller.contains("PlayPause") || caller.contains("AudioFocus")) {
            skip = "the pause came from a media key, the media session or audio focus";
        } else if (caller.contains("RemoteController")) {
            skip = "the pause came from a phone remote";
        } else if (caller.contains("checkSleepTimer")) {
            skip = "the pause came from the sleep timer";
        } else if (caller.contains("AutoFrameRate") || caller.contains("AfrRestart")) {
            skip = "the pause belongs to a frame-rate switch, which resumes on its own";
        }

        sResumeDecided = true;

        if (skip != null) {
            FocusLog.d(TAG, "dewidebug panel closed: paused %s ms after the close, NOT resuming because %s (paused by: %s)",
                    pauseAfterClose, skip, caller);
            return;
        }

        FocusLog.d(TAG, "dewidebug panel closed: paused %s ms after the close with no key from the user, resuming (paused by: %s)",
                pauseAfterClose, caller);
        player.setPlayWhenReady(true);
        FocusLog.d(TAG, "dewidebug panel closed: after resume %s", describe(player));
    }

    private static String describe(PlayerEngine player) {
        return String.format("position=%s ms playWhenReady=%s playing=%s loading=%s engineBlocked=%s",
                player.getPositionMs(), player.getPlayWhenReady(), player.isPlaying(), player.isLoading(), player.isEngineBlocked());
    }
}
