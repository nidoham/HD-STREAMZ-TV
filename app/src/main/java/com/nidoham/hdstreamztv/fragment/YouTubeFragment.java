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
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
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

/**
 * Professional YouTube Fragment - Crash-Safe YouTube Viewer
 * 
 * Enhanced Features:
 * - Comprehensive crash prevention with null checks
 * - Safe initialization and cleanup
 * - Robust error handling
 * - First-time YouTube sign-in page detection
 * - Smart back navigation within WebView
 * - Advanced session management
 * - Professional UI state management
 * 
 * @author HD Streamz TV Development Team
 * @version 4.2 - Crash-Safe Edition
 */
public class YouTubeFragment extends Fragment {
    
    // ===============================
    // Constants
    // ===============================
    private static final String TAG = "YouTubeFragment";
    private static final String PREFS_NAME = "youtube_fragment_prefs";
    private static final String KEY_FIRST_LAUNCH = "is_first_launch";
    private static final String KEY_USER_SIGNED_IN = "user_signed_in";
    
    // YouTube URLs
    private static final String YOUTUBE_SIGNIN_URL = "https://accounts.google.com/signin/v2/identifier?service=youtube";
    private static final String YOUTUBE_URL = "https://www.youtube.com";
    private static final String YOUTUBE_MOBILE_URL = "https://m.youtube.com";
    
    // User Agent
    private static final String USER_AGENT = "Mozilla/5.0 (Linux; Android " + Build.VERSION.RELEASE +
            "; " + Build.MODEL + ") AppleWebKit/537.36 (KHTML, like Gecko) " +
            "Chrome/120.0.0.0 Mobile Safari/537.36";
    
    // Timeouts and delays
    private static final int NETWORK_TIMEOUT = 10000; // 10 seconds
    
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
    // Network and State Management
    // ===============================
    private ConnectivityManager connectivityManager;
    private ConnectivityManager.NetworkCallback networkCallback;
    private SharedPreferences sharedPreferences;
    private Handler mainHandler;
    
    // ===============================
    // State Variables
    // ===============================
    private boolean isPageLoaded = false;
    private boolean isNetworkAvailable = false;
    private boolean isFragmentActive = false;
    private boolean isFirstLaunch = true;
    private boolean isUserSignedIn = false;
    private boolean isLoading = false;
    private boolean isInitialized = false;
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
                
                // Delay initial load to ensure everything is set up
                if (mainHandler != null) {
                    mainHandler.postDelayed(this::performInitialLoad, 1000);
                }
                
                isInitialized = true;
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
        isFragmentActive = true;
        
