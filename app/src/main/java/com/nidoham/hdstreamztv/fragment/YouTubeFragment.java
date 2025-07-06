package com.nidoham.hdstreamztv.fragment;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import com.nidoham.hdstreamztv.PlayerActivity;
import com.nidoham.hdstreamztv.example.data.link.youtube.YouTube;
import com.nidoham.hdstreamztv.newpipe.extractors.helper.YouTubeStreamLinkFetcher;
import com.nidoham.hdstreamztv.template.model.settings.Template;
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
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.nidoham.hdstreamztv.databinding.FragmentYoutubeBinding;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Professional YouTube Fragment Implementation with View Binding
 * 
 * This fragment provides a comprehensive YouTube WebView experience with:
 * - View Binding for type-safe view access
 * - Advanced network monitoring and error handling
 * - Professional lifecycle management
 * - Optimized performance and memory management
 * - Clean architecture and maintainable code structure
 * 
 * @author HD Streamz TV Development Team
 * @version 9.0 - View Binding Edition
 * @since 2024
 */
public class YouTubeFragment extends Fragment {

    // ===============================
    // CONSTANTS & CONFIGURATION
    // ===============================
    
    private static final String TAG = "YouTubeFragment";
    
    // Preference Constants
    private static final String PREFS_NAME = "youtube_fragment_prefs";
    private static final String KEY_FIRST_LAUNCH = "is_first_launch";
    private static final String KEY_USER_SIGNED_IN = "user_signed_in";
    
    // URL Constants
    private static final String YOUTUBE_SIGNIN_URL = "https://accounts.google.com/signin/v2/identifier?service=youtube";
    private static final String YOUTUBE_URL = "https://www.youtube.com";
    private static final String YOUTUBE_MOBILE_URL = "https://m.youtube.com";
    
    // Configuration Constants
    private static final String USER_AGENT = "Mozilla/5.0 (Linux; Android " + Build.VERSION.RELEASE +
            "; " + Build.MODEL + ") AppleWebKit/537.36 (KHTML, like Gecko) " +
            "Chrome/120.0.0.0 Mobile Safari/537.36";
    
    // Timeout Constants
    private static final int PAGE_LOAD_TIMEOUT = 20000; // 20 seconds
    private static final int INITIALIZATION_DELAY = 1000; // 1 second
    private static final int LOAD_DELAY = 500; // 0.5 seconds
    
    // Retry Constants
    private static final int MAX_RETRY_COUNT = 3;
    
    // UI Constants
    private static final int SWIPE_REFRESH_DISTANCE = 150;

    // ===============================
    // VIEW BINDING
    // ===============================
    
    private FragmentYoutubeBinding binding;

    // ===============================
    // CORE MANAGEMENT COMPONENTS
    // ===============================
    
    // Network Management
    private ConnectivityManager connectivityManager;
    private ConnectivityManager.NetworkCallback networkCallback;
    
    // Data Management
    private SharedPreferences sharedPreferences;
    
    // Threading Management
    private Handler mainHandler;

    // ===============================
    // STATE MANAGEMENT
    // ===============================
    
    // Atomic State Variables (Thread-Safe)
    private final AtomicBoolean isPageLoaded = new AtomicBoolean(false);
    private final AtomicBoolean isNetworkAvailable = new AtomicBoolean(false);
    private final AtomicBoolean isFragmentActive = new AtomicBoolean(false);
    private final AtomicBoolean isInitialized = new AtomicBoolean(false);
    private final AtomicBoolean isLoading = new AtomicBoolean(false);
    
    // User State Variables
    private boolean isFirstLaunch = true;
    private boolean isUserSignedIn = false;
    
    // Retry Management
    private int retryCount = 0;

    // ===============================
    // FRAGMENT LIFECYCLE METHODS
    // ===============================

