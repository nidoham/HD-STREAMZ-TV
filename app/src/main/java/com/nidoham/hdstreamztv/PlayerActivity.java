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
import com.nidoham.hdstreamztv.template.model.settings.Template;

/**
 * Professional Video Player Activity with Enhanced Architecture
 * 
 * Features:
 * - Robust ExoPlayer integration with proper lifecycle management
 * - Gesture-based controls with intelligent auto-hide functionality
 * - Player lock mechanism to prevent accidental interactions
 * - High-precision progress tracking and seeking
 * - Comprehensive error handling with recovery mechanisms
 * - Memory-optimized resource management
 * - Screen orientation change handling without video restart
 * 
 * @author Professional Enhanced Version
 * @version 5.0
 */
public class PlayerActivity extends AppCompatActivity {
    
    private static final String TAG = "PlayerActivity";
    
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
    private static final String EXTRA_VIDEO_URL = "link";
    private static final String EXTRA_VIDEO_NAME = "name";
    private static final String EXTRA_VIDEO_CATEGORY = "category";
    
    // State Save Keys
    private static final String SAVED_PLAYBACK_POSITION = "playback_position";
    private static final String SAVED_PLAY_WHEN_READY = "play_when_ready";
    private static final String SAVED_PLAYER_LOCKED = "player_locked";
    
    // Core Components
    private ActivityPlayerBinding binding;
    private ExoPlayer player;
    
    // Thread Management
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Handler progressHandler = new Handler(Looper.getMainLooper());
    
    // Scheduled Tasks
    private final Runnable hideControlsTask = this::hideControls;
    private final Runnable hideLockButtonTask = this::hideLockButtonDelayed;
    private Runnable progressUpdateTask;
    
    // State Management
    private final PlayerStateManager stateManager = new PlayerStateManager();
    private final VideoInfoManager videoManager = new VideoInfoManager();
    private final ErrorRecoveryManager errorManager = new ErrorRecoveryManager();
    
    // ========================================================================================
    // State Management Classes
    // ========================================================================================
    
    /**
     * Manages all player-related state with thread safety
     */
    private static class PlayerStateManager {
        private volatile boolean isLocked = false;
        private volatile boolean isSeeking = false;
        private volatile boolean controlsVisible = false;
        private volatile boolean playWhenReady = true;
        private volatile long lastPosition = 0L;
        private volatile int lastPlaybackState = Player.STATE_IDLE;
        private volatile boolean initialized = false;
        
        public synchronized boolean isLocked() { return isLocked; }
        public synchronized void setLocked(boolean locked) { this.isLocked = locked; }
        
        public synchronized boolean isSeeking() { return isSeeking; }
        public synchronized void setSeeking(boolean seeking) { this.isSeeking = seeking; }
        
        public synchronized boolean areControlsVisible() { return controlsVisible; }
        public synchronized void setControlsVisible(boolean visible) { this.controlsVisible = visible; }
        
        public synchronized boolean shouldPlayWhenReady() { return playWhenReady; }
        public synchronized void setPlayWhenReady(boolean play) { this.playWhenReady = play; }
        
        public synchronized long getLastPosition() { return lastPosition; }
        public synchronized void setLastPosition(long position) { this.lastPosition = position; }
        
        public synchronized int getLastPlaybackState() { return lastPlaybackState; }
        public synchronized void setLastPlaybackState(int state) { this.lastPlaybackState = state; }
        
        public synchronized boolean isInitialized() { return initialized; }
        public synchronized void setInitialized(boolean init) { this.initialized = init; }
        
        public synchronized void reset() {
            isLocked = false;
            isSeeking = false;
            controlsVisible = false;
            playWhenReady = true;
            lastPosition = 0L;
            lastPlaybackState = Player.STATE_IDLE;
            initialized = false;
        }
    }
    
    /**
     * Manages video information with validation
     */
    private static class VideoInfoManager {
        private String videoUrl;
        private String videoName;
        private int videoCategory;
        
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
        
