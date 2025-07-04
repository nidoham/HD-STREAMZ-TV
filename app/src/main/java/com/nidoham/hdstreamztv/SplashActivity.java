package com.nidoham.hdstreamztv;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

public class SplashActivity extends AppCompatActivity {

    private static final long SPLASH_DURATION = 2000; // 2 seconds

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set the status bar color to the theme's background color.
        // This replaces the immersive mode to make the status bar visible with a custom color.
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.md_theme_background));

        setContentView(R.layout.activity_splash);

        // Set the app icon
        ImageView splashImage = findViewById(R.id.splash_icon);
        splashImage.setImageResource(R.drawable.app_icon);

        // Transition to MainActivity after splash duration
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            startActivity(new Intent(SplashActivity.this, MainActivity.class));
            finish();
        }, SPLASH_DURATION);
    }
}