    /**
     * Creates and initializes the fragment view with View Binding
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, 
                           @Nullable ViewGroup container,
                           @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "Creating YouTube Fragment view with View Binding");
        
        try {
            // Initialize View Binding
            binding = FragmentYoutubeBinding.inflate(inflater, container, false);
            
            if (binding != null) {
                initializeAllComponents();
                scheduleInitialization();
                
                isInitialized.set(true);
                Log.d(TAG, "Fragment view created successfully with View Binding");
                
                return binding.getRoot();
            } else {
                Log.e(TAG, "View Binding initialization failed");
                return null;
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error creating view with View Binding", e);
            showSafeToast("ভিউ তৈরি করতে সমস্যা হয়েছে");
            return null;
        }
    }

    /**
     * Handles fragment resume lifecycle
     */
    @Override
    public void onResume() {
        super.onResume();
        isFragmentActive.set(true);
        
        try {
            resumeWebView();
            registerNetworkCallback();
            
            Log.d(TAG, "Fragment resumed successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error in onResume", e);
        }
    }

    /**
     * Handles fragment pause lifecycle
     */
    @Override
    public void onPause() {
        super.onPause();
        isFragmentActive.set(false);
        
        try {
            pauseWebView();
            Log.d(TAG, "Fragment paused successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error in onPause", e);
        }
    }

    /**
     * Handles fragment destruction and cleanup
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        isFragmentActive.set(false);
        isInitialized.set(false);
        
        try {
            performCleanup();
            
            // Clean up View Binding
            if (binding != null) {
                binding = null;
                Log.d(TAG, "View Binding cleaned up");
            }
            
            Log.d(TAG, "Fragment destroyed successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error in onDestroyView", e);
        }
    }

    // ===============================
    // INITIALIZATION METHODS
    // ===============================

    /**
     * Initializes all fragment components in proper order
     */
    private void initializeAllComponents() {
        initializeHandlers();
        initializePreferences();
        setupAllSystems();
    }

    /**
     * Initializes threading handlers
     */
    private void initializeHandlers() {
        mainHandler = new Handler(Looper.getMainLooper());
        Log.d(TAG, "Handlers initialized");
    }

    /**
     * Initializes shared preferences
     */
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

    /**
     * Sets up all system components
     */
    private void setupAllSystems() {
        setupNetworkMonitoring();
        configureWebView();
        setupUserInterface();
        setupEventHandlers();
        
        // Set initial UI state
        updateUIState(UIState.IDLE);
    }

    /**
     * Schedules delayed initialization
     */
    private void scheduleInitialization() {
        scheduleDelayedTask(() -> {
            if (isFragmentActive.get()) {
                performInitialLoad();
            }
        }, INITIALIZATION_DELAY);
    }

    // ===============================
    // WEBVIEW CONFIGURATION
    // ===============================

    /**
     * Configures WebView with optimal settings using View Binding
     */
    @SuppressLint("SetJavaScriptEnabled")
    private void configureWebView() {
        try {
            if (binding == null || binding.webview == null) {
                Log.w(TAG, "WebView is null, cannot configure");
                return;
            }
            
            WebSettings webSettings = binding.webview.getSettings();
            if (webSettings == null) {
                Log.w(TAG, "WebSettings is null, cannot configure");
                return;
            }
            
            // Configure core functionality
            configureWebViewCore(webSettings);
            
            // Configure performance settings
            configureWebViewPerformance(webSettings);
            
            // Configure security settings
            configureWebViewSecurity(webSettings);
            
            // Set up WebView clients and additional settings
            setupWebViewClients();
            setupWebViewAdditionalSettings();
            
            Log.d(TAG, "WebView configured successfully");
            
        } catch (Exception e) {
            Log.e(TAG, "Error configuring WebView", e);
        }
    }

