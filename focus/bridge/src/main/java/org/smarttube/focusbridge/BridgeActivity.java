package org.smarttube.focusbridge;

import android.app.Activity;
import android.app.SearchManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

public class BridgeActivity extends Activity {
    private static final String TAG = "FocusBridge";
    private static final String TARGET_PACKAGE = "org.smarttube.focus";
    private static final String TARGET_ACTIVITY = "com.liskovsoft.smartyoutubetv2.tv.ui.main.SplashActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        forward(getIntent());
        finish();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        forward(intent);
        finish();
    }

    private void forward(Intent incoming) {
        Intent target = new Intent();
        target.setComponent(new ComponentName(TARGET_PACKAGE, TARGET_ACTIVITY));
        target.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        String action = incoming != null ? incoming.getAction() : null;
        Uri data = incoming != null ? incoming.getData() : null;
        String query = incoming != null ? incoming.getStringExtra(SearchManager.QUERY) : null;

        if (data != null && data.isHierarchical() && !TextUtils.isEmpty(query) && TextUtils.isEmpty(data.getQueryParameter("query"))
                && TextUtils.isEmpty(data.getQueryParameter("q")) && TextUtils.isEmpty(data.getQueryParameter("search_query"))) {
            Log.i(TAG, "dewidebug bridge: link " + data + " has no query, using the spoken query instead");
            data = null;
        }

        if (data != null) {
            target.setAction(Intent.ACTION_VIEW);
            target.setData(data);
            Log.i(TAG, "dewidebug bridge: forwarding link to Focus, action=" + action + " data=" + data);
        } else if (!TextUtils.isEmpty(query)) {
            Uri search = new Uri.Builder().scheme("youtube").authority("search")
                    .appendQueryParameter("query", query)
                    .appendQueryParameter("isVoice", "true")
                    .build();
            target.setAction(Intent.ACTION_VIEW);
            target.setData(search);
            Log.i(TAG, "dewidebug bridge: voice query \"" + query + "\" sent to Focus as " + search);
        } else {
            target.setAction(Intent.ACTION_MAIN);
            target.addCategory(Intent.CATEGORY_LAUNCHER);
            Log.i(TAG, "dewidebug bridge: no link or query (action=" + action + "), opening Focus normally");
        }

        try {
            startActivity(target);
        } catch (ActivityNotFoundException | SecurityException e) {
            Log.e(TAG, "dewidebug bridge: could not open Focus (" + TARGET_PACKAGE + "): " + e.getMessage());
        }
    }
}
