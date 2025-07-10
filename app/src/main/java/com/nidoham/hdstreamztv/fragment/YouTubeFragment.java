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
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.nidoham.hdstreamztv.PlayerActivity;
import com.nidoham.hdstreamztv.databinding.FragmentYoutubeBinding;
import com.nidoham.hdstreamztv.example.data.link.youtube.YouTube;
import com.nidoham.hdstreamztv.template.model.settings.Template;

import org.schabi.newpipe.extractor.ServiceList;
import org.schabi.newpipe.util.ExtractorHelper;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class YouTubeFragment extends Fragment {
    
    private static final String TAG = "YouTubeFragment";
    private static final String PREFS_NAME = "youtube_fragment_prefs";
    private static final String KEY_FIRST_LAUNCH = "is_first_launch";
    private static final String YOUTUBE_SIGNIN_URL = "https://accounts.google.com/signin/v2/identifier?service=youtube";
    private static final String YOUTUBE_URL = "https://www.youtube.com";
    private static final String YOUTUBE_MOBILE_URL = "https://m.youtube.com";
    private static final String USER_AGENT = "Mozilla/5.0 (Linux; Android " + Build.VERSION.RELEASE +
            "; " + Build.MODEL + ") AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36";
    
    private FragmentYoutubeBinding binding;
    private ConnectivityManager connectivityManager;
    private ConnectivityManager.NetworkCallback networkCallback;
    private SharedPreferences sharedPreferences;
    private Handler mainHandler;
    
    // Use CompositeDisposable for better management
    private final CompositeDisposable compositeDisposable = new CompositeDisposable();
    
    private boolean isFirstLaunch = true;
    private boolean isNetworkAvailable = false;
    private boolean isPageLoaded = false;
    private boolean isFragmentActive = false;
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentYoutubeBinding.inflate(inflater, container, false);
        initializeComponents();
        return binding.getRoot();
    }
    
    @Override
    public void onResume() {
        super.onResume();
        isFragmentActive = true;
        resumeWebView();
        registerNetworkCallback();
    }
    
    @Override
    public void onPause() {
        super.onPause();
        isFragmentActive = false;
        pauseWebView();
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        isFragmentActive = false;
        cleanup();
        binding = null;
    }
    
    private void initializeComponents() {
        mainHandler = new Handler(Looper.getMainLooper());
        initializePreferences();
        setupNetworkMonitoring();
        configureWebView();
        setupUI();
        loadYouTube();
    }
    
    private void initializePreferences() {
        Context context = getContext();
        if (context != null) {
            sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            isFirstLaunch = sharedPreferences.getBoolean(KEY_FIRST_LAUNCH, true);
        }
    }
    
    private void setupNetworkMonitoring() {
        Context context = getContext();
        if (context != null) {
            connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            isNetworkAvailable = checkNetworkAvailability();
            createNetworkCallback();
        }
    }
    
    @SuppressLint("SetJavaScriptEnabled")
    private void configureWebView() {
        if (binding.webview == null) return;
        
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
    }
    
    private void setupUI() {
        // Swipe refresh
        if (binding.swipeRefresh != null) {
            binding.swipeRefresh.setColorSchemeResources(
                android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light
            );
            binding.swipeRefresh.setOnRefreshListener(this::handleRefresh);
        }
        
        // Back press handling
        if (binding.webview != null) {
            binding.webview.setOnKeyListener((v, keyCode, event) -> {
                if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_DOWN) {
                    return handleBackPress();
                }
                return false;
            });
        }
        
        // Button handlers
        if (binding.retryButton != null) {
            binding.retryButton.setOnClickListener(v -> loadYouTube());
        }
        if (binding.settingsButton != null) {
            binding.settingsButton.setOnClickListener(v -> handleStreamExtraction());
        }
    }
    
    private void loadYouTube() {
        if (binding.webview == null) return;
        
        if (!isNetworkAvailable) {
            showNetworkError();
            return;
        }
        
        showLoading();
        String url = isFirstLaunch ? YOUTUBE_SIGNIN_URL :
                     (shouldUseMobileVersion() ? YOUTUBE_MOBILE_URL : YOUTUBE_URL);
        
        // Ensure WebView operations are on main thread
        mainHandler.post(() -> {
            if (binding != null && binding.webview != null) {
                binding.webview.loadUrl(url);
            }
        });
    }
    
    private boolean shouldUseMobileVersion() {
        return getResources().getConfiguration().smallestScreenWidthDp < 600;
    }
    
    private class YouTubeWebViewClient extends WebViewClient {
        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            isPageLoaded = false;
            showLoading();
            
            if (url != null && url.contains("youtube.com") && !url.contains("accounts.google.com")) {
                markFirstLaunchCompleted();
            }
        }
        
        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            isPageLoaded = true;
            hideLoading();
        }
        
        @Override
        public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
            super.onReceivedError(view, request, error);
            if (request != null && request.isForMainFrame()) {
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
    
    private void createNetworkCallback() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            networkCallback = new ConnectivityManager.NetworkCallback() {
                @Override
                public void onAvailable(@NonNull Network network) {
                    isNetworkAvailable = true;
                    if (isFragmentActive && getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            hideNetworkError();
                            if (!isPageLoaded) {
                                loadYouTube();
                            }
                        });
                    }
                }
                
                @Override
                public void onLost(@NonNull Network network) {
                    isNetworkAvailable = false;
                    // Uncomment if you want to show network error immediately
                    // if (isFragmentActive && getActivity() != null) {
                    //     requireActivity().runOnUiThread(this::showNetworkError);
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
        }
    }
    
    private void unregisterNetworkCallback() {
        if (connectivityManager != null && networkCallback != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                connectivityManager.unregisterNetworkCallback(networkCallback);
            } catch (Exception e) {
                Log.w(TAG, "Error unregistering network callback", e);
            }
        }
    }
    
    private void handleRefresh() {
        if (isNetworkAvailable) {
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
    
    private void handleStreamExtraction() {
        if (!isFragmentActive || getContext() == null || binding == null || binding.webview == null) {
            Log.w(TAG, "Fragment not active or context/binding is null");
            return;
        }
        
        String url = binding.webview.getUrl();
        
        if (url == null || url.trim().isEmpty()) {
            showToast("Please navigate to a YouTube video");
            return;
        }
        
        // Show loading state
        showLoading();
        
        int serviceId = ServiceList.YouTube.getServiceId();
        
        Disposable disposable = ExtractorHelper.getStreamInfo(serviceId, url, false)
            .subscribeOn(Schedulers.io()) // Background thread for network/extraction
            .observeOn(AndroidSchedulers.mainThread()) // Main thread for UI updates
            .subscribe(
                streamInfo -> {
                    // Hide loading state
                    hideLoading();
                    
                    // Double-check fragment is still active and context exists
                    if (!isFragmentActive || getContext() == null) {
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
                        String videoUrl = streamInfo.getVideoStreams().get(0).getUrl();
                        String videoTitle = streamInfo.getName() != null ? streamInfo.getName() : "Unknown Title";
                        
                        if (videoUrl == null || videoUrl.trim().isEmpty()) {
                            showToast("Invalid video URL extracted");
                            return;
                        }
                        
                        Context context = getContext();
                        if (context != null) {
                            Intent intent = new Intent(context, PlayerActivity.class);
                            intent.putExtra("name", videoTitle);
                            intent.putExtra("link", videoUrl);
                            intent.putExtra("category", Template.YOUTUBE);
                            context.startActivity(intent);
                        }
                        
                    } catch (Exception e) {
                        Log.e(TAG, "Error launching player or accessing stream data", e);
                        showToast("Error: Unable to play video. " + e.getLocalizedMessage());
                    }
                },
                throwable -> {
                    // Hide loading state
                    hideLoading();
                    
                    Log.e(TAG, "Failed to extract stream info", throwable);
                    
                    if (isFragmentActive && getContext() != null) {
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
    
    private void showToast(String message) {
        Context context = getContext();
        if (context != null && isFragmentActive) {
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
    
    private void markFirstLaunchCompleted() {
        if (isFirstLaunch && sharedPreferences != null) {
            isFirstLaunch = false;
            sharedPreferences.edit().putBoolean(KEY_FIRST_LAUNCH, false).apply();
        }
    }
    
    private void setupCookieManager() {
        CookieManager cookieManager = CookieManager.getInstance();
        if (cookieManager != null && binding != null && binding.webview != null) {
            cookieManager.setAcceptCookie(true);
            cookieManager.setAcceptThirdPartyCookies(binding.webview, true);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                cookieManager.flush();
            }
        }
    }
    
    private void resumeWebView() {
        if (binding != null && binding.webview != null) {
            binding.webview.onResume();
            binding.webview.resumeTimers();
        }
    }
    
    private void pauseWebView() {
        if (binding != null && binding.webview != null) {
            binding.webview.onPause();
            binding.webview.pauseTimers();
        }
    }
    
    private void cleanup() {
        unregisterNetworkCallback();
        
        // Dispose all RxJava subscriptions
        if (!compositeDisposable.isDisposed()) {
            compositeDisposable.clear();
        }
        
        if (binding != null && binding.webview != null) {
            binding.webview.pauseTimers();
            binding.webview.clearHistory();
            binding.webview.clearCache(true);
            binding.webview.loadUrl("about:blank");
            
            // Destroy WebView on main thread
            mainHandler.post(() -> {
                if (binding != null && binding.webview != null) {
                    binding.webview.destroy();
                }
            });
        }
        
        if (mainHandler != null) {
            mainHandler.removeCallbacksAndMessages(null);
        }
    }
}
