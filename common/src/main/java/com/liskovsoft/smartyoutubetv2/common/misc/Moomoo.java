package com.liskovsoft.smartyoutubetv2.common.misc;

import android.content.Context;

import com.liskovsoft.mediaserviceinterfaces.data.MediaGroup;
import com.liskovsoft.sharedutils.helpers.MessageHelpers;
import com.liskovsoft.sharedutils.mylogger.Log;
import com.liskovsoft.sharedutils.rx.RxHelper;
import com.liskovsoft.smartyoutubetv2.common.R;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.BrowseSection;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.Video;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.VideoGroup;
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.manager.PlayerConstants;
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.manager.PlayerEngine;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.reactivex.Observable;

public final class Moomoo {
    private static final String TAG = Moomoo.class.getSimpleName();
    public static final int SECTION_ID = 9001;
    private static final String TITLE = "Moomoo";
    private static final String[][] CHANNELS = {
            {"UCG2CL6EUjG8TVT1Tpl9nJdg", "Ms Rachel"},
            {"UCAOtE1V7Ots4DjM8JLlrYgg", "Peppa Pig"},
            {"UCVzLLZkDuFGAE2BGdBuBNBg", "Bluey"},
            {"UC574PCE8QOIoXsOGAyL3obQ", "Hey Duggee"},
            {"UC0fXNmjDoC7ckPyT6qM8Urw", "CBeebies"},
            {"UCbCmjCuTUZos6Inko4u57UQ", "Cocomelon"}
    };
    private static final long MAX_TICK_GAP_MS = 70_000;
    private static final long BEDTIME_RESET_MS = 60 * 60 * 1_000;
    private static long sWatchedMs;
    private static long sLastWatchTickMs;
    private static boolean sBedtimeAnnounced;
    private static final long PRELOAD_BEFORE_END_MS = 120_000;
    private static boolean sRegistered;
    private static String sPreloadedKey;
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

    public static boolean isRegistered() {
        return sRegistered;
    }

    public static boolean isMoomooVideo(Video video) {
        VideoGroup group = video != null ? video.getGroup() : null;
        return group != null && group.getSection() != null && group.getSection().getId() == SECTION_ID;
    }

    private static boolean hasNextInRow(Video video) {
        VideoGroup group = video.getGroup();
        int index = group.indexOf(video);
        return index >= 0 && index < group.getSize() - 1;
    }

    public static int playbackMode(Video video, int storedMode) {
        if (!isMoomooVideo(video)) {
            return storedMode;
        }

        VideoGroup group = video.getGroup();
        int index = group.indexOf(video);
        boolean hasNext = index >= 0 && index < group.getSize() - 1;
        boolean bedtime = sBedtimeAnnounced;
        Log.d(TAG, "dewidebug Moomoo playback: index=%s size=%s hasNext=%s bedtime=%s watched=%s ms", index, group.getSize(), hasNext, bedtime, sWatchedMs);

        return hasNext && !bedtime ? PlayerConstants.PLAYBACK_MODE_ALL : PlayerConstants.PLAYBACK_MODE_PAUSE;
    }

    public interface NextVideo {
        Video get();
    }

    public static void onTickle(Context context, PlayerEngine player, Video video, NextVideo nextVideo) {
        if (player == null || !isMoomooVideo(video)) {
            return;
        }

        countWatching(context, player);
        preloadNextIfNeeded(context, player, video, nextVideo);
    }

    private static void countWatching(Context context, PlayerEngine player) {
        long now = System.currentTimeMillis();

        if (sLastWatchTickMs > 0 && now - sLastWatchTickMs > BEDTIME_RESET_MS) {
            Log.d(TAG, "dewidebug Moomoo bedtime count reset after a break");
            sWatchedMs = 0;
            sBedtimeAnnounced = false;
        }

        long gapMs = now - sLastWatchTickMs;

        if (player.isPlaying() && sLastWatchTickMs > 0 && gapMs <= MAX_TICK_GAP_MS) {
            sWatchedMs += gapMs;
        }

        sLastWatchTickMs = now;
        int limitMinutes = FocusSettings.bedtimeMinutes(context);
        boolean overLimit = limitMinutes > 0 && sWatchedMs >= limitMinutes * 60_000L;

        if (!overLimit && sBedtimeAnnounced) {
            Log.d(TAG, "dewidebug Moomoo bedtime lifted: limit now %s min, watched %s ms", limitMinutes, sWatchedMs);
            sBedtimeAnnounced = false;
        }

        if (overLimit && !sBedtimeAnnounced) {
            sBedtimeAnnounced = true;
            Log.d(TAG, "dewidebug Moomoo bedtime reached: watched %s ms, limit %s min", sWatchedMs, limitMinutes);
            MessageHelpers.showLongMessage(context, "Last one! Bedtime after this episode \uD83C\uDF19");
        }
    }

    private static void preloadNextIfNeeded(Context context, PlayerEngine player, Video video, NextVideo nextVideo) {
        if (sBedtimeAnnounced || !hasNextInRow(video) || !player.containsMedia()) {
            return;
        }

        long remainingMs;

        try {
            remainingMs = player.getDurationMs() - player.getPositionMs();
        } catch (RuntimeException e) {
            Log.e(TAG, "dewidebug Moomoo preload skipped: player not ready (%s)", e.toString());
            return;
        }

        if (remainingMs <= 0 || remainingMs > PRELOAD_BEFORE_END_MS) {
            return;
        }

        Video next = nextVideo.get();
        String key = next != null && next.videoId != null ? video.videoId + ">" + next.videoId : null;

        if (key == null || key.equals(sPreloadedKey)) {
            return;
        }

        sPreloadedKey = key;
        Log.d(TAG, "dewidebug Moomoo preloading next episode %s, %s ms before the end", next.videoId, remainingMs);
        FocusSpeed.prefetchFormatInfo(context, next);
    }

    private static List<MediaGroup> loadRows() {
        List<MediaGroup> rows = new ArrayList<>();

        for (String[] channel : CHANNELS) {
            FocusRows.addChip(rows, channel[0], channel[1], FocusRows.POPULAR, 1);
        }

        return rows;
    }
}
