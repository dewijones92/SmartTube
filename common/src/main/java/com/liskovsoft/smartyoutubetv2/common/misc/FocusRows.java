package com.liskovsoft.smartyoutubetv2.common.misc;

import com.liskovsoft.mediaserviceinterfaces.ContentService;
import com.liskovsoft.mediaserviceinterfaces.data.MediaGroup;
import com.liskovsoft.mediaserviceinterfaces.data.MediaItem;
import com.liskovsoft.sharedutils.mylogger.Log;
import com.liskovsoft.youtubeapi.service.YouTubeServiceManager;

import java.util.ArrayList;
import java.util.List;

final class FocusRows {
    private static final String TAG = FocusRows.class.getSimpleName();
    static final String LATEST = "Latest";
    static final String POPULAR = "Popular";
    private static final String DOT = " · ";

    private FocusRows() {
    }

    static ContentService content() {
        return YouTubeServiceManager.instance().getContentService();
    }

    static void addChip(List<MediaGroup> rows, String channelId, String name, String chipTitle, int fallbackIndex) {
        MediaGroup group = loadChip(channelId, chipTitle, fallbackIndex);
        add(rows, group, name + DOT + chipTitle, channelId);
    }

    static void addSearch(List<MediaGroup> rows, String channelId, String query, String title) {
        MediaGroup group = null;

        try {
            group = content().getChannelSearchObserve(channelId, query).blockingFirst();
        } catch (Exception e) {
            Log.e(TAG, "dewidebug search '%s' in %s failed: %s", query, channelId, e.getMessage());
        }

        add(rows, group, title, channelId);
    }

    static void add(List<MediaGroup> rows, MediaGroup group, String title, String source) {
        if (group != null && !group.isEmpty()) {
            rows.add(new TitledGroup(group, title, null));
            Log.d(TAG, "dewidebug row '%s': %s videos", title, group.getMediaItems().size());
        } else {
            Log.e(TAG, "dewidebug row '%s' empty (source %s)", title, source);
        }
    }

    static MediaGroup filtered(MediaGroup group, String title, List<MediaItem> items) {
        return new TitledGroup(group, title, items);
    }

    private static MediaGroup loadChip(String channelId, String chipTitle, int fallbackIndex) {
        try {
            List<MediaGroup> chips = content().getChannelSortingOptionsObserve(channelId).blockingFirst();

            if (chips == null || chips.isEmpty()) {
                Log.e(TAG, "dewidebug channel %s: no sort options", channelId);
                return null;
            }

            MediaGroup chip = null;

            for (MediaGroup option : chips) {
                if (option != null && chipTitle.equalsIgnoreCase(option.getTitle())) {
                    chip = option;
                    break;
                }
            }

            if (chip == null) {
                chip = chips.size() > fallbackIndex ? chips.get(fallbackIndex) : chips.get(0);
                Log.d(TAG, "dewidebug channel %s: no chip '%s', using '%s'", channelId, chipTitle, chip != null ? chip.getTitle() : null);
            }

            return content().continueGroupObserve(chip).blockingFirst();
        } catch (Exception e) {
            Log.e(TAG, "dewidebug channel %s failed: %s", channelId, e.getMessage());
            return null;
        }
    }

    private static final class TitledGroup implements MediaGroup {
        private final MediaGroup mGroup;
        private final String mTitle;
        private final List<MediaItem> mItems;

        TitledGroup(MediaGroup group, String title, List<MediaItem> items) {
            mGroup = group;
            mTitle = title;
            mItems = items;
        }

        @Override
        public int getType() {
            return mGroup.getType();
        }

        @Override
        public List<MediaItem> getMediaItems() {
            return mItems != null ? mItems : mGroup.getMediaItems();
        }

        @Override
        public String getTitle() {
            return mTitle;
        }

        @Override
        public String getChannelId() {
            return mGroup.getChannelId();
        }

        @Override
        public String getParams() {
            return mGroup.getParams();
        }

        @Override
        public String getReloadPageKey() {
            return null;
        }

        @Override
        public String getNextPageKey() {
            return null;
        }

        @Override
        public String getChannelUrl() {
            return mGroup.getChannelUrl();
        }

        @Override
        public boolean isEmpty() {
            List<MediaItem> items = getMediaItems();
            return items == null || items.isEmpty();
        }
    }

    static List<MediaItem> partWatched(MediaGroup group, int minPercent, int maxPercent) {
        List<MediaItem> result = new ArrayList<>();

        if (group == null || group.getMediaItems() == null) {
            return result;
        }

        for (MediaItem item : group.getMediaItems()) {
            if (item != null && !item.isLive() && item.getPercentWatched() >= minPercent && item.getPercentWatched() <= maxPercent) {
                result.add(item);
            }
        }

        return result;
    }
}