        public String getVideoUrl() { return videoUrl; }
        public String getVideoName() { return videoName; }
        public int getVideoCategory() { return videoCategory; }
        
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
                if (player != null && !isFinishing()) {
                    try {
                        player.prepare();
                        if (stateManager.shouldPlayWhenReady()) {
                            player.play();
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
    // Activity Lifecycle
    // ========================================================================================
    
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate: Starting PlayerActivity");
        
        if (!initializeComponents()) {
            Log.e(TAG, "onCreate: Failed to initialize activity");
            return;
        }
        
        if (!extractVideoInfo()) {
            Log.e(TAG, "onCreate: Invalid video information");
            return;
        }
        
        restoreState(savedInstanceState);
        setupVideoPlayer();
    }
    
    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart: Activity starting");
        
        if (player != null && binding != null) {
            binding.playerView.onResume();
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume: Activity resuming");
        
        enforceFullscreenMode();
        
        if (player != null && stateManager.shouldPlayWhenReady()) {
            player.play();
        }
        
        startProgressUpdates();
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause: Activity pausing");
        
        pauseAndSaveState();
        stopProgressUpdates();
    }
    
    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop: Activity stopping");
        
        saveCurrentState();
        
        if (binding != null) {
            binding.playerView.onPause();
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy: Cleaning up resources");
        
        cleanup();
    }
    
    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        
        if (player != null) {
            outState.putLong(SAVED_PLAYBACK_POSITION, player.getCurrentPosition());
            outState.putBoolean(SAVED_PLAY_WHEN_READY, player.getPlayWhenReady());
            outState.putBoolean(SAVED_PLAYER_LOCKED, stateManager.isLocked());
            
            Log.d(TAG, "State saved - position: " + player.getCurrentPosition());
        }
    }
    
    private void restoreState(@Nullable Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            long savedPosition = savedInstanceState.getLong(SAVED_PLAYBACK_POSITION, 0);
            boolean playWhenReady = savedInstanceState.getBoolean(SAVED_PLAY_WHEN_READY, true);
            boolean wasLocked = savedInstanceState.getBoolean(SAVED_PLAYER_LOCKED, false);
            
            stateManager.setLastPosition(savedPosition);
            stateManager.setPlayWhenReady(playWhenReady);
            stateManager.setLocked(wasLocked);
            
            Log.d(TAG, "State restored - position: " + savedPosition);
        }
    }
    
    // ========================================================================================
    // Initialization Methods
    // ========================================================================================
    
    private boolean initializeComponents() {
        try {
            binding = ActivityPlayerBinding.inflate(getLayoutInflater());
            setContentView(binding.getRoot());
            
            enforceFullscreenMode();
            initializeProgressTask();
            
            Log.d(TAG, "Activity initialization successful");
            return true;
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize activity", e);
            showErrorAndFinish("Failed to start player: " + e.getMessage());
            return false;
        }
    }
    
    private boolean extractVideoInfo() {
        try {
            if (!videoManager.extractFromIntent(getIntent().getExtras())) {
                showErrorAndFinish("Invalid video data provided");
                return false;
            }
            
            Log.d(TAG, "Video info extracted: " + videoManager.getVideoName());
            return true;
            
        } catch (Exception e) {
            Log.e(TAG, "Error extracting video info", e);
            showErrorAndFinish("Error processing video data: " + e.getMessage());
            return false;
        }
    }
    
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
    
    private void initializeProgressTask() {
        progressUpdateTask = new Runnable() {
            @Override
            public void run() {
                if (player != null && !stateManager.isSeeking() && !isFinishing()) {
                    updateProgressDisplay();
                    
                    if (player.isPlaying()) {
                        progressHandler.postDelayed(this, PROGRESS_UPDATE_INTERVAL_MS);
                    }
                }
            }
        };
    }
    
    // ========================================================================================
    // Player Setup
    // ========================================================================================
    
    private void setupVideoPlayer() {
        try {
            Log.d(TAG, "Setting up video player");
            
            createPlayer();
            setupUI();
            setupVideoTitle();
            prepareMedia();
            
            stateManager.setInitialized(true);
            showControls();
            
            Log.d(TAG, "Video player setup complete");
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to setup video player", e);
            showErrorAndFinish("Failed to setup player: " + e.getMessage());
        }
    }
    
