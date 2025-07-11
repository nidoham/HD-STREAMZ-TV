package com.nidoham.hdstreamztv.fragment;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
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
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.nidoham.hdstreamztv.PlayerActivity;
import com.nidoham.hdstreamztv.databinding.FragmentYoutubeBinding;
import com.nidoham.hdstreamztv.template.model.settings.Template;

import org.schabi.newpipe.extractor.ServiceList;
import org.schabi.newpipe.extractor.stream.StreamInfo;
import org.schabi.newpipe.extractor.stream.VideoStream;
import org.schabi.newpipe.util.ExtractorHelper;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * Professional YouTube Fragment with Enhanced Quality Selection
 * 
 * Features:
 * - WebView-based YouTube integration with proper lifecycle management
 * - Network connectivity monitoring and handling
 * - Stream extraction with quality selection dialog
 * - Automatic and manual quality selection options
 * - Comprehensive error handling and recovery
 * - Memory-optimized resource management
 * - Thread-safe operations with RxJava
 * 
 * @author Professional Enhanced Version
 * @version 3.0
 */
public class YouTubeFragment extends Fragment {
    
    private static final String TAG = "YouTubeFragment";
    private static final String PREFS_NAME = "youtube_fragment_prefs";
    private static final String KEY_FIRST_LAUNCH = "is_first_launch";
    private static final String KEY_PREFERRED_QUALITY = "preferred_quality";
    private static final String KEY_AUTO_SELECT_QUALITY = "auto_select_quality";
    
    // YouTube URLs
    private static final String YOUTUBE_SIGNIN_URL = "https://accounts.google.com/signin/v2/identifier?service=youtube";
    private static final String YOUTUBE_URL = "https://www.youtube.com";
    private static final String YOUTUBE_MOBILE_URL = "https://m.youtube.com";
    private static final String USER_AGENT = "Mozilla/5.0 (Linux; Android " + Build.VERSION.RELEASE +
            "; " + Build.MODEL + ") AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36";
    
    // Core Components
    private FragmentYoutubeBinding binding;
    private ConnectivityManager connectivityManager;
    private ConnectivityManager.NetworkCallback networkCallback;
    private SharedPreferences sharedPreferences;
    private Handler mainHandler;
    
    // RxJava Management
    private final CompositeDisposable compositeDisposable = new CompositeDisposable();
    
    // State Management
    private final FragmentStateManager stateManager = new FragmentStateManager();
    private final QualitySelectionManager qualityManager = new QualitySelectionManager();
    
    // ========================================================================================
    // State Management Classes
    // ========================================================================================
    
    /**
     * Manages fragment state with thread safety
     */
    private static class FragmentStateManager {
        private volatile boolean isFirstLaunch = true;
        private volatile boolean isNetworkAvailable = false;
        private volatile boolean isPageLoaded = false;
        private volatile boolean isFragmentActive = false;
        
        public synchronized boolean isFirstLaunch() { return isFirstLaunch; }
        public synchronized void setFirstLaunch(boolean firstLaunch) { this.isFirstLaunch = firstLaunch; }
        
        public synchronized boolean isNetworkAvailable() { return isNetworkAvailable; }
        public synchronized void setNetworkAvailable(boolean available) { this.isNetworkAvailable = available; }
        
        public synchronized boolean isPageLoaded() { return isPageLoaded; }
        public synchronized void setPageLoaded(boolean loaded) { this.isPageLoaded = loaded; }
        
        public synchronized boolean isFragmentActive() { return isFragmentActive; }
        public synchronized void setFragmentActive(boolean active) { this.isFragmentActive = active; }
        
        public synchronized void reset() {
            isFirstLaunch = true;
            isNetworkAvailable = false;
            isPageLoaded = false;
            isFragmentActive = false;
        }
    }
    
    /**
     * Manages quality selection preferences and operations
     */
    private class QualitySelectionManager {
        
        public String getPreferredQuality() {
            if (sharedPreferences != null) {
                return sharedPreferences.getString(KEY_PREFERRED_QUALITY, "720p");
            }
            return "720p";
        }
        
