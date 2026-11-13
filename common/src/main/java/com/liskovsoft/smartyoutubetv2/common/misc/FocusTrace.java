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
    private static Boolean sEnabled;
    private static String sLastPauseCaller = "";
    private static long sLastPauseAtMs = -1;
    private static boolean sLastPlayWhenReady;
    private static int sLastState = -1;

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

        FocusLog.d(TAG, "dewidebug key %s %s %s repeat=%s source=0x%s device=%s",
                where, KeyEvent.keyCodeToString(event.getKeyCode()), actionName(event.getAction()), repeat,
                Integer.toHexString(event.getSource()), event.getDeviceId());
        FocusPanels.onKey(event);
    }

    public static void onLifecycle(Activity activity, String event) {
        if (activity == null || !isEnabled(activity)) {
            return;
        }

        FocusLog.d(TAG, "dewidebug life %s %s task=%s finishing=%s",
                activity.getClass().getSimpleName(), event, activity.getTaskId(), activity.isFinishing());
    }

    public static void attachPlayer(Context context, Player player) {
        if (player == null || !isEnabled(context)) {
            return;
        }

        sLastPlayWhenReady = player.getPlayWhenReady();
        sLastState = player.getPlaybackState();
        FocusLog.d(TAG, "dewidebug player attached: playWhenReady=%s state=%s", sLastPlayWhenReady, stateName(sLastState));

        player.addListener(new Player.EventListener() {
            @Override
            public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
                boolean playChanged = playWhenReady != sLastPlayWhenReady;
                boolean stateChanged = playbackState != sLastState;
                sLastPlayWhenReady = playWhenReady;
                sLastState = playbackState;

                if (playChanged) {
                    String caller = caller();

                    if (!playWhenReady) {
                        sLastPauseCaller = caller;
                        sLastPauseAtMs = SystemClock.uptimeMillis();
                    }

                    FocusLog.d(TAG, "dewidebug player playWhenReady -> %s (state=%s position=%s ms) called from: %s",
                            playWhenReady, stateName(playbackState), player.getCurrentPosition(), caller);
                } else if (stateChanged) {
                    FocusLog.d(TAG, "dewidebug player state -> %s (playWhenReady=%s position=%s ms)",
                            stateName(playbackState), playWhenReady, player.getCurrentPosition());
                }
            }

            @Override
            public void onIsPlayingChanged(boolean isPlaying) {
                FocusLog.d(TAG, "dewidebug player isPlaying -> %s (playWhenReady=%s state=%s suppression=%s position=%s ms)",
                        isPlaying, player.getPlayWhenReady(), stateName(player.getPlaybackState()),
                        player.getPlaybackSuppressionReason(), player.getCurrentPosition());
            }

            @Override
            public void onPlaybackSuppressionReasonChanged(int playbackSuppressionReason) {
                FocusLog.d(TAG, "dewidebug player suppression -> %s (0=none, 1=transient audio focus loss)", playbackSuppressionReason);
            }

            @Override
            public void onPositionDiscontinuity(int reason) {
                FocusLog.d(TAG, "dewidebug player discontinuity reason=%s (0=transition 1=seek 2=seek adjust 3=ad 4=internal) position=%s ms",
                        reason, player.getCurrentPosition());
            }

            @Override
            public void onSeekProcessed() {
                FocusLog.d(TAG, "dewidebug player seek processed: position=%s ms called from: %s", player.getCurrentPosition(), caller());
            }

            @Override
            public void onPlayerError(ExoPlaybackException error) {
                FocusLog.e(TAG, "dewidebug player error type=%s: %s", error.type, error.getMessage());
            }
        });
    }

    static String lastPauseCaller() {
        return sLastPauseCaller;
    }

    static long lastPauseAtMs() {
        return sLastPauseAtMs;
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

        return taken == 0 ? "(no app frames: player-internal or system)" : result.toString();
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
