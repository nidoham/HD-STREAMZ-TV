package com.nidoham.hdstreamztv;

import android.annotation.SuppressLint;
import android.content.pm.ActivityInfo;
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
 * Professional Video Player Activity with Enhanced Architecture
 * 
 * This activity provides a comprehensive video playback experience with:
 * - Robust ExoPlayer integration with proper lifecycle management
 * - Gesture-based controls with intelligent auto-hide functionality
 * - Player lock mechanism to prevent accidental interactions
 * - YouTube content extraction with proper async handling
 * - High-precision progress tracking and seeking
 * - Comprehensive error handling with recovery mechanisms
 * - Memory-optimized resource management
 * - Professional UI/UX patterns
 * 
 * @author Enhanced Professional Version
 * @version 3.0
 */
public class PlayerActivity extends AppCompatActivity {

    // ========================================================================================
    // CONSTANTS & CONFIGURATION
    // ========================================================================================
    
    private static final String TAG = "PlayerActivity";
    
    // UI Timing Constants
    private static final int CONTROLS_AUTO_HIDE_DELAY_MS = 3000;
    private static final int LOCK_BUTTON_HIDE_DELAY_MS = 2000;
    private static final int PROGRESS_UPDATE_INTERVAL_MS = 500;
    private static final int ERROR_RETRY_DELAY_MS = 2000;
    
    // Player Configuration
    private static final int SEEK_INCREMENT_MS = 10000;
    private static final int SEEK_BAR_PRECISION = 1000;
    private static final int MAX_RETRY_ATTEMPTS = 3;
    
    // Intent Keys
    private static final String EXTRA_VIDEO_URL = "link";
    private static final String EXTRA_VIDEO_NAME = "name";
    private static final String EXTRA_VIDEO_CATEGORY = "category";
    
    // ========================================================================================
    // CORE COMPONENTS
    // ========================================================================================
    
    private ActivityPlayerBinding binding;
    private ExoPlayer exoPlayer;
    
    // Thread Management
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Handler progressHandler = new Handler(Looper.getMainLooper());
    
    // Scheduled Tasks
    private final Runnable hideControlsRunnable = this::hidePlayerControls;
    private final Runnable hideLockButtonRunnable = this::hideLockButtonDelayed;
    private Runnable progressUpdateRunnable;
    
    // State Management
    private final PlayerStateManager stateManager = new PlayerStateManager();
    private final VideoInfoManager videoInfoManager = new VideoInfoManager();
    private final ErrorRecoveryManager errorRecoveryManager = new ErrorRecoveryManager();

    // ========================================================================================
    // STATE MANAGEMENT CLASSES
    // ========================================================================================
    
    /**
     * Manages all player-related state with thread safety
     */
    private static class PlayerStateManager {
        private volatile boolean isLocked = false;
        private volatile boolean isUserSeeking = false;
        private volatile boolean areControlsVisible = false;
        private volatile boolean shouldAutoPlay = true;
        private volatile long lastKnownPosition = 0L;
        private volatile int lastPlaybackState = Player.STATE_IDLE;
        private volatile boolean isInitialized = false;
        
        // Getters and setters with proper synchronization
        public synchronized boolean isLocked() { return isLocked; }
        public synchronized void setLocked(boolean locked) { this.isLocked = locked; }
        
        public synchronized boolean isUserSeeking() { return isUserSeeking; }
        public synchronized void setUserSeeking(boolean seeking) { this.isUserSeeking = seeking; }
        
        public synchronized boolean areControlsVisible() { return areControlsVisible; }
        public synchronized void setControlsVisible(boolean visible) { this.areControlsVisible = visible; }
        
        public synchronized boolean shouldAutoPlay() { return shouldAutoPlay; }
        public synchronized void setShouldAutoPlay(boolean autoPlay) { this.shouldAutoPlay = autoPlay; }
        
        public synchronized long getLastKnownPosition() { return lastKnownPosition; }
        public synchronized void setLastKnownPosition(long position) { this.lastKnownPosition = position; }
        
        public synchronized int getLastPlaybackState() { return lastPlaybackState; }
        public synchronized void setLastPlaybackState(int state) { this.lastPlaybackState = state; }
        
        public synchronized boolean isInitialized() { return isInitialized; }
        public synchronized void setInitialized(boolean initialized) { this.isInitialized = initialized; }
        
        public synchronized void reset() {
            isLocked = false;
            isUserSeeking = false;
            areControlsVisible = false;
            shouldAutoPlay = true;
            lastKnownPosition = 0L;
            lastPlaybackState = Player.STATE_IDLE;
            isInitialized = false;
        }
    }
    
    /**
     * Manages video information with validation
     */
    private static class VideoInfoManager {
        private String videoUrl;
        private String videoName;
        private int videoCategory;
        private String resolvedUrl;
        private boolean isUrlResolved = false;
        
        public boolean extractFromIntent(@Nullable Bundle extras) {
            if (extras == null) return false;
            
            videoUrl = extras.getString(EXTRA_VIDEO_URL);
            videoName = extras.getString(EXTRA_VIDEO_NAME);
            videoCategory = extras.getInt(EXTRA_VIDEO_CATEGORY, -1);
            
            return isValid();
        }
        
