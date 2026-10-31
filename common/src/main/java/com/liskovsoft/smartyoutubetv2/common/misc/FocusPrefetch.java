package com.liskovsoft.smartyoutubetv2.common.misc;

import com.liskovsoft.mediaserviceinterfaces.data.MediaGroup;
import com.liskovsoft.sharedutils.mylogger.Log;

import java.util.List;
import java.util.Map;

import io.reactivex.Observable;

final class FocusPrefetch {
    private static final String TAG = FocusPrefetch.class.getSimpleName();
    private static final long MAX_AGE_MS = 3 * 60 * 1_000;
    private static final Warm<MediaGroup> sSubscriptions = new Warm<>("Subscriptions");
    private static final Warm<List<MediaGroup>> sHome = new Warm<>("Home");

    private FocusPrefetch() {
    }

    static void wrap(Map<Integer, Observable<MediaGroup>> grids, Map<Integer, Observable<List<MediaGroup>>> rows) {
        Observable<MediaGroup> subscriptions = grids.get(MediaGroup.TYPE_SUBSCRIPTIONS);
        Observable<List<MediaGroup>> home = rows.get(MediaGroup.TYPE_HOME);

        if (subscriptions != null) {
            grids.put(MediaGroup.TYPE_SUBSCRIPTIONS, sSubscriptions.wrap(subscriptions));
        }

        if (home != null) {
            rows.put(MediaGroup.TYPE_HOME, sHome.wrap(home));
        }
    }

    static void invalidateAll() {
        sSubscriptions.invalidate("Shorts setting changed by No");
        sHome.invalidate("Shorts setting changed by No");
    }

    static void start() {
        sSubscriptions.start();
        sHome.start();
    }

    private static final class Warm<T> {
        private final String mName;
        private Observable<T> mSource;
        private Observable<T> mCached;
        private long mStartedMs;

        Warm(String name) {
            mName = name;
        }

        synchronized Observable<T> wrap(Observable<T> source) {
            mSource = source;
            invalidate("data source replaced");
            return Observable.defer(this::take);
        }

        synchronized void start() {
            if (mSource == null) {
                Log.e(TAG, "dewidebug prefetch %s skipped: section not registered yet", mName);
                return;
            }

            if (mCached != null && System.currentTimeMillis() - mStartedMs < MAX_AGE_MS) {
                Log.d(TAG, "dewidebug prefetch %s skipped: already warm", mName);
                return;
            }

            Observable<T> cached = mSource.cache();
            mCached = cached;
            mStartedMs = System.currentTimeMillis();
            cached.subscribe(
                    result -> Log.d(TAG, "dewidebug prefetch %s ready after %s ms", mName, System.currentTimeMillis() - mStartedMs),
                    error -> {
                        Log.e(TAG, "dewidebug prefetch %s failed, discarding: %s", mName, error.getMessage());
                        invalidateIf(cached, "prefetch failed");
                    });
            Log.d(TAG, "dewidebug prefetch %s started", mName);
        }

        synchronized void invalidateIf(Observable<T> expected, String reason) {
            if (mCached == expected) {
                invalidate(reason);
            }
        }

        synchronized void invalidate(String reason) {
            if (mCached != null) {
                Log.d(TAG, "dewidebug prefetch %s discarded: %s", mName, reason);
            }
            mCached = null;
        }

        private synchronized Observable<T> take() {
            if (mCached != null && System.currentTimeMillis() - mStartedMs < MAX_AGE_MS) {
                Observable<T> cached = mCached;
                mCached = null;
                Log.d(TAG, "dewidebug %s served from prefetch (%s ms old)", mName, System.currentTimeMillis() - mStartedMs);
                return cached;
            }

            mCached = null;
            Log.d(TAG, "dewidebug %s fetched fresh (no warm prefetch)", mName);
            return mSource;
        }
    }
}
