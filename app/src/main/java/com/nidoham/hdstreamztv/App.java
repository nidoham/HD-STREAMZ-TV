package com.nidoham.hdstreamztv;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.preference.PreferenceManager;

import com.nidoham.hdstreamztv.error.ReCaptchaActivity;

import org.schabi.newpipe.DownloaderImpl;
import org.schabi.newpipe.extractor.NewPipe;
import org.schabi.newpipe.extractor.downloader.Downloader;
import org.schabi.newpipe.extractor.localization.ContentCountry;
import org.schabi.newpipe.extractor.localization.Localization;

import java.util.Locale;

/**
 * Main Application class for HDStreamzTV
 * Handles NewPipe initialization, configuration, and provides global context
 */
public class App extends Application {

    // Constants
    private static final String TAG = "HDStreamzTV";
    public static final boolean DEBUG = false;
    private static final String RECAPTCHA_COOKIES_KEY = "recaptcha_cookies_key"; // Hardcoded to avoid resource dependency

    // Static instance for global access
    private static volatile App instance; // Volatile for thread safety
    private static volatile Context appContext;

    // Application state
    private boolean isNewPipeInitialized = false;
    private Locale currentLocale;
    private DownloaderImpl downloader;

    @Override
    public void onCreate() {
        super.onCreate();

        // Set static references for global access
        synchronized (App.class) {
            instance = this;
            appContext = getApplicationContext();
        }
        currentLocale = new Locale("bn", "BD"); // Default to Bangladesh (English)

        // Initialize the application
        initializeApplication();

        Log.i(TAG, "HDStreamzTV Application created successfully");
    }

    /**
     * Initialize the application components
     */
    private void initializeApplication() {
        try {
            initializeNewPipe();
            isNewPipeInitialized = true;
            Log.i(TAG, "Application initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize application", e);
            isNewPipeInitialized = false;
            handleInitializationError(e);
        }
    }

    /**
     * Initialize NewPipe with custom downloader and localization settings
     */
    private void initializeNewPipe() {
        // Create localization configuration for Bangladesh
        Localization localization = new Localization("en", "BD");
        ContentCountry contentCountry = new ContentCountry("BD");

        // Get configured downloader
        downloader = getDownloader();

        // Initialize NewPipe with custom downloader and localization
        NewPipe.init(downloader, localization, contentCountry);

        Log.d(TAG, "NewPipe initialized with locale: " + currentLocale.toString());
    }

    /**
     * Create and configure the downloader with cookies
     * @return Configured downloader instance
     */
    private DownloaderImpl getDownloader() {
        final DownloaderImpl downloaderImpl = DownloaderImpl.init(null); // Fixed typo
        setCookiesToDownloader(downloaderImpl);
        return downloaderImpl;
    }

    /**
     * Set cookies to the downloader for authentication and preferences
     * @param downloader The downloader instance to configure
     */
    private void setCookiesToDownloader(final DownloaderImpl downloader) {
        try {
            final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(appContext);
            // Set ReCaptcha cookies
            downloader.setCookie(ReCaptchaActivity.RECAPTCHA_COOKIES_KEY, 
                                prefs.getString(RECAPTCHA_COOKIES_KEY, null));
            // Update YouTube restricted mode cookies
            downloader.updateYoutubeRestrictedModeCookies(appContext);
            Log.d(TAG, "Cookies configured for downloader");
        } catch (Exception e) {
            Log.e(TAG, "Failed to set cookies to downloader", e);
            handleInitializationError(e);
        }
    }

    /**
     * Handle initialization errors
     * @param error The exception that occurred during initialization
     */
    private void handleInitializationError(Exception error) {
        Log.e(TAG, "Initialization error details: " + error.getMessage(), error);
        // TODO: Implement crash analytics or user notification if needed
    }

    // Global Context Methods

    /**
     * Get the singleton application instance
     * @return The App instance
     */
    public static App getInstance() {
        return instance;
    }

    /**
     * Get global application context
     * @return Application context
     */
    public static Context getAppContext() {
        return appContext;
    }

    /**
     * Get global application context (alternative method name)
     * @return Application context
     */
    public static Context getContext() {
        return appContext;
    }

    /**
     * Check if application instance is available
     * @return true if instance is available
     */
    public static boolean isInstanceAvailable() {
        return instance != null;
    }

    // NewPipe Management Methods

    /**
     * Check if NewPipe is properly initialized
     * @return true if NewPipe is initialized, false otherwise
     */
    public boolean isNewPipeInitialized() {
        return isNewPipeInitialized && checkNewPipeStatus();
    }

    /**
     * Internal method to check NewPipe status
     * @return true if NewPipe is working correctly
     */
    private boolean checkNewPipeStatus() {
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
            isNewPipeInitialized = true;
            Log.i(TAG, "NewPipe reinitialized successfully");
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Failed to reinitialize NewPipe", e);
            isNewPipeInitialized = false;
            handleInitializationError(e);
            return false;
        }
    }

    /**
     * Get the current downloader instance
     * @return Current downloader or null if not initialized
     */
    public DownloaderImpl getDownloaderInstance() {
        return downloader;
    }

    /**
     * Update downloader cookies (useful for authentication changes)
     */
    public void updateDownloaderCookies() {
        if (downloader != null) {
            setCookiesToDownloader(downloader);
            Log.d(TAG, "Downloader cookies updated");
        }
    }

    // Utility Methods

    /**
     * Get the current locale being used by the application
     * @return Current locale
     */
    public Locale getCurrentLocale() {
        return currentLocale;
    }

    /**
     * Update application locale
     * @param newLocale New locale to use
     * @return true if update was successful
     */
    public boolean updateLocale(Locale newLocale) {
        if (newLocale != null && !newLocale.equals(currentLocale)) {
            currentLocale = newLocale;
            // Reinitialize NewPipe with new locale
            return reinitializeNewPipe();
        }
        return false;
    }

    /**
     * Check if the application is running in debug mode
     * @return true if debug mode is enabled
     */
    public boolean isDebugMode() {
        return DEBUG;
    }

    /**
     * Get shared preferences instance
     * @return SharedPreferences instance
     */
    public SharedPreferences getPreferences() {
        return PreferenceManager.getDefaultSharedPreferences(appContext);
    }

    /**
     * Get string resource by ID
     * @param resId Resource ID
     * @return String resource value
     */
    public String getStringResource(int resId) {
        return appContext.getString(resId);
    }

    /**
     * Get string resource by ID with format arguments
     * @param resId Resource ID
     * @param formatArgs Format arguments
     * @return Formatted string resource value
     */
    public String getStringResource(int resId, Object... formatArgs) {
        return appContext.getString(resId, formatArgs);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        Log.w(TAG, "Low memory warning received");
        // Clear non-essential resources
        if (downloader != null) {
            downloader = null;
        }
        isNewPipeInitialized = false;
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        Log.i(TAG, "HDStreamzTV Application terminated");
        // Clear static references
        synchronized (App.class) {
            instance = null;
            appContext = null;
        }
    }
}