    private void createPlayer() {
        try {
            player = new ExoPlayer.Builder(this)
                .setSeekBackIncrementMs(SEEK_INCREMENT_MS)
                .setSeekForwardIncrementMs(SEEK_INCREMENT_MS)
                .build();
            
            binding.playerView.setPlayer(player);
            binding.playerView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FIT);
            binding.playerView.setUseController(false);
            
            player.addListener(new PlayerEventListener());
            
            Log.d(TAG, "ExoPlayer created and configured");
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to create ExoPlayer", e);
            throw new RuntimeException("ExoPlayer creation failed", e);
        }
    }
    
    private void prepareMedia() {
        String mediaUrl = videoManager.getVideoUrl();
        prepareMediaWithUrl(mediaUrl);
    }
    
    private void prepareMediaWithUrl(String mediaUrl) {
        try {
            if (player == null || mediaUrl == null || mediaUrl.trim().isEmpty()) {
                throw new IllegalStateException("Invalid player or URL state");
            }
            
            Log.d(TAG, "Preparing media with URL: " + mediaUrl);
            
            MediaItem mediaItem = MediaItem.fromUri(Uri.parse(mediaUrl.trim()));
            player.setMediaItem(mediaItem);
            
            long lastPosition = stateManager.getLastPosition();
            if (lastPosition > 0) {
                player.seekTo(lastPosition);
            }
            
            player.prepare();
            
            if (stateManager.shouldPlayWhenReady()) {
                player.play();
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to prepare media", e);
            handleMediaError(e);
        }
    }
    
    private void handleMediaError(Exception error) {
        String errorMessage = "Failed to load video: " + error.getMessage();
        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
        
        PlaybackException playbackError = new PlaybackException(
            errorMessage, error, PlaybackException.ERROR_CODE_IO_UNSPECIFIED);
            
        if (errorManager.shouldRetry(playbackError)) {
            errorManager.attemptRecovery();
        } else {
            showErrorAndFinish("Unable to play video after multiple attempts");
        }
    }
    
    // ========================================================================================
    // Player Event Handling
    // ========================================================================================
    
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
        }
        
