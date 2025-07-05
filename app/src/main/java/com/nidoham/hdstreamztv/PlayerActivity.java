package com.nidoham.hdstreamztv;

import android.annotation.SuppressLint;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.SeekBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.common.VideoSize;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.AspectRatioFrameLayout;

import com.nidoham.hdstreamztv.databinding.ActivityPlayerBinding;
import com.nidoham.hdstreamztv.newpipe.extractors.helper.YouTubeLinkExtractor;
import com.nidoham.hdstreamztv.newpipe.extractors.helper.YouTubeTitleFetcher;
import com.nidoham.hdstreamztv.template.model.settings.Template;

/**
 * পেশাদার ভিডিও প্লেয়ার অ্যাক্টিভিটি - উন্নত আর্কিটেকচার সহ
 * 
 * এই অ্যাক্টিভিটি একটি সম্পূর্ণ ভিডিও প্লেব্যাক অভিজ্ঞতা প্রদান করে:
 * - শক্তিশালী ExoPlayer ইন্টিগ্রেশন সঠিক লাইফসাইকেল ম্যানেজমেন্ট সহ
 * - জেসচার-ভিত্তিক নিয়ন্ত্রণ বুদ্ধিমান অটো-হাইড কার্যকারিতা সহ
 * - প্লেয়ার লক মেকানিজম দুর্ঘটনাজনিত ইন্টারঅ্যাকশন প্রতিরোধের জন্য
 * - YouTube কন্টেন্ট এক্সট্র্যাকশন সঠিক async হ্যান্ডলিং সহ
 * - উচ্চ-নির্ভুলতা প্রগ্রেস ট্র্যাকিং এবং সিকিং
 * - ব্যাপক ত্রুটি হ্যান্ডলিং পুনরুদ্ধার মেকানিজম সহ
 * - মেমরি-অপ্টিমাইজড রিসোর্স ম্যানেজমেন্ট
 * - স্ক্রিন ওরিয়েন্টেশন পরিবর্তনে ভিডিও পুনরায় শুরু না হওয়ার সমাধান
 * 
 * @author Enhanced Professional Version
 * @version 4.0
 */
public class PlayerActivity extends AppCompatActivity {
    
    // ========================================================================================
    // ধ্রুবক এবং কনফিগারেশন - Constants & Configuration
    // ========================================================================================
    
    private static final String TAG = "VideoPlayerActivity";
    
    // UI টাইমিং ধ্রুবক - UI Timing Constants
    private static final int CONTROL_AUTO_HIDE_DELAY_MILLISECONDS = 3000;
    private static final int LOCK_BUTTON_HIDE_DELAY_MILLISECONDS = 2000;
    private static final int PROGRESS_UPDATE_INTERVAL_MILLISECONDS = 500;
    private static final int ERROR_RETRY_DELAY_MILLISECONDS = 2000;
    
    // প্লেয়ার কনফিগারেশন - Player Configuration
    private static final int SEEK_INCREMENT_MILLISECONDS = 10000;
    private static final int SEEK_BAR_MAXIMUM_PRECISION = 1000;
    private static final int MAXIMUM_RETRY_ATTEMPTS = 3;
    
    // ইনটেন্ট কী - Intent Keys
    private static final String EXTRA_VIDEO_URL_KEY = "link";
    private static final String EXTRA_VIDEO_NAME_KEY = "name";
    private static final String EXTRA_VIDEO_CATEGORY_KEY = "category";
    
    // স্টেট সেভ কী - State Save Keys
    private static final String SAVED_PLAYBACK_POSITION_KEY = "playback_position";
    private static final String SAVED_PLAY_WHEN_READY_KEY = "play_when_ready";
    private static final String SAVED_PLAYER_LOCKED_KEY = "player_locked";
    
    // ========================================================================================
    // মূল কম্পোনেন্ট - Core Components
    // ========================================================================================
    
    private ActivityPlayerBinding viewBinding;
    private ExoPlayer mediaPlayer;
    
    // থ্রেড ম্যানেজমেন্ট - Thread Management
    private final Handler mainThreadHandler = new Handler(Looper.getMainLooper());
    private final Handler progressUpdateHandler = new Handler(Looper.getMainLooper());
    
    // নির্ধারিত কাজ - Scheduled Tasks
    private final Runnable hideControlsTask = this::hidePlayerControlsFromView;
    private final Runnable hideLockButtonTask = this::hideLockButtonAfterDelay;
    private Runnable progressUpdateTask;
    
    // স্টেট ম্যানেজমেন্ট - State Management
    private final VideoPlayerStateManager playerStateManager = new VideoPlayerStateManager();
    private final VideoInformationManager videoInfoManager = new VideoInformationManager();
    private final ErrorRecoveryManager errorRecoveryManager = new ErrorRecoveryManager();
    
    // ========================================================================================
    // স্টেট ম্যানেজমেন্ট ক্লাস - State Management Classes
    // ========================================================================================
    
    /**
     * সমস্ত প্লেয়ার-সম্পর্কিত স্টেট থ্রেড নিরাপত্তার সাথে পরিচালনা করে
     * Manages all player-related state with thread safety
     */
    private static class VideoPlayerStateManager {
        
        // প্লেয়ার স্টেট ভেরিয়েবল - Player State Variables
        private volatile boolean isPlayerLocked = false;
        private volatile boolean isUserCurrentlySeeking = false;
        private volatile boolean areControlsCurrentlyVisible = false;
        private volatile boolean shouldAutoPlayWhenReady = true;
        private volatile long lastKnownPlaybackPosition = 0L;
        private volatile int lastRecordedPlaybackState = Player.STATE_IDLE;
        private volatile boolean isPlayerInitialized = false;
        
        // সিঙ্ক্রোনাইজড গেটার এবং সেটার - Synchronized Getters and Setters
        
        public synchronized boolean isPlayerCurrentlyLocked() { 
            return isPlayerLocked; 
        }
        
        public synchronized void setPlayerLockState(boolean locked) { 
            this.isPlayerLocked = locked; 
        }
        
        public synchronized boolean isUserCurrentlySeeking() { 
            return isUserCurrentlySeeking; 
        }
        
        public synchronized void setUserSeekingState(boolean seeking) { 
            this.isUserCurrentlySeeking = seeking; 
        }
        
        public synchronized boolean areControlsCurrentlyVisible() { 
            return areControlsCurrentlyVisible; 
        }
        
        public synchronized void setControlsVisibilityState(boolean visible) { 
            this.areControlsCurrentlyVisible = visible; 
        }
        
        public synchronized boolean shouldAutoPlayWhenReady() { 
            return shouldAutoPlayWhenReady; 
        }
        
        public synchronized void setAutoPlayState(boolean autoPlay) { 
            this.shouldAutoPlayWhenReady = autoPlay; 
        }
        
        public synchronized long getLastKnownPlaybackPosition() { 
            return lastKnownPlaybackPosition; 
        }
        
        public synchronized void updateLastKnownPosition(long position) { 
            this.lastKnownPlaybackPosition = position; 
        }
        
        public synchronized int getLastRecordedPlaybackState() { 
            return lastRecordedPlaybackState; 
        }
        
        public synchronized void updateLastPlaybackState(int state) { 
            this.lastRecordedPlaybackState = state; 
        }
        
        public synchronized boolean isPlayerInitialized() { 
            return isPlayerInitialized; 
        }
        
        public synchronized void setPlayerInitializationState(boolean initialized) { 
            this.isPlayerInitialized = initialized; 
        }
        
        /**
         * সমস্ত স্টেট রিসেট করে - Reset all states
         */
        public synchronized void resetAllStates() {
            isPlayerLocked = false;
            isUserCurrentlySeeking = false;
            areControlsCurrentlyVisible = false;
            shouldAutoPlayWhenReady = true;
            lastKnownPlaybackPosition = 0L;
            lastRecordedPlaybackState = Player.STATE_IDLE;
            isPlayerInitialized = false;
        }
    }
    
