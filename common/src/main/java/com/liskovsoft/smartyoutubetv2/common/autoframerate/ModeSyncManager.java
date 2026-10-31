package com.liskovsoft.smartyoutubetv2.common.autoframerate;

import android.app.Activity;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.Window;
import android.view.WindowManager;
import com.liskovsoft.smartyoutubetv2.common.exoplayer.selector.FormatItem;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ModeSyncManager {
    private static ModeSyncManager sInstance;
    private FormatItem mFormatItem;
    private AutoFrameRateHelper mFrameRateHelper;
    private final List<WeakReference<Activity>> mActivities = new ArrayList<>();
    private int mKeptModeId;

    private ModeSyncManager() {
        // NOP
    }

    public static ModeSyncManager instance() {
        if (sInstance == null) {
            sInstance = new ModeSyncManager();
        }

        return sInstance;
    }

    public void save(FormatItem formatItem) {
        mFormatItem = formatItem;
    }

    public void restore(Activity activity) {
        if (mFrameRateHelper == null) {
            return;
        }

        new Handler(Looper.myLooper()).postDelayed(() -> applyAfr(activity), 1_000);
    }

    private void applyAfr(Activity activity) {
        if (mFormatItem != null) {
            mFrameRateHelper.apply(activity, mFormatItem);
        } else {
            //mFrameRateHelper.restoreOriginalState(activity);
        }
    }

    public void setAfrHelper(AutoFrameRateHelper frameRateHelper) {
        mFrameRateHelper = frameRateHelper;
    }

    public void register(Activity activity) {
        mActivities.add(new WeakReference<>(activity));
        applyKeptMode(activity);
    }

    public void unregister(Activity activity) {
        for (Iterator<WeakReference<Activity>> it = mActivities.iterator(); it.hasNext(); ) {
            Activity item = it.next().get();
            if (item == null || item == activity) {
                it.remove();
            }
        }
    }

    public boolean isKeepingMode() {
        return mKeptModeId > 0;
    }

    public void keepMode(int modeId) {
        setKeptMode(modeId);
    }

    public void clearKeptMode() {
        setKeptMode(0);
    }

    private void setKeptMode(int modeId) {
        if (mKeptModeId == modeId) {
            return;
        }

        mKeptModeId = modeId;

        for (Iterator<WeakReference<Activity>> it = mActivities.iterator(); it.hasNext(); ) {
            Activity activity = it.next().get();
            if (activity == null) {
                it.remove();
            } else {
                applyKeptMode(activity);
            }
        }
    }

    private void applyKeptMode(Activity activity) {
        if (Build.VERSION.SDK_INT < 23) {
            return;
        }

        Window window = activity.getWindow();

        if (window == null) {
            return;
        }

        WindowManager.LayoutParams params = window.getAttributes();

        if (params.preferredDisplayModeId != mKeptModeId) {
            params.preferredDisplayModeId = mKeptModeId;
            window.setAttributes(params);
        }
    }
}
