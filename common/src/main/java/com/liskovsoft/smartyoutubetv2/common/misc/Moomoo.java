package com.liskovsoft.smartyoutubetv2.common.misc;

import android.content.Context;

import com.liskovsoft.mediaserviceinterfaces.ContentService;
import com.liskovsoft.mediaserviceinterfaces.data.MediaGroup;
import com.liskovsoft.mediaserviceinterfaces.data.MediaItem;
import com.liskovsoft.sharedutils.mylogger.Log;
import com.liskovsoft.sharedutils.rx.RxHelper;
import com.liskovsoft.smartyoutubetv2.common.R;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.BrowseSection;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.Video;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.VideoGroup;
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.manager.PlayerConstants;
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.manager.PlayerEngine;
import com.liskovsoft.youtubeapi.service.YouTubeServiceManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.reactivex.Observable;

public final class Moomoo {
    private static final String TAG = Moomoo.class.getSimpleName();
    public static final int SECTION_ID = 9001;
    private static final String TITLE = "Moomoo";
    private static final String POPULAR = "Popular";
    private static final int POPULAR_CHIP_INDEX = 1;
    private static final String[][] CHANNELS = {
            {"UCG2CL6EUjG8TVT1Tpl9nJdg", "Ms Rachel"},
            {"UCAOtE1V7Ots4DjM8JLlrYgg", "Peppa Pig"}
    };
    private static final long PRELOAD_BEFORE_END_MS = 120_000;
    private static boolean sRegistered;
    private static String sPreloadedVideoId;
    private static Video sSectionItem;

    private Moomoo() {
    }

    public static void register(Context context, Map<Integer, BrowseSection> sections, Map<Integer, Observable<List<MediaGroup>>> rows) {
        if (context == null || !context.getResources().getBoolean(R.bool.focus_mode_available)) {
            return;
        }

        sections.put(SECTION_ID, new BrowseSection(SECTION_ID, TITLE, BrowseSection.TYPE_ROW, R.drawable.icon_moomoo, false));
        rows.put(SECTION_ID, RxHelper.fromCallable(Moomoo::loadRows));
        sRegistered = true;
        Log.d(TAG, "dewidebug Moomoo section registered id=%s", SECTION_ID);
    }

    public static void insertAfterChannels(List<Video> visible) {
        if (!sRegistered || visible == null) {
            return;
        }

        if (sSectionItem == null) {
            sSectionItem = new Video();
            sSectionItem.sectionId = SECTION_ID;
        }

        int index = 0;

        for (int i = 0; i < visible.size(); i++) {
            Video item = visible.get(i);
            if (item != null && item.sectionId == MediaGroup.TYPE_CHANNEL_UPLOADS) {
                index = i + 1;
                break;
            }
        }

        visible.add(index, sSectionItem);
    }

    public static boolean isMoomooVideo(Video video) {
        VideoGroup group = video != null ? video.getGroup() : null;
        return group != null && group.getSection() != null && group.getSection().getId() == SECTION_ID;
    }

    public static int playbackMode(Video video, int storedMode) {
        if (!isMoomooVideo(video)) {
            return storedMode;
        }

        VideoGroup group = video.getGroup();
        int index = group.indexOf(video);
        boolean hasNext = index >= 0 && index < group.getSize() - 1;
        Log.d(TAG, "dewidebug Moomoo playback: index=%s size=%s hasNext=%s", index, group.getSize(), hasNext);

        return hasNext ? PlayerConstants.PLAYBACK_MODE_ALL : PlayerConstants.PLAYBACK_MODE_PAUSE;
    }

    public static void preloadNextIfNeeded(Context context, PlayerEngine player, Video video, Video next) {
        if (player == null || next == null || next.videoId == null || !isMoomooVideo(video)) {
            return;
        }

        long remainingMs = player.getDurationMs() - player.getPositionMs();

        if (remainingMs <= 0 || remainingMs > PRELOAD_BEFORE_END_MS || next.videoId.equals(sPreloadedVideoId)) {
            return;
        }

        sPreloadedVideoId = next.videoId;
        Log.d(TAG, "dewidebug Moomoo preloading next episode %s, %s ms before the end", next.videoId, remainingMs);
        FocusSpeed.prefetchFormatInfo(context, next);
    }

    private static List<MediaGroup> loadRows() {
        ContentService content = YouTubeServiceManager.instance().getContentService();
        List<MediaGroup> rows = new ArrayList<>();

        for (String[] channel : CHANNELS) {
            MediaGroup popular = loadPopular(content, channel[0]);

            if (popular != null && !popular.isEmpty()) {
                rows.add(new TitledGroup(popular, channel[1] + " \u00B7 " + POPULAR));
                Log.d(TAG, "dewidebug Moomoo row %s: %s videos", channel[1], popular.getMediaItems().size());
            } else {
                Log.e(TAG, "dewidebug Moomoo row %s: no popular videos (channel %s)", channel[1], channel[0]);
            }
        }

        return rows;
    }

    private static MediaGroup loadPopular(ContentService content, String channelId) {
        try {
            List<MediaGroup> chips = content.getChannelSortingOptionsObserve(channelId).blockingFirst();

            if (chips == null || chips.isEmpty()) {
                Log.e(TAG, "dewidebug Moomoo channel %s: no sort options", channelId);
                return null;
            }

            MediaGroup chip = null;

            for (MediaGroup option : chips) {
                if (option != null && POPULAR.equalsIgnoreCase(option.getTitle())) {
                    chip = option;
                    break;
                }
            }

            if (chip == null) {
                chip = chips.size() > POPULAR_CHIP_INDEX ? chips.get(POPULAR_CHIP_INDEX) : chips.get(0);
                Log.d(TAG, "dewidebug Moomoo channel %s: no chip titled Popular, using '%s'", channelId, chip != null ? chip.getTitle() : null);
            }

            return content.continueGroupObserve(chip).blockingFirst();
        } catch (Exception e) {
            Log.e(TAG, "dewidebug Moomoo channel %s failed: %s", channelId, e.getMessage());
            return null;
        }
    }

    private static final class TitledGroup implements MediaGroup {
        private final MediaGroup mGroup;
        private final String mTitle;

        TitledGroup(MediaGroup group, String title) {
            mGroup = group;
            mTitle = title;
        }

        @Override
        public int getType() {
            return mGroup.getType();
        }

        @Override
        public List<MediaItem> getMediaItems() {
            return mGroup.getMediaItems();
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
            return mGroup.isEmpty();
        }
    }
}
