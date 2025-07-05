package com.nidoham.hdstreamztv.fragment;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.Patterns;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.nidoham.hdstreamztv.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Professional YouTube Fragment - Enhanced Link Monitoring System
 * 
 * Key Features:
 * - Real-time video link detection with smart timing
 * - Dynamic settings button visibility based on valid links
 * - Advanced DOM monitoring with retry mechanism
 * - Professional error handling and state management
 * - Optimized performance with proper lifecycle management
 * 
 * @author HD Streamz TV Development Team
 * @version 6.0 - Professional Link Monitoring Edition
 */
public class YouTubeFragment extends Fragment {

    // ===============================
    // Constants & Configuration
    // ===============================
    
    private static final String TAG = "YouTubeFragment";
    private static final String PREFS_NAME = "youtube_fragment_prefs";
    private static final String KEY_FIRST_LAUNCH = "is_first_launch";
    private static final String KEY_USER_SIGNED_IN = "user_signed_in";
    
    // URLs
    private static final String YOUTUBE_SIGNIN_URL = "https://accounts.google.com/signin/v2/identifier?service=youtube";
    private static final String YOUTUBE_URL = "https://www.youtube.com";
    private static final String YOUTUBE_MOBILE_URL = "https://m.youtube.com";
    
    // User Agent
    private static final String USER_AGENT = "Mozilla/5.0 (Linux; Android " + Build.VERSION.RELEASE +
            "; " + Build.MODEL + ") AppleWebKit/537.36 (KHTML, like Gecko) " +
            "Chrome/120.0.0.0 Mobile Safari/537.36";
    
    // Link Monitoring Configuration
    private static final String JAVASCRIPT_INTERFACE_NAME = "LinkMonitor";
    private static final int LINK_MONITOR_INITIAL_DELAY = 5000; // 5 seconds after page load
    private static final int LINK_MONITOR_INTERVAL = 4000; // 4 seconds between checks
    private static final int LINK_MONITOR_MAX_RETRIES = 5;
    private static final int MIN_LINKS_TO_SHOW_BUTTON = 1;
    private static final int DOM_READY_CHECK_INTERVAL = 1000; // 1 second
    private static final int DOM_READY_MAX_ATTEMPTS = 10;
    
    // Timeouts
    private static final int NETWORK_TIMEOUT = 15000; // 15 seconds
    private static final int PAGE_LOAD_TIMEOUT = 20000; // 20 seconds

    // ===============================
    // UI Components
    // ===============================
    
    private WebView webView;
    private ProgressBar progressBar;
    private ProgressBar loadingIndicator;
    private SwipeRefreshLayout swipeRefreshLayout;
    private CoordinatorLayout coordinatorLayout;
    private LinearLayout networkStatusContainer;
    private LinearLayout errorContainer;
    private TextView networkStatusText;
    private TextView retryButton;
    private TextView errorTitle;
    private TextView errorMessage;
    private ImageButton retryErrorButton;
    private ImageButton settingsButton;

    // ===============================
    // Core Management
    // ===============================
    
    private ConnectivityManager connectivityManager;
    private ConnectivityManager.NetworkCallback networkCallback;
    private SharedPreferences sharedPreferences;
    private Handler mainHandler;
    private Handler linkMonitorHandler;

    // ===============================
    // Link Monitoring System
    // ===============================
    
    private final Set<String> detectedLinks = new HashSet<>();
    private final List<String> validVideoLinks = new ArrayList<>();
    private final AtomicBoolean isLinkMonitoringActive = new AtomicBoolean(false);
    private final AtomicInteger linkMonitorRetryCount = new AtomicInteger(0);
    private final AtomicInteger domReadyAttempts = new AtomicInteger(0);
    private final AtomicInteger validLinkCount = new AtomicInteger(0);
    
    private Runnable linkMonitorRunnable;
    private Runnable domReadyChecker;

    // ===============================
    // State Management
    // ===============================
    
    private final AtomicBoolean isPageLoaded = new AtomicBoolean(false);
    private final AtomicBoolean isNetworkAvailable = new AtomicBoolean(false);
    private final AtomicBoolean isFragmentActive = new AtomicBoolean(false);
    private final AtomicBoolean isInitialized = new AtomicBoolean(false);
    private final AtomicBoolean isLoading = new AtomicBoolean(false);
    private final AtomicBoolean isDomReady = new AtomicBoolean(false);
    
    private boolean isFirstLaunch = true;
    private boolean isUserSignedIn = false;
    private int retryCount = 0;
    private static final int MAX_RETRY_COUNT = 3;

