package com.liskovsoft.smartyoutubetv2.common.misc;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;

import com.liskovsoft.smartyoutubetv2.common.R;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.Video;
import com.liskovsoft.youtubeapi.service.YouTubeServiceManager;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class FocusSpeed {
    private static final String TAG = FocusSpeed.class.getSimpleName();
    private static final List<WeakReference<Activity>> sActivities = new ArrayList<>();
    private static boolean sTracking;
    private static int sStickyModeId = -1;

    private FocusSpeed() {
    }

    public static boolean isEnabled(Context context) {
        return context != null && context.getResources().getBoolean(R.bool.focus_mode_available);
    }

    public static void warmEngine(Context context) {
        if (!isEnabled(context)) {
            return;
        }

        FocusLog.d(TAG, "dewidebug warming player engine at launch");
        YouTubeServiceManager.instance().refreshCacheIfNeeded();
        trackActivities(context);
    }

    public static void prefetchFormatInfo(Context context, Video video) {
        prefetchFormatInfo(context, video != null ? video.videoId : null);
    }

    public static void prefetchFormatInfo(Context context, String videoId) {
        if (!isEnabled(context) || videoId == null) {
            return;
        }

        long startMs = System.currentTimeMillis();
        FocusLog.d(TAG, "dewidebug format prefetch started: %s", videoId);
        YouTubeServiceManager.instance().getMediaItemService().getFormatInfoObserve(videoId).subscribe(
                info -> FocusLog.d(TAG, "dewidebug format prefetch %s ready after %s ms", videoId, System.currentTimeMillis() - startMs),
                error -> FocusLog.e(TAG, "dewidebug format prefetch %s failed: %s", videoId, error.getMessage()));
    }

    public static boolean allowAfrOverlay(Context context) {
        return !isEnabled(context);
    }

    public static synchronized boolean isStickyModeActive() {
        return sStickyModeId > 0;
    }

    public static synchronized <M> M baseMode(M currentMode, M originalMode) {
        return sStickyModeId > 0 && originalMode != null ? originalMode : currentMode;
    }

    public static synchronized void onAfrModeApplied(Context context, int modeId) {
        if (!isEnabled(context) || Build.VERSION.SDK_INT < 23) {
            return;
        }

        sStickyModeId = modeId;
        int applied = 0;

        for (Iterator<WeakReference<Activity>> it = sActivities.iterator(); it.hasNext(); ) {
            Activity activity = it.next().get();
            if (activity == null || activity.isDestroyed()) {
                it.remove();
            } else if (applyStickyMode(activity)) {
                applied++;
            }
        }

        FocusLog.d(TAG, "dewidebug AFR mode %s made sticky, applied to %s other screens", modeId, applied);
    }

    private static synchronized void trackActivities(Context context) {
        if (sTracking || !(context.getApplicationContext() instanceof Application)) {
            return;
        }

        sTracking = true;
        ((Application) context.getApplicationContext()).registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
                remember(activity);
            }

            @Override
            public void onActivityStarted(Activity activity) {
                remember(activity);
            }

            @Override
            public void onActivityResumed(Activity activity) {
                FocusMode.onActivityResumed(activity);
            }

            @Override
            public void onActivityPaused(Activity activity) {
            }

            @Override
            public void onActivityStopped(Activity activity) {
            }

            @Override
            public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
            }

            @Override
            public void onActivityDestroyed(Activity activity) {
                forget(activity);
            }
        });
    }

    private static synchronized void remember(Activity activity) {
        boolean known = false;

        for (WeakReference<Activity> ref : sActivities) {
            if (ref.get() == activity) {
                known = true;
                break;
            }
        }

        if (!known) {
            sActivities.add(new WeakReference<>(activity));
        }

        applyStickyMode(activity);
    }

    private static synchronized void forget(Activity activity) {
        for (Iterator<WeakReference<Activity>> it = sActivities.iterator(); it.hasNext(); ) {
            Activity known = it.next().get();
            if (known == null || known == activity) {
                it.remove();
            }
        }
    }

    private static boolean applyStickyMode(Activity activity) {
        if (sStickyModeId <= 0 || Build.VERSION.SDK_INT < 23) {
            return false;
        }

        Window window = activity.getWindow();

        if (window == null) {
            return false;
        }

        WindowManager.LayoutParams params = window.getAttributes();

        if (params.preferredDisplayModeId == sStickyModeId) {
            return false;
        }

        params.preferredDisplayModeId = sStickyModeId;
        window.setAttributes(params);
        FocusLog.d(TAG, "dewidebug sticky AFR mode %s set on %s", sStickyModeId, activity.getClass().getSimpleName());
        return true;
    }
}
