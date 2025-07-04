package com.nidoham.hdstreamztv;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.SeekBar;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;

import com.nidoham.hdstreamztv.databinding.ActivityPlayerBinding;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class PlayerActivity extends AppCompatActivity {

    private ActivityPlayerBinding binding;
    private ExoPlayer player;

    private boolean isPlaying = true;
    private boolean isLocked = false;

    private final Handler controlsHandler = new Handler(Looper.getMainLooper());
    private final Runnable hideControlsRunnable = () -> binding.controlsContainer.setVisibility(View.GONE);

    private final Handler progressHandler = new Handler(Looper.getMainLooper());
    private Runnable progressRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        binding = ActivityPlayerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // ** THE FIX IS HERE **
        // The deprecated lines have been removed. 
        // The hideSystemUI() method below handles fullscreen correctly.

        hideSystemUI();

        String videoName = getIntent().getStringExtra("name"); 
        if (videoName == null || videoName.isEmpty()) {
            Toast.makeText(this, "Video NAME not found", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        
        String videoUrl = getIntent().getStringExtra("link"); 
        if (videoUrl == null || videoUrl.isEmpty()) {
            Toast.makeText(this, "Video URL not found", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        
        binding.channelTitleText.setText(videoName+ "(Auto)");

        initializePlayer(videoUrl);
        setupClickListeners();
    }

    private void initializePlayer(String videoUrl) {
        try {
            player = new ExoPlayer.Builder(this).build();
            binding.playerView.setPlayer(player);

            MediaItem mediaItem = MediaItem.fromUri(Uri.parse(videoUrl));
            player.setMediaItem(mediaItem);
            
            player.addListener(new Player.Listener() {
                @Override
                public void onIsPlayingChanged(boolean isPlaying) {
                    PlayerActivity.this.isPlaying = isPlaying;
                    if (isPlaying) {
                        binding.playPauseButton.setImageResource(R.drawable.ic_pause);
                        startProgressUpdater();
                    } else {
                        binding.playPauseButton.setImageResource(R.drawable.ic_play);
                        stopProgressUpdater();
                    }
                }
            });

            player.prepare();
            player.play();
            
            showControls();

        } catch (Exception e) {
            Toast.makeText(this, "Error initializing player: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void setupClickListeners() {
        binding.getRoot().setOnClickListener(v -> {
            if (!isLocked) {
                if (binding.controlsContainer.getVisibility() == View.VISIBLE) {
                    hideControls();
                } else {
                    showControls();
                }
            } else {
                binding.lockButton.setVisibility(View.VISIBLE);
                controlsHandler.postDelayed(() -> binding.lockButton.setVisibility(View.GONE), 2000);
            }
        });

        // --- Top Bar ---
        binding.closeButton.setOnClickListener(v -> finish());
        binding.resizeButton.setOnClickListener(v -> toggleOrientation()); 

        // --- Center Controls ---
        binding.playPauseButton.setOnClickListener(v -> togglePlayPause());
        binding.rewindButton.setOnClickListener(v -> seekRelative(-10000));
        binding.forwardButton.setOnClickListener(v -> seekRelative(10000));

        // --- Bottom Controls ---
        binding.lockButton.setOnClickListener(v -> toggleLockState());
        binding.fullscreenButton.setOnClickListener(v -> toggleOrientation());
        binding.volumeButton.setOnClickListener(v -> Toast.makeText(this, "Volume button clicked", Toast.LENGTH_SHORT).show());
        binding.settingsButton.setOnClickListener(v -> Toast.makeText(this, "Settings button clicked", Toast.LENGTH_SHORT).show());

        // --- Seek Bar ---
        binding.seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && player != null) {
                    long duration = player.getDuration();
                    if (duration > 0) {
                        long newPosition = (duration * progress) / 100;
                        binding.currentTimeText.setText(formatTime(newPosition));
                    }
                }
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                stopProgressUpdater();
                controlsHandler.removeCallbacks(hideControlsRunnable);
            }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (player != null) {
                    long duration = player.getDuration();
                    long newPosition = (duration * seekBar.getProgress()) / 100;
                    player.seekTo(newPosition);
                }
                startProgressUpdater();
                autoHideControls();
            }
        });
    }

    private void togglePlayPause() {
        if (player == null) return;
        if (player.isPlaying()) {
            player.pause();
        } else {
            player.play();
        }
    }

    private void seekRelative(long seekMillis) {
        if (player == null) return;
        long newPosition = player.getCurrentPosition() + seekMillis;
        newPosition = Math.max(0, Math.min(newPosition, player.getDuration()));
        player.seekTo(newPosition);
    }
    
    private void toggleOrientation() {
        int currentOrientation = getResources().getConfiguration().orientation;
        if (currentOrientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }
    }

    private void toggleLockState() {
        isLocked = !isLocked;
        if (isLocked) {
            binding.lockButton.setImageResource(R.drawable.ic_lock_open); // Assumes you have 'ic_lock_open.xml'
            hideAllControlsExceptLock();
        } else {
            binding.lockButton.setImageResource(R.drawable.ic_lock);
            showControls();
        }
    }
    
    private void showControls() {
        binding.controlsContainer.setVisibility(View.VISIBLE);
        if (!isLocked) {
            binding.topBar.setVisibility(View.VISIBLE);
            binding.centerControls.setVisibility(View.VISIBLE);
            binding.bottomControls.setVisibility(View.VISIBLE);
        }
        autoHideControls();
    }

    private void hideControls() {
        binding.controlsContainer.setVisibility(View.GONE);
    }
    
    private void hideAllControlsExceptLock() {
        binding.topBar.setVisibility(View.INVISIBLE);
        binding.centerControls.setVisibility(View.INVISIBLE);
        binding.bottomControls.setVisibility(View.INVISIBLE); // Hide the whole container first

        // Then selectively re-show the parent and the lock button
        binding.bottomControls.setVisibility(View.VISIBLE); 
        binding.lockButton.setVisibility(View.VISIBLE);
    }

    private void autoHideControls() {
        controlsHandler.removeCallbacks(hideControlsRunnable);
        controlsHandler.postDelayed(hideControlsRunnable, 3000);
    }

    private void startProgressUpdater() {
        progressRunnable = () -> {
            if (player != null && player.isPlaying()) {
                long duration = player.getDuration();
                long position = player.getCurrentPosition();

                if (duration > 0) {
                    binding.seekBar.setMax(100);
                    binding.seekBar.setProgress((int) ((position * 100) / duration));
                    binding.totalTimeText.setText(formatTime(duration));
                }
                binding.currentTimeText.setText(formatTime(position));

                progressHandler.postDelayed(progressRunnable, 1000);
            }
        };
        progressHandler.post(progressRunnable);
    }

    private void stopProgressUpdater() {
        progressHandler.removeCallbacks(progressRunnable);
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        if (player != null && player.isPlaying()) {
            player.pause();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        hideSystemUI();
        if (player != null && isPlaying) {
             player.play();
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        releasePlayer();
    }

    private void releasePlayer() {
        if (player != null) {
            stopProgressUpdater();
            controlsHandler.removeCallbacksAndMessages(null);
            player.release();
            player = null;
        }
    }
    
    // This is the modern, non-deprecated way to handle fullscreen.
    private void hideSystemUI() {
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        if (controller != null) {
            controller.hide(WindowInsetsCompat.Type.systemBars());
            controller.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        }
    }
    
    @SuppressLint("DefaultLocale")
    private String formatTime(long millis) {
        if (millis < 0) millis = 0;
        
        long hours = TimeUnit.MILLISECONDS.toHours(millis);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60;
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60;

        if (hours > 0) {
            return String.format("%02d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format("%02d:%02d", minutes, seconds);
        }
    }
}