    // ===============================
    // Fragment Lifecycle
    // ===============================

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                           @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "Creating YouTube Fragment view");
        
        try {
            View view = inflater.inflate(R.layout.fragment_youtube, container, false);
            
            if (view != null) {
                initializeComponents(view);
                initializePreferences();
                setupNetworkMonitoring();
                configureWebView();
                setupSwipeRefresh();
                setupErrorHandling();
                setupBackPressHandling();
                setupLinkMonitoringSystem();
                
                // Delayed initialization to ensure everything is ready
                scheduleDelayedTask(() -> {
                    if (isFragmentActive.get()) {
                        performInitialLoad();
                    }
                }, 1000);
                
                isInitialized.set(true);
                Log.d(TAG, "Fragment view created successfully");
            }
            
            return view;
            
        } catch (Exception e) {
            Log.e(TAG, "Error creating view", e);
            showSafeToast("ভিউ তৈরি করতে সমস্যা হয়েছে");
            return null;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        isFragmentActive.set(true);
        
        try {
            if (webView != null) {
                webView.onResume();
                webView.resumeTimers();
            }
            
            registerNetworkCallback();
            
            // Resume link monitoring if page is loaded
            if (isPageLoaded.get() && isDomReady.get()) {
                startLinkMonitoring();
            }
            
            Log.d(TAG, "Fragment resumed");
        } catch (Exception e) {
            Log.e(TAG, "Error in onResume", e);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        isFragmentActive.set(false);
        
        try {
            if (webView != null) {
                webView.onPause();
                webView.pauseTimers();
            }
            
            stopLinkMonitoring();
            Log.d(TAG, "Fragment paused");
        } catch (Exception e) {
            Log.e(TAG, "Error in onPause", e);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isFragmentActive.set(false);
        isInitialized.set(false);
        
        try {
            unregisterNetworkCallback();
            stopLinkMonitoring();
            cleanupWebView();
            cleanupHandlers();
            
            Log.d(TAG, "Fragment destroyed");
        } catch (Exception e) {
            Log.e(TAG, "Error in onDestroy", e);
        }
    }

    // ===============================
    // Enhanced Link Monitoring System
    // ===============================

    /**
     * Setup comprehensive link monitoring system
     */
    private void setupLinkMonitoringSystem() {
        try {
            // Initialize collections
            detectedLinks.clear();
            validVideoLinks.clear();
            validLinkCount.set(0);
            
            // Create DOM ready checker
            domReadyChecker = new Runnable() {
                @Override
                public void run() {
                    if (!isFragmentActive.get() || !isPageLoaded.get()) {
                        return;
                    }
                    
                    checkDomReadiness();
                }
            };
            
            // Create enhanced link monitor
            linkMonitorRunnable = new Runnable() {
                @Override
                public void run() {
                    if (!isFragmentActive.get() || !isPageLoaded.get() || !isDomReady.get()) {
                        scheduleNextMonitoringCycle();
                        return;
                    }
                    
                    performLinkExtraction();
                    scheduleNextMonitoringCycle();
                }
            };
            
            Log.d(TAG, "Enhanced link monitoring system initialized");
            
        } catch (Exception e) {
            Log.e(TAG, "Error setting up link monitoring system", e);
        }
    }

    /**
     * Check if DOM is ready for link extraction
     */
    private void checkDomReadiness() {
        try {
            if (webView == null || !isFragmentActive.get()) {
                return;
            }
            
            String domReadyScript = 
                "javascript:(function() {" +
                "  try {" +
                "    var readyState = document.readyState;" +
                "    var hasContent = document.body && document.body.children.length > 0;" +
                "    var hasYouTubeContent = document.querySelector('ytd-app, #content, .ytd-page-manager') !== null;" +
                "    " +
                "    var result = {" +
                "      'readyState': readyState," +
                "      'hasContent': hasContent," +
                "      'hasYouTubeContent': hasYouTubeContent," +
                "      'isReady': readyState === 'complete' && hasContent && hasYouTubeContent" +
                "    };" +
                "    " +
                "    if (window." + JAVASCRIPT_INTERFACE_NAME + ") {" +
                "      window." + JAVASCRIPT_INTERFACE_NAME + ".onDomReadyCheck(JSON.stringify(result));" +
                "    }" +
                "    " +
                "    return result;" +
                "  } catch(e) {" +
                "    if (window." + JAVASCRIPT_INTERFACE_NAME + ") {" +
                "      window." + JAVASCRIPT_INTERFACE_NAME + ".onDomReadyCheck('{\"isReady\":false,\"error\":\"' + e.message + '\"}');" +
                "    }" +
                "    return {isReady: false, error: e.message};" +
                "  }" +
                "})();";
            
            webView.evaluateJavascript(domReadyScript, null);
            
        } catch (Exception e) {
            Log.e(TAG, "Error checking DOM readiness", e);
        }
    }

    /**
     * Process DOM ready check result
     */
    private void processDomReadyResult(String resultJson) {
        try {
            JSONObject result = new JSONObject(resultJson);
            boolean isReady = result.optBoolean("isReady", false);
            
            if (isReady) {
                isDomReady.set(true);
                domReadyAttempts.set(0);
                Log.d(TAG, "DOM is ready - starting link monitoring");
                startLinkMonitoring();
            } else {
                int attempts = domReadyAttempts.incrementAndGet();
                if (attempts < DOM_READY_MAX_ATTEMPTS) {
                    Log.d(TAG, "DOM not ready yet, attempt " + attempts + "/" + DOM_READY_MAX_ATTEMPTS);
                    scheduleDelayedTask(domReadyChecker, DOM_READY_CHECK_INTERVAL);
                } else {
                    Log.w(TAG, "DOM ready check max attempts reached, proceeding anyway");
                    isDomReady.set(true);
                    startLinkMonitoring();
                }
            }
            
        } catch (JSONException e) {
            Log.e(TAG, "Error processing DOM ready result", e);
            // Fallback: assume DOM is ready after max attempts
            if (domReadyAttempts.incrementAndGet() >= DOM_READY_MAX_ATTEMPTS) {
                isDomReady.set(true);
                startLinkMonitoring();
            }
        }
    }

    /**
     * Start link monitoring with proper timing
     */
    private void startLinkMonitoring() {
        try {
            if (!isLinkMonitoringActive.compareAndSet(false, true)) {
                Log.d(TAG, "Link monitoring already active");
                return;
            }
            
            if (!isFragmentActive.get() || !isPageLoaded.get()) {
                isLinkMonitoringActive.set(false);
                Log.d(TAG, "Cannot start link monitoring - fragment inactive or page not loaded");
                return;
            }
            
            linkMonitorRetryCount.set(0);
            
            // Start monitoring with initial delay
            scheduleDelayedTask(linkMonitorRunnable, LINK_MONITOR_INITIAL_DELAY);
            
            Log.d(TAG, "Link monitoring started successfully");
            
        } catch (Exception e) {
            Log.e(TAG, "Error starting link monitoring", e);
            isLinkMonitoringActive.set(false);
        }
    }

    /**
     * Stop link monitoring
     */
    private void stopLinkMonitoring() {
        try {
            if (isLinkMonitoringActive.compareAndSet(true, false)) {
                if (linkMonitorHandler != null) {
                    linkMonitorHandler.removeCallbacks(linkMonitorRunnable);
                    linkMonitorHandler.removeCallbacks(domReadyChecker);
                }
                Log.d(TAG, "Link monitoring stopped");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error stopping link monitoring", e);
        }
    }

    /**
     * Perform enhanced link extraction
     */
    private void performLinkExtraction() {
        try {
            if (webView == null || !isFragmentActive.get()) {
                return;
            }
            
            String enhancedLinkExtractionScript = 
                "javascript:(function() {" +
                "  try {" +
                "    var links = [];" +
                "    var videoLinks = [];" +
                "    var uniqueLinks = new Set();" +
                "    " +
                "    // Enhanced link extraction with multiple selectors" +
                "    var selectors = [" +
                "      'a[href*=\"watch?v=\"]'," +
                "      'a[href*=\"youtu.be/\"]'," +
                "      'a[href*=\"/video/\"]'," +
                "      'a[href*=\"embed/\"]'," +
                "      'ytd-video-renderer a'," +
                "      'ytd-compact-video-renderer a'," +
                "      'ytd-grid-video-renderer a'," +
                "      '.ytd-thumbnail a'," +
                "      '#video-title'," +
                "      '.video-title-link'" +
                "    ];" +
                "    " +
                "    // Extract links using multiple methods" +
                "    selectors.forEach(function(selector) {" +
                "      try {" +
                "        var elements = document.querySelectorAll(selector);" +
                "        for (var i = 0; i < elements.length; i++) {" +
                "          var href = elements[i].href;" +
                "          if (href && href.length > 0 && !uniqueLinks.has(href)) {" +
                "            uniqueLinks.add(href);" +
                "            links.push(href);" +
                "            " +
                "            // Check for video patterns" +
                "            if (href.includes('watch?v=') || href.includes('youtu.be/') || " +
                "                href.includes('/video/') || href.includes('embed/')) {" +
                "              videoLinks.push(href);" +
                "            }" +
                "          }" +
                "        }" +
                "      } catch(e) { console.warn('Selector error:', selector, e); }" +
                "    });" +
                "    " +
                "    // Also check for video elements" +
                "    var videos = document.querySelectorAll('video');" +
                "    for (var i = 0; i < videos.length; i++) {" +
                "      var src = videos[i].src || videos[i].currentSrc;" +
                "      if (src && src.length > 0 && !uniqueLinks.has(src)) {" +
                "        uniqueLinks.add(src);" +
                "        videoLinks.push(src);" +
                "      }" +
                "    }" +
                "    " +
                "    // Get page info" +
                "    var pageInfo = {" +
                "      'url': window.location.href," +
                "      'title': document.title," +
                "      'isVideoPage': window.location.href.includes('watch?v=')," +
                "      'isChannelPage': window.location.href.includes('/channel/') || window.location.href.includes('/c/')," +
                "      'isHomePage': window.location.href === 'https://www.youtube.com/' || window.location.href === 'https://m.youtube.com/'" +
                "    };" +
                "    " +
                "    // Create comprehensive result" +
                "    var result = {" +
                "      'totalLinks': links.length," +
                "      'videoLinks': videoLinks," +
                "      'allLinks': links.slice(0, 100)," + // Increased limit
                "      'pageInfo': pageInfo," +
                "      'timestamp': Date.now()" +
                "    };" +
                "    " +
                "    // Send to Android" +
                "    if (window." + JAVASCRIPT_INTERFACE_NAME + ") {" +
                "      window." + JAVASCRIPT_INTERFACE_NAME + ".onLinksDetected(JSON.stringify(result));" +
                "    }" +
                "    " +
                "    return result;" +
                "  } catch(e) {" +
                "    var errorResult = {" +
                "      'error': e.message," +
                "      'totalLinks': 0," +
                "      'videoLinks': []," +
                "      'allLinks': []" +
                "    };" +
                "    " +
                "    if (window." + JAVASCRIPT_INTERFACE_NAME + ") {" +
                "      window." + JAVASCRIPT_INTERFACE_NAME + ".onLinksDetected(JSON.stringify(errorResult));" +
                "    }" +
                "    " +
                "    return errorResult;" +
                "  }" +
                "})();";
            
            webView.evaluateJavascript(enhancedLinkExtractionScript, null);
            
        } catch (Exception e) {
            Log.e(TAG, "Error performing link extraction", e);
        }
    }

    /**
     * Process detected links with enhanced validation
     */
    private void processDetectedLinks(String linksJson) {
        try {
            JSONObject result = new JSONObject(linksJson);
            
            // Check for errors
            if (result.has("error")) {
                String error = result.optString("error");
                Log.w(TAG, "Link extraction error: " + error);
                handleLinkExtractionRetry();
                return;
            }
            
            int totalLinks = result.optInt("totalLinks", 0);
            JSONArray videoLinksArray = result.optJSONArray("videoLinks");
            JSONArray allLinksArray = result.optJSONArray("allLinks");
            JSONObject pageInfo = result.optJSONObject("pageInfo");
            
            // Clear previous data
            synchronized (detectedLinks) {
                detectedLinks.clear();
            }
            synchronized (validVideoLinks) {
                validVideoLinks.clear();
            }
            
            // Process all links
            if (allLinksArray != null) {
                for (int i = 0; i < allLinksArray.length(); i++) {
                    String link = allLinksArray.optString(i);
                    if (isValidLink(link)) {
                        synchronized (detectedLinks) {
                            detectedLinks.add(link);
                        }
                    }
                }
            }
            
            // Process video links with enhanced validation
            if (videoLinksArray != null) {
                for (int i = 0; i < videoLinksArray.length(); i++) {
                    String videoLink = videoLinksArray.optString(i);
                    if (isValidVideoLink(videoLink)) {
                        synchronized (validVideoLinks) {
                            validVideoLinks.add(videoLink);
                        }
                    }
                }
            }
            
            int newValidLinkCount = validVideoLinks.size();
            validLinkCount.set(newValidLinkCount);
            
            // Log page information
            if (pageInfo != null) {
                String pageUrl = pageInfo.optString("url", "unknown");
                boolean isVideoPage = pageInfo.optBoolean("isVideoPage", false);
                Log.d(TAG, "Page analysis - URL: " + pageUrl + ", Is video page: " + isVideoPage);
            }
            
            Log.d(TAG, "Links processed successfully - Total: " + totalLinks +
                   ", Valid general: " + detectedLinks.size() +
                   ", Valid video: " + newValidLinkCount);
            
            // Reset retry count on success
            linkMonitorRetryCount.set(0);
            
            // Update UI
            updateSettingsButtonVisibility();
            
        } catch (JSONException e) {
            Log.e(TAG, "Error processing detected links JSON", e);
            handleLinkExtractionRetry();
        } catch (Exception e) {
            Log.e(TAG, "Error processing detected links", e);
            handleLinkExtractionRetry();
        }
    }

    /**
     * Handle link extraction retry logic
     */
    private void handleLinkExtractionRetry() {
        try {
            int retries = linkMonitorRetryCount.incrementAndGet();
            if (retries < LINK_MONITOR_MAX_RETRIES) {
                Log.d(TAG, "Retrying link extraction, attempt " + retries + "/" + LINK_MONITOR_MAX_RETRIES);
                // Retry with exponential backoff
                int delay = LINK_MONITOR_INTERVAL * retries;
                scheduleDelayedTask(this::performLinkExtraction, delay);
            } else {
                Log.w(TAG, "Max link extraction retries reached");
                linkMonitorRetryCount.set(0);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error handling link extraction retry", e);
        }
    }

    /**
     * Schedule next monitoring cycle
     */
    private void scheduleNextMonitoringCycle() {
        try {
            if (isLinkMonitoringActive.get() && isFragmentActive.get()) {
                scheduleDelayedTask(linkMonitorRunnable, LINK_MONITOR_INTERVAL);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error scheduling next monitoring cycle", e);
        }
    }

    /**
     * Enhanced link validation
     */
    private boolean isValidLink(String link) {
        try {
            if (link == null || link.trim().isEmpty()) {
                return false;
            }
            
            // Basic URL pattern validation
            if (!Patterns.WEB_URL.matcher(link).matches()) {
                return false;
            }
            
            // Check for valid protocols
            String lowerLink = link.toLowerCase();
            if (!lowerLink.startsWith("http://") && !lowerLink.startsWith("https://")) {
                return false;
            }
            
            // Check for YouTube domains
            return lowerLink.contains("youtube.com") || 
                   lowerLink.contains("youtu.be") || 
                   lowerLink.contains("googlevideo.com");
            
        } catch (Exception e) {
            Log.e(TAG, "Error validating link: " + link, e);
            return false;
        }
    }

    /**
     * Enhanced video link validation
     */
    private boolean isValidVideoLink(String link) {
        try {
            if (!isValidLink(link)) {
                return false;
            }
            
            String lowerLink = link.toLowerCase();
            
            // YouTube video patterns
            if (lowerLink.contains("youtube.com/watch?v=") ||
                lowerLink.contains("youtu.be/") ||
                lowerLink.contains("youtube.com/embed/") ||
                lowerLink.contains("youtube.com/v/")) {
                return true;
            }
            
            // Generic video patterns
            if (lowerLink.contains("/video/") ||
                lowerLink.endsWith(".mp4") ||
                lowerLink.endsWith(".webm") ||
                lowerLink.endsWith(".m3u8") ||
                lowerLink.contains("googlevideo.com")) {
                return true;
            }
            
            return false;
            
        } catch (Exception e) {
            Log.e(TAG, "Error validating video link: " + link, e);
            return false;
        }
    }

    /**
     * Update settings button visibility with animation
     */
    private void updateSettingsButtonVisibility() {
        try {
            if (settingsButton == null || !isFragmentActive.get()) {
                return;
            }
            
            runOnUiThread(() -> {
                try {
                    int currentValidLinks = validLinkCount.get();
                    boolean shouldShow = currentValidLinks >= MIN_LINKS_TO_SHOW_BUTTON;
                    
                    if (shouldShow && settingsButton.getVisibility() != View.VISIBLE) {
                        settingsButton.setVisibility(View.VISIBLE);
                        settingsButton.setAlpha(0f);
                        settingsButton.animate()
                            .alpha(1f)
                            .setDuration(300)
                            .start();
                        
                        Log.d(TAG, "Settings button shown - " + currentValidLinks + " valid links found");
                        
                    } else if (!shouldShow && settingsButton.getVisibility() == View.VISIBLE) {
                        settingsButton.animate()
                            .alpha(0f)
                            .setDuration(300)
                            .withEndAction(() -> settingsButton.setVisibility(View.GONE))
                            .start();
                        
                        Log.d(TAG, "Settings button hidden - insufficient valid links");
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error updating settings button visibility", e);
                }
            });
            
        } catch (Exception e) {
            Log.e(TAG, "Error in updateSettingsButtonVisibility", e);
        }
    }

    // ===============================
    // JavaScript Interface
    // ===============================

    /**
     * Enhanced JavaScript interface for link monitoring
     */
    private class LinkMonitorInterface {
        
        @JavascriptInterface
        public void onLinksDetected(String linksJson) {
            try {
                runOnUiThread(() -> processDetectedLinks(linksJson));
            } catch (Exception e) {
                Log.e(TAG, "Error in onLinksDetected interface", e);
            }
        }
        
        @JavascriptInterface
        public void onDomReadyCheck(String resultJson) {
            try {
                runOnUiThread(() -> processDomReadyResult(resultJson));
            } catch (Exception e) {
                Log.e(TAG, "Error in onDomReadyCheck interface", e);
            }
        }
        
        @JavascriptInterface
        public void onError(String error) {
            try {
                Log.w(TAG, "JavaScript interface error: " + error);
            } catch (Exception e) {
                Log.e(TAG, "Error in onError interface", e);
            }
        }
    }

    // ===============================
    // Enhanced WebViewClient
    // ===============================

    private class YouTubeWebViewClient extends WebViewClient {
        
        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            try {
                super.onPageStarted(view, url, favicon);
                
                // Reset states
                isPageLoaded.set(false);
                isDomReady.set(false);
                domReadyAttempts.set(0);
                
                // Stop current monitoring
                stopLinkMonitoring();
                
                // Hide settings button during loading
                if (settingsButton != null) {
                    settingsButton.setVisibility(View.GONE);
                }
                
                updateUIState(UIState.LOADING);
                
                Log.d(TAG, "Page loading started: " + url);
                
                // Update user state
                if (url != null && url.contains("youtube.com") && !url.contains("accounts.google.com")) {
                    markFirstLaunchCompleted();
                    markUserSignedIn();
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Error in onPageStarted", e);
            }
        }
        
        @Override
        public void onPageFinished(WebView view, String url) {
            try {
                super.onPageFinished(view, url);
                
                isPageLoaded.set(true);
                updateUIState(UIState.SUCCESS);
                retryCount = 0;
                
                // Inject custom JavaScript
                injectCustomJavaScript(view);
                
                Log.d(TAG, "Page loading finished: " + url);
                
                // Start DOM ready checking after a short delay
                scheduleDelayedTask(() -> {
                    if (isFragmentActive.get() && isPageLoaded.get()) {
                        domReadyAttempts.set(0);
                        checkDomReadiness();
                    }
                }, 2000);
                
            } catch (Exception e) {
                Log.e(TAG, "Error in onPageFinished", e);
            }
        }
        
        @Override
        public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
            try {
                super.onReceivedError(view, request, error);
                
                if (request != null && request.isForMainFrame()) {
                    isPageLoaded.set(false);
                    isDomReady.set(false);
                    stopLinkMonitoring();
                    
                    // Hide settings button on error
                    if (settingsButton != null) {
                        settingsButton.setVisibility(View.GONE);
                    }
                    
                    updateUIState(UIState.ERROR);
                    handleWebViewError(error);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error in onReceivedError", e);
            }
        }
        
        // ... (rest of WebViewClient methods remain the same)
        
        private void injectCustomJavaScript(WebView view) {
            try {
                if (view != null) {
                    String customScript =
                        "javascript:(function() {" +
                        "  try {" +
                        "    // Enhanced custom script" +
                        "    document.body.style.userSelect = 'none';" +
                        "    document.body.style.webkitUserSelect = 'none';" +
                        "    " +
                        "    // Add mutation observer for dynamic content" +
                        "    if (window.MutationObserver && !window.hdStreamzObserver) {" +
                        "      window.hdStreamzObserver = new MutationObserver(function(mutations) {" +
                        "        // Notify about DOM changes" +
                        "        if (window." + JAVASCRIPT_INTERFACE_NAME + " && mutations.length > 0) {" +
                        "          setTimeout(function() {" +
                        "            // Trigger link re-extraction after DOM changes" +
                        "          }, 1000);" +
                        "        }" +
                        "      });" +
                        "      " +
                        "      window.hdStreamzObserver.observe(document.body, {" +
                        "        childList: true," +
                        "        subtree: true" +
                        "      });" +
                        "    }" +
                        "    " +
                        "    console.log('HD Streamz TV - Enhanced script with link monitoring injected');" +
                        "  } catch(e) { " +
                        "    console.error('Enhanced script injection failed:', e); " +
                        "    if (window." + JAVASCRIPT_INTERFACE_NAME + ") {" +
                        "      window." + JAVASCRIPT_INTERFACE_NAME + ".onError('Script injection failed: ' + e.message);" +
                        "    }" +
                        "  }" +
                        "})();";
                    
                    view.evaluateJavascript(customScript, result -> {
                        Log.d(TAG, "Enhanced custom JavaScript injected successfully");
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "Error injecting enhanced JavaScript", e);
            }
        }
        
        private void handleWebViewError(WebResourceError error) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && error != null) {
                    int errorCode = error.getErrorCode();
                    switch (errorCode) {
                        case ERROR_HOST_LOOKUP:
                        case ERROR_CONNECT:
                        case ERROR_TIMEOUT:
                            updateUIState(UIState.NETWORK_ERROR);
                            break;
                        case ERROR_FILE_NOT_FOUND:
                            showSafeToast("YouTube সার্ভারে সমস্যা হয়েছে। কিছুক্ষণ পর চেষ্টা করুন");
                            break;
                        default:
                            showSafeToast("পেজ লোড করতে সমস্যা হয়েছে। আবার চেষ্টা করুন");
                            break;
                    }
                } else {
                    showSafeToast("পেজ লোড করতে সমস্যা হয়েছে। আবার চেষ্টা করুন");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error handling WebView error", e);
            }
        }
    }

    // ===============================
    // Utility Methods
    // ===============================

    /**
     * Schedule delayed task safely
     */
    private void scheduleDelayedTask(Runnable task, long delay) {
        try {
            if (linkMonitorHandler != null && task != null) {
                linkMonitorHandler.postDelayed(task, delay);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error scheduling delayed task", e);
        }
    }

    /**
     * Run task on UI thread safely
     */
    private void runOnUiThread(Runnable task) {
        try {
            if (mainHandler != null && task != null) {
                mainHandler.post(task);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error running task on UI thread", e);
        }
    }

    /**
     * Clean up handlers safely
     */
    private void cleanupHandlers() {
        try {
            if (mainHandler != null) {
                mainHandler.removeCallbacksAndMessages(null);
                mainHandler = null;
            }
            
            if (linkMonitorHandler != null) {
                linkMonitorHandler.removeCallbacksAndMessages(null);
                linkMonitorHandler = null;
            }
            
            Log.d(TAG, "Handlers cleaned up");
        } catch (Exception e) {
            Log.e(TAG, "Error cleaning up handlers", e);
        }
    }

    // ===============================
    // Public API Methods
    // ===============================

    /**
     * Get detected valid video links (thread-safe)
     */
    public List<String> getValidVideoLinks() {
        synchronized (validVideoLinks) {
            return new ArrayList<>(validVideoLinks);
        }
    }

    /**
     * Get all detected links (thread-safe)
     */
    public Set<String> getAllDetectedLinks() {
        synchronized (detectedLinks) {
            return new HashSet<>(detectedLinks);
        }
    }

    /**
     * Get valid link count
     */
    public int getValidLinkCount() {
        return validLinkCount.get();
    }

    /**
     * Force refresh link monitoring
     */
    public void refreshLinkMonitoring() {
        try {
            if (isPageLoaded.get() && isDomReady.get()) {
                performLinkExtraction();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error refreshing link monitoring", e);
        }
    }

    // ===============================
    // Settings Button Handler
    // ===============================

    /**
     * Enhanced settings button click handler
     */
    private void handleSettingsClick() {
        try {
            int currentValidLinks = validLinkCount.get();
            
            if (currentValidLinks > 0) {
                StringBuilder message = new StringBuilder();
                message.append("পাওয়া গেছে ").append(currentValidLinks).append(" টি ভ্যালিড ভিডিও লিংক!\n");
                
                synchronized (detectedLinks) {
                    message.append("মোট লিংক: ").append(detectedLinks.size());
                }
                
                showSafeToast(message.toString());
                
                // Log detected links for debugging
                Log.d(TAG, "=== Valid Video Links Found ===");
                synchronized (validVideoLinks) {
                    for (int i = 0; i < validVideoLinks.size() && i < 10; i++) { // Log first 10
                        Log.d(TAG, (i + 1) + ". " + validVideoLinks.get(i));
                    }
                }
                Log.d(TAG, "=== End of Links ===");
                
            } else {
                showSafeToast("কোন ভ্যালিড ভিডিও লিংক পাওয়া যায়নি");
                
                // Force refresh monitoring
                if (isPageLoaded.get()) {
                    Log.d(TAG, "Forcing link monitoring refresh");
                    refreshLinkMonitoring();
                }
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error handling settings click", e);
            showSafeToast("সেটিংস বাটন ক্লিক করতে সমস্যা হয়েছে");
        }
    }

    // ===============================
    // Component Initialization (Simplified)
    // ===============================

    private void initializeComponents(View view) {
        try {
            if (view == null) {
                Log.e(TAG, "View is null in initializeComponents");
                return;
            }
            
            // Initialize handlers first
            mainHandler = new Handler(Looper.getMainLooper());
            linkMonitorHandler = new Handler(Looper.getMainLooper());
            
            // Find UI components
            coordinatorLayout = view.findViewById(R.id.coordinator);
            webView = view.findViewById(R.id.webview);
            progressBar = view.findViewById(R.id.progressBar);
            loadingIndicator = view.findViewById(R.id.loading_indicator);
            swipeRefreshLayout = view.findViewById(R.id.swipe_refresh);
            networkStatusContainer = view.findViewById(R.id.network_status_container);
            errorContainer = view.findViewById(R.id.error_container);
            networkStatusText = view.findViewById(R.id.network_status_text);
            retryButton = view.findViewById(R.id.retry_button);
            errorTitle = view.findViewById(R.id.error_title);
            errorMessage = view.findViewById(R.id.error_message);
            retryErrorButton = view.findViewById(R.id.retry_error_button);
            settingsButton = view.findViewById(R.id.settings_button);
            
            // Initially hide settings button
            if (settingsButton != null) {
                settingsButton.setVisibility(View.GONE);
            }
            
            updateUIState(UIState.IDLE);
            
            Log.d(TAG, "Components initialized successfully");
            
        } catch (Exception e) {
            Log.e(TAG, "Error initializing components", e);
        }
    }

    // ===============================
    // WebView Configuration (Enhanced)
    // ===============================

    @SuppressLint("SetJavaScriptEnabled")
    private void configureWebView() {
        try {
            if (webView == null) {
                Log.w(TAG, "WebView is null, cannot configure");
                return;
            }
            
            WebSettings webSettings = webView.getSettings();
            if (webSettings == null) {
                Log.w(TAG, "WebSettings is null, cannot configure");
                return;
            }
            
            // Core JavaScript and DOM support
            webSettings.setJavaScriptEnabled(true);
            webSettings.setDomStorageEnabled(true);
            webSettings.setDatabaseEnabled(true);
            
            // Enhanced caching strategy
            webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
           // webSettings.setAppCacheEnabled(true);
            
            // Viewport and zoom configuration
            webSettings.setLoadWithOverviewMode(true);
            webSettings.setUseWideViewPort(true);
            webSettings.setBuiltInZoomControls(true);
            webSettings.setDisplayZoomControls(false);
            webSettings.setSupportZoom(true);
            
            // Media playback optimization
            webSettings.setMediaPlaybackRequiresUserGesture(false);
            webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
            
            // Performance improvements
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                webSettings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.TEXT_AUTOSIZING);
            }
            
            // Security settings
            webSettings.setAllowFileAccess(false);
            webSettings.setAllowContentAccess(false);
            webSettings.setAllowFileAccessFromFileURLs(false);
            webSettings.setAllowUniversalAccessFromFileURLs(false);
            
            // Location and popup control
            webSettings.setGeolocationEnabled(true);
            webSettings.setJavaScriptCanOpenWindowsAutomatically(false);
            
            // Enhanced User Agent
            webSettings.setUserAgentString(USER_AGENT);
            
            // Add enhanced JavaScript interface
            webView.addJavascriptInterface(new LinkMonitorInterface(), JAVASCRIPT_INTERFACE_NAME);
            
            // Cookie management
            setupCookieManager();
            
            // Set custom clients
            webView.setWebViewClient(new YouTubeWebViewClient());
            webView.setWebChromeClient(new YouTubeWebChromeClient());
            
            // Hardware acceleration
            webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
            
            // Enable focus for back press handling
            webView.setFocusable(true);
            webView.setFocusableInTouchMode(true);
            
            Log.d(TAG, "WebView configured successfully with enhanced link monitoring");
            
        } catch (Exception e) {
            Log.e(TAG, "Error configuring WebView", e);
        }
    }

    // ===============================
    // Remaining Methods (Unchanged)
    // ===============================
    
    // ... (Include all other methods from the original code that weren't modified)
    // This includes: initializePreferences, setupNetworkMonitoring, setupSwipeRefresh,
    // setupErrorHandling, setupBackPressHandling, UI state management methods,
    // YouTube loading methods, utility methods, etc.

    private void initializePreferences() {
        try {
            Context context = getContext();
            if (context != null) {
                sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                isFirstLaunch = sharedPreferences.getBoolean(KEY_FIRST_LAUNCH, true);
                isUserSignedIn = sharedPreferences.getBoolean(KEY_USER_SIGNED_IN, false);
                
                Log.d(TAG, "Preferences initialized - First launch: " + isFirstLaunch +
                       ", User signed in: " + isUserSignedIn);
            } else {
                Log.w(TAG, "Context is null, using default preferences");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error initializing preferences", e);
        }
    }

    private void setupNetworkMonitoring() {
        try {
            Context context = getContext();
            if (context != null) {
                connectivityManager = (ConnectivityManager)
                    context.getSystemService(Context.CONNECTIVITY_SERVICE);
                
                isNetworkAvailable.set(checkNetworkAvailability());
                createNetworkCallback();
                
                Log.d(TAG, "Network monitoring configured");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error setting up network monitoring", e);
        }
    }

    private void createNetworkCallback() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                networkCallback = new ConnectivityManager.NetworkCallback() {
                    @Override
                    public void onAvailable(@NonNull Network network) {
                        isNetworkAvailable.set(true);
                        if (isFragmentActive.get() && getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                try {
                                    hideNetworkError();
                                    if (!isPageLoaded.get() && !isLoading.get() && isInitialized.get()) {
                                        performInitialLoad();
                                    }
                                } catch (Exception e) {
                                    Log.e(TAG, "Error in network available callback", e);
                                }
                            });
                        }
                    }
                    
                    @Override
                    public void onLost(@NonNull Network network) {
                        isNetworkAvailable.set(false);
                        if (isFragmentActive.get() && getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                try {
                                    showNetworkError();
                                } catch (Exception e) {
                                    Log.e(TAG, "Error in network lost callback", e);
                                }
                            });
                        }
                    }
                };
            }
        } catch (Exception e) {
            Log.e(TAG, "Error creating network callback", e);
        }
    }

    private void registerNetworkCallback() {
        try {
            if (connectivityManager != null && networkCallback != null &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                NetworkRequest.Builder builder = new NetworkRequest.Builder();
                connectivityManager.registerNetworkCallback(builder.build(), networkCallback);
                Log.d(TAG, "Network callback registered");
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to register network callback", e);
        }
    }

    private void unregisterNetworkCallback() {
        try {
            if (connectivityManager != null && networkCallback != null &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                connectivityManager.unregisterNetworkCallback(networkCallback);
                Log.d(TAG, "Network callback unregistered");
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to unregister network callback", e);
        }
    }

    private boolean checkNetworkAvailability() {
        try {
            if (connectivityManager == null) return false;
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Network activeNetwork = connectivityManager.getActiveNetwork();
                if (activeNetwork == null) return false;
                
                NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(activeNetwork);
                return capabilities != null &&
                       (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET));
            } else {
                android.net.NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
                return activeNetworkInfo != null && activeNetworkInfo.isConnected();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking network availability", e);
            return false;
        }
    }

    private void setupSwipeRefresh() {
        try {
            if (swipeRefreshLayout != null) {
                swipeRefreshLayout.setColorSchemeResources(
                    android.R.color.holo_blue_bright,
                    android.R.color.holo_green_light,
                    android.R.color.holo_orange_light,
                    android.R.color.holo_red_light
                );
                
                swipeRefreshLayout.setDistanceToTriggerSync(150);
                swipeRefreshLayout.setOnRefreshListener(this::handleRefresh);
                
                Log.d(TAG, "SwipeRefresh configured");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error setting up SwipeRefresh", e);
        }
    }

    private void setupErrorHandling() {
        try {
            if (retryButton != null) {
                retryButton.setOnClickListener(v -> handleRetry());
            }
            
            if (retryErrorButton != null) {
                retryErrorButton.setOnClickListener(v -> handleRetry());
            }
            
            if (settingsButton != null) {
                settingsButton.setOnClickListener(v -> handleSettingsClick());
            }
            
            Log.d(TAG, "Error handling configured");
        } catch (Exception e) {
            Log.e(TAG, "Error setting up error handling", e);
        }
    }

    private void setupBackPressHandling() {
        try {
            if (webView != null) {
                webView.setOnKeyListener((v, keyCode, event) -> {
                    if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_DOWN) {
                        return handleWebViewBackPress();
                    }
                    return false;
                });
                
                webView.requestFocus();
            }
            
            View rootView = getView();
            if (rootView != null) {
                rootView.setFocusableInTouchMode(true);
                rootView.requestFocus();
                rootView.setOnKeyListener((v, keyCode, event) -> {
                    if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_DOWN) {
                        return handleWebViewBackPress();
                    }
                    return false;
                });
            }
            
            Log.d(TAG, "Back press handling configured");
            
        } catch (Exception e) {
            Log.e(TAG, "Error setting up back press handling", e);
        }
    }

    private boolean handleWebViewBackPress() {
        try {
            if (webView != null && webView.canGoBack()) {
                webView.goBack();
                Log.d(TAG, "Navigating back in WebView history");
                showSafeToast("পূর্ববর্তী পেজে ফিরে যাচ্ছে");
                return true;
            } else {
                showSafeToast("আর পিছনে যাওয়ার মতো পেজ নেই");
                Log.d(TAG, "No back history available in WebView");
                return true;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error handling back press", e);
            return true;
        }
    }

    public boolean onBackPressed() {
        return handleWebViewBackPress();
    }

    private void handleRefresh() {
        try {
            if (isNetworkAvailable.get()) {
                retryCount = 0;
                reloadYouTube();
            } else {
                if (swipeRefreshLayout != null) {
                    swipeRefreshLayout.setRefreshing(false);
                }
                showNetworkError();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error handling refresh", e);
        }
    }

    private void handleRetry() {
        try {
            if (retryCount < MAX_RETRY_COUNT) {
                retryCount++;
                hideError();
                performInitialLoad();
            } else {
                showSafeToast("সর্বোচ্চ চেষ্টার সীমা পৌঁছেছে। পরে আবার চেষ্টা করুন");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error handling retry", e);
        }
    }

    private enum UIState {
        IDLE, LOADING, ERROR, NETWORK_ERROR, SUCCESS
    }

    private void updateUIState(UIState state) {
        try {
            if (!isFragmentActive.get() || !isInitialized.get()) return;
            
            switch (state) {
                case LOADING:
                    showLoading();
                    break;
                case ERROR:
                    showError();
                    break;
                case NETWORK_ERROR:
                    showNetworkError();
                    break;
                case SUCCESS:
                    hideLoading();
                    hideError();
                    hideNetworkError();
                    break;
                case IDLE:
                default:
                    hideLoading();
                    hideError();
                    hideNetworkError();
                    break;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating UI state", e);
        }
    }

    private void showLoading() {
        try {
            isLoading.set(true);
            if (progressBar != null) {
                progressBar.setVisibility(View.VISIBLE);
            }
            if (loadingIndicator != null) {
                loadingIndicator.setVisibility(View.VISIBLE);
            }
            if (errorContainer != null) {
                errorContainer.setVisibility(View.GONE);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error showing loading", e);
        }
    }

    private void hideLoading() {
        try {
            isLoading.set(false);
            if (progressBar != null) {
                progressBar.setVisibility(View.GONE);
            }
            if (loadingIndicator != null) {
                loadingIndicator.setVisibility(View.GONE);
            }
            if (swipeRefreshLayout != null) {
                swipeRefreshLayout.setRefreshing(false);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error hiding loading", e);
        }
    }

    private void showError() {
        try {
            if (errorContainer != null) {
                errorContainer.setVisibility(View.VISIBLE);
            }
            if (webView != null) {
                webView.setVisibility(View.GONE);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error showing error", e);
        }
    }

    private void hideError() {
        try {
            if (errorContainer != null) {
                errorContainer.setVisibility(View.GONE);
            }
            if (webView != null) {
                webView.setVisibility(View.VISIBLE);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error hiding error", e);
        }
    }

    private void showNetworkError() {
        try {
            if (networkStatusContainer != null) {
                networkStatusContainer.setVisibility(View.VISIBLE);
                if (networkStatusText != null) {
                    networkStatusText.setText("ইন্টারনেট সংযোগ নেই");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error showing network error", e);
        }
    }

    private void hideNetworkError() {
        try {
            if (networkStatusContainer != null) {
                networkStatusContainer.setVisibility(View.GONE);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error hiding network error", e);
        }
    }

    private void performInitialLoad() {
        try {
            if (!isFragmentActive.get() || !isInitialized.get()) {
                Log.d(TAG, "Fragment not active or initialized, skipping load");
                return;
            }
            
            if (!isNetworkAvailable.get()) {
                updateUIState(UIState.NETWORK_ERROR);
                return;
            }
            
            updateUIState(UIState.LOADING);
            
            scheduleDelayedTask(() -> {
                if (isFragmentActive.get() && isInitialized.get()) {
                    loadYouTube();
                }
            }, 500);
        } catch (Exception e) {
            Log.e(TAG, "Error performing initial load", e);
        }
    }

    private void loadYouTube() {
        try {
            if (webView == null || !isFragmentActive.get() || !isInitialized.get()) {
                Log.w(TAG, "Cannot load YouTube - WebView null or fragment inactive");
                return;
            }
            
            String urlToLoad = determineUrlToLoad();
            webView.loadUrl(urlToLoad);
            isPageLoaded.set(false);
            
            Log.d(TAG, "Loading YouTube: " + urlToLoad);
            
            // Set timeout for loading
            scheduleDelayedTask(() -> {
                if (isLoading.get() && !isPageLoaded.get() && isFragmentActive.get()) {
                    Log.w(TAG, "Loading timeout reached");
                    updateUIState(UIState.ERROR);
                }
            }, PAGE_LOAD_TIMEOUT);
        } catch (Exception e) {
            Log.e(TAG, "Error loading YouTube", e);
            updateUIState(UIState.ERROR);
        }
    }

    private String determineUrlToLoad() {
        try {
            if (isFirstLaunch && !isUserSignedIn) {
                Log.d(TAG, "First launch - showing sign-in page");
                return YOUTUBE_SIGNIN_URL;
            } else {
                return shouldUseMobileVersion() ? YOUTUBE_MOBILE_URL : YOUTUBE_URL;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error determining URL", e);
            return YOUTUBE_URL;
        }
    }

    private void reloadYouTube() {
        try {
            if (webView != null && isFragmentActive.get()) {
                webView.reload();
                Log.d(TAG, "Reloading YouTube");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error reloading YouTube", e);
        }
    }

    private boolean shouldUseMobileVersion() {
        try {
            return getResources().getConfiguration().smallestScreenWidthDp < 600;
        } catch (Exception e) {
            Log.e(TAG, "Error checking mobile version", e);
            return true;
        }
    }

    private void markFirstLaunchCompleted() {
        try {
            if (isFirstLaunch && sharedPreferences != null) {
                isFirstLaunch = false;
                sharedPreferences.edit()
                    .putBoolean(KEY_FIRST_LAUNCH, false)
                    .apply();
                Log.d(TAG, "First launch completed");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error marking first launch completed", e);
        }
    }

    private void markUserSignedIn() {
        try {
            if (!isUserSignedIn && sharedPreferences != null) {
                isUserSignedIn = true;
                sharedPreferences.edit()
                    .putBoolean(KEY_USER_SIGNED_IN, true)
                    .apply();
                Log.d(TAG, "User signed in");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error marking user signed in", e);
        }
    }

    private void setupCookieManager() {
        try {
            CookieManager cookieManager = CookieManager.getInstance();
            if (cookieManager != null && webView != null) {
                cookieManager.setAcceptCookie(true);
                cookieManager.setAcceptThirdPartyCookies(webView, true);
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    cookieManager.flush();
                }
                
                Log.d(TAG, "Cookie manager configured");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error setting up cookie manager", e);
        }
    }

    private void showSafeToast(String message) {
        try {
            Context context = getContext();
            if (context != null && isFragmentActive.get() && message != null) {
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error showing toast", e);
        }
    }

    private void cleanupWebView() {
        try {
            if (webView != null) {
                webView.pauseTimers();
                webView.clearHistory();
                webView.clearCache(true);
                webView.clearFormData();
                webView.loadUrl("about:blank");
                webView.destroy();
                webView = null;
                Log.d(TAG, "WebView cleaned up");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error cleaning up WebView", e);
        }
    }

    // ===============================
    // Enhanced WebChromeClient
    // ===============================

    private class YouTubeWebChromeClient extends WebChromeClient {
        
        @Override
        public void onProgressChanged(WebView view, int newProgress) {
            try {
                super.onProgressChanged(view, newProgress);
                
                if (progressBar != null) {
                    if (newProgress == 100) {
                        progressBar.setVisibility(View.GONE);
                    } else {
                        progressBar.setVisibility(View.VISIBLE);
                        progressBar.setProgress(newProgress);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error in onProgressChanged", e);
            }
        }
        
        @Override
        public void onReceivedTitle(WebView view, String title) {
            try {
                super.onReceivedTitle(view, title);
                Log.d(TAG, "Page title: " + title);
                
                if (errorTitle != null && title != null) {
                    if (title.contains("Error") || title.contains("404")) {
                        errorTitle.setText("পেজ পাওয়া যায়নি");
                        if (errorMessage != null) {
                            errorMessage.setText("অনুরোধকৃত পেজটি খুঁজে পাওয়া যায়নি");
                        }
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error in onReceivedTitle", e);
            }
        }
        
        @Override
        public boolean onConsoleMessage(android.webkit.ConsoleMessage consoleMessage) {
            try {
                if (consoleMessage != null) {
                    Log.d(TAG, "WebView Console [" + consoleMessage.messageLevel() + "]: " +
                           consoleMessage.message() + " -- From line " +
                           consoleMessage.lineNumber() + " of " + consoleMessage.sourceId());
                }
                return true;
            } catch (Exception e) {
                Log.e(TAG, "Error in onConsoleMessage", e);
                return true;
            }
        }
        
        @Override
        public void onPermissionRequest(android.webkit.PermissionRequest request) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && request != null) {
                    String[] permissions = request.getResources();
                    if (permissions != null) {
                        for (String permission : permissions) {
                            if (permission.equals(android.webkit.PermissionRequest.RESOURCE_VIDEO_CAPTURE) ||
                                permission.equals(android.webkit.PermissionRequest.RESOURCE_AUDIO_CAPTURE)) {
                                request.grant(new String[]{permission});
                                Log.d(TAG, "Granted permission: " + permission);
                                return;
                            }
                        }
                    }
                    request.deny();
                    Log.d(TAG, "Denied permission request");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error in onPermissionRequest", e);
            }
        }
        
        @Override
        public void onShowCustomView(View view, CustomViewCallback callback) {
            try {
                super.onShowCustomView(view, callback);
                Log.d(TAG, "Entering fullscreen mode");
            } catch (Exception e) {
                Log.e(TAG, "Error in onShowCustomView", e);
            }
        }
        
        @Override
        public void onHideCustomView() {
            try {
                super.onHideCustomView();
                Log.d(TAG, "Exiting fullscreen mode");
            } catch (Exception e) {
                Log.e(TAG, "Error in onHideCustomView", e);
            }
        }
    }
}