        public void setPreferredQuality(String quality) {
            if (sharedPreferences != null) {
                sharedPreferences.edit().putString(KEY_PREFERRED_QUALITY, quality).apply();
            }
        }
        
        public boolean isAutoSelectEnabled() {
            if (sharedPreferences != null) {
                return sharedPreferences.getBoolean(KEY_AUTO_SELECT_QUALITY, false);
            }
            return false;
        }
        
        public void setAutoSelectEnabled(boolean enabled) {
            if (sharedPreferences != null) {
                sharedPreferences.edit().putBoolean(KEY_AUTO_SELECT_QUALITY, enabled).apply();
            }
        }
        
        public VideoStream selectBestQuality(List<VideoStream> videoStreams) {
            if (videoStreams == null || videoStreams.isEmpty()) {
                return null;
            }
            
            String preferredQuality = getPreferredQuality();
            
            // First, try to find exact match
            for (VideoStream stream : videoStreams) {
                if (preferredQuality.equals(stream.getResolution())) {
                    Log.d(TAG, "Found exact quality match: " + preferredQuality);
                    return stream;
                }
            }
            
            // If no exact match, find closest quality
            return findClosestQuality(preferredQuality, videoStreams);
        }
        
        private VideoStream findClosestQuality(String preferredQuality, List<VideoStream> videoStreams) {
            int preferredHeight = parseResolutionHeight(preferredQuality);
            
            VideoStream bestMatch = null;
            int smallestDifference = Integer.MAX_VALUE;
            
            for (VideoStream stream : videoStreams) {
                int streamHeight = parseResolutionHeight(stream.getResolution());
                int difference = Math.abs(streamHeight - preferredHeight);
                
                if (difference < smallestDifference) {
                    smallestDifference = difference;
                    bestMatch = stream;
                }
            }
            
            if (bestMatch != null) {
                Log.d(TAG, "Found closest quality match: " + bestMatch.getResolution() + 
                      " (preferred: " + preferredQuality + ")");
            }
            
            return bestMatch;
        }
        
        private int parseResolutionHeight(String resolution) {
            if (resolution == null || resolution.trim().isEmpty()) {
                return 0;
            }
            
            try {
                String numbers = resolution.replaceAll("[^0-9]", "");
                if (!numbers.isEmpty()) {
                    return Integer.parseInt(numbers);
                }
            } catch (NumberFormatException e) {
                Log.w(TAG, "Failed to parse resolution: " + resolution);
            }
            
            return 0;
        }
    }
    
    /**
     * Quality option data class
     */
    private static class QualityOption {
        private final String resolution;
        private final String displayText;
        private final VideoStream videoStream;
        
        public QualityOption(String resolution, String displayText, VideoStream videoStream) {
            this.resolution = resolution;
            this.displayText = displayText;
            this.videoStream = videoStream;
        }
        
        public String getResolution() { return resolution; }
        public String getDisplayText() { return displayText; }
        public VideoStream getVideoStream() { return videoStream; }
        
        @Override
        public String toString() {
            return displayText;
        }
    }
    
    // ========================================================================================
    // Fragment Lifecycle
    // ========================================================================================
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView: Creating YouTube fragment view");
        