        public boolean isValid() {
            return videoUrl != null && !videoUrl.trim().isEmpty() &&
                   videoName != null && !videoName.trim().isEmpty() &&
                   videoCategory >= 0;
        }
        
        // Getters
        public String getVideoUrl() { return videoUrl; }
        public String getVideoName() { return videoName; }
        public int getVideoCategory() { return videoCategory; }
        public String getResolvedUrl() { return isUrlResolved ? resolvedUrl : videoUrl; }
        
        public void setResolvedUrl(String url) {
            this.resolvedUrl = url;
            this.isUrlResolved = true;
        }
        
        public boolean isYouTubeVideo() {
            return videoCategory == Template.YOUTUBE;
        }
    }
    
    /**
     * Handles error recovery with retry logic
     */
    private class ErrorRecoveryManager {
        private int retryCount = 0;
        private long lastErrorTime = 0;
        
        public boolean shouldRetry(PlaybackException error) {
            long currentTime = System.currentTimeMillis();
            
            // Reset retry count if enough time has passed
            if (currentTime - lastErrorTime > 30000) { // 30 seconds
                retryCount = 0;
            }
            
            lastErrorTime = currentTime;
            return retryCount < MAX_RETRY_ATTEMPTS;
        }
        
        public void attemptRecovery() {
            retryCount++;
            Log.w(TAG, "Attempting recovery, attempt: " + retryCount);
            
            mainHandler.postDelayed(() -> {
                if (exoPlayer != null && !isFinishing()) {
                    try {
                        exoPlayer.prepare();
                        if (stateManager.shouldAutoPlay()) {
                            exoPlayer.play();
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Recovery attempt failed", e);
                    }
                }
            }, ERROR_RETRY_DELAY_MS);
        }
        
        public void reset() {
            retryCount = 0;
            lastErrorTime = 0;
        }
    }

    // ========================================================================================
    // ACTIVITY LIFECYCLE
    // ========================================================================================
    
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate: Initializing PlayerActivity");
        
        if (!initializeActivity()) {
            Log.e(TAG, "onCreate: Failed to initialize activity");
            return;
        }
        
        if (!extractAndValidateVideoInfo()) {
            Log.e(TAG, "onCreate: Invalid video information");
            return;
        }
        
        setupVideoPlayer();
    }
    
    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart: Activity starting");
        
        if (exoPlayer != null && binding != null) {
            binding.playerView.onResume();
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume: Activity resuming");
        
        enforceFullscreenMode();
        
        if (exoPlayer != null && stateManager.shouldAutoPlay()) {
            exoPlayer.play();
        }
        
        startProgressUpdates();
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause: Activity pausing");
        
        pausePlayerAndSaveState();
        stopProgressUpdates();
    }
    
    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop: Activity stopping");
        
        saveCurrentPlayerState();
        
        if (binding != null) {
            binding.playerView.onPause();
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy: Cleaning up resources");
        
        releaseAllResources();
    }

    // ========================================================================================
    // INITIALIZATION METHODS
    // ========================================================================================
    
