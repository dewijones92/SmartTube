package com.liskovsoft.smartyoutubetv2.common.misc;

import android.content.Context;

import com.liskovsoft.mediaserviceinterfaces.data.MediaGroup;
import com.liskovsoft.mediaserviceinterfaces.data.MediaItem;
import com.liskovsoft.sharedutils.mylogger.Log;
import com.liskovsoft.sharedutils.rx.RxHelper;
import com.liskovsoft.smartyoutubetv2.common.R;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.BrowseSection;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.Video;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.reactivex.Observable;

final class FocusSections {
    private static final String TAG = FocusSections.class.getSimpleName();
    static final int TENNIS_ID = 9002;
    static final int CONTINUE_ID = 9003;
    private static final String TENNIS_TV = "UCbcxFkd6B9xUU54InHv4Tig";
    private static final int MIN_PERCENT = 5;
    private static final int MAX_PERCENT = 95;
    private static Video sTennisItem;
    private static Video sContinueItem;

    private FocusSections() {
    }

    static void register(Context context, Map<Integer, BrowseSection> sections, Map<Integer, Observable<List<MediaGroup>>> rows) {
        sections.put(TENNIS_ID, new BrowseSection(TENNIS_ID, "Tennis", BrowseSection.TYPE_ROW, R.drawable.icon_tennis, false));
        rows.put(TENNIS_ID, RxHelper.fromCallable(FocusSections::loadTennis));
        sections.put(CONTINUE_ID, new BrowseSection(CONTINUE_ID, "Continue watching", BrowseSection.TYPE_ROW, R.drawable.icon_continue, true));
        rows.put(CONTINUE_ID, RxHelper.fromCallable(FocusSections::loadContinue));
        Log.d(TAG, "dewidebug Tennis and Continue watching sections registered");
    }

    static void insertAfter(List<Video> visible, int afterSectionId) {
        if (sTennisItem == null) {
            sTennisItem = new Video();
            sTennisItem.sectionId = TENNIS_ID;
            sContinueItem = new Video();
            sContinueItem.sectionId = CONTINUE_ID;
        }

        int index = visible.size();

        for (int i = 0; i < visible.size(); i++) {
            Video item = visible.get(i);
            if (item != null && item.sectionId == afterSectionId) {
                index = i + 1;
                break;
            }
        }

        visible.add(index, sContinueItem);
        visible.add(index, sTennisItem);
    }

    private static List<MediaGroup> loadTennis() {
        List<MediaGroup> rows = new ArrayList<>();
        FocusRows.addChip(rows, TENNIS_TV, "Tennis TV", FocusRows.LATEST, 0);
        FocusRows.addSearch(rows, TENNIS_TV, "Alcaraz", "Carlos Alcaraz");
        return rows;
    }

    private static List<MediaGroup> loadContinue() {
        List<MediaGroup> rows = new ArrayList<>();

        try {
            MediaGroup history = FocusRows.content().getHistoryObserve().blockingFirst();
            List<MediaItem> items = FocusRows.partWatched(history, MIN_PERCENT, MAX_PERCENT);
            Log.d(TAG, "dewidebug continue watching: %s of %s history items part-watched", items.size(),
                    history != null && history.getMediaItems() != null ? history.getMediaItems().size() : 0);

            if (!items.isEmpty()) {
                rows.add(FocusRows.filtered(history, "Continue watching", items));
            }
        } catch (Exception e) {
            Log.e(TAG, "dewidebug continue watching failed: %s", e.getMessage());
        }

        return rows;
    }
}