    /**
     * ভিডিও তথ্য যাচাইকরণ সহ পরিচালনা করে
     * Manages video information with validation
     */
    private static class VideoInformationManager {
        
        private String originalVideoUrl;
        private String videoDisplayName;
        private int videoContentCategory;
        private String resolvedVideoUrl;
        private boolean isVideoUrlResolved = false;
        
        /**
         * ইনটেন্ট থেকে তথ্য নিষ্কাশন করে - Extract information from intent
         */
        public boolean extractVideoInfoFromIntent(@Nullable Bundle intentExtras) {
            if (intentExtras == null) return false;
            
            originalVideoUrl = intentExtras.getString(EXTRA_VIDEO_URL_KEY);
            videoDisplayName = intentExtras.getString(EXTRA_VIDEO_NAME_KEY);
            videoContentCategory = intentExtras.getInt(EXTRA_VIDEO_CATEGORY_KEY, -1);
            
            return isVideoInformationValid();
        }
        
        /**
         * ভিডিও তথ্য বৈধতা যাচাই - Validate video information
         */
        public boolean isVideoInformationValid() {
            return originalVideoUrl != null && !originalVideoUrl.trim().isEmpty() &&
                   videoDisplayName != null && !videoDisplayName.trim().isEmpty() &&
                   videoContentCategory >= 0;
        }
        
        // গেটার মেথড - Getter Methods
        public String getOriginalVideoUrl() { return originalVideoUrl; }
        public String getVideoDisplayName() { return videoDisplayName; }
        public int getVideoContentCategory() { return videoContentCategory; }
        
        public String getFinalVideoUrlForPlayback() { 
            return isVideoUrlResolved ? resolvedVideoUrl : originalVideoUrl; 
        }
        
        public void setResolvedVideoUrl(String resolvedUrl) {
            this.resolvedVideoUrl = resolvedUrl;
            this.isVideoUrlResolved = true;
        }
        
        public boolean isYouTubeVideoContent() {
            return videoContentCategory == Template.YOUTUBE;
        }
    }
    
    /**
     * পুনরায় চেষ্টা লজিক সহ ত্রুটি পুনরুদ্ধার হ্যান্ডেল করে
     * Handles error recovery with retry logic
     */
    private class ErrorRecoveryManager {
        
        private int currentRetryCount = 0;
        private long lastErrorOccurrenceTime = 0;
        
        /**
         * পুনরায় চেষ্টা করা উচিত কিনা নির্ধারণ করে - Determine if should retry
         */
        public boolean shouldAttemptRetry(PlaybackException playbackError) {
            long currentTime = System.currentTimeMillis();
            
            // যথেষ্ট সময় অতিবাহিত হলে পুনরায় চেষ্টার সংখ্যা রিসেট করুন
            // Reset retry count if enough time has passed
            if (currentTime - lastErrorOccurrenceTime > 30000) { // 30 সেকেন্ড
                currentRetryCount = 0;
            }
            
            lastErrorOccurrenceTime = currentTime;
            return currentRetryCount < MAXIMUM_RETRY_ATTEMPTS;
        }
        
        /**
         * পুনরুদ্ধারের চেষ্টা করে - Attempt recovery
         */
        public void attemptErrorRecovery() {
            currentRetryCount++;
            Log.w(TAG, "পুনরুদ্ধারের চেষ্টা করা হচ্ছে, চেষ্টা: " + currentRetryCount);
            
            mainThreadHandler.postDelayed(() -> {
                if (mediaPlayer != null && !isFinishing()) {
                    try {
                        mediaPlayer.prepare();
                        if (playerStateManager.shouldAutoPlayWhenReady()) {
                            mediaPlayer.play();
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "পুনরুদ্ধারের চেষ্টা ব্যর্থ", e);
                    }
                }
            }, ERROR_RETRY_DELAY_MILLISECONDS);
        }
        
        /**
         * ত্রুটি পুনরুদ্ধার স্টেট রিসেট করে - Reset error recovery state
         */
        public void resetErrorRecoveryState() {
            currentRetryCount = 0;
            lastErrorOccurrenceTime = 0;
        }
    }
    
    // ========================================================================================
    // অ্যাক্টিভিটি লাইফসাইকেল - Activity Lifecycle
    // ========================================================================================
    
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate: PlayerActivity শুরু করা হচ্ছে");
        
        if (!initializeActivityComponents()) {
            Log.e(TAG, "onCreate: অ্যাক্টিভিটি শুরু করতে ব্যর্থ");
            return;
        }
        
        if (!extractAndValidateVideoInformation()) {
            Log.e(TAG, "onCreate: অবৈধ ভিডিও তথ্য");
            return;
        }
        
        // সেভ করা স্টেট পুনরুদ্ধার করুন - Restore saved state
        restorePlayerStateFromBundle(savedInstanceState);
        
