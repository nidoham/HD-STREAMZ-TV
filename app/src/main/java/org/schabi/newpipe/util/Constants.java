package org.schabi.newpipe.util;

/**
 * A utility class containing constants used across the application.
 * This class is generated from the Kotlin file with @file:JvmName("Constants").
 */
public final class Constants {
    // Prevent instantiation of this utility class
    private Constants() {
        throw new AssertionError("No instances.");
    }

    /**
     * Default duration for throttle functions across the app, in milliseconds.
     */
    public static final long DEFAULT_THROTTLE_TIMEOUT = 120L;

    /**
     * Key for service ID in intents or bundles.
     */
    public static final String KEY_SERVICE_ID = "key_service_id";

    /**
     * Key for URL in intents or bundles.
     */
    public static final String KEY_URL = "key_url";

    /**
     * Key for title in intents or bundles.
     */
    public static final String KEY_TITLE = "key_title";

    /**
     * Key for link type in intents or bundles.
     */
    public static final String KEY_LINK_TYPE = "key_link_type";

    /**
     * Key for open search action in intents or bundles.
     */
    public static final String KEY_OPEN_SEARCH = "key_open_search";

    /**
     * Key for search string in intents or bundles.
     */
    public static final String KEY_SEARCH_STRING = "key_search_string";

    /**
     * Key for theme change events or settings.
     */
    public static final String KEY_THEME_CHANGE = "key_theme_change";

    /**
     * Key for main page change events or settings.
     */
    public static final String KEY_MAIN_PAGE_CHANGE = "key_main_page_change";

    /**
     * Constant indicating no service ID is specified.
     */
    public static final int NO_SERVICE_ID = -1;
}