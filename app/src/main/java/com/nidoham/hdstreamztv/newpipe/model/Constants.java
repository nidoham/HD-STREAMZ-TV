package com.nidoham.hdstreamztv.newpipe.model;

public final class Constants {

    private Constants() {
        // Utility class: prevent instantiation
    }

    /**
     * Default duration when using throttle functions across the app, in milliseconds.
     */
    public static final long DEFAULT_THROTTLE_TIMEOUT = 120L;

    public static final String KEY_SERVICE_ID = "key_service_id";
    public static final String KEY_URL = "key_url";
    public static final String KEY_TITLE = "key_title";
    public static final String KEY_LINK_TYPE = "key_link_type";
    public static final String KEY_OPEN_SEARCH = "key_open_search";
    public static final String KEY_SEARCH_STRING = "key_search_string";

    public static final String KEY_THEME_CHANGE = "key_theme_change";
    public static final String KEY_MAIN_PAGE_CHANGE = "key_main_page_change";

    public static final int NO_SERVICE_ID = -1;
}