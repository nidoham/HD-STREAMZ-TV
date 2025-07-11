package bd.nidoham.intent;

public class IntentKeys {

    // KIOSK Keys
    public static final int EXTRA_KIOSK_YOUTUBE = 0;
    public static final int EXTRA_KIOSK_LINK = 1;
    
    // UI Timing Constants
    private static final int CONTROL_AUTO_HIDE_DELAY_MS = 3000;
    private static final int LOCK_BUTTON_HIDE_DELAY_MS = 2000;
    private static final int PROGRESS_UPDATE_INTERVAL_MS = 500;
    private static final int ERROR_RETRY_DELAY_MS = 2000;
    
    // Player Configuration
    private static final int SEEK_INCREMENT_MS = 10000;
    private static final int SEEK_BAR_MAX_PRECISION = 1000;
    private static final int MAX_RETRY_ATTEMPTS = 3;
    
    // Intent Keys
    public static final String EXTRA_VIDEO_URL = "link";
    public static final String EXTRA_VIDEO_NAME = "name";
    public static final String EXTRA_VIDEO_CATEGORY = "category";
    public static final String EXTRA_VIDEO_QUALITY = "quality";
    
    // State Save Keys
    private static final String SAVED_PLAYBACK_POSITION = "playback_position";
    private static final String SAVED_PLAY_WHEN_READY = "play_when_ready";
    private static final String SAVED_PLAYER_LOCKED = "player_locked";
}
