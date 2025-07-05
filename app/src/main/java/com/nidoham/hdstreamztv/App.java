package com.nidoham.hdstreamztv;

import android.app.Application;
import android.util.Log;
import org.schabi.newpipe.extractor.NewPipe;
import org.schabi.newpipe.extractor.localization.ContentCountry;
import org.schabi.newpipe.extractor.localization.Localization;
import java.util.Locale;

public class App extends Application {
    private static final String TAG = "HDStreamzTV";
    private static App instance;
    
    @Override
    public void onCreate() {
        super.onCreate();
        instance = this; // Store the instance
        
        try {
            // Initialize NewPipe with custom downloader
            initializeNewPipe();
            
            Log.i(TAG, "NewPipe initialized successfully");
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize NewPipe", e);
            // You might want to handle this error appropriately
            // For example, show a dialog or fallback to alternative methods
        }
    }
    
    private void initializeNewPipe() {
        // Get system locale
        Locale systemLocale = Locale.getDefault();
        
        // Create localization object
        Localization localization = new Localization(
            systemLocale.getLanguage(),
            systemLocale.getCountry()
        );
        
        // Create content country
        ContentCountry contentCountry = new ContentCountry(systemLocale.getCountry());
        
        // Initialize NewPipe with custom downloader and localization
        NewPipe.init(new DownloadImpl(), localization, contentCountry);
        
        Log.d(TAG, "NewPipe initialized with locale: " + systemLocale.toString());
    }
    
    /**
     * Get application instance
     */
    public static App getInstance() {
        return instance;
    }
    
    /**
     * Check if NewPipe is initialized
     */
    public boolean isNewPipeInitialized() {
        try {
            return NewPipe.getDownloader() != null;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Reinitialize NewPipe if needed (useful for error recovery)
     */
    public void reinitializeNewPipe() {
        try {
            initializeNewPipe();
            Log.i(TAG, "NewPipe reinitialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Failed to reinitialize NewPipe", e);
        }
    }
}