    /**
     * Configures core WebView functionality
     */
    private void configureWebViewCore(WebSettings webSettings) {
        // JavaScript and DOM support
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setDatabaseEnabled(true);
        
        // Viewport and zoom configuration
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setUseWideViewPort(true);
        webSettings.setBuiltInZoomControls(true);
        webSettings.setDisplayZoomControls(false);
        webSettings.setSupportZoom(true);
        
        // Media playback optimization
        webSettings.setMediaPlaybackRequiresUserGesture(false);
        webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
        
        // Location and popup control
        webSettings.setGeolocationEnabled(true);
        webSettings.setJavaScriptCanOpenWindowsAutomatically(false);
    }

    /**
     * Configures WebView performance settings
     */
    private void configureWebViewPerformance(WebSettings webSettings) {
        // Caching strategy
        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
        
        // Performance improvements
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            webSettings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.TEXT_AUTOSIZING);
        }
        
        // User Agent
        webSettings.setUserAgentString(USER_AGENT);
    }

    /**
     * Configures WebView security settings
     */
    private void configureWebViewSecurity(WebSettings webSettings) {
        webSettings.setAllowFileAccess(false);
        webSettings.setAllowContentAccess(false);
        webSettings.setAllowFileAccessFromFileURLs(false);
        webSettings.setAllowUniversalAccessFromFileURLs(false);
    }

    /**
     * Sets up WebView clients
     */
    private void setupWebViewClients() {
        if (binding != null && binding.webview != null) {
            binding.webview.setWebViewClient(new YouTubeWebViewClient());
            binding.webview.setWebChromeClient(new YouTubeWebChromeClient());
        }
    }

    /**
     * Sets up additional WebView settings
     */
    private void setupWebViewAdditionalSettings() {
        if (binding == null || binding.webview == null) return;
        
        // Cookie management
        setupCookieManager();
        
        // Hardware acceleration
        binding.webview.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        
        // Focus handling
        binding.webview.setFocusable(true);
        binding.webview.setFocusableInTouchMode(true);
    }

    // ===============================
    // WEBVIEW CLIENT IMPLEMENTATION
    // ===============================

    /**
     * Custom WebViewClient for YouTube-specific handling
     */
    private class YouTubeWebViewClient extends WebViewClient {
        
        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            try {
                super.onPageStarted(view, url, favicon);
                handlePageStarted(url);
            } catch (Exception e) {
                Log.e(TAG, "Error in onPageStarted", e);
            }
        }
        
        @Override
        public void onPageFinished(WebView view, String url) {
            try {
                super.onPageFinished(view, url);
                handlePageFinished(view, url);
            } catch (Exception e) {
                Log.e(TAG, "Error in onPageFinished", e);
            }
        }
        
        @Override
        public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
            try {
                super.onReceivedError(view, request, error);
                
                if (request != null && request.isForMainFrame()) {
                    handlePageError(error);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error in onReceivedError", e);
            }
        }
        
        /**
         * Handles page start events
         */
        private void handlePageStarted(String url) {
            isPageLoaded.set(false);
            updateUIState(UIState.LOADING);
            
            Log.d(TAG, "Page loading started: " + url);
            
            // Update user state for YouTube URLs
            if (url != null && url.contains("youtube.com") && !url.contains("accounts.google.com")) {
                markFirstLaunchCompleted();
                markUserSignedIn();
            }
        }
        
        /**
         * Handles page finish events
         */
        private void handlePageFinished(WebView view, String url) {
            isPageLoaded.set(true);
            updateUIState(UIState.SUCCESS);
            retryCount = 0;
            
            // Inject custom JavaScript
            injectCustomJavaScript(view);
            
            Log.d(TAG, "Page loading finished: " + url);
        }
        
        /**
         * Handles page errors
         */
        private void handlePageError(WebResourceError error) {
            isPageLoaded.set(false);
            updateUIState(UIState.ERROR);
            handleWebViewError(error);
        }
        
        /**
         * Injects custom JavaScript for enhanced functionality
         */
        private void injectCustomJavaScript(WebView view) {
            try {
                if (view != null) {
                    String customScript = buildCustomJavaScript();
                    
                    view.evaluateJavascript(customScript, result -> {
                        Log.d(TAG, "Custom JavaScript injected successfully");
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "Error injecting JavaScript", e);
            }
        }
        
        /**
         * Builds custom JavaScript code
         */
        private String buildCustomJavaScript() {
            return "javascript:(function() {" +
                   "  try {" +
                   "    document.body.style.userSelect = 'none';" +
                   "    document.body.style.webkitUserSelect = 'none';" +
                   "    console.log('HD Streamz TV - Enhanced script injected');" +
                   "  } catch(e) { " +
                   "    console.error('Script injection failed:', e); " +
                   "  }" +
                   "})();";
        }
        
        /**
         * Handles WebView errors with appropriate responses
         */
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
    // WEBCHROME CLIENT IMPLEMENTATION
    // ===============================

    /**
     * Custom WebChromeClient for enhanced YouTube experience
     */
    private class YouTubeWebChromeClient extends WebChromeClient {
        
        @Override
        public void onProgressChanged(WebView view, int newProgress) {
            try {
                super.onProgressChanged(view, newProgress);
                updateProgressBar(newProgress);
            } catch (Exception e) {
                Log.e(TAG, "Error in onProgressChanged", e);
            }
        }
        
        @Override
        public void onReceivedTitle(WebView view, String title) {
            try {
                super.onReceivedTitle(view, title);
                handleTitleReceived(title);
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
                handlePermissionRequest(request);
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
        
        /**
         * Updates progress bar based on loading progress using View Binding
         */
        private void updateProgressBar(int progress) {
            if (binding != null && binding.progressBar != null) {
                if (progress == 100) {
                    binding.progressBar.setVisibility(View.GONE);
                } else {
                    binding.progressBar.setVisibility(View.VISIBLE);
                    binding.progressBar.setProgress(progress);
                }
            }
        }
        
        /**
         * Handles received page title
         */
        private void handleTitleReceived(String title) {
            Log.d(TAG, "Page title: " + title);
            
            if (binding != null && binding.errorTitle != null && title != null) {
                if (title.contains("Error") || title.contains("404")) {
                    binding.errorTitle.setText("পেজ পাওয়া যায়নি");
                    if (binding.errorMessage != null) {
                        binding.errorMessage.setText("অনুরোধকৃত পেজটি খুঁজে পাওয়া যায়নি");
                    }
                }
            }
        }
        
        /**
         * Handles permission requests
         */
        private void handlePermissionRequest(android.webkit.PermissionRequest request) {
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
        }
    }

    // ===============================
    // NETWORK MANAGEMENT
    // ===============================

    /**
     * Sets up comprehensive network monitoring
     */
    private void setupNetworkMonitoring() {
        try {
            Context context = getContext();
            if (context != null) {
                connectivityManager = (ConnectivityManager)
                    context.getSystemService(Context.CONNECTIVITY_SERVICE);
                
                isNetworkAvailable.set(checkNetworkAvailability());
                createNetworkCallback();
                
                Log.d(TAG, "Network monitoring configured successfully");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error setting up network monitoring", e);
        }
    }

    /**
     * Creates network callback for monitoring connectivity changes
     */
    private void createNetworkCallback() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                networkCallback = new ConnectivityManager.NetworkCallback() {
                    @Override
                    public void onAvailable(@NonNull Network network) {
                        handleNetworkAvailable();
                    }
                    
                    @Override
                    public void onLost(@NonNull Network network) {
                        handleNetworkLost();
                    }
                };
            }
        } catch (Exception e) {
            Log.e(TAG, "Error creating network callback", e);
        }
    }

    /**
     * Handles network becoming available
     */
    private void handleNetworkAvailable() {
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

    /**
     * Handles network being lost
     */
    private void handleNetworkLost() {
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

    /**
     * Registers network callback
     */
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

    /**
     * Unregisters network callback
     */
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

    /**
     * Checks current network availability
     */
    private boolean checkNetworkAvailability() {
        try {
            if (connectivityManager == null) return false;
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                return checkNetworkAvailabilityModern();
            } else {
                return checkNetworkAvailabilityLegacy();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking network availability", e);
            return false;
        }
    }

    /**
     * Checks network availability for modern Android versions
     */
    private boolean checkNetworkAvailabilityModern() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Network activeNetwork = connectivityManager.getActiveNetwork();
            if (activeNetwork == null) return false;
            
            NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(activeNetwork);
            return capabilities != null &&
                   (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET));
        }
        return false;
    }

    /**
     * Checks network availability for legacy Android versions
     */
    private boolean checkNetworkAvailabilityLegacy() {
        android.net.NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    // ===============================
    // USER INTERFACE SETUP
    // ===============================

    /**
     * Sets up all user interface components using View Binding
     */
    private void setupUserInterface() {
        setupSwipeRefresh();
        setupBackPressHandling();
    }

    /**
     * Configures swipe refresh functionality using View Binding
     */
    private void setupSwipeRefresh() {
        try {
            if (binding != null && binding.swipeRefresh != null) {
                binding.swipeRefresh.setColorSchemeResources(
                    android.R.color.holo_blue_bright,
                    android.R.color.holo_green_light,
                    android.R.color.holo_orange_light,
                    android.R.color.holo_red_light
                );
                
                binding.swipeRefresh.setDistanceToTriggerSync(SWIPE_REFRESH_DISTANCE);
                binding.swipeRefresh.setOnRefreshListener(this::handleRefresh);
                
                Log.d(TAG, "SwipeRefresh configured successfully");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error setting up SwipeRefresh", e);
        }
    }

    /**
     * Sets up back press handling for WebView navigation using View Binding
     */
    private void setupBackPressHandling() {
        try {
            setupWebViewBackPress();
            setupRootViewBackPress();
            
            Log.d(TAG, "Back press handling configured successfully");
            
        } catch (Exception e) {
            Log.e(TAG, "Error setting up back press handling", e);
        }
    }

    /**
     * Sets up WebView back press handling using View Binding
     */
    private void setupWebViewBackPress() {
        if (binding != null && binding.webview != null) {
            binding.webview.setOnKeyListener((v, keyCode, event) -> {
                if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_DOWN) {
                    return handleWebViewBackPress();
                }
                return false;
            });
            
            binding.webview.requestFocus();
        }
    }

    /**
     * Sets up root view back press handling using View Binding
     */
    private void setupRootViewBackPress() {
        if (binding != null && binding.getRoot() != null) {
            binding.getRoot().setFocusableInTouchMode(true);
            binding.getRoot().requestFocus();
            binding.getRoot().setOnKeyListener((v, keyCode, event) -> {
                if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_DOWN) {
                    return handleWebViewBackPress();
                }
                return false;
            });
        }
    }

    // ===============================
    // EVENT HANDLERS SETUP
    // ===============================

    /**
     * Sets up all event handlers using View Binding
     */
    private void setupEventHandlers() {
        setupErrorHandlers();
        setupDownloadHandler();
    }

    /**
     * Sets up error handling event listeners using View Binding
     */
    private void setupErrorHandlers() {
        try {
            if (binding != null) {
                if (binding.retryButton != null) {
                    binding.retryButton.setOnClickListener(v -> handleRetry());
                }
                
                if (binding.retryErrorButton != null) {
                    binding.retryErrorButton.setOnClickListener(v -> handleRetry());
                }
            }
            
            Log.d(TAG, "Error handlers configured successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error setting up error handlers", e);
        }
    }

    /**
     * Sets up download button handler using View Binding
     */
    private void setupDownloadHandler() {
        try {
            if (binding != null && binding.settingsButton != null) {
                binding.settingsButton.setOnClickListener(v -> handleDownloadClick());
            }
            
            Log.d(TAG, "Download handler configured successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error setting up download handler", e);
        }
    }

    // ===============================
    // EVENT HANDLING METHODS
    // ===============================

    /**
     * Handles refresh action using View Binding
     */
    private void handleRefresh() {
        try {
            if (isNetworkAvailable.get()) {
                retryCount = 0;
                reloadYouTube();
            } else {
                if (binding != null && binding.swipeRefresh != null) {
                    binding.swipeRefresh.setRefreshing(false);
                }
                showNetworkError();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error handling refresh", e);
        }
    }

    /**
     * Handles retry action
     */
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

    /**
     * Handles download button click
     */
    /**
private void handleDownloadClick() {
    try {
        String channelName = "YouTube";
        String originalUrl = binding.webview.getUrl();
        
        if (originalUrl == null || originalUrl.trim().isEmpty()) {
            Log.e(TAG, "Invalid URL: WebView URL is null or empty");
            Toast.makeText(getContext(), "No URL available", Toast.LENGTH_SHORT).show();
            return;
        }
        
        final String channelUrl = originalUrl.trim();
        Log.d(TAG, "Processing URL: " + channelUrl);
        
        YouTubeStreamLinkFetcher linkExtractor = new YouTubeStreamLinkFetcher();
        linkExtractor.extractStreamLink(channelUrl, new YouTubeStreamLinkFetcher.OnStreamLinkListener() {
            
            @Override
            public void onStreamLinkExtracted(YouTubeStreamLinkFetcher.StreamData streamData) {
                Log.d(TAG, "YouTube URL successfully resolved");
                
                new Handler(Looper.getMainLooper()).post(() -> {
                    try {
                        String streamUrl = streamData.getVideoUrl();
                        if (streamUrl == null || streamUrl.isEmpty()) {
                            Log.e(TAG, "No video stream URL available");
                            Toast.makeText(getContext(), "No video stream available", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        
                        Intent intent = new Intent(getContext(), PlayerActivity.class);
                        intent.putExtra("name", channelName);
                        intent.putExtra("link", streamUrl);
                        intent.putExtra("category", Template.YOUTUBE);
                        
                        getContext().startActivity(intent);
                        Log.d(TAG, "PlayerActivity launched with extracted URL");
                        
                    } catch (Exception e) {
                        Log.e(TAG, "Error launching PlayerActivity", e);
                        Toast.makeText(getContext(), "Failed to launch video player", Toast.LENGTH_SHORT).show();
                    }
                });
            }
            
            @Override
            public void onError(String error, Throwable throwable) {
                Log.w(TAG, "YouTube URL extraction failed: " + error, throwable);
                new Handler(Looper.getMainLooper()).post(() -> {
                    Toast.makeText(getContext(), "Extraction failed: " + error, Toast.LENGTH_SHORT).show();
                });
            }
            
            @Override
            public void onExtractionStarted() {
                Log.d(TAG, "YouTube extraction started");
            }
            
            @Override
            public void onProgress(String message) {
                Log.d(TAG, "Extraction progress: " + message);
            }
        });
        
        Log.d(TAG, "Download button clicked - extraction started");
        
    } catch (Exception e) {
        Log.e(TAG, "Error handling download click", e);
        Toast.makeText(getContext(), "Unexpected error occurred", Toast.LENGTH_SHORT).show();
    }
}

    /**
     * Handles WebView back press navigation using View Binding
     */
    private boolean handleWebViewBackPress() {
        try {
            if (binding != null && binding.webview != null && binding.webview.canGoBack()) {
                binding.webview.goBack();
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

    /**
     * Public method for handling back press from parent activity
     */
    public boolean onBackPressed() {
        return handleWebViewBackPress();
    }

    // ===============================
    // UI STATE MANAGEMENT
    // ===============================

    /**
     * UI State enumeration
     */
    private enum UIState {
        IDLE, LOADING, ERROR, NETWORK_ERROR, SUCCESS
    }

    /**
     * Updates UI state with proper error handling using View Binding
     */
    private void updateUIState(UIState state) {
        try {
            if (!isFragmentActive.get() || !isInitialized.get() || binding == null) return;
            
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
                    showSuccess();
                    break;
                case IDLE:
                default:
                    showIdle();
                    break;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating UI state", e);
        }
    }

    /**
     * Shows loading state using View Binding
     */
    private void showLoading() {
        if (binding == null) return;
        
        isLoading.set(true);
        setViewVisibility(binding.progressBar, View.VISIBLE);
        setViewVisibility(binding.loadingIndicator, View.VISIBLE);
        setViewVisibility(binding.errorContainer, View.GONE);
    }

    /**
     * Shows success state
     */
    private void showSuccess() {
        hideLoading();
        hideError();
        hideNetworkError();
    }

    /**
     * Shows idle state
     */
    private void showIdle() {
        hideLoading();
        hideError();
        hideNetworkError();
    }

    /**
     * Hides loading indicators using View Binding
     */
    private void hideLoading() {
        if (binding == null) return;
        
        isLoading.set(false);
        setViewVisibility(binding.progressBar, View.GONE);
        setViewVisibility(binding.loadingIndicator, View.GONE);
        
        if (binding.swipeRefresh != null) {
            binding.swipeRefresh.setRefreshing(false);
        }
    }

    /**
     * Shows error state using View Binding
     */
    private void showError() {
        if (binding == null) return;
        
        setViewVisibility(binding.errorContainer, View.VISIBLE);
        setViewVisibility(binding.webview, View.GONE);
    }

    /**
     * Hides error state using View Binding
     */
    private void hideError() {
        if (binding == null) return;
        
        setViewVisibility(binding.errorContainer, View.GONE);
        setViewVisibility(binding.webview, View.VISIBLE);
    }

    /**
     * Shows network error state using View Binding
     */
    private void showNetworkError() {
        if (binding == null) return;
        
        setViewVisibility(binding.networkStatusContainer, View.VISIBLE);
        if (binding.networkStatusText != null) {
            binding.networkStatusText.setText("ইন্টারনেট সংযোগ নেই");
        }
    }

    /**
     * Hides network error state using View Binding
     */
    private void hideNetworkError() {
        if (binding == null) return;
        
        setViewVisibility(binding.networkStatusContainer, View.GONE);
    }

    /**
     * Safely sets view visibility
     */
    private void setViewVisibility(View view, int visibility) {
        try {
            if (view != null) {
                view.setVisibility(visibility);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error setting view visibility", e);
        }
    }

    // ===============================
    // YOUTUBE LOADING METHODS
    // ===============================

    /**
     * Performs initial YouTube loading
     */
    private void performInitialLoad() {
        try {
            if (!canPerformLoad()) {
                Log.d(TAG, "Cannot perform load - conditions not met");
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
            }, LOAD_DELAY);
            
        } catch (Exception e) {
            Log.e(TAG, "Error performing initial load", e);
        }
    }

    /**
     * Checks if loading can be performed
     */
    private boolean canPerformLoad() {
        return isFragmentActive.get() && isInitialized.get() && binding != null;
    }

    /**
     * Loads YouTube with proper error handling and timeout using View Binding
     */
    private void loadYouTube() {
        try {
            if (!canLoadYouTube()) {
                Log.w(TAG, "Cannot load YouTube - conditions not met");
                return;
            }
            
            String urlToLoad = determineUrlToLoad();
            binding.webview.loadUrl(urlToLoad);
            isPageLoaded.set(false);
            
            Log.d(TAG, "Loading YouTube: " + urlToLoad);
            
            // Set loading timeout
            scheduleLoadingTimeout();
            
        } catch (Exception e) {
            Log.e(TAG, "Error loading YouTube", e);
            updateUIState(UIState.ERROR);
        }
    }

    /**
     * Checks if YouTube can be loaded using View Binding
     */
    private boolean canLoadYouTube() {
        return binding != null && binding.webview != null && 
               isFragmentActive.get() && isInitialized.get();
    }

    /**
     * Schedules loading timeout
     */
    private void scheduleLoadingTimeout() {
        scheduleDelayedTask(() -> {
            if (isLoading.get() && !isPageLoaded.get() && isFragmentActive.get()) {
                Log.w(TAG, "Loading timeout reached");
                updateUIState(UIState.ERROR);
            }
        }, PAGE_LOAD_TIMEOUT);
    }

    /**
     * Determines which URL to load based on user state
     */
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

    /**
     * Reloads YouTube page using View Binding
     */
    private void reloadYouTube() {
        try {
            if (binding != null && binding.webview != null && isFragmentActive.get()) {
                binding.webview.reload();
                Log.d(TAG, "Reloading YouTube");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error reloading YouTube", e);
        }
    }

    /**
     * Determines if mobile version should be used
     */
    private boolean shouldUseMobileVersion() {
        try {
            return getResources().getConfiguration().smallestScreenWidthDp < 600;
        } catch (Exception e) {
            Log.e(TAG, "Error checking mobile version", e);
            return true;
        }
    }

    // ===============================
    // PREFERENCE MANAGEMENT
    // ===============================

    /**
     * Marks first launch as completed
     */
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

    /**
     * Marks user as signed in
     */
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

    // ===============================
    // WEBVIEW LIFECYCLE MANAGEMENT
    // ===============================

    /**
     * Resumes WebView operations using View Binding
     */
    private void resumeWebView() {
        try {
            if (binding != null && binding.webview != null) {
                binding.webview.onResume();
                binding.webview.resumeTimers();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error resuming WebView", e);
        }
    }

    /**
     * Pauses WebView operations using View Binding
     */
    private void pauseWebView() {
        try {
            if (binding != null && binding.webview != null) {
                binding.webview.onPause();
                binding.webview.pauseTimers();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error pausing WebView", e);
        }
    }

    // ===============================
    // UTILITY METHODS
    // ===============================

    /**
     * Sets up cookie manager for WebView using View Binding
     */
    private void setupCookieManager() {
        try {
            CookieManager cookieManager = CookieManager.getInstance();
            if (cookieManager != null && binding != null && binding.webview != null) {
                cookieManager.setAcceptCookie(true);
                cookieManager.setAcceptThirdPartyCookies(binding.webview, true);
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    cookieManager.flush();
                }
                
                Log.d(TAG, "Cookie manager configured successfully");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error setting up cookie manager", e);
        }
    }

    /**
     * Schedules a delayed task safely
     */
    private void scheduleDelayedTask(Runnable task, long delay) {
        try {
            if (mainHandler != null && task != null) {
                mainHandler.postDelayed(task, delay);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error scheduling delayed task", e);
        }
    }

    /**
     * Shows toast message safely
     */
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

    // ===============================
    // CLEANUP METHODS
    // ===============================

    /**
     * Performs comprehensive cleanup
     */
    private void performCleanup() {
        unregisterNetworkCallback();
        cleanupWebView();
        cleanupHandlers();
    }

    /**
     * Cleans up WebView resources using View Binding
     */
    private void cleanupWebView() {
        try {
            if (binding != null && binding.webview != null) {
                binding.webview.pauseTimers();
                binding.webview.clearHistory();
                binding.webview.clearCache(true);
                binding.webview.clearFormData();
                binding.webview.loadUrl("about:blank");
                binding.webview.destroy();
                Log.d(TAG, "WebView cleaned up successfully");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error cleaning up WebView", e);
        }
    }

    /**
     * Cleans up handlers and removes callbacks
     */
    private void cleanupHandlers() {
        try {
            if (mainHandler != null) {
                mainHandler.removeCallbacksAndMessages(null);
                mainHandler = null;
            }
            
            Log.d(TAG, "Handlers cleaned up successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error cleaning up handlers", e);
        }
    }
}
