package com.nidoham.hdstreamztv;

import android.app.Application;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.preference.PreferenceManager;

import com.nidoham.hdstreamztv.error.ReCaptchaActivity;

import org.schabi.newpipe.extractor.NewPipe;
import org.schabi.newpipe.extractor.downloader.Downloader;
import org.schabi.newpipe.extractor.localization.ContentCountry;
import org.schabi.newpipe.extractor.localization.Localization;

import java.util.Locale;

/**
 * Main Application class for HDStreamzTV
 * Handles NewPipe initialization and configuration
 */
public class App extends Application {
    
    // Constants
    private static final String TAG = "HDStreamzTV";
    public static final boolean DEBUG = false;
    
    // Static instance
    private static App instance;
    
    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        
        initializeApplication();
    }
    
    /**
     * Initialize the application components
     */
    private void initializeApplication() {
        try {
            initializeNewPipe();
            Log.i(TAG, "Application initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize application", e);
            handleInitializationError(e);
        }
    }
    
    /**
     * Initialize NewPipe with custom downloader and localization settings
     */
    private void initializeNewPipe() {
        // Get system locale for localization
        Locale systemLocale = Locale.getDefault();
        
        // Create localization configuration
        Localization localization = new Localization(
            systemLocale.getLanguage(),
            systemLocale.getCountry()
        );
        
        // Create content country configuration
        ContentCountry contentCountry = new ContentCountry(systemLocale.getCountry());
        
        // Initialize NewPipe with custom downloader and localization
        NewPipe.init(getDownloader(), localization, contentCountry);
        
        Log.d(TAG, "NewPipe initialized with locale: " + systemLocale.toString());
    }
    
    /**
     * Create and configure the downloader with cookies
     * @return Configured downloader instance
     */
    private Downloader getDownloader() {
        final DownloaderImpl downloader = DownloaderImpl.init(null);
        setCookiesToDownloader(downloader);
        return downloader;
    }
    
    /**
     * Set cookies to the downloader for authentication and preferences
     * @param downloader The downloader instance to configure
     */
    private void setCookiesToDownloader(final DownloaderImpl downloader) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(
                getApplicationContext());
        final String key = getApplicationContext().getString(R.string.recaptcha_cookies_key);
        
        // Set ReCaptcha cookies
        downloader.setCookie(ReCaptchaActivity.RECAPTCHA_COOKIES_KEY, 
                            prefs.getString(key, null));
        
        // Update YouTube restricted mode cookies
        downloader.updateYoutubeRestrictedModeCookies(getApplicationContext());
    }
    
    /**
     * Handle initialization errors
     * @param error The exception that occurred during initialization
     */
    private void handleInitializationError(Exception error) {
        // Log detailed error information
        Log.e(TAG, "Initialization error details: " + error.getMessage(), error);
        
        // Here you can add additional error handling logic such as:
        // - Showing user-friendly error dialogs
        // - Attempting fallback initialization methods
        // - Reporting crashes to analytics services
        // - Graceful degradation of functionality
    }
    
    // Public API methods
    
    /**
     * Get the singleton application instance
     * @return The App instance
     */
    public static App getInstance() {
        return instance;
    }
    
    /**
     * Check if NewPipe is properly initialized
     * @return true if NewPipe is initialized, false otherwise
     */
    public boolean isNewPipeInitialized() {
        try {
            return NewPipe.getDownloader() != null;
        } catch (Exception e) {
            Log.w(TAG, "Error checking NewPipe initialization status", e);
            return false;
        }
    }
    
    /**
     * Reinitialize NewPipe (useful for error recovery)
     * @return true if reinitialization was successful, false otherwise
     */
    public boolean reinitializeNewPipe() {
        try {
            initializeNewPipe();
            Log.i(TAG, "NewPipe reinitialized successfully");
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Failed to reinitialize NewPipe", e);
            handleInitializationError(e);
            return false;
        }
    }
    
    /**
     * Get the current locale being used by the application
     * @return Current locale
     */
    public Locale getCurrentLocale() {
        return Locale.getDefault();
    }
    
    /**
     * Check if the application is running in debug mode
     * @return true if debug mode is enabled
     */
    public boolean isDebugMode() {
        return DEBUG;
    }
}