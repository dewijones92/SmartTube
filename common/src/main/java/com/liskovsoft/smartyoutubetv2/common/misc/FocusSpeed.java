package com.liskovsoft.smartyoutubetv2.common.misc;

import android.content.Context;

import com.liskovsoft.sharedutils.mylogger.Log;
import com.liskovsoft.smartyoutubetv2.common.R;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.Video;
import com.liskovsoft.youtubeapi.service.YouTubeServiceManager;

public final class FocusSpeed {
    private static final String TAG = FocusSpeed.class.getSimpleName();

    private FocusSpeed() {
    }

    public static boolean isEnabled(Context context) {
        return context != null && context.getResources().getBoolean(R.bool.focus_mode_available);
    }

    public static void warmEngine(Context context) {
        if (!isEnabled(context)) {
            return;
        }

        Log.d(TAG, "dewidebug warming player engine at launch");
        YouTubeServiceManager.instance().refreshCacheIfNeeded();
    }

    public static void prefetchFormatInfo(Context context, Video video) {
        if (!isEnabled(context) || video == null || video.videoId == null) {
            return;
        }

        long startMs = System.currentTimeMillis();
        String videoId = video.videoId;
        Log.d(TAG, "dewidebug format prefetch started on click: %s", videoId);
        YouTubeServiceManager.instance().getMediaItemService().getFormatInfoObserve(videoId).subscribe(
                info -> Log.d(TAG, "dewidebug format prefetch %s ready after %s ms", videoId, System.currentTimeMillis() - startMs),
                error -> Log.e(TAG, "dewidebug format prefetch %s failed: %s", videoId, error.getMessage()));
    }

    public static boolean allowAfrOverlay(Context context) {
        return !isEnabled(context);
    }
}
