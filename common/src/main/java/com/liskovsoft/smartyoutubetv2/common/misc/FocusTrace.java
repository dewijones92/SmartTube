package com.liskovsoft.smartyoutubetv2.common.misc;

import android.app.Activity;
import android.content.Context;
import android.os.SystemClock;
import android.view.KeyEvent;

import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.Player;
import com.liskovsoft.smartyoutubetv2.common.R;

public final class FocusTrace {
    private static final String TAG = FocusTrace.class.getSimpleName();
    private static final int STACK_FRAMES = 10;
    private static final int RAW_FRAMES = 6;
    private static Boolean sEnabled;
    private static String sLastPauseCaller = "";
    private static long sLastPauseAtMs = -1;
    private static boolean sLastPlayWhenReady;
    private static int sLastState = -1;
    private static String sResumedActivity = "";

    private FocusTrace() {
    }

    public static void onKey(Context context, String where, KeyEvent event) {
        if (event == null || !isEnabled(context)) {
            return;
        }

        int repeat = event.getRepeatCount();

        if (repeat > 0 && repeat % 10 != 0) {
            return;
        }

        String key = keyName(event.getKeyCode());
        FocusLog.d(TAG, "dewidebug key %s %s %s repeat=%s source=0x%s device=%s",
                where, key, actionName(event.getAction()), repeat,
                Integer.toHexString(event.getSource()), event.getDeviceId());
        FocusPanels.onKey(event);
    }

    public static void onSetPlayWhenReady(Context context, boolean play) {
        if (!isEnabled(context)) {
            return;
        }

        String caller = caller();

        if (!play) {
            sLastPauseCaller = caller;
            sLastPauseAtMs = SystemClock.uptimeMillis();
        }

        FocusLog.d(TAG, "dewidebug view setPlayWhenReady(%s) resumed=%s called from: %s", play, sResumedActivity, caller);
    }

    public static void onLifecycle(Activity activity, String event) {
        if (activity == null || !isEnabled(activity)) {
            return;
        }

        String name = activity.getClass().getSimpleName();

        if ("resumed".equals(event)) {
            sResumedActivity = name;
        } else if ("paused".equals(event) && name.equals(sResumedActivity)) {
            sResumedActivity = "";
        }

        FocusLog.d(TAG, "dewidebug life %s %s task=%s finishing=%s",
                name, event, activity.getTaskId(), activity.isFinishing());
    }