        binding = FragmentYoutubeBinding.inflate(inflater, container, false);
        initializeComponents();
        return binding.getRoot();
    }
    
    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume: Fragment resuming");
        
        stateManager.setFragmentActive(true);
        resumeWebView();
        registerNetworkCallback();
    }
    
    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "onPause: Fragment pausing");
        
        stateManager.setFragmentActive(false);
        pauseWebView();
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.d(TAG, "onDestroyView: Cleaning up fragment resources");
        
        stateManager.setFragmentActive(false);
        cleanup();
        binding = null;
    }
    
    // ========================================================================================
    // Initialization Methods
    // ========================================================================================
    
    private void initializeComponents() {
        Log.d(TAG, "Initializing fragment components");
        
        try {
            mainHandler = new Handler(Looper.getMainLooper());
            initializePreferences();
            setupNetworkMonitoring();
            configureWebView();
            setupUI();
            loadYouTube();
            
            Log.d(TAG, "Fragment components initialized successfully");
            
        } catch (Exception e) {
            Log.e(TAG, "Error initializing fragment components", e);
            showToast("Failed to initialize YouTube fragment: " + e.getMessage());
        }
    }
    
    private void initializePreferences() {
        Context context = getContext();
        if (context != null) {
            sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            stateManager.setFirstLaunch(sharedPreferences.getBoolean(KEY_FIRST_LAUNCH, true));
            
            Log.d(TAG, "Preferences initialized - First launch: " + stateManager.isFirstLaunch());
        }
    }
    
    private void setupNetworkMonitoring() {
        Context context = getContext();
        if (context != null) {
            connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            stateManager.setNetworkAvailable(checkNetworkAvailability());
            createNetworkCallback();
            
            Log.d(TAG, "Network monitoring setup - Available: " + stateManager.isNetworkAvailable());
        }
    }
    
    @SuppressLint("SetJavaScriptEnabled")
    private void configureWebView() {
        if (binding.webview == null) {
            Log.w(TAG, "WebView is null, cannot configure");
            return;
        }
        
        try {
            WebSettings settings = binding.webview.getSettings();
            settings.setJavaScriptEnabled(true);
            settings.setDomStorageEnabled(true);
            settings.setLoadWithOverviewMode(true);
            settings.setUseWideViewPort(true);
            settings.setBuiltInZoomControls(true);
            settings.setDisplayZoomControls(false);
            settings.setMediaPlaybackRequiresUserGesture(false);
            settings.setUserAgentString(USER_AGENT);
            settings.setCacheMode(WebSettings.LOAD_DEFAULT);
            
            // Security settings
            settings.setAllowFileAccess(false);
            settings.setAllowContentAccess(false);
            
            binding.webview.setWebViewClient(new YouTubeWebViewClient());
            binding.webview.setWebChromeClient(new YouTubeWebChromeClient());
            
            setupCookieManager();
            binding.webview.setLayerType(View.LAYER_TYPE_HARDWARE, null);
            
            Log.d(TAG, "WebView configured successfully");
            
        } catch (Exception e) {
            Log.e(TAG, "Error configuring WebView", e);
        }
    }
    
    private void setupUI() {
        try {
            setupSwipeRefresh();
            setupBackPressHandling();
            setupButtonHandlers();
            
            Log.d(TAG, "UI setup completed");
            
        } catch (Exception e) {
            Log.e(TAG, "Error setting up UI", e);
        }
    }
    
    private void setupSwipeRefresh() {
        if (binding.swipeRefresh != null) {
            binding.swipeRefresh.setColorSchemeResources(
                android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light
            );
            binding.swipeRefresh.setOnRefreshListener(this::handleRefresh);
        }
    }
    
    private void setupBackPressHandling() {
        if (binding.webview != null) {
            binding.webview.setOnKeyListener((v, keyCode, event) -> {
                if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_DOWN) {
                    return handleBackPress();
                }
                return false;
            });
        }
    }
    
    private void setupButtonHandlers() {
        if (binding.retryButton != null) {
            binding.retryButton.setOnClickListener(v -> {
                Log.d(TAG, "Retry button clicked");
                loadYouTube();
            });
        }
        
        if (binding.settingsButton != null) {
            binding.settingsButton.setOnClickListener(v -> {
                Log.d(TAG, "Settings button clicked");
                handleStreamExtraction();
            });
        }
    }
    
    // ========================================================================================
    // WebView Management
    // ========================================================================================
    
    private void loadYouTube() {
        if (binding.webview == null) {
            Log.w(TAG, "Cannot load YouTube - WebView is null");
            return;
        }
        
        if (!stateManager.isNetworkAvailable()) {
            showNetworkError();
            return;
        }
        
        showLoading();
        String url = determineYouTubeUrl();
        
        Log.d(TAG, "Loading YouTube URL: " + url);
        
        // Ensure WebView operations are on main thread
        mainHandler.post(() -> {
            if (binding != null && binding.webview != null) {
                binding.webview.loadUrl(url);
            }
        });
    }
    
    private String determineYouTubeUrl() {
        if (stateManager.isFirstLaunch()) {
            return YOUTUBE_SIGNIN_URL;
        } else {
            return shouldUseMobileVersion() ? YOUTUBE_MOBILE_URL : YOUTUBE_URL;
        }
    }
    
    private boolean shouldUseMobileVersion() {
        return getResources().getConfiguration().smallestScreenWidthDp < 600;
    }
    
    private class YouTubeWebViewClient extends WebViewClient {
        
        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            Log.d(TAG, "Page started loading: " + url);
            
            stateManager.setPageLoaded(false);
            showLoading();
            
            if (url != null && url.contains("youtube.com") && !url.contains("accounts.google.com")) {
                markFirstLaunchCompleted();
            }
        }
        
        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            Log.d(TAG, "Page finished loading: " + url);
            
            stateManager.setPageLoaded(true);
            hideLoading();
        }
        
        @Override
        public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
            super.onReceivedError(view, request, error);
            
            if (request != null && request.isForMainFrame()) {
                Log.e(TAG, "WebView error for main frame: " + error.getDescription());
                showError();
            }
        }
    }
    
    private class YouTubeWebChromeClient extends WebChromeClient {
        
        @Override
        public void onProgressChanged(WebView view, int newProgress) {
            super.onProgressChanged(view, newProgress);
            
            if (binding != null && binding.progressBar != null) {
                if (newProgress == 100) {
                    binding.progressBar.setVisibility(View.GONE);
                } else {
                    binding.progressBar.setVisibility(View.VISIBLE);
                    binding.progressBar.setProgress(newProgress);
                }
            }
        }
        
        @Override
        public void onPermissionRequest(android.webkit.PermissionRequest request) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && request != null) {
                String[] permissions = request.getResources();
                if (permissions != null) {
                    for (String permission : permissions) {
                        if (permission.equals(android.webkit.PermissionRequest.RESOURCE_VIDEO_CAPTURE) ||
                            permission.equals(android.webkit.PermissionRequest.RESOURCE_AUDIO_CAPTURE)) {
                            request.grant(new String[]{permission});
                            return;
                        }
                    }
                }
                request.deny();
            }
        }
    }
    
    // ========================================================================================
    // Network Management
    // ========================================================================================
    
    private void createNetworkCallback() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            networkCallback = new ConnectivityManager.NetworkCallback() {
                @Override
                public void onAvailable(@NonNull Network network) {
                    Log.d(TAG, "Network became available");
                    stateManager.setNetworkAvailable(true);
                    
                    if (stateManager.isFragmentActive() && getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            hideNetworkError();
                            if (!stateManager.isPageLoaded()) {
                                loadYouTube();
                            }
                        });
                    }
                }
                
                @Override
                public void onLost(@NonNull Network network) {
                    Log.d(TAG, "Network lost");
                    stateManager.setNetworkAvailable(false);
                    
                    // Uncomment if you want to show network error immediately
                    // if (stateManager.isFragmentActive() && getActivity() != null) {
                    //     getActivity().runOnUiThread(this::showNetworkError);
                    // }
                }
            };
        }
    }
    
    private boolean checkNetworkAvailability() {
        if (connectivityManager == null) return false;
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Network activeNetwork = connectivityManager.getActiveNetwork();
            if (activeNetwork == null) return false;
            
            NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(activeNetwork);
            return capabilities != null && (
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
            );
        } else {
            android.net.NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        }
    }
    
    private void registerNetworkCallback() {
        if (connectivityManager != null && networkCallback != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            NetworkRequest.Builder builder = new NetworkRequest.Builder();
            connectivityManager.registerNetworkCallback(builder.build(), networkCallback);
            Log.d(TAG, "Network callback registered");
        }
    }
    
    private void unregisterNetworkCallback() {
        if (connectivityManager != null && networkCallback != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                connectivityManager.unregisterNetworkCallback(networkCallback);
                Log.d(TAG, "Network callback unregistered");
            } catch (Exception e) {
                Log.w(TAG, "Error unregistering network callback", e);
            }
        }
    }
    
    // ========================================================================================
    // Event Handlers
    // ========================================================================================
    
    private void handleRefresh() {
        Log.d(TAG, "Refresh requested");
        
        if (stateManager.isNetworkAvailable()) {
            loadYouTube();
        } else {
            if (binding != null && binding.swipeRefresh != null) {
                binding.swipeRefresh.setRefreshing(false);
            }
            showNetworkError();
        }
    }
    
    private boolean handleBackPress() {
        if (binding != null && binding.webview != null && binding.webview.canGoBack()) {
            binding.webview.goBack();
            return true;
        }
        return false;
    }
    
    public boolean onBackPressed() {
        return handleBackPress();
    }
    
    // ========================================================================================
    // Stream Extraction and Quality Selection
    // ========================================================================================
    
    private void handleStreamExtraction() {
        if (!stateManager.isFragmentActive() || getContext() == null || binding == null || binding.webview == null) {
            Log.w(TAG, "Fragment not active or context/binding is null");
            return;
        }
        
        String url = binding.webview.getUrl();
        
        if (url == null || url.trim().isEmpty()) {
            showToast("Please navigate to a YouTube video");
            return;
        }
        
        Log.d(TAG, "Starting stream extraction for URL: " + url);
        
        // Show loading state
        showLoading();
        
        int serviceId = ServiceList.YouTube.getServiceId();
        
        Disposable disposable = ExtractorHelper.getStreamInfo(serviceId, url, true)
            .subscribeOn(Schedulers.io()) // Background thread for network/extraction
            .observeOn(AndroidSchedulers.mainThread()) // Main thread for UI updates
            .subscribe(
                streamInfo -> {
                    hideLoading();
                    
                    if (!stateManager.isFragmentActive() || getContext() == null) {
                        Log.w(TAG, "Fragment no longer active, ignoring stream info result");
                        return;
                    }
                    
                    if (streamInfo == null) {
                        showToast("Stream info is null. This video might not be supported.");
                        return;
                    }
                    
                    if (streamInfo.getVideoStreams() == null || streamInfo.getVideoStreams().isEmpty()) {
                        showToast("No video streams found. This video might not be supported.");
                        return;
                    }
                    
                    try {
                        handleStreamInfoReceived(streamInfo);
                        
                    } catch (Exception e) {
                        Log.e(TAG, "Error processing stream data", e);
                        showToast("Error: Unable to process video. " + e.getLocalizedMessage());
                    }
                },
                throwable -> {
                    hideLoading();
                    Log.e(TAG, "Failed to extract stream info", throwable);
                    
                    if (stateManager.isFragmentActive() && getContext() != null) {
                        String errorMessage = "Error: Unable to extract video info. ";
                        if (throwable.getMessage() != null) {
                            errorMessage += throwable.getMessage();
                        } else {
                            errorMessage += "Please ensure your app is updated or try a different video.";
                        }
                        showToast(errorMessage);
                    }
                }
            );
        
        // Add to composite disposable for proper cleanup
        compositeDisposable.add(disposable);
    }
    
    private void handleStreamInfoReceived(StreamInfo streamInfo) {
        Context context = getContext();
        if (context == null) return;
        
        // Check if auto-select is enabled
        if (qualityManager.isAutoSelectEnabled()) {
            // Auto-select best quality
            VideoStream selectedStream = qualityManager.selectBestQuality(streamInfo.getVideoStreams());
            
            if (selectedStream != null) {
                launchPlayerWithStream(streamInfo, selectedStream);
            } else {
                showQualitySelectionDialog(streamInfo);
            }
        } else {
            // Show quality selection dialog
            showQualitySelectionDialog(streamInfo);
        }
    }
    
    private void showQualitySelectionDialog(StreamInfo streamInfo) {
        Context context = getContext();
        if (context == null) return;
        
        List<VideoStream> videoStreams = streamInfo.getVideoStreams();
        if (videoStreams == null || videoStreams.isEmpty()) {
            showToast("No video qualities available");
            return;
        }
        
        // Create quality options
        List<QualityOption> qualityOptions = createQualityOptions(videoStreams);
        
        if (qualityOptions.isEmpty()) {
            showToast("No valid video qualities found");
            return;
        }
        
        Log.d(TAG, "Showing quality selection dialog with " + qualityOptions.size() + " options");
        
        // Create dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Select Video Quality");
        
        // Create adapter for quality list
        QualityAdapter adapter = new QualityAdapter(context, qualityOptions);
        
        builder.setAdapter(adapter, (dialog, which) -> {
            QualityOption selectedOption = qualityOptions.get(which);
            
            // Save user preference
            qualityManager.setPreferredQuality(selectedOption.getResolution());
            
            launchPlayerWithQuality(streamInfo, selectedOption);
            dialog.dismiss();
        });
        
        // Add auto-select option
        builder.setNeutralButton("Auto Select", (dialog, which) -> {
            qualityManager.setAutoSelectEnabled(true);
            VideoStream selectedStream = qualityManager.selectBestQuality(videoStreams);
            
            if (selectedStream != null) {
                // Find the quality option for the selected stream
                for (QualityOption option : qualityOptions) {
                    if (option.getVideoStream().equals(selectedStream)) {
                        launchPlayerWithQuality(streamInfo, option);
                        break;
                    }
                }
            }
            dialog.dismiss();
        });
        
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        
        // Show dialog
        AlertDialog dialog = builder.create();
        dialog.show();
    }
    
    private List<QualityOption> createQualityOptions(List<VideoStream> videoStreams) {
        List<QualityOption> options = new ArrayList<>();
        Set<String> addedResolutions = new HashSet<>();
        
        // Sort streams by quality (highest first)
        List<VideoStream> sortedStreams = new ArrayList<>(videoStreams);
        sortedStreams.sort((stream1, stream2) -> {
            int height1 = parseResolutionHeight(stream1.getResolution());
            int height2 = parseResolutionHeight(stream2.getResolution());
            return Integer.compare(height2, height1); // Descending order
        });
        
        for (VideoStream stream : sortedStreams) {
            String resolution = stream.getResolution();
            String format = stream.getFormat() != null ? stream.getFormat().getName() : "Unknown";
            
            if (resolution != null && !resolution.trim().isEmpty() && 
                !addedResolutions.contains(resolution)) {
                
                String displayText = formatQualityText(resolution, format);
                options.add(new QualityOption(resolution, displayText, stream));
                addedResolutions.add(resolution);
            }
        }
        
        return options;
    }
    
    private int parseResolutionHeight(String resolution) {
        if (resolution == null || resolution.trim().isEmpty()) {
            return 0;
        }
        
        try {
            // Extract numbers from resolution string
            String numbers = resolution.replaceAll("[^0-9]", "");
            if (!numbers.isEmpty()) {
                return Integer.parseInt(numbers);
            }
        } catch (NumberFormatException e) {
            Log.w(TAG, "Failed to parse resolution: " + resolution);
        }
        
        return 0;
    }
    
    private String formatQualityText(String resolution, String format) {
        StringBuilder sb = new StringBuilder();
        
        // Add resolution
        sb.append(resolution);
        
        // Add quality description
        int height = parseResolutionHeight(resolution);
        if (height >= 1080) {
            sb.append(" (Full HD)");
        } else if (height >= 720) {
            sb.append(" (HD)");
        } else if (height >= 480) {
            sb.append(" (SD)");
        }
        
        // Add format if available
        if (format != null && !format.equals("Unknown")) {
            sb.append(" - ").append(format);
        }
        
        return sb.toString();
    }
    
    private void launchPlayerWithQuality(StreamInfo streamInfo, QualityOption selectedOption) {
        Context context = getContext();
        if (context == null) return;
        
        try {
            VideoStream selectedStream = selectedOption.getVideoStream();
            String videoUrl = selectedStream.getUrl();
            String videoTitle = streamInfo.getName() != null ? streamInfo.getName() : "Unknown Title";
            
            if (videoUrl == null || videoUrl.trim().isEmpty()) {
                showToast("Invalid video URL for selected quality");
                return;
            }
            
            // Show selected quality to user
            showToast("Playing in " + selectedOption.getDisplayText());
            
            Intent intent = new Intent(context, PlayerActivity.class);
            intent.putExtra("name", videoTitle);
            intent.putExtra("link", videoUrl);
            intent.putExtra("category", Template.YOUTUBE);
            intent.putExtra("quality", selectedOption.getResolution());
            context.startActivity(intent);
            
            Log.d(TAG, "Launching player with quality: " + selectedOption.getResolution());
            
        } catch (Exception e) {
            Log.e(TAG, "Error launching player with selected quality", e);
            showToast("Error: Unable to play video with selected quality. " + e.getLocalizedMessage());
        }
    }
    
    private void launchPlayerWithStream(StreamInfo streamInfo, VideoStream videoStream) {
        Context context = getContext();
        if (context == null) return;
        
        try {
            String videoUrl = videoStream.getUrl();
            String videoTitle = streamInfo.getName() != null ? streamInfo.getName() : "Unknown Title";
            String quality = videoStream.getResolution();
            
            if (videoUrl == null || videoUrl.trim().isEmpty()) {
                showToast("Invalid video URL");
                return;
            }
            
            showToast("Auto-selected quality: " + quality);
            
            Intent intent = new Intent(context, PlayerActivity.class);
            intent.putExtra("name", videoTitle);
            intent.putExtra("link", videoUrl);
            intent.putExtra("category", Template.YOUTUBE);
            intent.putExtra("quality", quality);
            context.startActivity(intent);
            
            Log.d(TAG, "Launching player with auto-selected quality: " + quality);
            
        } catch (Exception e) {
            Log.e(TAG, "Error launching player", e);
            showToast("Error: Unable to play video. " + e.getLocalizedMessage());
        }
    }
    
    /**
     * Custom adapter for quality selection dialog
     */
    private static class QualityAdapter extends ArrayAdapter<QualityOption> {
        private final Context context;
        private final List<QualityOption> options;
        
        public QualityAdapter(Context context, List<QualityOption> options) {
            super(context, android.R.layout.simple_list_item_1, options);
            this.context = context;
            this.options = options;
        }
        
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                LayoutInflater inflater = LayoutInflater.from(context);
                convertView = inflater.inflate(android.R.layout.simple_list_item_2, parent, false);
            }
            
            QualityOption option = options.get(position);
            
            TextView text1 = convertView.findViewById(android.R.id.text1);
            TextView text2 = convertView.findViewById(android.R.id.text2);
            
            text1.setText(option.getDisplayText());
            text2.setText("Tap to select this quality");
            text2.setTextSize(12);
            text2.setTextColor(android.graphics.Color.GRAY);
            
            return convertView;
        }
    }
    
    // ========================================================================================
    // UI State Management
    // ========================================================================================
    
    private void showToast(String message) {
        Context context = getContext();
        if (context != null && stateManager.isFragmentActive()) {
            Toast.makeText(context, message, Toast.LENGTH_LONG).show();
        }
    }
    
    private void showLoading() {
        if (binding == null) return;
        
        setViewVisibility(binding.progressBar, View.VISIBLE);
        setViewVisibility(binding.loadingIndicator, View.VISIBLE);
        setViewVisibility(binding.errorContainer, View.GONE);
    }
    
    private void hideLoading() {
        if (binding == null) return;
        
        setViewVisibility(binding.progressBar, View.GONE);
        setViewVisibility(binding.loadingIndicator, View.GONE);
        
        if (binding.swipeRefresh != null) {
            binding.swipeRefresh.setRefreshing(false);
        }
    }
    
    private void showError() {
        if (binding == null) return;
        
        setViewVisibility(binding.errorContainer, View.VISIBLE);
        setViewVisibility(binding.webview, View.GONE);
    }
    
    private void showNetworkError() {
        if (binding == null) return;
        
        setViewVisibility(binding.networkStatusContainer, View.VISIBLE);
        if (binding.networkStatusText != null) {
            binding.networkStatusText.setText("No internet connection");
        }
    }
    
    private void hideNetworkError() {
        if (binding == null) return;
        
        setViewVisibility(binding.networkStatusContainer, View.GONE);
    }
    
    private void setViewVisibility(View view, int visibility) {
        if (view != null) {
            view.setVisibility(visibility);
        }
    }
    
    // ========================================================================================
    // Utility Methods
    // ========================================================================================
    
    private void markFirstLaunchCompleted() {
        if (stateManager.isFirstLaunch() && sharedPreferences != null) {
            stateManager.setFirstLaunch(false);
            sharedPreferences.edit().putBoolean(KEY_FIRST_LAUNCH, false).apply();
            Log.d(TAG, "First launch completed and saved");
        }
    }
    
    private void setupCookieManager() {
        try {
            CookieManager cookieManager = CookieManager.getInstance();
            if (cookieManager != null && binding != null && binding.webview != null) {
                cookieManager.setAcceptCookie(true);
                cookieManager.setAcceptThirdPartyCookies(binding.webview, true);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    cookieManager.flush();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error setting up cookie manager", e);
        }
    }
    
    private void resumeWebView() {
        if (binding != null && binding.webview != null) {
            try {
                binding.webview.onResume();
                binding.webview.resumeTimers();
                Log.d(TAG, "WebView resumed");
            } catch (Exception e) {
                Log.e(TAG, "Error resuming WebView", e);
            }
        }
    }
    
    private void pauseWebView() {
        if (binding != null && binding.webview != null) {
            try {
                binding.webview.onPause();
                binding.webview.pauseTimers();
                Log.d(TAG, "WebView paused");
            } catch (Exception e) {
                Log.e(TAG, "Error pausing WebView", e);
            }
        }
    }
    
    // ========================================================================================
    // Resource Management
    // ========================================================================================
    
    private void cleanup() {
        try {
            Log.d(TAG, "Starting fragment cleanup");
            
            unregisterNetworkCallback();
            
            // Dispose all RxJava subscriptions
            if (!compositeDisposable.isDisposed()) {
                compositeDisposable.clear();
                Log.d(TAG, "RxJava subscriptions disposed");
            }
            
            // Clean up WebView
            if (binding != null && binding.webview != null) {
                binding.webview.pauseTimers();
                binding.webview.clearHistory();
                binding.webview.clearCache(true);
                binding.webview.loadUrl("about:blank");
                
                // Destroy WebView on main thread
                if (mainHandler != null) {
                    mainHandler.post(() -> {
                        if (binding != null && binding.webview != null) {
                            binding.webview.destroy();
                            Log.d(TAG, "WebView destroyed");
                        }
                    });
                }
            }
            
            // Clean up handlers
            if (mainHandler != null) {
                mainHandler.removeCallbacksAndMessages(null);
            }
            
            // Reset state
            stateManager.reset();
            
            Log.d(TAG, "Fragment cleanup completed");
            
        } catch (Exception e) {
            Log.e(TAG, "Error during fragment cleanup", e);
        }
    }
    
    // ========================================================================================
    // Public API Methods
    // ========================================================================================
    
    /**
     * Get current preferred quality setting
     */
    public String getPreferredQuality() {
        return qualityManager.getPreferredQuality();
    }
    
    /**
     * Set preferred quality
     */
    public void setPreferredQuality(String quality) {
        qualityManager.setPreferredQuality(quality);
        Log.d(TAG, "Preferred quality set to: " + quality);
    }
    
    /**
     * Check if auto-select quality is enabled
     */
    public boolean isAutoSelectQualityEnabled() {
        return qualityManager.isAutoSelectEnabled();
    }
    
    /**
     * Enable or disable auto-select quality
     */
    public void setAutoSelectQualityEnabled(boolean enabled) {
        qualityManager.setAutoSelectEnabled(enabled);
        Log.d(TAG, "Auto-select quality " + (enabled ? "enabled" : "disabled"));
    }
    
    /**
     * Refresh the current page
     */
    public void refreshPage() {
        if (binding != null && binding.webview != null) {
            binding.webview.reload();
        }
    }
    
    /**
     * Check if fragment is currently active
     */
    public boolean isFragmentActive() {
        return stateManager.isFragmentActive();
    }
    
    /**
     * Check if network is available
     */
    public boolean isNetworkAvailable() {
        return stateManager.isNetworkAvailable();
    }
    
    /**
     * Force network status check
     */
    public void checkNetworkStatus() {
        boolean available = checkNetworkAvailability();
        stateManager.setNetworkAvailable(available);
        
        if (available) {
            hideNetworkError();
        } else {
            showNetworkError();
        }
    }
}