    /**
     * Initialize activity components with proper error handling
     */
    private boolean initializeActivity() {
        try {
            // Initialize view binding
            binding = ActivityPlayerBinding.inflate(getLayoutInflater());
            setContentView(binding.getRoot());
            
            // Setup fullscreen mode
            enforceFullscreenMode();
            
            // Initialize progress update task
            initializeProgressUpdateTask();
            
            Log.d(TAG, "Activity initialization successful");
            return true;
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize activity", e);
            showErrorAndFinish("Failed to initialize player: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Extract and validate video information from intent
     */
    private boolean extractAndValidateVideoInfo() {
        try {
            if (!videoInfoManager.extractFromIntent(getIntent().getExtras())) {
                showErrorAndFinish("Invalid video data provided");
                return false;
            }
            
            Log.d(TAG, "Video info extracted: " + videoInfoManager.getVideoName());
            return true;
            
        } catch (Exception e) {
            Log.e(TAG, "Error extracting video information", e);
            showErrorAndFinish("Error processing video data: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Configure immersive fullscreen mode
     */
    private void enforceFullscreenMode() {
        try {
            WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
            
            WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(
                getWindow(), getWindow().getDecorView());
            
            if (controller != null) {
                controller.hide(WindowInsetsCompat.Type.systemBars());
                controller.setSystemBarsBehavior(
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
            
        } catch (Exception e) {
            Log.w(TAG, "Failed to set fullscreen mode", e);
        }
    }
    
    /**
     * Initialize the progress update task
     */
    private void initializeProgressUpdateTask() {
        progressUpdateRunnable = new Runnable() {
            @Override
            public void run() {
                if (exoPlayer != null && !stateManager.isUserSeeking() && !isFinishing()) {
                    updateProgressDisplay();
                    
                    if (exoPlayer.isPlaying()) {
                        progressHandler.postDelayed(this, PROGRESS_UPDATE_INTERVAL_MS);
                    }
                }
            }
        };
    }

    // ========================================================================================
    // PLAYER SETUP
    // ========================================================================================
    
    /**
     * Setup the complete video player system
     */
    private void setupVideoPlayer() {
        try {
            Log.d(TAG, "Setting up video player");
            
            createAndConfigureExoPlayer();
            setupUserInterface();
            
            // Setup title BEFORE preparing media to ensure it's visible immediately
            setupVideoTitle();
            
            prepareMediaForPlayback();
            
            stateManager.setInitialized(true);
            showPlayerControls();
            
            Log.d(TAG, "Video player setup completed");
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to setup video player", e);
            showErrorAndFinish("Failed to setup player: " + e.getMessage());
        }
    }
    
    /**
     * Create and configure ExoPlayer with optimal settings
     */
    private void createAndConfigureExoPlayer() {
        try {
            // Create ExoPlayer with optimized configuration
            exoPlayer = new ExoPlayer.Builder(this)
                .setSeekBackIncrementMs(SEEK_INCREMENT_MS)
                .setSeekForwardIncrementMs(SEEK_INCREMENT_MS)
                .build();
            
            // Configure PlayerView
            binding.playerView.setPlayer(exoPlayer);
            binding.playerView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FIT);
            binding.playerView.setUseController(false); // Use custom controls
            
            // Add comprehensive event listener
            exoPlayer.addListener(new PlayerEventListener());
            
            Log.d(TAG, "ExoPlayer created and configured");
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to create ExoPlayer", e);
            throw new RuntimeException("ExoPlayer creation failed", e);
        }
    }
    
    /**
     * Prepare media for playback with URL resolution
     */
    private void prepareMediaForPlayback() {
        if (videoInfoManager.isYouTubeVideo()) {
            resolveYouTubeUrlAndPrepare();
        } else {
            prepareMediaWithUrl(videoInfoManager.getVideoUrl());
        }
    }
    
    /**
     * Resolve YouTube URL asynchronously and prepare media
     */
    private void resolveYouTubeUrlAndPrepare() {
        Log.d(TAG, "Resolving YouTube URL");
        
        YouTubeLinkExtractor extractor = new YouTubeLinkExtractor();
        extractor.extractVideoLink(
            videoInfoManager.getVideoUrl(),
            YouTubeLinkExtractor.Quality.BEST,
            new YouTubeLinkExtractor.OnVideoLinkListener() {
                @Override
                public void onVideoLinkExtracted(String extractedUrl, String title) {
                    Log.d(TAG, "YouTube URL resolved successfully");
                    
                    runOnUiThread(() -> {
                        videoInfoManager.setResolvedUrl(extractedUrl);
                        prepareMediaWithUrl(extractedUrl);
                    });
                }
                
                @Override
                public void onError(String error) {
                    Log.w(TAG, "YouTube URL extraction failed: " + error);
                    
                    runOnUiThread(() -> {
                        // Fallback to original URL
                        prepareMediaWithUrl(videoInfoManager.getVideoUrl());
                    });
                }
            }
        );
    }
    
    /**
     * Prepare media with the given URL
     */
    private void prepareMediaWithUrl(String mediaUrl) {
        try {
            if (exoPlayer == null || mediaUrl == null || mediaUrl.trim().isEmpty()) {
                throw new IllegalStateException("Invalid player or URL state");
            }
            
            Log.d(TAG, "Preparing media with URL: " + mediaUrl);
            
            MediaItem mediaItem = MediaItem.fromUri(Uri.parse(mediaUrl.trim()));
            exoPlayer.setMediaItem(mediaItem);
            
            // Resume from last known position if available
            long lastPosition = stateManager.getLastKnownPosition();
            if (lastPosition > 0) {
                exoPlayer.seekTo(lastPosition);
            }
            
            exoPlayer.prepare();
            
            if (stateManager.shouldAutoPlay()) {
                exoPlayer.play();
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to prepare media", e);
            handleMediaPreparationError(e);
        }
    }
    
    /**
     * Handle media preparation errors
     */
    private void handleMediaPreparationError(Exception error) {
        String errorMessage = "Failed to load video: " + error.getMessage();
        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
        
        // Attempt recovery if possible
        if (errorRecoveryManager.shouldRetry(new PlaybackException(
            errorMessage, error, PlaybackException.ERROR_CODE_IO_UNSPECIFIED))) {
            errorRecoveryManager.attemptRecovery();
        } else {
            showErrorAndFinish("Unable to play video after multiple attempts");
        }
    }

    // ========================================================================================
    // PLAYER EVENT HANDLING
    // ========================================================================================
    
    /**
     * Comprehensive player event listener
     */
    private class PlayerEventListener implements Player.Listener {
        
        @Override
        public void onPlaybackStateChanged(int playbackState) {
            Log.d(TAG, "Playback state changed: " + playbackState);
            
            stateManager.setLastPlaybackState(playbackState);
            handlePlaybackStateChange(playbackState);
        }
        
        @Override
        public void onIsPlayingChanged(boolean isPlaying) {
            Log.d(TAG, "Playing state changed: " + isPlaying);
            handlePlayingStateChange(isPlaying);
        }
        
        @Override
        public void onPlayerError(@NonNull PlaybackException error) {
            Log.e(TAG, "Player error occurred", error);
            handlePlayerError(error);
        }
        
        @Override
        public void onVideoSizeChanged(@NonNull VideoSize videoSize) {
            Log.d(TAG, "Video size changed: " + videoSize.width + "x" + videoSize.height);
            // PlayerView handles aspect ratio automatically
        }
        
        @Override
        public void onPositionDiscontinuity(@NonNull Player.PositionInfo oldPosition,
                                          @NonNull Player.PositionInfo newPosition,
                                          int reason) {
            // Update position tracking
            stateManager.setLastKnownPosition(newPosition.positionMs);
        }
    }
    
    /**
     * Handle playback state changes
     */
    private void handlePlaybackStateChange(int playbackState) {
        switch (playbackState) {
            case Player.STATE_READY:
                Log.d(TAG, "Player ready");
                errorRecoveryManager.reset();
                startProgressUpdates();
                break;
                
            case Player.STATE_ENDED:
                Log.d(TAG, "Playback ended");
                handlePlaybackCompleted();
                break;
                
            case Player.STATE_BUFFERING:
                Log.d(TAG, "Player buffering");
                // Could show buffering indicator here
                break;
                
            case Player.STATE_IDLE:
                Log.d(TAG, "Player idle");
                break;
        }
    }
    
    /**
     * Handle playing state changes
     */
    private void handlePlayingStateChange(boolean isPlaying) {
        updatePlayPauseButtonState(isPlaying);
        
        if (isPlaying) {
            startProgressUpdates();
        } else {
            stopProgressUpdates();
        }
    }
    
    /**
     * Handle player errors with recovery logic
     */
    private void handlePlayerError(@NonNull PlaybackException error) {
        String errorMessage = "Playback error: " + error.getMessage();
        Log.e(TAG, errorMessage, error);
        
        Toast.makeText(this, "Video playback error occurred", Toast.LENGTH_SHORT).show();
        
        if (errorRecoveryManager.shouldRetry(error)) {
            errorRecoveryManager.attemptRecovery();
        } else {
            showErrorAndFinish("Unable to recover from playback error");
        }
    }
    
    /**
     * Handle playback completion
     */
    private void handlePlaybackCompleted() {
        if (exoPlayer != null) {
            exoPlayer.seekTo(0);
            exoPlayer.pause();
        }
        
        showPlayerControls();
        Toast.makeText(this, "Video completed", Toast.LENGTH_SHORT).show();
    }

    // ========================================================================================
    // VIDEO TITLE MANAGEMENT
    // ========================================================================================

    /**
     * Setup video title display with improved error handling
     */
    private void setupVideoTitle() {
        Log.d(TAG, "Setting up video title");
        
        // For YouTube videos, try to fetch the actual title
        if (videoInfoManager.isYouTubeVideo()) {
            fetchYouTubeTitle();
        } else if(videoInfoManager.getVideoName() != null && !videoInfoManager.getVideoName().trim().isEmpty()) {
        	updateVideoTitleDisplay(videoInfoManager.getVideoName());
        }
    }

    /**
     * Fetch YouTube video title asynchronously with improved handling
     */
    private void fetchYouTubeTitle() {
        try {
            Log.d(TAG, "Fetching YouTube title for: " + videoInfoManager.getVideoUrl());
            
            // Use a background thread for title fetching to avoid blocking UI
            new Thread(() -> {
                try {
                    YouTubeTitleFetcher titleFetcher = YouTubeTitleFetcher.getInstance(PlayerActivity.this);
                    
                    // Try with the original URL first
                    String urlToUse = videoInfoManager.getVideoUrl();
                    
                    Log.d(TAG, "Fetching title for video ID/URL: " + urlToUse);
                    
                    titleFetcher.getTitle(urlToUse, new YouTubeTitleFetcher.TitleCallback() {
                        @Override
                        public void onSuccess(String title) {
                            Log.d(TAG, "YouTube title fetched successfully: " + title);
                            
                            // Ensure we update on the main thread
                            runOnUiThread(() -> {
                                if (title != null && !title.trim().isEmpty()) {
                                    updateVideoTitleDisplay(title);
                                } else {
                                    Log.w(TAG, "Received empty title, keeping original");
                                }
                            });
                        }
                        
                        @Override
                        public void onError(String error) {
                            Log.w(TAG, "Failed to fetch YouTube title: " + error);
                            
                            // Keep the original title on error
                            runOnUiThread(() -> {
                                String fallbackTitle = videoInfoManager.getVideoName();
                                if (fallbackTitle != null && !fallbackTitle.trim().isEmpty()) {
                                    updateVideoTitleDisplay(fallbackTitle);
                                }
                            });
                        }
                    });
                    
                } catch (Exception e) {
                    Log.e(TAG, "Exception while fetching YouTube title", e);
                    
                    // Fallback to original title
                    runOnUiThread(() -> {
                        String fallbackTitle = videoInfoManager.getVideoName();
                        if (fallbackTitle != null) {
                            updateVideoTitleDisplay(fallbackTitle);
                        }
                    });
                }
            }).start();
            
        } catch (Exception e) {
            Log.e(TAG, "Error setting up YouTube title fetch", e);
            // Use original title as fallback
            updateVideoTitleDisplay(videoInfoManager.getVideoName());
        }
    }

    /**
     * Extract YouTube video ID from various URL formats
     */
    private String extractYouTubeVideoId(String url) {
        if (url == null || url.trim().isEmpty()) {
            return null;
        }
        
        try {
            // Handle different YouTube URL formats
            String videoId = null;
            
            // Standard YouTube URL: https://www.youtube.com/watch?v=VIDEO_ID
            if (url.contains("youtube.com/watch?v=")) {
                int startIndex = url.indexOf("v=") + 2;
                int endIndex = url.indexOf("&", startIndex);
                if (endIndex == -1) {
                    endIndex = url.length();
                }
                videoId = url.substring(startIndex, endIndex);
            }
            // Short YouTube URL: https://youtu.be/VIDEO_ID
            else if (url.contains("youtu.be/")) {
                int startIndex = url.lastIndexOf("/") + 1;
                int endIndex = url.indexOf("?", startIndex);
                if (endIndex == -1) {
                    endIndex = url.length();
                }
                videoId = url.substring(startIndex, endIndex);
            }
            // Mobile YouTube URL: https://m.youtube.com/watch?v=VIDEO_ID
            else if (url.contains("m.youtube.com/watch?v=")) {
                int startIndex = url.indexOf("v=") + 2;
                int endIndex = url.indexOf("&", startIndex);
                if (endIndex == -1) {
                    endIndex = url.length();
                }
                videoId = url.substring(startIndex, endIndex);
            }
            // If it's already just a video ID (11 characters)
            else if (url.length() == 11 && url.matches("[a-zA-Z0-9_-]+")) {
                videoId = url;
            }
            
            Log.d(TAG, "Extracted video ID: " + videoId + " from URL: " + url);
            return videoId;
            
        } catch (Exception e) {
            Log.e(TAG, "Error extracting YouTube video ID from: " + url, e);
            return null;
        }
    }

    /**
     * Update video title in UI with validation and formatting
     */
    private void updateVideoTitleDisplay(String title) {
        if (title == null || title.trim().isEmpty()) {
            Log.w(TAG, "Attempted to set empty title");
            return;
        }
        
        // Ensure we're on the main thread
        if (Looper.myLooper() != Looper.getMainLooper()) {
            runOnUiThread(() -> updateVideoTitleDisplay(title));
            return;
        }
        
        try {
            if (binding != null && binding.channelTitleText != null) {
                // Clean and format the title
                String cleanTitle = title.trim();
                
                // Limit title length if too long
                if (cleanTitle.length() > 100) {
                    cleanTitle = cleanTitle.substring(0, 97) + "...";
                }
                
                binding.channelTitleText.setText(cleanTitle);
                Log.d(TAG, "Video title updated to: " + cleanTitle);
                
            } else {
                Log.w(TAG, "Cannot update title - binding or text view is null");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating video title display", e);
        }
    }

    /**
     * Force refresh the video title (can be called externally if needed)
     */
    public void refreshVideoTitle() {
        Log.d(TAG, "Force refreshing video title");
        setupVideoTitle();
    }

    // ========================================================================================
    // USER INTERFACE SETUP
    // ========================================================================================
    
    /**
     * Setup all user interface components
     */
    private void setupUserInterface() {
        setupMainViewClickListener();
        setupTopBarControls();
        setupCenterPlaybackControls();
        setupBottomBarControls();
        setupSeekBarControls();
        
        Log.d(TAG, "User interface setup completed");
    }
    
    /**
     * Setup main view click listener for control toggle
     */
    private void setupMainViewClickListener() {
        binding.getRoot().setOnClickListener(view -> {
            if (stateManager.isLocked()) {
                showLockButtonTemporarily();
            } else {
                toggleControlsVisibility();
            }
        });
    }
    
    /**
     * Setup top bar control buttons
     */
    private void setupTopBarControls() {
        binding.closeButton.setOnClickListener(v -> {
            Log.d(TAG, "Close button clicked");
            finish();
        });
        
        binding.resizeButton.setOnClickListener(v -> {
            Log.d(TAG, "Resize button clicked");
            toggleScreenOrientation();
        });
    }
    
    /**
     * Setup center playback control buttons
     */
    private void setupCenterPlaybackControls() {
        binding.playPauseButton.setOnClickListener(v -> {
            Log.d(TAG, "Play/Pause button clicked");
            togglePlayPause();
        });
        
        binding.rewindButton.setOnClickListener(v -> {
            Log.d(TAG, "Rewind button clicked");
            seekRelative(-SEEK_INCREMENT_MS);
        });
        
        binding.forwardButton.setOnClickListener(v -> {
            Log.d(TAG, "Forward button clicked");
            seekRelative(SEEK_INCREMENT_MS);
        });
    }
    
    /**
     * Setup bottom bar control buttons
     */
    private void setupBottomBarControls() {
        binding.lockButton.setOnClickListener(v -> {
            Log.d(TAG, "Lock button clicked");
            togglePlayerLock();
        });
        
        binding.fullscreenButton.setOnClickListener(v -> {
            Log.d(TAG, "Fullscreen button clicked");
            toggleScreenOrientation();
        });
        
        binding.volumeButton.setOnClickListener(v -> {
            Log.d(TAG, "Volume button clicked");
            handleVolumeControl();
        });
        
        binding.settingsButton.setOnClickListener(v -> {
            Log.d(TAG, "Settings button clicked");
            handlePlayerSettings();
        });
    }
    
    /**
     * Setup seek bar with precise control
     */
    private void setupSeekBarControls() {
        binding.seekBar.setMax(SEEK_BAR_PRECISION);
        binding.seekBar.setOnSeekBarChangeListener(new SeekBarChangeListener());
    }
    
    /**
     * Enhanced seek bar change listener
     */
    private class SeekBarChangeListener implements SeekBar.OnSeekBarChangeListener {
        
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if (fromUser && exoPlayer != null) {
                updateTimeDisplayForProgress(progress);
            }
        }
        
        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            Log.d(TAG, "User started seeking");
            
            stateManager.setUserSeeking(true);
            stopProgressUpdates();
            cancelScheduledHideControls();
        }
        
        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            Log.d(TAG, "User stopped seeking");
            
            if (exoPlayer != null) {
                performSeekToProgress(seekBar.getProgress());
            }
            
            stateManager.setUserSeeking(false);
            startProgressUpdates();
            scheduleAutoHideControls();
        }
    }

    // ========================================================================================
    // PLAYBACK CONTROL METHODS
    // ========================================================================================
    
    /**
     * Toggle play/pause state with validation
     */
    private void togglePlayPause() {
        if (exoPlayer == null) {
            Log.w(TAG, "Cannot toggle play/pause - player is null");
            return;
        }
        
        try {
            if (exoPlayer.isPlaying()) {
                exoPlayer.pause();
                Log.d(TAG, "Player paused");
            } else {
                exoPlayer.play();
                Log.d(TAG, "Player resumed");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error toggling play/pause", e);
        }
    }
    
    /**
     * Update play/pause button appearance
     */
    private void updatePlayPauseButtonState(boolean isPlaying) {
        if (binding != null) {
            binding.playPauseButton.setImageResource(
                isPlaying ? R.drawable.ic_pause : R.drawable.ic_play);
        }
    }
    
    /**
     * Seek to relative position with bounds checking
     */
    private void seekRelative(long seekMilliseconds) {
        if (exoPlayer == null) {
            Log.w(TAG, "Cannot seek - player is null");
            return;
        }
        
        try {
            long currentPosition = exoPlayer.getCurrentPosition();
            long newPosition = currentPosition + seekMilliseconds;
            long duration = exoPlayer.getDuration();
            
            // Ensure position is within valid bounds
            if (duration != C.TIME_UNSET) {
                newPosition = Math.max(0, Math.min(newPosition, duration));
            } else {
                newPosition = Math.max(0, newPosition);
            }
            
            exoPlayer.seekTo(newPosition);
            stateManager.setLastKnownPosition(newPosition);
            
            Log.d(TAG, "Seeked to position: " + newPosition);
            
        } catch (Exception e) {
            Log.e(TAG, "Error during seek operation", e);
        }
    }
    
    /**
     * Perform seek based on progress bar position
     */
    private void performSeekToProgress(int progress) {
        if (exoPlayer == null) {
            Log.w(TAG, "Cannot seek to progress - player is null");
            return;
        }
        
        try {
            long duration = exoPlayer.getDuration();
            if (duration != C.TIME_UNSET && duration > 0) {
                long newPosition = (duration * progress) / SEEK_BAR_PRECISION;
                exoPlayer.seekTo(newPosition);
                stateManager.setLastKnownPosition(newPosition);
                
                Log.d(TAG, "Seeked to progress position: " + newPosition);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error seeking to progress", e);
        }
    }

    // ========================================================================================
    // PROGRESS TRACKING
    // ========================================================================================
    
    /**
     * Start progress updates with proper scheduling
     */
    private void startProgressUpdates() {
        stopProgressUpdates(); // Ensure no duplicate updates
        
        if (progressUpdateRunnable != null) {
            progressHandler.post(progressUpdateRunnable);
        }
    }
    
    /**
     * Stop progress updates
     */
    private void stopProgressUpdates() {
        if (progressUpdateRunnable != null) {
            progressHandler.removeCallbacks(progressUpdateRunnable);
        }
    }
    
    /**
     * Update progress display with current playback position
     */
    private void updateProgressDisplay() {
        if (exoPlayer == null || binding == null) return;
        
        try {
            long currentPosition = exoPlayer.getCurrentPosition();
            long duration = exoPlayer.getDuration();
            
            // Update current time display
            binding.currentTimeText.setText(formatTime(currentPosition));
            
            // Update duration and progress
            if (duration != C.TIME_UNSET && duration > 0) {
                binding.totalTimeText.setText(formatTime(duration));
                int progress = (int) ((currentPosition * SEEK_BAR_PRECISION) / duration);
                binding.seekBar.setProgress(progress);
            } else {
                binding.totalTimeText.setText("--:--");
                binding.seekBar.setProgress(0);
            }
            
            // Update state
            stateManager.setLastKnownPosition(currentPosition);
            
        } catch (Exception e) {
            Log.e(TAG, "Error updating progress display", e);
        }
    }
    
    /**
     * Update time display for seek bar progress
     */
    private void updateTimeDisplayForProgress(int progress) {
        if (exoPlayer == null || binding == null) return;
        
        try {
            long duration = exoPlayer.getDuration();
            if (duration != C.TIME_UNSET && duration > 0) {
                long newPosition = (duration * progress) / SEEK_BAR_PRECISION;
                binding.currentTimeText.setText(formatTime(newPosition));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating time display for progress", e);
        }
    }
    
    /**
     * Format time in milliseconds to readable format (HH:MM:SS or MM:SS)
     */
    @SuppressLint("DefaultLocale")
    private String formatTime(long milliseconds) {
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
    // CONTROLS VISIBILITY MANAGEMENT
    // ========================================================================================
    
    /**
     * Toggle controls visibility intelligently
     */
    private void toggleControlsVisibility() {
        if (stateManager.areControlsVisible()) {
            hidePlayerControls();
        } else {
            showPlayerControls();
        }
    }
    
    /**
     * Show player controls with proper state management
     */
    private void showPlayerControls() {
        if (binding == null) return;
        
        try {
            binding.controlsContainer.setVisibility(View.VISIBLE);
            stateManager.setControlsVisible(true);
            
            if (stateManager.isLocked()) {
                showOnlyLockButton();
            } else {
                showAllControlElements();
            }
            
            scheduleAutoHideControls();
            Log.d(TAG, "Player controls shown");
            
        } catch (Exception e) {
            Log.e(TAG, "Error showing player controls", e);
        }
    }
    
    /**
     * Hide player controls
     */
    private void hidePlayerControls() {
        if (binding == null) return;
        
        try {
            binding.controlsContainer.setVisibility(View.GONE);
            stateManager.setControlsVisible(false);
            cancelScheduledHideControls();
            
            Log.d(TAG, "Player controls hidden");
            
        } catch (Exception e) {
            Log.e(TAG, "Error hiding player controls", e);
        }
    }
    
    /**
     * Show all control elements when unlocked
     */
    private void showAllControlElements() {
        if (binding == null) return;
        
        binding.topBar.setVisibility(View.VISIBLE);
        binding.centerControls.setVisibility(View.VISIBLE);
        binding.bottomControls.setVisibility(View.VISIBLE);
        
        // Show all bottom control buttons
        binding.fullscreenButton.setVisibility(View.VISIBLE);
        binding.volumeButton.setVisibility(View.VISIBLE);
        binding.settingsButton.setVisibility(View.VISIBLE);
        binding.lockButton.setVisibility(View.VISIBLE);
    }
    
    /**
     * Show only lock button when player is locked
     */
    private void showOnlyLockButton() {
        if (binding == null) return;
        
        binding.topBar.setVisibility(View.GONE);
        binding.centerControls.setVisibility(View.GONE);
        binding.bottomControls.setVisibility(View.VISIBLE);
        
        // Hide all buttons except lock button
        binding.fullscreenButton.setVisibility(View.GONE);
        binding.volumeButton.setVisibility(View.GONE);
        binding.settingsButton.setVisibility(View.GONE);
        binding.lockButton.setVisibility(View.VISIBLE);
    }
    
    /**
     * Show lock button temporarily when player is locked
     */
    private void showLockButtonTemporarily() {
        if (binding == null) return;
        
        try {
            binding.controlsContainer.setVisibility(View.VISIBLE);
            showOnlyLockButton();
            
            mainHandler.removeCallbacks(hideLockButtonRunnable);
            mainHandler.postDelayed(hideLockButtonRunnable, LOCK_BUTTON_HIDE_DELAY_MS);
            
        } catch (Exception e) {
            Log.e(TAG, "Error showing lock button temporarily", e);
        }
    }
    
    /**
     * Hide lock button after delay when locked
     */
    private void hideLockButtonDelayed() {
        if (stateManager.isLocked() && binding != null) {
            binding.controlsContainer.setVisibility(View.GONE);
        }
    }
    
    /**
     * Schedule automatic hiding of controls
     */
    private void scheduleAutoHideControls() {
        if (stateManager.isLocked()) return;
        
        cancelScheduledHideControls();
        mainHandler.postDelayed(hideControlsRunnable, CONTROLS_AUTO_HIDE_DELAY_MS);
    }
    
    /**
     * Cancel scheduled hiding of controls
     */
    private void cancelScheduledHideControls() {
        mainHandler.removeCallbacks(hideControlsRunnable);
    }

    // ========================================================================================
    // PLAYER LOCK FUNCTIONALITY
    // ========================================================================================
    
    /**
     * Toggle player lock state
     */
    private void togglePlayerLock() {
        boolean newLockState = !stateManager.isLocked();
        stateManager.setLocked(newLockState);
        
        if (newLockState) {
            lockPlayer();
        } else {
            unlockPlayer();
        }
    }
    
    /**
     * Lock player controls
     */
    private void lockPlayer() {
        if (binding == null) return;
        
        try {
            binding.lockButton.setImageResource(R.drawable.ic_lock_open);
            showOnlyLockButton();
            
            Toast.makeText(this, "Player locked - tap to unlock", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "Player locked");
            
        } catch (Exception e) {
            Log.e(TAG, "Error locking player", e);
        }
    }
    
    /**
     * Unlock player controls
     */
    private void unlockPlayer() {
        if (binding == null) return;
        
        try {
            binding.lockButton.setImageResource(R.drawable.ic_lock);
            showPlayerControls();
            
            Toast.makeText(this, "Player unlocked", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "Player unlocked");
            
        } catch (Exception e) {
            Log.e(TAG, "Error unlocking player", e);
        }
    }

    // ========================================================================================
    // SCREEN ORIENTATION MANAGEMENT
    // ========================================================================================
    
    /**
     * Toggle screen orientation between landscape and portrait
     */
    private void toggleScreenOrientation() {
        try {
            int currentOrientation = getResources().getConfiguration().orientation;
            
            if (currentOrientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE) {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
                Log.d(TAG, "Switched to portrait orientation");
            } else {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
                Log.d(TAG, "Switched to landscape orientation");
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error toggling screen orientation", e);
        }
    }

    // ========================================================================================
    // ADDITIONAL CONTROL HANDLERS
    // ========================================================================================
    
    /**
     * Handle volume control (placeholder for future implementation)
     */
    private void handleVolumeControl() {
        Toast.makeText(this, "Volume controls - Coming soon", Toast.LENGTH_SHORT).show();
        // TODO: Implement volume control panel with system volume integration
    }
    
    /**
     * Handle player settings (placeholder for future implementation)
     */
    private void handlePlayerSettings() {
        Toast.makeText(this, "Player settings - Coming soon", Toast.LENGTH_SHORT).show();
        // TODO: Implement settings panel (quality selection, subtitles, playback speed, etc.)
    }

    // ========================================================================================
    // STATE PERSISTENCE
    // ========================================================================================
    
    /**
     * Pause player and save current state
     */
    private void pausePlayerAndSaveState() {
        if (exoPlayer != null) {
            try {
                stateManager.setShouldAutoPlay(exoPlayer.isPlaying());
                exoPlayer.pause();
                
                Log.d(TAG, "Player paused and state saved");
                
            } catch (Exception e) {
                Log.e(TAG, "Error pausing player and saving state", e);
            }
        }
    }
    
    /**
     * Save current player state for restoration
     */
    private void saveCurrentPlayerState() {
        if (exoPlayer != null) {
            try {
                stateManager.setLastKnownPosition(exoPlayer.getCurrentPosition());
                stateManager.setLastPlaybackState(exoPlayer.getPlaybackState());
                
                Log.d(TAG, "Player state saved");
                
            } catch (Exception e) {
                Log.e(TAG, "Error saving player state", e);
            }
        }
    }

    // ========================================================================================
    // RESOURCE MANAGEMENT
    // ========================================================================================
    
    /**
     * Release all resources and perform cleanup
     */
    private void releaseAllResources() {
        try {
            Log.d(TAG, "Starting resource cleanup");
            
            // Stop all scheduled tasks
            stopProgressUpdates();
            mainHandler.removeCallbacksAndMessages(null);
            progressHandler.removeCallbacksAndMessages(null);
            
            // Release ExoPlayer
            if (exoPlayer != null) {
                exoPlayer.release();
                exoPlayer = null;
                Log.d(TAG, "ExoPlayer released");
            }
            
            // Clear state
            stateManager.reset();
            errorRecoveryManager.reset();
            
            // Clear binding reference
            binding = null;
            
            Log.d(TAG, "Resource cleanup completed");
            
        } catch (Exception e) {
            Log.e(TAG, "Error during resource cleanup", e);
        }
    }

    // ========================================================================================
    // UTILITY METHODS
    // ========================================================================================
    
    /**
     * Show error message and finish activity
     */
    private void showErrorAndFinish(String errorMessage) {
        Log.e(TAG, "Showing error and finishing: " + errorMessage);
        
        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
        finish();
    }
    
    /**
     * Check if activity is in a valid state for operations
     */
    private boolean isActivityValid() {
        return !isFinishing() && !isDestroyed() && binding != null;
    }
}
