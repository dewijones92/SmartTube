package com.liskovsoft.smartyoutubetv2.common.misc;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.text.TextUtils;
import android.view.Window;

import com.liskovsoft.mediaserviceinterfaces.data.MediaGroup;
import com.liskovsoft.sharedutils.prefs.GlobalPreferences;
import com.liskovsoft.smartyoutubetv2.common.R;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.BrowseSection;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.Video;
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.manager.PlayerConstants;
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.ui.UiOptionItem;
import com.liskovsoft.smartyoutubetv2.common.app.models.search.SearchTagsProvider;
import com.liskovsoft.smartyoutubetv2.common.app.models.search.vineyard.Tag;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.AppDialogPresenter;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.BrowsePresenter;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.SearchPresenter;
import com.liskovsoft.smartyoutubetv2.common.prefs.MainUIData;
import com.liskovsoft.smartyoutubetv2.common.prefs.PlayerTweaksData;
import com.liskovsoft.smartyoutubetv2.common.utils.IntentExtractor;
import com.liskovsoft.youtubeapi.search.SearchTagStorage;
import com.liskovsoft.youtubeapi.service.internal.MediaServiceData;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.reactivex.Observable;

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
    private static volatile boolean sActive;
    private static volatile boolean sKids;
    private static volatile boolean sPromptShowing;
    private static volatile String sChoice;

    private FocusMode() {
    }

    public static boolean isActive() {
        return sActive;
    }

    public static void registerSections(Context context, Map<Integer, BrowseSection> sections,
                                        Map<Integer, Observable<MediaGroup>> grids, Map<Integer, Observable<List<MediaGroup>>> rows) {
        Moomoo.register(context, sections, rows);

        if (context != null && context.getResources().getBoolean(R.bool.focus_mode_available)) {
            FocusSections.register(context, sections, rows);
            FocusPrefetch.wrap(grids, rows);
        }
    }

    public static void onAppLaunch(Context context, Intent intent) {
        if (context == null) {
            FocusLog.e(TAG, "dewidebug prompt skipped: no context at app launch");
            return;
        }

        if (!context.getResources().getBoolean(R.bool.focus_mode_available)) {
            return;
        }

        FocusSpeed.warmEngine(context);

        if (IntentExtractor.hasData(intent)) {
            FocusLog.d(TAG, "dewidebug prompt skipped: launched with a link or cast, keeping active=%s", sActive);
            applyShortsHiding(context, sActive);
            return;
        }

        FocusLog.d(TAG, "dewidebug app launch: defaulting to focus mode and showing prompt on a plain background");
        sChoice = null;
        BrowsePresenter.instance(context);
        setKids(context, false);
        setActive(context, true, "default before prompt");
        FocusPrefetch.start(context);

        AppDialogPresenter dialog = AppDialogPresenter.instance(context);
        dialog.appendSingleButton(UiOptionItem.from(context.getString(R.string.focus_mode_yes), option -> {
            sChoice = "Yes";
            setActive(context, true, "user chose Yes");
            dialog.closeDialog();
            FocusLog.d(TAG, "dewidebug landing: search page (user chose Yes)");
            SearchPresenter.instance(context).startSearch(null);
        }));
        dialog.appendSingleButton(UiOptionItem.from(context.getString(R.string.focus_mode_no), option -> {
            sChoice = "No";
            FocusPrefetch.invalidateAll("Shorts setting changed by No");
            setActive(context, false, "user chose No");
            dialog.closeDialog();
            FocusLog.d(TAG, "dewidebug landing: Home (user chose No)");
            BrowsePresenter.instance(context).selectSection(MediaGroup.TYPE_HOME);
        }));
        dialog.appendSingleButton(UiOptionItem.from(context.getString(R.string.focus_mode_kids), option -> {
            sChoice = "Kids";
            setActive(context, true, "user chose Kids");
            setKids(context, true);
            dialog.closeDialog();
            FocusLog.d(TAG, "dewidebug landing: Moomoo (user chose Kids)");
            BrowsePresenter.instance(context).selectSection(Moomoo.SECTION_ID);
        }));
        sPromptShowing = true;
        dialog.showDialog(context.getString(R.string.focus_mode_title), () -> {
            sPromptShowing = false;
            if (sChoice == null) {
                FocusLog.d(TAG, "dewidebug prompt closed with no choice (Back, Home or replaced): staying in focus mode on section %s", bootSectionId(-1));
            } else {
                FocusLog.d(TAG, "dewidebug prompt closed: choice=%s active=%s kids=%s", sChoice, sActive, sKids);
            }
        });
    }

    public static void applyPromptBackground(Window window) {
        if (window == null) {
            return;
        }

        if (sPromptShowing) {
            window.setBackgroundDrawable(new ColorDrawable(Color.BLACK));
            FocusLog.d(TAG, "dewidebug prompt background: solid, so nothing loading behind it shows");
        }
    }

    public static void onDialogDestroyed() {
        if (sPromptShowing) {
            sPromptShowing = false;
            FocusLog.d(TAG, "dewidebug prompt destroyed without closing (system destroy): later dialogs keep their normal background");
        }
    }

    public static boolean plainSearchTags(String query, SearchTagsProvider.ResultsCallback callback) {
        if (!sActive || !TextUtils.isEmpty(query)) {
            return false;
        }

        List<String> own = null;

        try {
            own = GlobalPreferences.sInstance != null ? SearchTagStorage.getTags() : null;
        } catch (RuntimeException | ExceptionInInitializerError e) {
            FocusLog.e(TAG, "dewidebug plain search: reading saved searches failed: %s", e.getMessage());
        }

        FocusLog.d(TAG, "dewidebug plain search: empty box shows %s saved searches, no YouTube suggestions",
                own != null ? own.size() : 0);
        callback.onResults(Tag.from(own));
        return true;
    }

    public static void onRefresh() {
        FocusPrefetch.invalidateAll("refresh requested");
    }

    public static List<Video> filterSections(List<Video> items) {
        if (items == null) {
            return null;
        }

        List<Video> visible = new ArrayList<>();

        if (sKids && Moomoo.isRegistered()) {
            Moomoo.insertAfterChannels(visible);
            return visible;
        }

        for (Video item : items) {
            if (item == null || !sActive || !isHiddenSection(item.sectionId)) {
                visible.add(item);
            }
        }

        Moomoo.insertAfterChannels(visible);

        if (Moomoo.isRegistered()) {
            FocusSections.insertAfter(visible, Moomoo.SECTION_ID);
        }

        return visible;
    }

    public static int bootSectionId(int storedId) {
        if (sKids) {
            return Moomoo.SECTION_ID;
        }

        return sActive ? MediaGroup.TYPE_CHANNEL_UPLOADS : storedId;
    }

    public static boolean topButtonEnabled(boolean storedValue) {
        return storedValue && !sKids;
    }

    public static boolean playerButtonEnabled(int button, boolean storedValue) {
        boolean blockedForKids = (button & (PlayerTweaksData.PLAYER_BUTTON_SEARCH | PlayerTweaksData.PLAYER_BUTTON_OPEN_CHANNEL)) != 0;
        return storedValue && !(sKids && blockedForKids);
    }

    private static void setKids(Context context, boolean kids) {
        if (sKids == kids) {
            return;
        }

        sKids = kids;
        FocusLog.d(TAG, "dewidebug kids=%s", kids);
        BrowsePresenter.instance(context).updateSections();
        MainUIData.instance(context).onDataChange();
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
        FocusLog.d(TAG, "dewidebug active=%s changed=%s reason=%s", active, changed, reason);

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
                FocusLog.d(TAG, "dewidebug saved user shorts mask=%s before hiding all", savedMask);
            }
            data.setContentHidden(MediaServiceData.CONTENT_SHORTS_ALL, true);
        } else if (prefs.contains(KEY_SAVED_SHORTS_MASK)) {
            int savedMask = prefs.getInt(KEY_SAVED_SHORTS_MASK, 0);
            data.setContentHidden(MediaServiceData.CONTENT_SHORTS_ALL, false);
            if (savedMask != 0) {
                data.setContentHidden(savedMask, true);
            }
            prefs.edit().remove(KEY_SAVED_SHORTS_MASK).apply();
            FocusLog.d(TAG, "dewidebug restored user shorts mask=%s", savedMask);
        } else {
            FocusLog.d(TAG, "dewidebug normal mode, no saved shorts mask to restore");
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
