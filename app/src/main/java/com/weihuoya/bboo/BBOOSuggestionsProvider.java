package com.weihuoya.bboo;

import android.content.SearchRecentSuggestionsProvider;

/**
 * Created by zhangwei on 2016/4/19.
 */
public class BBOOSuggestionsProvider extends SearchRecentSuggestionsProvider {
    public final static String AUTHORITY = "com.weihuoya.bboo.BBOOSuggestionsProvider";
    public final static int MODE = DATABASE_MODE_QUERIES;

    public BBOOSuggestionsProvider() {
        setupSuggestions(AUTHORITY, MODE);
    }
}