        setupCompleteVideoPlayerSystem();
    }
    
    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart: অ্যাক্টিভিটি শুরু হচ্ছে");
        
        if (mediaPlayer != null && viewBinding != null) {
            viewBinding.playerView.onResume();
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume: অ্যাক্টিভিটি পুনরায় শুরু হচ্ছে");
        
        enforceFullscreenImmersiveMode();
        
        if (mediaPlayer != null && playerStateManager.shouldAutoPlayWhenReady()) {
            mediaPlayer.play();
        }
        
        startProgressUpdateCycle();
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause: অ্যাক্টিভিটি বিরতি নিচ্ছে");
        
        pausePlayerAndSaveCurrentState();
        stopProgressUpdateCycle();
    }
    
    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop: অ্যাক্টিভিটি বন্ধ হচ্ছে");
        
        saveCurrentPlayerStateForRestoration();
        
        if (viewBinding != null) {
            viewBinding.playerView.onPause();
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy: রিসোর্স পরিষ্কার করা হচ্ছে");
        
        releaseAllResourcesAndCleanup();
    }
    
    /**
     * কনফিগারেশন পরিবর্তনের সময় স্টেট সেভ করে (স্ক্রিন রোটেশন)
     * Save state during configuration changes (screen rotation)
     */
    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        
        if (mediaPlayer != null) {
            outState.putLong(SAVED_PLAYBACK_POSITION_KEY, mediaPlayer.getCurrentPosition());
            outState.putBoolean(SAVED_PLAY_WHEN_READY_KEY, mediaPlayer.getPlayWhenReady());
            outState.putBoolean(SAVED_PLAYER_LOCKED_KEY, playerStateManager.isPlayerCurrentlyLocked());
            
            Log.d(TAG, "স্টেট সেভ করা হয়েছে - পজিশন: " + mediaPlayer.getCurrentPosition());
        }
    }
    
    /**
     * কনফিগারেশন পরিবর্তনের পরে স্টেট পুনরুদ্ধার করে
     * Restore state after configuration changes
     */
    private void restorePlayerStateFromBundle(@Nullable Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            long savedPosition = savedInstanceState.getLong(SAVED_PLAYBACK_POSITION_KEY, 0);
            boolean playWhenReady = savedInstanceState.getBoolean(SAVED_PLAY_WHEN_READY_KEY, true);
            boolean wasLocked = savedInstanceState.getBoolean(SAVED_PLAYER_LOCKED_KEY, false);
            
            playerStateManager.updateLastKnownPosition(savedPosition);
            playerStateManager.setAutoPlayState(playWhenReady);
            playerStateManager.setPlayerLockState(wasLocked);
            
            Log.d(TAG, "স্টেট পুনরুদ্ধার করা হয়েছে - পজিশন: " + savedPosition);
        }
    }
    
    // ========================================================================================
    // ইনিশিয়ালাইজেশন মেথড - Initialization Methods
    // ========================================================================================
    
    /**
     * সঠিক ত্রুটি হ্যান্ডলিং সহ অ্যাক্টিভিটি কম্পোনেন্ট শুরু করে
     * Initialize activity components with proper error handling
     */
    private boolean initializeActivityComponents() {
        try {
            // ভিউ বাইন্ডিং শুরু করুন - Initialize view binding
            viewBinding = ActivityPlayerBinding.inflate(getLayoutInflater());
            setContentView(viewBinding.getRoot());
            
            // ফুলস্ক্রিন মোড সেটআপ করুন - Setup fullscreen mode
            enforceFullscreenImmersiveMode();
            
            // প্রগ্রেস আপডেট টাস্ক শুরু করুন - Initialize progress update task
            initializeProgressUpdateTask();
            
            Log.d(TAG, "অ্যাক্টিভিটি ইনিশিয়ালাইজেশন সফল");
            return true;
            
        } catch (Exception e) {
            Log.e(TAG, "অ্যাক্টিভিটি শুরু করতে ব্যর্থ", e);
            showErrorMessageAndFinishActivity("প্লেয়ার শুরু করতে ব্যর্থ: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * ইনটেন্ট থেকে ভিডিও তথ্য নিষ্কাশন এবং যাচাই করে
     * Extract and validate video information from intent
     */
    private boolean extractAndValidateVideoInformation() {
        try {
            if (!videoInfoManager.extractVideoInfoFromIntent(getIntent().getExtras())) {
                showErrorMessageAndFinishActivity("অবৈধ ভিডিও ডেটা প্রদান করা হয়েছে");
                return false;
            }
            
            Log.d(TAG, "ভিডিও তথ্য নিষ্কাশিত: " + videoInfoManager.getVideoDisplayName());
            return true;
            
        } catch (Exception e) {
            Log.e(TAG, "ভিডিও তথ্য নিষ্কাশনে ত্রুটি", e);
            showErrorMessageAndFinishActivity("ভিডিও ডেটা প্রক্রিয়াকরণে ত্রুটি: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * নিমজ্জনকারী ফুলস্ক্রিন মোড কনফিগার করে
     * Configure immersive fullscreen mode
     */
    private void enforceFullscreenImmersiveMode() {
        try {
            WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
            
            WindowInsetsControllerCompat windowController = WindowCompat.getInsetsController(
                getWindow(), getWindow().getDecorView());
            
            if (windowController != null) {
                windowController.hide(WindowInsetsCompat.Type.systemBars());
                windowController.setSystemBarsBehavior(
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
            
        } catch (Exception e) {
            Log.w(TAG, "ফুলস্ক্রিন মোড সেট করতে ব্যর্থ", e);
        }
    }
    
    /**
     * প্রগ্রেস আপডেট টাস্ক শুরু করে
     * Initialize the progress update task
     */
    private void initializeProgressUpdateTask() {
        progressUpdateTask = new Runnable() {
            @Override
            public void run() {
                if (mediaPlayer != null && !playerStateManager.isUserCurrentlySeeking() && !isFinishing()) {
                    updateProgressDisplayInformation();
                    
                    if (mediaPlayer.isPlaying()) {
                        progressUpdateHandler.postDelayed(this, PROGRESS_UPDATE_INTERVAL_MILLISECONDS);
                    }
                }
            }
        };
    }
    
    // ========================================================================================
    // প্লেয়ার সেটআপ - Player Setup
    // ========================================================================================
    
    /**
     * সম্পূর্ণ ভিডিও প্লেয়ার সিস্টেম সেটআপ করে
     * Setup the complete video player system
     */
    private void setupCompleteVideoPlayerSystem() {
        try {
            Log.d(TAG, "ভিডিও প্লেয়ার সেটআপ করা হচ্ছে");
            
            createAndConfigureMediaPlayer();
            setupUserInterfaceComponents();
            
            // মিডিয়া প্রস্তুত করার আগে টাইটেল সেটআপ করুন
            // Setup title BEFORE preparing media to ensure it's visible immediately
            setupVideoTitleDisplay();
            
            prepareMediaContentForPlayback();
            
            playerStateManager.setPlayerInitializationState(true);
            showPlayerControlsToUser();
            
            Log.d(TAG, "ভিডিও প্লেয়ার সেটআপ সম্পন্ন");
            
        } catch (Exception e) {
            Log.e(TAG, "ভিডিও প্লেয়ার সেটআপ করতে ব্যর্থ", e);
            showErrorMessageAndFinishActivity("প্লেয়ার সেটআপ করতে ব্যর্থ: " + e.getMessage());
        }
    }
    
    /**
     * সর্বোত্তম সেটিংস সহ ExoPlayer তৈরি এবং কনফিগার করে
     * Create and configure ExoPlayer with optimal settings
     */
    private void createAndConfigureMediaPlayer() {
        try {
            // অপ্টিমাইজড কনফিগারেশন সহ ExoPlayer তৈরি করুন
            // Create ExoPlayer with optimized configuration
            mediaPlayer = new ExoPlayer.Builder(this)
                .setSeekBackIncrementMs(SEEK_INCREMENT_MILLISECONDS)
                .setSeekForwardIncrementMs(SEEK_INCREMENT_MILLISECONDS)
                .build();
            
            // PlayerView কনফিগার করুন - Configure PlayerView
            viewBinding.playerView.setPlayer(mediaPlayer);
            viewBinding.playerView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FIT);
            viewBinding.playerView.setUseController(false); // কাস্টম কন্ট্রোল ব্যবহার করুন
            
            // ব্যাপক ইভেন্ট লিসেনার যোগ করুন - Add comprehensive event listener
            mediaPlayer.addListener(new VideoPlayerEventListener());
            
            Log.d(TAG, "ExoPlayer তৈরি এবং কনফিগার করা হয়েছে");
            
        } catch (Exception e) {
            Log.e(TAG, "ExoPlayer তৈরি করতে ব্যর্থ", e);
            throw new RuntimeException("ExoPlayer তৈরি ব্যর্থ", e);
        }
    }
    
    /**
     * URL রেজোলিউশন সহ প্লেব্যাকের জন্য মিডিয়া প্রস্তুত করে
     * Prepare media for playback with URL resolution
     */
    private void prepareMediaContentForPlayback() {
        if (videoInfoManager.isYouTubeVideoContent()) {
            resolveYouTubeUrlAndPrepareMedia();
        } else {
            prepareMediaWithDirectUrl(videoInfoManager.getOriginalVideoUrl());
        }
    }
    
    /**
     * অ্যাসিঙ্ক্রোনাসভাবে YouTube URL রেজোলভ করে এবং মিডিয়া প্রস্তুত করে
     * Resolve YouTube URL asynchronously and prepare media
     */
    private void resolveYouTubeUrlAndPrepareMedia() {
        Log.d(TAG, "YouTube URL রেজোলভ করা হচ্ছে");
        
        YouTubeLinkExtractor linkExtractor = new YouTubeLinkExtractor();
        linkExtractor.extractVideoLink(
            videoInfoManager.getOriginalVideoUrl(),
            YouTubeLinkExtractor.Quality.BEST,
            new YouTubeLinkExtractor.OnVideoLinkListener() {
                @Override
                public void onVideoLinkExtracted(String extractedUrl, String title) {
                    Log.d(TAG, "YouTube URL সফলভাবে রেজোলভ করা হয়েছে");
                    
                    runOnUiThread(() -> {
                        videoInfoManager.setResolvedVideoUrl(extractedUrl);
                        prepareMediaWithDirectUrl(extractedUrl);
                    });
                }
                
                @Override
                public void onError(String error) {
                    Log.w(TAG, "YouTube URL এক্সট্র্যাকশন ব্যর্থ: " + error);
                    
                    runOnUiThread(() -> {
                        // মূল URL এ ফলব্যাক - Fallback to original URL
                        prepareMediaWithDirectUrl(videoInfoManager.getOriginalVideoUrl());
                    });
                }
            }
        );
    }
    
    /**
     * প্রদত্ত URL দিয়ে মিডিয়া প্রস্তুত করে
     * Prepare media with the given URL
     */
    private void prepareMediaWithDirectUrl(String mediaUrl) {
        try {
            if (mediaPlayer == null || mediaUrl == null || mediaUrl.trim().isEmpty()) {
                throw new IllegalStateException("অবৈধ প্লেয়ার বা URL স্টেট");
            }
            
            Log.d(TAG, "URL দিয়ে মিডিয়া প্রস্তুত করা হচ্ছে: " + mediaUrl);
            
            MediaItem mediaItem = MediaItem.fromUri(Uri.parse(mediaUrl.trim()));
            mediaPlayer.setMediaItem(mediaItem);
            
            // শেষ জানা অবস্থান থেকে পুনরায় শুরু করুন যদি উপলব্ধ থাকে
            // Resume from last known position if available
            long lastPosition = playerStateManager.getLastKnownPlaybackPosition();
            if (lastPosition > 0) {
                mediaPlayer.seekTo(lastPosition);
            }
            
            mediaPlayer.prepare();
            
            if (playerStateManager.shouldAutoPlayWhenReady()) {
                mediaPlayer.play();
            }
            
        } catch (Exception e) {
            Log.e(TAG, "মিডিয়া প্রস্তুত করতে ব্যর্থ", e);
            handleMediaPreparationError(e);
        }
    }
    
    /**
     * মিডিয়া প্রস্তুতির ত্রুটি হ্যান্ডেল করে
     * Handle media preparation errors
     */
    private void handleMediaPreparationError(Exception error) {
        String errorMessage = "ভিডিও লোড করতে ব্যর্থ: " + error.getMessage();
        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
        
        // সম্ভব হলে পুনরুদ্ধারের চেষ্টা করুন - Attempt recovery if possible
        if (errorRecoveryManager.shouldAttemptRetry(new PlaybackException(
            errorMessage, error, PlaybackException.ERROR_CODE_IO_UNSPECIFIED))) {
            errorRecoveryManager.attemptErrorRecovery();
        } else {
            showErrorMessageAndFinishActivity("একাধিক চেষ্টার পরেও ভিডিও চালাতে অক্ষম");
        }
    }
    
    // ========================================================================================
    // প্লেয়ার ইভেন্ট হ্যান্ডলিং - Player Event Handling
    // ========================================================================================
    
    /**
     * ব্যাপক প্লেয়ার ইভেন্ট লিসেনার
     * Comprehensive player event listener
     */
    private class VideoPlayerEventListener implements Player.Listener {
        
        @Override
        public void onPlaybackStateChanged(int playbackState) {
            Log.d(TAG, "প্লেব্যাক স্টেট পরিবর্তিত: " + playbackState);
            
            playerStateManager.updateLastPlaybackState(playbackState);
            handlePlaybackStateTransition(playbackState);
        }
        
        @Override
        public void onIsPlayingChanged(boolean isPlaying) {
            Log.d(TAG, "প্লেয়িং স্টেট পরিবর্তিত: " + isPlaying);
            handlePlayingStateTransition(isPlaying);
        }
        
        @Override
        public void onPlayerError(@NonNull PlaybackException error) {
            Log.e(TAG, "প্লেয়ার ত্রুটি ঘটেছে", error);
            handlePlayerErrorOccurrence(error);
        }
        
        @Override
        public void onVideoSizeChanged(@NonNull VideoSize videoSize) {
            Log.d(TAG, "ভিডিও সাইজ পরিবর্তিত: " + videoSize.width + "x" + videoSize.height);
            // PlayerView স্বয়ংক্রিয়ভাবে অ্যাসপেক্ট রেশিও হ্যান্ডেল করে
        }
        
        @Override
        public void onPositionDiscontinuity(@NonNull Player.PositionInfo oldPosition,
                                          @NonNull Player.PositionInfo newPosition,
                                          int reason) {
            // অবস্থান ট্র্যাকিং আপডেট করুন - Update position tracking
            playerStateManager.updateLastKnownPosition(newPosition.positionMs);
        }
    }
    
    /**
     * প্লেব্যাক স্টেট পরিবর্তন হ্যান্ডেল করে
     * Handle playback state changes
     */
    private void handlePlaybackStateTransition(int playbackState) {
        switch (playbackState) {
            case Player.STATE_READY:
                Log.d(TAG, "প্লেয়ার প্রস্তুত");
                errorRecoveryManager.resetErrorRecoveryState();
                startProgressUpdateCycle();
                break;
                
            case Player.STATE_ENDED:
                Log.d(TAG, "প্লেব্যাক শেষ");
                handlePlaybackCompletionEvent();
                break;
                
            case Player.STATE_BUFFERING:
                Log.d(TAG, "প্লেয়ার বাফারিং");
                // এখানে বাফারিং ইন্ডিকেটর দেখানো যেতে পারে
                break;
                
            case Player.STATE_IDLE:
                Log.d(TAG, "প্লেয়ার নিষ্ক্রিয়");
                break;
        }
    }
    
    /**
     * প্লেয়িং স্টেট পরিবর্তন হ্যান্ডেল করে
     * Handle playing state changes
     */
    private void handlePlayingStateTransition(boolean isPlaying) {
        updatePlayPauseButtonAppearance(isPlaying);
        
        if (isPlaying) {
            startProgressUpdateCycle();
        } else {
            stopProgressUpdateCycle();
        }
    }
    
    /**
     * পুনরুদ্ধার লজিক সহ প্লেয়ার ত্রুটি হ্যান্ডেল করে
     * Handle player errors with recovery logic
     */
    private void handlePlayerErrorOccurrence(@NonNull PlaybackException error) {
        String errorMessage = "প্লেব্যাক ত্রুটি: " + error.getMessage();
        Log.e(TAG, errorMessage, error);
        
        Toast.makeText(this, "ভিডিও প্লেব্যাক ত্রুটি ঘটেছে", Toast.LENGTH_SHORT).show();
        
        if (errorRecoveryManager.shouldAttemptRetry(error)) {
            errorRecoveryManager.attemptErrorRecovery();
        } else {
            showErrorMessageAndFinishActivity("প্লেব্যাক ত্রুটি থেকে পুনরুদ্ধার করতে অক্ষম");
        }
    }
    
    /**
     * প্লেব্যাক সমাপ্তি হ্যান্ডেল করে
     * Handle playback completion
     */
    private void handlePlaybackCompletionEvent() {
        if (mediaPlayer != null) {
            mediaPlayer.seekTo(0);
            mediaPlayer.pause();
        }
        
        showPlayerControlsToUser();
        Toast.makeText(this, "ভিডিও সম্পন্ন", Toast.LENGTH_SHORT).show();
    }
    
    // ========================================================================================
    // ভিডিও টাইটেল ম্যানেজমেন্ট - Video Title Management
    // ========================================================================================
    
    /**
     * উন্নত ত্রুটি হ্যান্ডলিং সহ ভিডিও টাইটেল ডিসপ্লে সেটআপ করে
     * Setup video title display with improved error handling
     */
    private void setupVideoTitleDisplay() {
        Log.d(TAG, "ভিডিও টাইটেল সেটআপ করা হচ্ছে");
        
        // YouTube ভিডিওর জন্য, প্রকৃত টাইটেল আনার চেষ্টা করুন
        // For YouTube videos, try to fetch the actual title
        if (videoInfoManager.isYouTubeVideoContent()) {
            fetchYouTubeTitleAsynchronously();
        } else if (videoInfoManager.getVideoDisplayName() != null && 
                   !videoInfoManager.getVideoDisplayName().trim().isEmpty()) {
            updateVideoTitleInDisplay(videoInfoManager.getVideoDisplayName());
        }
    }
    
    /**
     * উন্নত হ্যান্ডলিং সহ অ্যাসিঙ্ক্রোনাসভাবে YouTube ভিডিও টাইটেল আনে
     * Fetch YouTube video title asynchronously with improved handling
     */
    private void fetchYouTubeTitleAsynchronously() {
        try {
            Log.d(TAG, "YouTube টাইটেল আনা হচ্ছে: " + videoInfoManager.getOriginalVideoUrl());
            
            // UI ব্লক এড়াতে ব্যাকগ্রাউন্ড থ্রেড ব্যবহার করুন
            // Use a background thread for title fetching to avoid blocking UI
            new Thread(() -> {
                try {
                    YouTubeTitleFetcher titleFetcher = YouTubeTitleFetcher.getInstance(PlayerActivity.this);
                    
                    // প্রথমে মূল URL দিয়ে চেষ্টা করুন - Try with the original URL first
                    String urlToUse = videoInfoManager.getOriginalVideoUrl();
                    
                    Log.d(TAG, "ভিডিও ID/URL এর জন্য টাইটেল আনা হচ্ছে: " + urlToUse);
                    
                    titleFetcher.getTitle(urlToUse, new YouTubeTitleFetcher.TitleCallback() {
                        @Override
                        public void onSuccess(String title) {
                            Log.d(TAG, "YouTube টাইটেল সফলভাবে আনা হয়েছে: " + title);
                            
                            // মূল থ্রেডে আপডেট নিশ্চিত করুন - Ensure we update on the main thread
                            runOnUiThread(() -> {
                                if (title != null && !title.trim().isEmpty()) {
                                    updateVideoTitleInDisplay(title);
                                } else {
                                    Log.w(TAG, "খালি টাইটেল পেয়েছি, মূল রাখা হচ্ছে");
                                }
                            });
                        }
                        
                        @Override
                        public void onError(String error) {
                            Log.w(TAG, "YouTube টাইটেল আনতে ব্যর্থ: " + error);
                            
                            // ত্রুটিতে মূল টাইটেল রাখুন - Keep the original title on error
                            runOnUiThread(() -> {
                                String fallbackTitle = videoInfoManager.getVideoDisplayName();
                                if (fallbackTitle != null && !fallbackTitle.trim().isEmpty()) {
                                    updateVideoTitleInDisplay(fallbackTitle);
                                }
                            });
                        }
                    });
                    
                } catch (Exception e) {
                    Log.e(TAG, "YouTube টাইটেল আনার সময় ব্যতিক্রম", e);
                    
                    // মূল টাইটেলে ফলব্যাক - Fallback to original title
                    runOnUiThread(() -> {
                        String fallbackTitle = videoInfoManager.getVideoDisplayName();
                        if (fallbackTitle != null) {
                            updateVideoTitleInDisplay(fallbackTitle);
                        }
                    });
                }
            }).start();
            
        } catch (Exception e) {
            Log.e(TAG, "YouTube টাইটেল আনার সেটআপে ত্রুটি", e);
            // ফলব্যাক হিসেবে মূল টাইটেল ব্যবহার করুন - Use original title as fallback
            updateVideoTitleInDisplay(videoInfoManager.getVideoDisplayName());
        }
    }
    
    /**
     * যাচাইকরণ এবং ফরম্যাটিং সহ UI তে ভিডিও টাইটেল আপডেট করে
     * Update video title in UI with validation and formatting
     */
    private void updateVideoTitleInDisplay(String title) {
        if (title == null || title.trim().isEmpty()) {
            Log.w(TAG, "খালি টাইটেল সেট করার চেষ্টা");
            return;
        }
        
        // নিশ্চিত করুন যে আমরা মূল থ্রেডে আছি - Ensure we're on the main thread
        if (Looper.myLooper() != Looper.getMainLooper()) {
            runOnUiThread(() -> updateVideoTitleInDisplay(title));
            return;
        }
        
        try {
            if (viewBinding != null && viewBinding.channelTitleText != null) {
                // টাইটেল পরিষ্কার এবং ফরম্যাট করুন - Clean and format the title
                String cleanTitle = title.trim();
                
                // খুব দীর্ঘ হলে টাইটেলের দৈর্ঘ্য সীমিত করুন - Limit title length if too long
                if (cleanTitle.length() > 100) {
                    cleanTitle = cleanTitle.substring(0, 97) + "...";
                }
                
                viewBinding.channelTitleText.setText(cleanTitle);
                Log.d(TAG, "ভিডিও টাইটেল আপডেট করা হয়েছে: " + cleanTitle);
                
            } else {
                Log.w(TAG, "টাইটেল আপডেট করতে পারছি না - বাইন্ডিং বা টেক্সট ভিউ null");
            }
        } catch (Exception e) {
            Log.e(TAG, "ভিডিও টাইটেল ডিসপ্লে আপডেট করতে ত্রুটি", e);
        }
    }
    
    /**
     * ভিডিও টাইটেল জোরপূর্বক রিফ্রেশ করে (প্রয়োজনে বাহ্যিকভাবে কল করা যেতে পারে)
     * Force refresh the video title (can be called externally if needed)
     */
    public void forceRefreshVideoTitle() {
        Log.d(TAG, "জোরপূর্বক ভিডিও টাইটেল রিফ্রেশ করা হচ্ছে");
        setupVideoTitleDisplay();
    }
    
    // ========================================================================================
    // ইউজার ইন্টারফেস সেটআপ - User Interface Setup
    // ========================================================================================
    
    /**
     * সমস্ত ইউজার ইন্টারফেস কম্পোনেন্ট সেটআপ করে
     * Setup all user interface components
     */
    private void setupUserInterfaceComponents() {
        setupMainViewClickHandler();
        setupTopBarControlButtons();
        setupCenterPlaybackControlButtons();
        setupBottomBarControlButtons();
        setupSeekBarControlHandlers();
        
        Log.d(TAG, "ইউজার ইন্টারফেস সেটআপ সম্পন্ন");
    }
    
    /**
     * কন্ট্রোল টগলের জন্য মূল ভিউ ক্লিক লিসেনার সেটআপ করে
     * Setup main view click listener for control toggle
     */
    private void setupMainViewClickHandler() {
        viewBinding.getRoot().setOnClickListener(view -> {
            if (playerStateManager.isPlayerCurrentlyLocked()) {
                showLockButtonTemporarilyToUser();
            } else {
                toggleControlsVisibilityState();
            }
        });
    }
    
    /**
     * টপ বার কন্ট্রোল বাটন সেটআপ করে
     * Setup top bar control buttons
     */
    private void setupTopBarControlButtons() {
        viewBinding.closeButton.setOnClickListener(v -> {
            Log.d(TAG, "বন্ধ বাটন ক্লিক করা হয়েছে");
            finish();
        });
        
        viewBinding.resizeButton.setOnClickListener(v -> {
            Log.d(TAG, "রিসাইজ বাটন ক্লিক করা হয়েছে");
            toggleScreenOrientationMode();
        });
    }
    
    /**
     * কেন্দ্রীয় প্লেব্যাক কন্ট্রোল বাটন সেটআপ করে
     * Setup center playback control buttons
     */
    private void setupCenterPlaybackControlButtons() {
        viewBinding.playPauseButton.setOnClickListener(v -> {
            Log.d(TAG, "প্লে/পজ বাটন ক্লিক করা হয়েছে");
            togglePlayPauseState();
        });
        
        viewBinding.rewindButton.setOnClickListener(v -> {
            Log.d(TAG, "রিওয়াইন্ড বাটন ক্লিক করা হয়েছে");
            seekToRelativePosition(-SEEK_INCREMENT_MILLISECONDS);
        });
        
        viewBinding.forwardButton.setOnClickListener(v -> {
            Log.d(TAG, "ফরওয়ার্ড বাটন ক্লিক করা হয়েছে");
            seekToRelativePosition(SEEK_INCREMENT_MILLISECONDS);
        });
    }
    
    /**
     * বটম বার কন্ট্রোল বাটন সেটআপ করে
     * Setup bottom bar control buttons
     */
    private void setupBottomBarControlButtons() {
        viewBinding.lockButton.setOnClickListener(v -> {
            Log.d(TAG, "লক বাটন ক্লিক করা হয়েছে");
            togglePlayerLockState();
        });
        
        viewBinding.fullscreenButton.setOnClickListener(v -> {
            Log.d(TAG, "ফুলস্ক্রিন বাটন ক্লিক করা হয়েছে");
            toggleScreenOrientationMode();
        });
        
        viewBinding.volumeButton.setOnClickListener(v -> {
            Log.d(TAG, "ভলিউম বাটন ক্লিক করা হয়েছে");
            handleVolumeControlAction();
        });
        
        viewBinding.settingsButton.setOnClickListener(v -> {
            Log.d(TAG, "সেটিংস বাটন ক্লিক করা হয়েছে");
            handlePlayerSettingsAction();
        });
    }
    
    /**
     * নির্ভুল নিয়ন্ত্রণ সহ সিক বার সেটআপ করে
     * Setup seek bar with precise control
     */
    private void setupSeekBarControlHandlers() {
        viewBinding.seekBar.setMax(SEEK_BAR_MAXIMUM_PRECISION);
        viewBinding.seekBar.setOnSeekBarChangeListener(new SeekBarChangeHandler());
    }
    
    /**
     * উন্নত সিক বার পরিবর্তন লিসেনার
     * Enhanced seek bar change listener
     */
    private class SeekBarChangeHandler implements SeekBar.OnSeekBarChangeListener {
        
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if (fromUser && mediaPlayer != null) {
                updateTimeDisplayForSeekProgress(progress);
            }
        }
        
        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            Log.d(TAG, "ইউজার সিকিং শুরু করেছে");
            
            playerStateManager.setUserSeekingState(true);
            stopProgressUpdateCycle();
            cancelScheduledControlsHiding();
        }
        
        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            Log.d(TAG, "ইউজার সিকিং বন্ধ করেছে");
            
            if (mediaPlayer != null) {
                performSeekToSpecificProgress(seekBar.getProgress());
            }
            
            playerStateManager.setUserSeekingState(false);
            startProgressUpdateCycle();
            scheduleAutomaticControlsHiding();
        }
    }
    
    // ========================================================================================
    // প্লেব্যাক কন্ট্রোল মেথড - Playback Control Methods
    // ========================================================================================
    
    /**
     * যাচাইকরণ সহ প্লে/পজ স্টেট টগল করে
     * Toggle play/pause state with validation
     */
    private void togglePlayPauseState() {
        if (mediaPlayer == null) {
            Log.w(TAG, "প্লে/পজ টগল করতে পারছি না - প্লেয়ার null");
            return;
        }
        
        try {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.pause();
                Log.d(TAG, "প্লেয়ার পজ করা হয়েছে");
            } else {
                mediaPlayer.play();
                Log.d(TAG, "প্লেয়ার পুনরায় শুরু করা হয়েছে");
            }
        } catch (Exception e) {
            Log.e(TAG, "প্লে/পজ টগল করতে ত্রুটি", e);
        }
    }
    
    /**
     * প্লে/পজ বাটনের চেহারা আপডেট করে
     * Update play/pause button appearance
     */
    private void updatePlayPauseButtonAppearance(boolean isPlaying) {
        if (viewBinding != null) {
            viewBinding.playPauseButton.setImageResource(
                isPlaying ? R.drawable.ic_pause : R.drawable.ic_play);
        }
    }
    
    /**
     * সীমা পরীক্ষা সহ আপেক্ষিক অবস্থানে সিক করে
     * Seek to relative position with bounds checking
     */
    private void seekToRelativePosition(long seekMilliseconds) {
        if (mediaPlayer == null) {
            Log.w(TAG, "সিক করতে পারছি না - প্লেয়ার null");
            return;
        }
        
        try {
            long currentPosition = mediaPlayer.getCurrentPosition();
            long newPosition = currentPosition + seekMilliseconds;
            long duration = mediaPlayer.getDuration();
            
            // অবস্থান বৈধ সীমার মধ্যে আছে তা নিশ্চিত করুন
            // Ensure position is within valid bounds
            if (duration != C.TIME_UNSET) {
                newPosition = Math.max(0, Math.min(newPosition, duration));
            } else {
                newPosition = Math.max(0, newPosition);
            }
            
            mediaPlayer.seekTo(newPosition);
            playerStateManager.updateLastKnownPosition(newPosition);
            
            Log.d(TAG, "অবস্থানে সিক করা হয়েছে: " + newPosition);
            
        } catch (Exception e) {
            Log.e(TAG, "সিক অপারেশনে ত্রুটি", e);
        }
    }
    
    /**
     * প্রগ্রেস বার অবস্থানের ভিত্তিতে সিক সম্পাদন করে
     * Perform seek based on progress bar position
     */
    private void performSeekToSpecificProgress(int progress) {
        if (mediaPlayer == null) {
            Log.w(TAG, "প্রগ্রেসে সিক করতে পারছি না - প্লেয়ার null");
            return;
        }
        
        try {
            long duration = mediaPlayer.getDuration();
            if (duration != C.TIME_UNSET && duration > 0) {
                long newPosition = (duration * progress) / SEEK_BAR_MAXIMUM_PRECISION;
                mediaPlayer.seekTo(newPosition);
                playerStateManager.updateLastKnownPosition(newPosition);
                
                Log.d(TAG, "প্রগ্রেস অবস্থানে সিক করা হয়েছে: " + newPosition);
            }
        } catch (Exception e) {
            Log.e(TAG, "প্রগ্রেসে সিক করতে ত্রুটি", e);
        }
    }
    
    // ========================================================================================
    // প্রগ্রেস ট্র্যাকিং - Progress Tracking
    // ========================================================================================
    
    /**
     * সঠিক সময়সূচী সহ প্রগ্রেস আপডেট শুরু করে
     * Start progress updates with proper scheduling
     */
    private void startProgressUpdateCycle() {
        stopProgressUpdateCycle(); // ডুপ্লিকেট আপডেট নিশ্চিত করুন না
        
        if (progressUpdateTask != null) {
            progressUpdateHandler.post(progressUpdateTask);
        }
    }
    
    /**
     * প্রগ্রেস আপডেট বন্ধ করে
     * Stop progress updates
     */
    private void stopProgressUpdateCycle() {
        if (progressUpdateTask != null) {
            progressUpdateHandler.removeCallbacks(progressUpdateTask);
        }
    }
    
    /**
     * বর্তমান প্লেব্যাক অবস্থান সহ প্রগ্রেস ডিসপ্লে আপডেট করে
     * Update progress display with current playback position
     */
    private void updateProgressDisplayInformation() {
        if (mediaPlayer == null || viewBinding == null) return;
        
        try {
            long currentPosition = mediaPlayer.getCurrentPosition();
            long duration = mediaPlayer.getDuration();
            
            // বর্তমান সময় ডিসপ্লে আপডেট করুন - Update current time display
            viewBinding.currentTimeText.setText(formatTimeToReadableString(currentPosition));
            
            // সময়কাল এবং প্রগ্রেস আপডেট করুন - Update duration and progress
            if (duration != C.TIME_UNSET && duration > 0) {
                viewBinding.totalTimeText.setText(formatTimeToReadableString(duration));
                int progress = (int) ((currentPosition * SEEK_BAR_MAXIMUM_PRECISION) / duration);
                viewBinding.seekBar.setProgress(progress);
            } else {
                viewBinding.totalTimeText.setText("--:--");
                viewBinding.seekBar.setProgress(0);
            }
            
            // স্টেট আপডেট করুন - Update state
            playerStateManager.updateLastKnownPosition(currentPosition);
            
        } catch (Exception e) {
            Log.e(TAG, "প্রগ্রেস ডিসপ্লে আপডেট করতে ত্রুটি", e);
        }
    }
    
    /**
     * সিক বার প্রগ্রেসের জন্য সময় ডিসপ্লে আপডেট করে
     * Update time display for seek bar progress
     */
    private void updateTimeDisplayForSeekProgress(int progress) {
        if (mediaPlayer == null || viewBinding == null) return;
        
        try {
            long duration = mediaPlayer.getDuration();
            if (duration != C.TIME_UNSET && duration > 0) {
                long newPosition = (duration * progress) / SEEK_BAR_MAXIMUM_PRECISION;
                viewBinding.currentTimeText.setText(formatTimeToReadableString(newPosition));
            }
        } catch (Exception e) {
            Log.e(TAG, "প্রগ্রেসের জন্য সময় ডিসপ্লে আপডেট করতে ত্রুটি", e);
        }
    }
    
    /**
     * মিলিসেকেন্ডে সময়কে পাঠযোগ্য ফরম্যাটে ফরম্যাট করে (HH:MM:SS বা MM:SS)
     * Format time in milliseconds to readable format (HH:MM:SS or MM:SS)
     */
    @SuppressLint("DefaultLocale")
    private String formatTimeToReadableString(long milliseconds) {
        if (milliseconds < 0) return "00:00";
        
        long totalSeconds = milliseconds / 1000;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        
        if (hours > 0) {
            return String.format("%02d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format("%02d:%02d", minutes, seconds);
        }
    }
    
    // ========================================================================================
    // কন্ট্রোল দৃশ্যমানতা ম্যানেজমেন্ট - Controls Visibility Management
    // ========================================================================================
    
    /**
     * বুদ্ধিমত্তার সাথে কন্ট্রোল দৃশ্যমানতা টগল করে
     * Toggle controls visibility intelligently
     */
    private void toggleControlsVisibilityState() {
        if (playerStateManager.areControlsCurrentlyVisible()) {
            hidePlayerControlsFromView();
        } else {
            showPlayerControlsToUser();
        }
    }
    
    /**
     * সঠিক স্টেট ম্যানেজমেন্ট সহ প্লেয়ার কন্ট্রোল দেখায়
     * Show player controls with proper state management
     */
    private void showPlayerControlsToUser() {
        if (viewBinding == null) return;
        
        try {
            viewBinding.controlsContainer.setVisibility(View.VISIBLE);
            playerStateManager.setControlsVisibilityState(true);
            
            if (playerStateManager.isPlayerCurrentlyLocked()) {
                showOnlyLockButtonToUser();
            } else {
                showAllControlElementsToUser();
            }
            
            scheduleAutomaticControlsHiding();
            Log.d(TAG, "প্লেয়ার কন্ট্রোল দেখানো হয়েছে");
            
        } catch (Exception e) {
            Log.e(TAG, "প্লেয়ার কন্ট্রোল দেখাতে ত্রুটি", e);
        }
    }
    
    /**
     * প্লেয়ার কন্ট্রোল লুকায়
     * Hide player controls
     */
    private void hidePlayerControlsFromView() {
        if (viewBinding == null) return;
        
        try {
            viewBinding.controlsContainer.setVisibility(View.GONE);
            playerStateManager.setControlsVisibilityState(false);
            cancelScheduledControlsHiding();
            
            Log.d(TAG, "প্লেয়ার কন্ট্রোল লুকানো হয়েছে");
            
        } catch (Exception e) {
            Log.e(TAG, "প্লেয়ার কন্ট্রোল লুকাতে ত্রুটি", e);
        }
    }
    
    /**
     * আনলক অবস্থায় সমস্ত কন্ট্রোল এলিমেন্ট দেখায়
     * Show all control elements when unlocked
     */
    private void showAllControlElementsToUser() {
        if (viewBinding == null) return;
        
        viewBinding.topBar.setVisibility(View.VISIBLE);
        viewBinding.centerControls.setVisibility(View.VISIBLE);
        viewBinding.bottomControls.setVisibility(View.VISIBLE);
        
        // সমস্ত বটম কন্ট্রোল বাটন দেখান - Show all bottom control buttons
        viewBinding.fullscreenButton.setVisibility(View.VISIBLE);
        viewBinding.volumeButton.setVisibility(View.VISIBLE);
        viewBinding.settingsButton.setVisibility(View.VISIBLE);
        viewBinding.lockButton.setVisibility(View.VISIBLE);
    }
    
    /**
     * প্লেয়ার লক থাকলে শুধুমাত্র লক বাটন দেখায়
     * Show only lock button when player is locked
     */
    private void showOnlyLockButtonToUser() {
        if (viewBinding == null) return;
        
        viewBinding.topBar.setVisibility(View.GONE);
        viewBinding.centerControls.setVisibility(View.GONE);
        viewBinding.bottomControls.setVisibility(View.VISIBLE);
        
        // লক বাটন ছাড়া সব বাটন লুকান - Hide all buttons except lock button
        viewBinding.fullscreenButton.setVisibility(View.GONE);
        viewBinding.volumeButton.setVisibility(View.GONE);
        viewBinding.settingsButton.setVisibility(View.GONE);
        viewBinding.lockButton.setVisibility(View.VISIBLE);
    }
    
    /**
     * প্লেয়ার লক থাকলে সাময়িকভাবে লক বাটন দেখায়
     * Show lock button temporarily when player is locked
     */
    private void showLockButtonTemporarilyToUser() {
        if (viewBinding == null) return;
        
        try {
            viewBinding.controlsContainer.setVisibility(View.VISIBLE);
            showOnlyLockButtonToUser();
            
            mainThreadHandler.removeCallbacks(hideLockButtonTask);
            mainThreadHandler.postDelayed(hideLockButtonTask, LOCK_BUTTON_HIDE_DELAY_MILLISECONDS);
            
        } catch (Exception e) {
            Log.e(TAG, "সাময়িকভাবে লক বাটন দেখাতে ত্রুটি", e);
        }
    }
    
    /**
     * লক অবস্থায় বিলম্বের পরে লক বাটন লুকায়
     * Hide lock button after delay when locked
     */
    private void hideLockButtonAfterDelay() {
        if (playerStateManager.isPlayerCurrentlyLocked() && viewBinding != null) {
            viewBinding.controlsContainer.setVisibility(View.GONE);
        }
    }
    
    /**
     * কন্ট্রোলের স্বয়ংক্রিয় লুকানো নির্ধারণ করে
     * Schedule automatic hiding of controls
     */
    private void scheduleAutomaticControlsHiding() {
        if (playerStateManager.isPlayerCurrentlyLocked()) return;
        
        cancelScheduledControlsHiding();
        mainThreadHandler.postDelayed(hideControlsTask, CONTROL_AUTO_HIDE_DELAY_MILLISECONDS);
    }
    
    /**
     * নির্ধারিত কন্ট্রোল লুকানো বাতিল করে
     * Cancel scheduled hiding of controls
     */
    private void cancelScheduledControlsHiding() {
        mainThreadHandler.removeCallbacks(hideControlsTask);
    }
    
    // ========================================================================================
    // প্লেয়ার লক কার্যকারিতা - Player Lock Functionality
    // ========================================================================================
    
    /**
     * প্লেয়ার লক স্টেট টগল করে
     * Toggle player lock state
     */
    private void togglePlayerLockState() {
        boolean newLockState = !playerStateManager.isPlayerCurrentlyLocked();
        playerStateManager.setPlayerLockState(newLockState);
        
        if (newLockState) {
            lockPlayerControls();
        } else {
            unlockPlayerControls();
        }
    }
    
    /**
     * প্লেয়ার কন্ট্রোল লক করে
     * Lock player controls
     */
    private void lockPlayerControls() {
        if (viewBinding == null) return;
        
        try {
            viewBinding.lockButton.setImageResource(R.drawable.ic_lock_open);
            showOnlyLockButtonToUser();
            
            Toast.makeText(this, "প্লেয়ার লক করা হয়েছে - আনলক করতে ট্যাপ করুন", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "প্লেয়ার লক করা হয়েছে");
            
        } catch (Exception e) {
            Log.e(TAG, "প্লেয়ার লক করতে ত্রুটি", e);
        }
    }
    
    /**
     * প্লেয়ার কন্ট্রোল আনলক করে
     * Unlock player controls
     */
    private void unlockPlayerControls() {
        if (viewBinding == null) return;
        
        try {
            viewBinding.lockButton.setImageResource(R.drawable.ic_lock);
            showPlayerControlsToUser();
            
            Toast.makeText(this, "প্লেয়ার আনলক করা হয়েছে", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "প্লেয়ার আনলক করা হয়েছে");
            
        } catch (Exception e) {
            Log.e(TAG, "প্লেয়ার আনলক করতে ত্রুটি", e);
        }
    }
    
    // ========================================================================================
    // স্ক্রিন ওরিয়েন্টেশন ম্যানেজমেন্ট - Screen Orientation Management
    // ========================================================================================
    
    /**
     * ল্যান্ডস্কেপ এবং পোর্ট্রেটের মধ্যে স্ক্রিন ওরিয়েন্টেশন টগল করে
     * Toggle screen orientation between landscape and portrait
     */
    private void toggleScreenOrientationMode() {
        try {
            int currentOrientation = getResources().getConfiguration().orientation;
            
            if (currentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
                Log.d(TAG, "পোর্ট্রেট ওরিয়েন্টেশনে পরিবর্তিত");
            } else {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
                Log.d(TAG, "ল্যান্ডস্কেপ ওরিয়েন্টেশনে পরিবর্তিত");
            }
            
        } catch (Exception e) {
            Log.e(TAG, "স্ক্রিন ওরিয়েন্টেশন টগল করতে ত্রুটি", e);
        }
        
        viewBinding.playerView.onResume();
    }
    
    // ========================================================================================
    // অতিরিক্ত কন্ট্রোল হ্যান্ডলার - Additional Control Handlers
    // ========================================================================================
    
    /**
     * ভলিউম কন্ট্রোল হ্যান্ডেল করে (ভবিষ্যত বাস্তবায়নের জন্য প্লেসহোল্ডার)
     * Handle volume control (placeholder for future implementation)
     */
    private void handleVolumeControlAction() {
        Toast.makeText(this, "ভলিউম কন্ট্রোল - শীঘ্রই আসছে", Toast.LENGTH_SHORT).show();
        // TODO: সিস্টেম ভলিউম ইন্টিগ্রেশন সহ ভলিউম কন্ট্রোল প্যানেল বাস্তবায়ন করুন
    }
    
    /**
     * প্লেয়ার সেটিংস হ্যান্ডেল করে (ভবিষ্যত বাস্তবায়নের জন্য প্লেসহোল্ডার)
     * Handle player settings (placeholder for future implementation)
     */
    private void handlePlayerSettingsAction() {
        Toast.makeText(this, "প্লেয়ার সেটিংস - শীঘ্রই আসছে", Toast.LENGTH_SHORT).show();
        // TODO: সেটিংস প্যানেল বাস্তবায়ন করুন (কোয়ালিটি নির্বাচন, সাবটাইটেল, প্লেব্যাক গতি, ইত্যাদি)
    }
    
    // ========================================================================================
    // স্টেট পার্সিস্টেন্স - State Persistence
    // ========================================================================================
    
    /**
     * প্লেয়ার বিরতি দেয় এবং বর্তমান স্টেট সেভ করে
     * Pause player and save current state
     */
    private void pausePlayerAndSaveCurrentState() {
        if (mediaPlayer != null) {
            try {
                playerStateManager.setAutoPlayState(mediaPlayer.isPlaying());
                mediaPlayer.pause();
                
                Log.d(TAG, "প্লেয়ার বিরতি দেওয়া হয়েছে এবং স্টেট সেভ করা হয়েছে");
                
            } catch (Exception e) {
                Log.e(TAG, "প্লেয়ার বিরতি দিতে এবং স্টেট সেভ করতে ত্রুটি", e);
            }
        }
    }
    
    /**
     * পুনরুদ্ধারের জন্য বর্তমান প্লেয়ার স্টেট সেভ করে
     * Save current player state for restoration
     */
    private void saveCurrentPlayerStateForRestoration() {
        if (mediaPlayer != null) {
            try {
                playerStateManager.updateLastKnownPosition(mediaPlayer.getCurrentPosition());
                playerStateManager.updateLastPlaybackState(mediaPlayer.getPlaybackState());
                
                Log.d(TAG, "প্লেয়ার স্টেট সেভ করা হয়েছে");
                
            } catch (Exception e) {
                Log.e(TAG, "প্লেয়ার স্টেট সেভ করতে ত্রুটি", e);
            }
        }
    }
    
    // ========================================================================================
    // রিসোর্স ম্যানেজমেন্ট - Resource Management
    // ========================================================================================
    
    /**
     * সমস্ত রিসোর্স রিলিজ করে এবং পরিষ্কার করে
     * Release all resources and perform cleanup
     */
    private void releaseAllResourcesAndCleanup() {
        try {
            Log.d(TAG, "রিসোর্স পরিষ্কার করা শুরু");
            
            // সমস্ত নির্ধারিত কাজ বন্ধ করুন - Stop all scheduled tasks
            stopProgressUpdateCycle();
            mainThreadHandler.removeCallbacksAndMessages(null);
            progressUpdateHandler.removeCallbacksAndMessages(null);
            
            // ExoPlayer রিলিজ করুন - Release ExoPlayer
            if (mediaPlayer != null) {
                mediaPlayer.release();
                mediaPlayer = null;
                Log.d(TAG, "ExoPlayer রিলিজ করা হয়েছে");
            }
            
            // স্টেট পরিষ্কার করুন - Clear state
            playerStateManager.resetAllStates();
            errorRecoveryManager.resetErrorRecoveryState();
            
            // বাইন্ডিং রেফারেন্স পরিষ্কার করুন - Clear binding reference
            viewBinding = null;
            
            Log.d(TAG, "রিসোর্স পরিষ্কার করা সম্পন্ন");
            
        } catch (Exception e) {
            Log.e(TAG, "রিসোর্স পরিষ্কার করার সময় ত্রুটি", e);
        }
    }
    
    // ========================================================================================
    // ইউটিলিটি মেথড - Utility Methods
    // ========================================================================================
    
    /**
     * ত্রুটি বার্তা দেখায় এবং অ্যাক্টিভিটি শেষ করে
     * Show error message and finish activity
     */
    private void showErrorMessageAndFinishActivity(String errorMessage) {
        Log.e(TAG, "ত্রুটি দেখানো হচ্ছে এবং শেষ করা হচ্ছে: " + errorMessage);
        
        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
        finish();
    }
    
    /**
     * অ্যাক্টিভিটি অপারেশনের জন্য বৈধ অবস্থায় আছে কিনা পরীক্ষা করে
     * Check if activity is in a valid state for operations
     */
    private boolean isActivityInValidState() {
        return !isFinishing() && !isDestroyed() && viewBinding != null;
    }
    
    /**
     * বর্তমান প্লেয়ার অবস্থা লগ করে (ডিবাগিং এর জন্য)
     * Log current player state (for debugging)
     */
    private void logCurrentPlayerState() {
        if (mediaPlayer != null) {
            Log.d(TAG, "বর্তমান প্লেয়ার স্টেট: " +
                "অবস্থান=" + mediaPlayer.getCurrentPosition() +
                ", সময়কাল=" + mediaPlayer.getDuration() +
                ", চলছে=" + mediaPlayer.isPlaying() +
                ", লক=" + playerStateManager.isPlayerCurrentlyLocked());
        }
    }
    
    /**
     * প্লেয়ার স্বাস্থ্য পরীক্ষা করে
     * Check player health
     */
    private boolean isPlayerHealthy() {
        return mediaPlayer != null && 
               playerStateManager.isPlayerInitialized() && 
               isActivityInValidState();
    }
}
