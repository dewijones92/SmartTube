package com.liskovsoft.smartyoutubetv2.common.misc;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import com.liskovsoft.mediaserviceinterfaces.data.MediaGroup;
import com.liskovsoft.sharedutils.mylogger.Log;
import com.liskovsoft.smartyoutubetv2.common.R;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.Video;
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.manager.PlayerConstants;
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.ui.UiOptionItem;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.AppDialogPresenter;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.BrowsePresenter;
import com.liskovsoft.smartyoutubetv2.common.utils.IntentExtractor;
import com.liskovsoft.youtubeapi.service.internal.MediaServiceData;

import java.util.ArrayList;
import java.util.List;

public final class FocusMode {
    private static final String TAG = FocusMode.class.getSimpleName();
    private static final String PREFS_NAME = "focus_mode";
    private static final String KEY_SAVED_SHORTS_MASK = "saved_shorts_mask";
    private static final int[] HIDDEN_SECTIONS = {
            MediaGroup.TYPE_HOME,
            MediaGroup.TYPE_RECOMMENDED,
            MediaGroup.TYPE_SHORTS,
            MediaGroup.TYPE_TRENDING,
            MediaGroup.TYPE_KIDS_HOME,
            MediaGroup.TYPE_SPORTS,
            MediaGroup.TYPE_LIVE,
            MediaGroup.TYPE_GAMING,
            MediaGroup.TYPE_NEWS,
            MediaGroup.TYPE_MUSIC,
            MediaGroup.TYPE_MOVIES
    };
    private static boolean sActive;

    private FocusMode() {
    }

    public static boolean isActive() {
        return sActive;
    }

    public static void onAppLaunch(Context context, Intent intent) {
        if (context == null || !context.getResources().getBoolean(R.bool.focus_mode_available)) {
            return;
        }

        if (IntentExtractor.hasData(intent)) {
            Log.d(TAG, "dewidebug prompt skipped: launched with a link or cast, keeping active=%s", sActive);
            return;
        }

        Log.d(TAG, "dewidebug app launch: defaulting to focus mode and showing prompt");
        setActive(context, true, "default before prompt");

        AppDialogPresenter dialog = AppDialogPresenter.instance(context);
        dialog.appendSingleButton(UiOptionItem.from(context.getString(R.string.focus_mode_yes), option -> {
            setActive(context, true, "user chose Yes");
            dialog.closeDialog();
        }));
        dialog.appendSingleButton(UiOptionItem.from(context.getString(R.string.focus_mode_no), option -> {
            setActive(context, false, "user chose No");
            dialog.closeDialog();
        }));
        dialog.showDialog(context.getString(R.string.focus_mode_title),
                () -> Log.d(TAG, "dewidebug prompt closed: active=%s", sActive));
    }

    public static List<Video> filterSections(List<Video> items) {
        if (!sActive || items == null) {
            return items;
        }

        List<Video> visible = new ArrayList<>();

        for (Video item : items) {
            if (item == null || !isHiddenSection(item.sectionId)) {
                visible.add(item);
            }
        }

        return visible;
    }

    public static int bootSectionId(int storedId) {
        return sActive ? MediaGroup.TYPE_SUBSCRIPTIONS : storedId;
    }

    public static int playbackMode(int storedMode) {
        return sActive ? PlayerConstants.PLAYBACK_MODE_LIST : storedMode;
    }

    public static boolean suggestionsDisabled(boolean storedValue) {
        return sActive || storedValue;
    }

    private static void setActive(Context context, boolean active, String reason) {
        boolean changed = sActive != active;
        sActive = active;
        Log.d(TAG, "dewidebug active=%s changed=%s reason=%s", active, changed, reason);

        applyShortsHiding(context, active);

        if (changed) {
            BrowsePresenter.instance(context).updateSections();
        }
    }

    private static void applyShortsHiding(Context context, boolean active) {
        MediaServiceData data = MediaServiceData.instance();
        SharedPreferences prefs = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        if (active) {
            if (!prefs.contains(KEY_SAVED_SHORTS_MASK)) {
                int savedMask = currentShortsMask(data);
                prefs.edit().putInt(KEY_SAVED_SHORTS_MASK, savedMask).apply();
                Log.d(TAG, "dewidebug saved user shorts mask=%s before hiding all", savedMask);
            }
            data.setContentHidden(MediaServiceData.CONTENT_SHORTS_ALL, true);
        } else if (prefs.contains(KEY_SAVED_SHORTS_MASK)) {
            int savedMask = prefs.getInt(KEY_SAVED_SHORTS_MASK, 0);
            data.setContentHidden(MediaServiceData.CONTENT_SHORTS_ALL, false);
            if (savedMask != 0) {
                data.setContentHidden(savedMask, true);
            }
            prefs.edit().remove(KEY_SAVED_SHORTS_MASK).apply();
            Log.d(TAG, "dewidebug restored user shorts mask=%s", savedMask);
        } else {
            Log.d(TAG, "dewidebug normal mode, no saved shorts mask to restore");
        }
    }

    private static int currentShortsMask(MediaServiceData data) {
        int mask = 0;

        for (int bit = 0; bit < 31; bit++) {
            int flag = 1 << bit;
            if ((MediaServiceData.CONTENT_SHORTS_ALL & flag) != 0 && data.isContentHidden(flag)) {
                mask |= flag;
            }
        }

        return mask;
    }

    private static boolean isHiddenSection(int sectionId) {
        for (int hidden : HIDDEN_SECTIONS) {
            if (hidden == sectionId) {
                return true;
            }
        }

        return false;
    }
}
