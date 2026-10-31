package com.liskovsoft.smartyoutubetv2.common.misc;

import android.content.Context;

import com.liskovsoft.mediaserviceinterfaces.data.MediaGroup;
import com.liskovsoft.mediaserviceinterfaces.data.MediaItem;
import com.liskovsoft.sharedutils.mylogger.Log;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.reactivex.Observable;

final class FocusPrefetch {
    private static final String TAG = FocusPrefetch.class.getSimpleName();
    private static final long MAX_AGE_MS = 60 * 1_000;
    private static final int[] UNCACHED_SECTIONS = {
            MediaGroup.TYPE_HISTORY,
            MediaGroup.TYPE_NOTIFICATIONS,
            MediaGroup.TYPE_SETTINGS,
            MediaGroup.TYPE_PLAYBACK_QUEUE,
            MediaGroup.TYPE_BLOCKED_CHANNELS
    };
    private static final Map<Integer, Entry<MediaGroup>> sGrids = new HashMap<>();
    private static final Map<Integer, Entry<List<MediaGroup>>> sRows = new HashMap<>();

    private FocusPrefetch() {
    }

    static synchronized void wrap(Map<Integer, Observable<MediaGroup>> grids, Map<Integer, Observable<List<MediaGroup>>> rows) {
        for (Map.Entry<Integer, Observable<MediaGroup>> grid : grids.entrySet()) {
            if (isCached(grid.getKey())) {
                grid.setValue(entry(sGrids, grid.getKey()).wrap(grid.getValue()));
            }
        }

        for (Map.Entry<Integer, Observable<List<MediaGroup>>> row : rows.entrySet()) {
            if (isCached(row.getKey())) {
                row.setValue(entry(sRows, row.getKey()).wrap(row.getValue()));
            }
        }

        Log.d(TAG, "dewidebug section cache wrapping %s grids, %s rows", sGrids.size(), sRows.size());
    }

    static synchronized void start(Context context) {
        Entry<MediaGroup> subscriptions = sGrids.get(MediaGroup.TYPE_SUBSCRIPTIONS);
        Entry<List<MediaGroup>> home = sRows.get(MediaGroup.TYPE_HOME);

        if (subscriptions != null) {
            subscriptions.prefetch(group -> FocusSpeed.prefetchFormatInfo(context, firstVideoId(group)));
        }

        if (home != null) {
            home.prefetch(null);
        }
    }

    static synchronized void invalidateAll(String reason) {
        for (Entry<MediaGroup> entry : sGrids.values()) {
            entry.invalidate(reason);
        }

        for (Entry<List<MediaGroup>> entry : sRows.values()) {
            entry.invalidate(reason);
        }
    }

    private static String firstVideoId(MediaGroup group) {
        List<MediaItem> items = group != null ? group.getMediaItems() : null;

        if (items == null) {
            return null;
        }

        for (MediaItem item : items) {
            if (item != null && item.getVideoId() != null && !item.isLive() && !item.isUpcoming()) {
                return item.getVideoId();
            }
        }

        return null;
    }

    private static boolean isCached(int sectionId) {
        for (int id : UNCACHED_SECTIONS) {
            if (id == sectionId) {
                return false;
            }
        }

        return true;
    }

    private static <T> Entry<T> entry(Map<Integer, Entry<T>> entries, int sectionId) {
        Entry<T> entry = entries.get(sectionId);

        if (entry == null) {
            entry = new Entry<>("section " + sectionId);
            entries.put(sectionId, entry);
        }

        return entry;
    }

    private interface OnFirst<T> {
        void onFirst(T value);
    }

    private static final class Entry<T> {
        private final String mName;
        private Observable<T> mSource;
        private Observable<T> mCached;
        private Object mToken;
        private long mStartedMs;

        Entry(String name) {
            mName = name;
        }

        synchronized Observable<T> wrap(Observable<T> source) {
            mSource = source;
            invalidate("data source replaced");
            return Observable.defer(this::take);
        }

        synchronized void prefetch(OnFirst<T> onFirst) {
            if (mSource == null) {
                Log.e(TAG, "dewidebug prefetch %s skipped: not registered", mName);
                return;
            }

            long startedMs = System.currentTimeMillis();
            take().take(1).subscribe(
                    value -> {
                        Log.d(TAG, "dewidebug prefetch %s ready after %s ms", mName, System.currentTimeMillis() - startedMs);
                        if (onFirst != null) {
                            onFirst.onFirst(value);
                        }
                    },
                    error -> Log.e(TAG, "dewidebug prefetch %s failed: %s", mName, error.getMessage()));
        }

        synchronized void invalidate(String reason) {
            if (mCached != null) {
                Log.d(TAG, "dewidebug cache %s discarded: %s", mName, reason);
            }

            mCached = null;
            mToken = null;
        }

        private synchronized void invalidateIfToken(Object token, Throwable error) {
            if (mToken == token) {
                invalidate("load failed: " + error.getMessage());
            }
        }

        private synchronized Observable<T> take() {
            long ageMs = System.currentTimeMillis() - mStartedMs;

            if (mCached != null && ageMs < MAX_AGE_MS) {
                Log.d(TAG, "dewidebug %s served from cache (%s ms old)", mName, ageMs);
                return mCached;
            }

            Object token = new Object();
            mToken = token;
            mStartedMs = System.currentTimeMillis();
            mCached = mSource.doOnError(error -> invalidateIfToken(token, error)).cache();
            Log.d(TAG, "dewidebug %s fetching fresh", mName);
            return mCached;
        }
    }
}