    public static void attachPlayer(Context context, Player player) {
        if (player == null || !isEnabled(context)) {
            return;
        }

        sLastPlayWhenReady = player.getPlayWhenReady();
        sLastState = player.getPlaybackState();
        String id = "player@" + Integer.toHexString(System.identityHashCode(player));
        FocusLog.d(TAG, "dewidebug %s attached: playWhenReady=%s state=%s", id, sLastPlayWhenReady, stateName(sLastState));

        player.addListener(new Player.EventListener() {
            @Override
            public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
                boolean playChanged = playWhenReady != sLastPlayWhenReady;
                boolean stateChanged = playbackState != sLastState;
                sLastPlayWhenReady = playWhenReady;
                sLastState = playbackState;

                if (playChanged) {
                    String caller = caller();

                    if (!playWhenReady && !isRecentViewPause()) {
                        sLastPauseCaller = caller;
                        sLastPauseAtMs = SystemClock.uptimeMillis();
                    }

                    FocusLog.d(TAG, "dewidebug %s playWhenReady -> %s (state=%s position=%s ms resumed=%s) called from: %s",
                            id, playWhenReady, stateName(playbackState), player.getCurrentPosition(), sResumedActivity, caller);
                } else if (stateChanged) {
                    FocusLog.d(TAG, "dewidebug %s state -> %s (playWhenReady=%s position=%s ms)",
                            id, stateName(playbackState), playWhenReady, player.getCurrentPosition());
                }
            }

            @Override
            public void onIsPlayingChanged(boolean isPlaying) {
                FocusLog.d(TAG, "dewidebug %s isPlaying -> %s (playWhenReady=%s state=%s suppression=%s position=%s ms)",
                        id, isPlaying, player.getPlayWhenReady(), stateName(player.getPlaybackState()),
                        player.getPlaybackSuppressionReason(), player.getCurrentPosition());
            }

            @Override
            public void onPlaybackSuppressionReasonChanged(int playbackSuppressionReason) {
                FocusLog.d(TAG, "dewidebug %s suppression -> %s (0=none, 1=transient audio focus loss)", id, playbackSuppressionReason);
            }

            @Override
            public void onPositionDiscontinuity(int reason) {
                boolean seek = reason == Player.DISCONTINUITY_REASON_SEEK || reason == Player.DISCONTINUITY_REASON_SEEK_ADJUSTMENT;
                FocusLog.d(TAG, "dewidebug %s discontinuity reason=%s (0=transition 1=seek 2=seek adjust 3=ad 4=internal) position=%s ms%s",
                        id, reason, player.getCurrentPosition(), seek ? " called from: " + caller() : "");
            }

            @Override
            public void onSeekProcessed() {
                FocusLog.d(TAG, "dewidebug %s seek processed: position=%s ms", id, player.getCurrentPosition());
            }

            @Override
            public void onPlayerError(ExoPlaybackException error) {
                FocusLog.e(TAG, "dewidebug %s error type=%s (0=source 1=renderer 2=unexpected 3=remote 4=oom): %s", id, error.type, error.getMessage());
            }
        });
    }

    static String lastPauseCaller() {
        return sLastPauseCaller;
    }

    static long lastPauseAtMs() {
        return sLastPauseAtMs;
    }

    static String resumedActivity() {
        return sResumedActivity;
    }

    static String keyName(int code) {
        boolean printing = (code >= KeyEvent.KEYCODE_0 && code <= KeyEvent.KEYCODE_POUND)
                || (code >= KeyEvent.KEYCODE_A && code <= KeyEvent.KEYCODE_PERIOD)
                || (code >= KeyEvent.KEYCODE_SPACE && code <= KeyEvent.KEYCODE_SPACE)
                || (code >= KeyEvent.KEYCODE_GRAVE && code <= KeyEvent.KEYCODE_AT)
                || (code >= KeyEvent.KEYCODE_NUMPAD_0 && code <= KeyEvent.KEYCODE_NUMPAD_RIGHT_PAREN);
        return printing ? "(printing key)" : KeyEvent.keyCodeToString(code);
    }

    private static boolean isRecentViewPause() {
        return sLastPauseAtMs >= 0 && SystemClock.uptimeMillis() - sLastPauseAtMs < 50 && !sLastPauseCaller.startsWith("(no app frames)");
    }

    private static String caller() {
        StackTraceElement[] stack = new Throwable().getStackTrace();
        StringBuilder result = new StringBuilder();
        int taken = 0;

        for (StackTraceElement frame : stack) {
            String cls = frame.getClassName();

            if (cls.startsWith("java.") || cls.startsWith("android.") || cls.startsWith("com.android.")
                    || cls.startsWith("dalvik.") || cls.startsWith("com.google.android.exoplayer2.")
                    || cls.startsWith(FocusTrace.class.getName())) {
                continue;
            }

            if (result.length() > 0) {
                result.append(" < ");
            }

            result.append(cls.substring(cls.lastIndexOf('.') + 1)).append('.').append(frame.getMethodName())
                    .append(':').append(frame.getLineNumber());

            if (++taken >= STACK_FRAMES) {
                break;
            }
        }

        StringBuilder raw = new StringBuilder();
        int rawTaken = 0;

        for (StackTraceElement frame : stack) {
            String cls = frame.getClassName();

            if (cls.startsWith(FocusTrace.class.getName()) || cls.contains("ExoPlayerImpl") || cls.contains("SimpleExoPlayer")
                    || cls.contains("Lambda") || cls.startsWith("java.") || cls.startsWith("dalvik.")) {
                continue;
            }

            if (raw.length() > 0) {
                raw.append(" < ");
            }

            raw.append(cls.substring(cls.lastIndexOf('.') + 1)).append('.').append(frame.getMethodName())
                    .append(':').append(frame.getLineNumber());

            if (++rawTaken >= RAW_FRAMES) {
                break;
            }
        }

        return (taken == 0 ? "(no app frames)" : result.toString())
                + " | raw: " + raw + " | thread: " + Thread.currentThread().getName();
    }

    private static String actionName(int action) {
        switch (action) {
            case KeyEvent.ACTION_DOWN:
                return "DOWN";
            case KeyEvent.ACTION_UP:
                return "UP";
            default:
                return "action" + action;
        }
    }

    private static String stateName(int state) {
        switch (state) {
            case Player.STATE_IDLE:
                return "IDLE";
            case Player.STATE_BUFFERING:
                return "BUFFERING";
            case Player.STATE_READY:
                return "READY";
            case Player.STATE_ENDED:
                return "ENDED";
            default:
                return "state" + state;
        }
    }

    private static boolean isEnabled(Context context) {
        if (sEnabled == null && context != null) {
            sEnabled = context.getResources().getBoolean(R.bool.focus_mode_available);
        }

        return sEnabled != null && sEnabled;
    }
}