        @Override
        public void onPositionDiscontinuity(@NonNull Player.PositionInfo oldPosition,
                                          @NonNull Player.PositionInfo newPosition,
                                          int reason) {
            stateManager.setLastPosition(newPosition.positionMs);
        }
    }
    
    private void handlePlaybackStateChange(int playbackState) {
        switch (playbackState) {
            case Player.STATE_READY:
                Log.d(TAG, "Player ready");
                errorManager.reset();
                startProgressUpdates();
                break;
                
            case Player.STATE_ENDED:
                Log.d(TAG, "Playback ended");
                handlePlaybackComplete();
                break;
                
            case Player.STATE_BUFFERING:
                Log.d(TAG, "Player buffering");
                break;
                
            case Player.STATE_IDLE:
                Log.d(TAG, "Player idle");
                break;
        }
    }
    
    private void handlePlayingStateChange(boolean isPlaying) {
        updatePlayPauseButton(isPlaying);
        
        if (isPlaying) {
            startProgressUpdates();
        } else {
            stopProgressUpdates();
        }
    }
    
    private void handlePlayerError(@NonNull PlaybackException error) {
        String errorMessage = "Playback error: " + error.getMessage();
        Log.e(TAG, errorMessage, error);
        
        Toast.makeText(this, "Video playback error occurred", Toast.LENGTH_SHORT).show();
        
        if (errorManager.shouldRetry(error)) {
            errorManager.attemptRecovery();
        } else {
            showErrorAndFinish("Unable to recover from playback error");
        }
    }
    
    private void handlePlaybackComplete() {
        if (player != null) {
            player.seekTo(0);
            player.pause();
        }
        
        showControls();
        Toast.makeText(this, "Video completed", Toast.LENGTH_SHORT).show();
    }
    
    // ========================================================================================
    // Video Title Management
    // ========================================================================================
    
    private void setupVideoTitle() {
        Log.d(TAG, "Setting up video title");
        
        String title = videoManager.getVideoName();
        if (title != null && !title.trim().isEmpty()) {
            updateVideoTitle(title);
        }
    }
    
    private void updateVideoTitle(String title) {
        if (title == null || title.trim().isEmpty()) {
            Log.w(TAG, "Attempting to set empty title");
            return;
        }
        
        if (Looper.myLooper() != Looper.getMainLooper()) {
            runOnUiThread(() -> updateVideoTitle(title));
            return;
        }
        
        try {
            if (binding != null && binding.channelTitleText != null) {
                String cleanTitle = title.trim();
                
                if (cleanTitle.length() > 100) {
                    cleanTitle = cleanTitle.substring(0, 97) + "...";
                }
                
                binding.channelTitleText.setText(cleanTitle);
                Log.d(TAG, "Video title updated: " + cleanTitle);
                
            } else {
                Log.w(TAG, "Cannot update title - binding or text view is null");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating video title display", e);
        }
    }
    
    // ========================================================================================
    // User Interface Setup
    // ========================================================================================
    
    private void setupUI() {
        setupMainClickHandler();
        setupTopBarControls();
        setupCenterControls();
        setupBottomBarControls();
        setupSeekBar();
        
        Log.d(TAG, "User interface setup complete");
    }
    
    private void setupMainClickHandler() {
        binding.getRoot().setOnClickListener(view -> {
            if (stateManager.isLocked()) {
                showLockButtonTemporarily();
            } else {
                toggleControlsVisibility();
            }
        });
    }
    
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
    
    private void setupCenterControls() {
        binding.playPauseButton.setOnClickListener(v -> {
            Log.d(TAG, "Play/pause button clicked");
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
    
    private void setupSeekBar() {
        binding.seekBar.setMax(SEEK_BAR_MAX_PRECISION);
        binding.seekBar.setOnSeekBarChangeListener(new SeekBarChangeListener());
    }
    
    private class SeekBarChangeListener implements SeekBar.OnSeekBarChangeListener {
        
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if (fromUser && player != null) {
                updateTimeDisplayForProgress(progress);
            }
        }
        
        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            Log.d(TAG, "User started seeking");
            
            stateManager.setSeeking(true);
            stopProgressUpdates();
            cancelControlsHiding();
        }
        
        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            Log.d(TAG, "User stopped seeking");
            
            if (player != null) {
                seekToProgress(seekBar.getProgress());
            }
            
            stateManager.setSeeking(false);
            startProgressUpdates();
            scheduleControlsHiding();
        }
    }
    
    // ========================================================================================
    // Playback Control Methods
    // ========================================================================================
    
    private void togglePlayPause() {
        if (player == null) {
            Log.w(TAG, "Cannot toggle play/pause - player is null");
            return;
        }
        
        try {
            if (player.isPlaying()) {
                player.pause();
                Log.d(TAG, "Player paused");
            } else {
                player.play();
                Log.d(TAG, "Player resumed");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error toggling play/pause", e);
        }
    }
    
    private void updatePlayPauseButton(boolean isPlaying) {
        if (binding != null) {
            binding.playPauseButton.setImageResource(
                isPlaying ? R.drawable.ic_pause : R.drawable.ic_play);
        }
    }
    
    private void seekRelative(long seekMs) {
        if (player == null) {
            Log.w(TAG, "Cannot seek - player is null");
            return;
        }
        
        try {
            long currentPosition = player.getCurrentPosition();
            long newPosition = currentPosition + seekMs;
            long duration = player.getDuration();
            
            if (duration != C.TIME_UNSET) {
                newPosition = Math.max(0, Math.min(newPosition, duration));
            } else {
                newPosition = Math.max(0, newPosition);
            }
            
            player.seekTo(newPosition);
            stateManager.setLastPosition(newPosition);
            
            Log.d(TAG, "Seeked to position: " + newPosition);
            
        } catch (Exception e) {
            Log.e(TAG, "Error in seek operation", e);
        }
    }
    
    private void seekToProgress(int progress) {
        if (player == null) {
            Log.w(TAG, "Cannot seek to progress - player is null");
            return;
        }
        
        try {
            long duration = player.getDuration();
            if (duration != C.TIME_UNSET && duration > 0) {
                long newPosition = (duration * progress) / SEEK_BAR_MAX_PRECISION;
                player.seekTo(newPosition);
                stateManager.setLastPosition(newPosition);
                
                Log.d(TAG, "Seeked to progress position: " + newPosition);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error seeking to progress", e);
        }
    }
    
    // ========================================================================================
    // Progress Tracking
    // ========================================================================================
    
    private void startProgressUpdates() {
        stopProgressUpdates();
        
        if (progressUpdateTask != null) {
            progressHandler.post(progressUpdateTask);
        }
    }
    
    private void stopProgressUpdates() {
        if (progressUpdateTask != null) {
            progressHandler.removeCallbacks(progressUpdateTask);
        }
    }
    
    private void updateProgressDisplay() {
        if (player == null || binding == null) return;
        
        try {
            long currentPosition = player.getCurrentPosition();
            long duration = player.getDuration();
            
            binding.currentTimeText.setText(formatTime(currentPosition));
            
            if (duration != C.TIME_UNSET && duration > 0) {
                binding.totalTimeText.setText(formatTime(duration));
                int progress = (int) ((currentPosition * SEEK_BAR_MAX_PRECISION) / duration);
                binding.seekBar.setProgress(progress);
            } else {
                binding.totalTimeText.setText("--:--");
                binding.seekBar.setProgress(0);
            }
            
            stateManager.setLastPosition(currentPosition);
            
        } catch (Exception e) {
            Log.e(TAG, "Error updating progress display", e);
        }
    }
    
    private void updateTimeDisplayForProgress(int progress) {
        if (player == null || binding == null) return;
        
        try {
            long duration = player.getDuration();
            if (duration != C.TIME_UNSET && duration > 0) {
                long newPosition = (duration * progress) / SEEK_BAR_MAX_PRECISION;
                binding.currentTimeText.setText(formatTime(newPosition));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating time display for progress", e);
        }
    }
    
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
    // Controls Visibility Management
    // ========================================================================================
    
    private void toggleControlsVisibility() {
        if (stateManager.areControlsVisible()) {
            hideControls();
        } else {
            showControls();
        }
    }
    
    private void showControls() {
        if (binding == null) return;
        
        try {
            binding.controlsContainer.setVisibility(View.VISIBLE);
            stateManager.setControlsVisible(true);
            
            if (stateManager.isLocked()) {
                showOnlyLockButton();
            } else {
                showAllControls();
            }
            
            scheduleControlsHiding();
            Log.d(TAG, "Player controls shown");
            
        } catch (Exception e) {
            Log.e(TAG, "Error showing player controls", e);
        }
    }
    
    private void hideControls() {
        if (binding == null) return;
        
        try {
            binding.controlsContainer.setVisibility(View.GONE);
            stateManager.setControlsVisible(false);
            cancelControlsHiding();
            
            Log.d(TAG, "Player controls hidden");
            
        } catch (Exception e) {
            Log.e(TAG, "Error hiding player controls", e);
        }
    }
    
    private void showAllControls() {
        if (binding == null) return;
        
        binding.topBar.setVisibility(View.VISIBLE);
        binding.centerControls.setVisibility(View.VISIBLE);
        binding.bottomControls.setVisibility(View.VISIBLE);
        
        binding.fullscreenButton.setVisibility(View.VISIBLE);
        binding.volumeButton.setVisibility(View.VISIBLE);
        binding.settingsButton.setVisibility(View.VISIBLE);
        binding.lockButton.setVisibility(View.VISIBLE);
    }
    
    private void showOnlyLockButton() {
        if (binding == null) return;
        
        binding.topBar.setVisibility(View.GONE);
        binding.centerControls.setVisibility(View.GONE);
        binding.bottomControls.setVisibility(View.VISIBLE);
        
        binding.fullscreenButton.setVisibility(View.GONE);
        binding.volumeButton.setVisibility(View.GONE);
        binding.settingsButton.setVisibility(View.GONE);
        binding.lockButton.setVisibility(View.VISIBLE);
    }
    
    private void showLockButtonTemporarily() {
        if (binding == null) return;
        
        try {
            binding.controlsContainer.setVisibility(View.VISIBLE);
            showOnlyLockButton();
            
            mainHandler.removeCallbacks(hideLockButtonTask);
            mainHandler.postDelayed(hideLockButtonTask, LOCK_BUTTON_HIDE_DELAY_MS);
            
        } catch (Exception e) {
            Log.e(TAG, "Error showing lock button temporarily", e);
        }
    }
    
    private void hideLockButtonDelayed() {
        if (stateManager.isLocked() && binding != null) {
            binding.controlsContainer.setVisibility(View.GONE);
        }
    }
    
    private void scheduleControlsHiding() {
        if (stateManager.isLocked()) return;
        
        cancelControlsHiding();
        mainHandler.postDelayed(hideControlsTask, CONTROL_AUTO_HIDE_DELAY_MS);
    }
    
    private void cancelControlsHiding() {
        mainHandler.removeCallbacks(hideControlsTask);
    }
    
    // ========================================================================================
    // Player Lock Functionality
    // ========================================================================================
    
    private void togglePlayerLock() {
        boolean newLockState = !stateManager.isLocked();
        stateManager.setLocked(newLockState);
        
        if (newLockState) {
            lockPlayer();
        } else {
            unlockPlayer();
        }
    }
    
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
    
    private void unlockPlayer() {
        if (binding == null) return;
        
        try {
            binding.lockButton.setImageResource(R.drawable.ic_lock);
            showControls();
            
            Toast.makeText(this, "Player unlocked", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "Player unlocked");
            
        } catch (Exception e) {
            Log.e(TAG, "Error unlocking player", e);
        }
    }
    
    // ========================================================================================
    // Screen Orientation Management
    // ========================================================================================
    
    private void toggleScreenOrientation() {
        try {
            int currentOrientation = getResources().getConfiguration().orientation;
            
            if (currentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
                Log.d(TAG, "Changed to portrait orientation");
            } else {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
                Log.d(TAG, "Changed to landscape orientation");
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error toggling screen orientation", e);
        }
        
        if (binding != null) {
            binding.playerView.onResume();
        }
    }
    
    // ========================================================================================
    // Additional Control Handlers
    // ========================================================================================
    
    private void handleVolumeControl() {
        Toast.makeText(this, "Volume control - coming soon", Toast.LENGTH_SHORT).show();
        // TODO: Implement volume control panel with system volume integration
    }
    
    private void handlePlayerSettings() {
        Toast.makeText(this, "Player settings - coming soon", Toast.LENGTH_SHORT).show();
        // TODO: Implement settings panel (quality selection, subtitles, playback speed, etc.)
    }
    
    // ========================================================================================
    // State Persistence
    // ========================================================================================
    
    private void pauseAndSaveState() {
        if (player != null) {
            try {
                stateManager.setPlayWhenReady(player.isPlaying());
                player.pause();
                
                Log.d(TAG, "Player paused and state saved");
                
            } catch (Exception e) {
                Log.e(TAG, "Error pausing player and saving state", e);
            }
        }
    }
    
    private void saveCurrentState() {
        if (player != null) {
            try {
                stateManager.setLastPosition(player.getCurrentPosition());
                stateManager.setLastPlaybackState(player.getPlaybackState());
                
                Log.d(TAG, "Player state saved");
                
            } catch (Exception e) {
                Log.e(TAG, "Error saving player state", e);
            }
        }
    }
    
    // ========================================================================================
    // Resource Management
    // ========================================================================================
    
    private void cleanup() {
        try {
            Log.d(TAG, "Starting resource cleanup");
            
            stopProgressUpdates();
            mainHandler.removeCallbacksAndMessages(null);
            progressHandler.removeCallbacksAndMessages(null);
            
            if (player != null) {
                player.release();
                player = null;
                Log.d(TAG, "ExoPlayer released");
            }
            
            stateManager.reset();
            errorManager.reset();
            
            binding = null;
            
            Log.d(TAG, "Resource cleanup complete");
            
        } catch (Exception e) {
            Log.e(TAG, "Error during resource cleanup", e);
        }
    }
    
    // ========================================================================================
    // Utility Methods
    // ========================================================================================
    
    private void showErrorAndFinish(String errorMessage) {
        Log.e(TAG, "Showing error and finishing: " + errorMessage);
        
        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
        finish();
    }
    
    private boolean isActivityValid() {
        return !isFinishing() && !isDestroyed() && binding != null;
    }
    
    private void logPlayerState() {
        if (player != null) {
            Log.d(TAG, "Current player state: " +
                "position=" + player.getCurrentPosition() +
                ", duration=" + player.getDuration() +
                ", playing=" + player.isPlaying() +
                ", locked=" + stateManager.isLocked());
        }
    }
    
    private boolean isPlayerHealthy() {
        return player != null &&
               stateManager.isInitialized() &&
               isActivityValid();
    }
}