        try {
            if (webView != null) {
                webView.onResume();
                webView.resumeTimers();
            }
            
            registerNetworkCallback();
            Log.d(TAG, "Fragment resumed");
        } catch (Exception e) {
            Log.e(TAG, "Error in onResume", e);
        }
    }
    
    @Override
    public void onPause() {
        super.onPause();
        isFragmentActive = false;
        
        try {
            if (webView != null) {
                webView.onPause();
                webView.pauseTimers();
            }
            
            Log.d(TAG, "Fragment paused");
        } catch (Exception e) {
            Log.e(TAG, "Error in onPause", e);
        }
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        isFragmentActive = false;
        isInitialized = false;
        
        try {
            unregisterNetworkCallback();
            cleanupWebView();
            
            if (mainHandler != null) {
                mainHandler.removeCallbacksAndMessages(null);
                mainHandler = null;
            }
            
            Log.d(TAG, "Fragment destroyed");
        } catch (Exception e) {
            Log.e(TAG, "Error in onDestroy", e);
        }
    }
    
    // ===============================
    // Safe Initialization Methods
    // ===============================
    
    /**
     * Initialize UI components with null checks
     */
    private void initializeComponents(View view) {
        try {
            if (view == null) {
                Log.e(TAG, "View is null in initializeComponents");
                return;
            }
            
            // Initialize main handler first
            mainHandler = new Handler(Looper.getMainLooper());
            
            // Find UI components with null checks
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
            
            // Create missing components if not in XML
            if (progressBar == null && coordinatorLayout != null) {
                createProgressBar();
            }
            
            if (swipeRefreshLayout == null && coordinatorLayout != null && webView != null) {
                createSwipeRefreshLayout();
            }
            
            updateUIState(UIState.IDLE);
            
            Log.d(TAG, "Components initialized successfully");
            
        } catch (Exception e) {
            Log.e(TAG, "Error initializing components", e);
        }
    }
    
    /**
     * Initialize SharedPreferences safely
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
     * Create progress bar safely
     */
    private void createProgressBar() {
        try {
            Context context = getContext();
            if (context != null && coordinatorLayout != null) {
                progressBar = new ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal);
                CoordinatorLayout.LayoutParams params = new CoordinatorLayout.LayoutParams(
                    CoordinatorLayout.LayoutParams.MATCH_PARENT, 8);
                params.gravity = android.view.Gravity.TOP;
                
                progressBar.setLayoutParams(params);
                progressBar.setVisibility(View.GONE);
                progressBar.setMax(100);
                
                coordinatorLayout.addView(progressBar);
                Log.d(TAG, "Progress bar created programmatically");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error creating progress bar", e);
        }
    }
    
    /**
     * Create SwipeRefreshLayout safely
     */
    private void createSwipeRefreshLayout() {
        try {
            Context context = getContext();
            if (context != null && coordinatorLayout != null && webView != null) {
                swipeRefreshLayout = new SwipeRefreshLayout(context);
                CoordinatorLayout.LayoutParams params = new CoordinatorLayout.LayoutParams(
                    CoordinatorLayout.LayoutParams.MATCH_PARENT,
                    CoordinatorLayout.LayoutParams.MATCH_PARENT);
                
                swipeRefreshLayout.setLayoutParams(params);
                
                // Move WebView to SwipeRefreshLayout
                coordinatorLayout.removeView(webView);
                swipeRefreshLayout.addView(webView);
                coordinatorLayout.addView(swipeRefreshLayout);
                
                Log.d(TAG, "SwipeRefreshLayout created programmatically");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error creating SwipeRefreshLayout", e);
        }
    }
    
    // ===============================
    // WebView Configuration
    // ===============================
    
    /**
     * Configure WebView safely
     */
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
            
            // Modern caching strategy
            webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
            
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
            
            // Modern User Agent
            webSettings.setUserAgentString(USER_AGENT);
            
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
            
            Log.d(TAG, "WebView configured successfully");
            
        } catch (Exception e) {
            Log.e(TAG, "Error configuring WebView", e);
        }
    }
    
    /**
     * Setup cookie manager safely
     */
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
    
    // ===============================
    // Back Press Handling
    // ===============================
    
    /**
     * Setup back press handling safely
     */
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
    
    /**
     * Handle back press safely
     */
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
            return true; // Still consume to prevent app closing
        }
    }
    
    /**
     * Public method for Activity integration
     */
    public boolean onBackPressed() {
        return handleWebViewBackPress();
    }
    
    // ===============================
    // Network Monitoring
    // ===============================
    
    /**
     * Setup network monitoring safely
     */
    private void setupNetworkMonitoring() {
        try {
            Context context = getContext();
            if (context != null) {
                connectivityManager = (ConnectivityManager) 
                    context.getSystemService(Context.CONNECTIVITY_SERVICE);
                
                isNetworkAvailable = checkNetworkAvailability();
                createNetworkCallback();
                
                Log.d(TAG, "Network monitoring configured");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error setting up network monitoring", e);
        }
    }
    
    /**
     * Create network callback safely
     */
    private void createNetworkCallback() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                networkCallback = new ConnectivityManager.NetworkCallback() {
                    @Override
                    public void onAvailable(@NonNull Network network) {
                        isNetworkAvailable = true;
                        if (isFragmentActive && getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                try {
                                    hideNetworkError();
                                    if (!isPageLoaded && !isLoading && isInitialized) {
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
                        isNetworkAvailable = false;
                        if (isFragmentActive && getActivity() != null) {
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
    
    /**
     * Register network callback safely
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
     * Unregister network callback safely
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
     * Check network availability safely
     */
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
    
    // ===============================
    // SwipeRefresh and UI Control
    // ===============================
    
    /**
     * Configure SwipeRefreshLayout safely
     */
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
    
    /**
     * Setup error handling safely
     */
    private void setupErrorHandling() {
        try {
            if (retryButton != null) {
                retryButton.setOnClickListener(v -> handleRetry());
            }
            
            if (retryErrorButton != null) {
                retryErrorButton.setOnClickListener(v -> handleRetry());
            }
            
            if (settingsButton != null) {
                settingsButton.setOnClickListener(v -> {
                    showSafeToast("সেটিংস শীঘ্রই আসছে");
                });
            }
            
            Log.d(TAG, "Error handling configured");
        } catch (Exception e) {
            Log.e(TAG, "Error setting up error handling", e);
        }
    }
    
    /**
     * Handle refresh safely
     */
    private void handleRefresh() {
        try {
            if (isNetworkAvailable) {
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
    
    /**
     * Handle retry safely
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
    
    // ===============================
    // UI State Management
    // ===============================
    
    private enum UIState {
        IDLE, LOADING, ERROR, NETWORK_ERROR, SUCCESS
    }
    
    /**
     * Update UI state safely
     */
    private void updateUIState(UIState state) {
        try {
            if (!isFragmentActive || !isInitialized) return;
            
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
            isLoading = true;
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
            isLoading = false;
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
    
    // ===============================
    // YouTube Loading Methods
    // ===============================
    
    /**
     * Perform initial loading safely
     */
    private void performInitialLoad() {
        try {
            if (!isFragmentActive || !isInitialized) {
                Log.d(TAG, "Fragment not active or initialized, skipping load");
                return;
            }
            
            if (!isNetworkAvailable) {
                updateUIState(UIState.NETWORK_ERROR);
                return;
            }
            
            updateUIState(UIState.LOADING);
            
            if (mainHandler != null) {
                mainHandler.postDelayed(() -> {
                    if (isFragmentActive && isInitialized) {
                        loadYouTube();
                    }
                }, 500);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error performing initial load", e);
        }
    }
    
    /**
     * Load YouTube safely
     */
    private void loadYouTube() {
        try {
            if (webView == null || !isFragmentActive || !isInitialized) {
                Log.w(TAG, "Cannot load YouTube - WebView null or fragment inactive");
                return;
            }
            
            String urlToLoad = determineUrlToLoad();
            webView.loadUrl(urlToLoad);
            isPageLoaded = false;
            
            Log.d(TAG, "Loading YouTube: " + urlToLoad);
            
            // Set timeout for loading
            if (mainHandler != null) {
                mainHandler.postDelayed(() -> {
                    if (isLoading && !isPageLoaded && isFragmentActive) {
                        Log.w(TAG, "Loading timeout reached");
                        updateUIState(UIState.ERROR);
                    }
                }, NETWORK_TIMEOUT);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading YouTube", e);
            updateUIState(UIState.ERROR);
        }
    }
    
    /**
     * Determine URL to load safely
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
            return YOUTUBE_URL; // Fallback
        }
    }
    
    /**
     * Reload YouTube safely
     */
    private void reloadYouTube() {
        try {
            if (webView != null && isFragmentActive) {
                webView.reload();
                Log.d(TAG, "Reloading YouTube");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error reloading YouTube", e);
        }
    }
    
    /**
     * Check if mobile version should be used
     */
    private boolean shouldUseMobileVersion() {
        try {
            return getResources().getConfiguration().smallestScreenWidthDp < 600;
        } catch (Exception e) {
            Log.e(TAG, "Error checking mobile version", e);
            return true; // Default to mobile
        }
    }
    
    /**
     * Mark first launch completed safely
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
     * Mark user signed in safely
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
    // Utility Methods
    // ===============================
    
    /**
     * Show toast safely
     */
    private void showSafeToast(String message) {
        try {
            Context context = getContext();
            if (context != null && isFragmentActive && message != null) {
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error showing toast", e);
        }
    }
    
    /**
     * Clean up WebView safely
     */
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
    // Custom WebViewClient
    // ===============================
    
    private class YouTubeWebViewClient extends WebViewClient {
        
        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            try {
                super.onPageStarted(view, url, favicon);
                updateUIState(UIState.LOADING);
                isPageLoaded = false;
                Log.d(TAG, "Page loading started: " + url);
                
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
                updateUIState(UIState.SUCCESS);
                isPageLoaded = true;
                retryCount = 0;
                
                injectCustomJavaScript(view);
                Log.d(TAG, "Page loading finished: " + url);
                
                if (settingsButton != null) {
                    settingsButton.setVisibility(View.VISIBLE);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error in onPageFinished", e);
            }
        }
        
        @Override
        public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
            try {
                super.onReceivedError(view, request, error);
                
                if (request != null && request.isForMainFrame()) {
                    updateUIState(UIState.ERROR);
                    isPageLoaded = false;
                    handleWebViewError(error);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error in onReceivedError", e);
            }
        }
        
        @Override
        public void onReceivedHttpError(WebView view, WebResourceRequest request,
                                      WebResourceResponse errorResponse) {
            try {
                super.onReceivedHttpError(view, request, errorResponse);
                
                if (request != null && request.isForMainFrame() && errorResponse != null) {
                    updateUIState(UIState.ERROR);
                    int statusCode = errorResponse.getStatusCode();
                    
                    if (statusCode >= 500) {
                        showSafeToast("YouTube সার্ভারে সমস্যা হয়েছে। কিছুক্ষণ পর চেষ্টা করুন");
                    } else if (statusCode >= 400) {
                        showSafeToast("পেজ লোড করতে সমস্যা হয়েছে। আবার চেষ্টা করুন");
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error in onReceivedHttpError", e);
            }
        }
        
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            try {
                if (request != null && request.getUrl() != null) {
                    String url = request.getUrl().toString();
                    
                    if (url.contains("youtube.com") || url.contains("youtu.be") ||
                        url.contains("googlevideo.com") || url.contains("gstatic.com") ||
                        url.contains("google.com") || url.contains("accounts.google.com")) {
                        return false;
                    }
                }
                return true;
            } catch (Exception e) {
                Log.e(TAG, "Error in shouldOverrideUrlLoading", e);
                return true;
            }
        }
        
        @Override
        public void onReceivedSslError(WebView view, android.webkit.SslErrorHandler handler,
                                     android.net.http.SslError error) {
            try {
                handler.cancel();
                showSafeToast("নিরাপত্তা সমস্যার কারণে সংযোগ ব্যর্থ হয়েছে");
                updateUIState(UIState.ERROR);
            } catch (Exception e) {
                Log.e(TAG, "Error in onReceivedSslError", e);
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
        
        private void injectCustomJavaScript(WebView view) {
            try {
                if (view != null) {
                    String customScript =
                        "javascript:(function() {" +
                        "  try {" +
                        "    document.body.style.userSelect = 'none';" +
                        "    document.body.style.webkitUserSelect = 'none';" +
                        "    console.log('HD Streamz TV - Custom script injected');" +
                        "  } catch(e) { console.error('Script injection failed:', e); }" +
                        "})();";
                    
                    view.evaluateJavascript(customScript, result -> {
                        Log.d(TAG, "Custom JavaScript injected");
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "Error injecting JavaScript", e);
            }
        }
    }
    
    // ===============================
    // Custom WebChromeClient
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
