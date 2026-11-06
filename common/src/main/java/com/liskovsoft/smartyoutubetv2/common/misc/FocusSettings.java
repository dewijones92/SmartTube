package com.liskovsoft.smartyoutubetv2.common.misc;

import android.content.Context;
import android.content.SharedPreferences;

import com.liskovsoft.smartyoutubetv2.common.R;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.SettingsItem;
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.ui.OptionItem;
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.ui.UiOptionItem;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.AppDialogPresenter;

import java.util.ArrayList;
import java.util.List;

public final class FocusSettings {
    private static final String TAG = FocusSettings.class.getSimpleName();
    private static final String PREFS_NAME = "focus_mode";
    private static final String KEY_BEDTIME_MINUTES = "bedtime_minutes";
    private static final int DEFAULT_BEDTIME_MINUTES = 45;
    private static final int[] BEDTIME_CHOICES = {0, 30, 45, 60, 90};

    private FocusSettings() {
    }

    public static void addSettingsItem(Context context, List<SettingsItem> items) {
        if (!FocusSpeed.isEnabled(context)) {
            return;
        }

        items.add(new SettingsItem("SmartTube Focus", () -> show(context), R.drawable.settings_focus));
    }

    static int bedtimeMinutes(Context context) {
        if (context == null) {
            return 0;
        }

        return prefs(context).getInt(KEY_BEDTIME_MINUTES, DEFAULT_BEDTIME_MINUTES);
    }

    private static void show(Context context) {
        AppDialogPresenter dialog = AppDialogPresenter.instance(context);
        List<OptionItem> options = new ArrayList<>();
        int current = bedtimeMinutes(context);

        for (int minutes : BEDTIME_CHOICES) {
            String title = minutes == 0 ? "Off" : minutes + " minutes";
            options.add(UiOptionItem.from(title, option -> {
                prefs(context).edit().putInt(KEY_BEDTIME_MINUTES, minutes).apply();
                FocusLog.d(TAG, "dewidebug bedtime set to %s min", minutes);
            }, minutes == current));
        }

        dialog.appendRadioCategory("Moomoo bedtime: stop after", options);
        dialog.showDialog("SmartTube Focus");
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
}
