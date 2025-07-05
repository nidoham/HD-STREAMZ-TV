package com.nidoham.hdstreamztv.newpipe.extractors.helper;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;
import com.nidoham.hdstreamztv.newpipe.extractors.YouTubeVideoExtractor;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Easy-to-use wrapper class for extracting YouTube video titles
 * Provides simple methods with caching and error handling
 */
public class YouTubeTitleFetcher {
    
    private static YouTubeTitleFetcher instance;
    private YouTubeVideoExtractor extractor;
    private Map<String, String> titleCache;
    private Map<String, Long> cacheTimestamps;
    private Handler mainHandler;
    private Context context;
    
    // Cache expiry time (30 minutes)
    private static final long CACHE_EXPIRY_TIME = 30 * 60 * 1000;
    
    /**
     * Private constructor for singleton pattern
     */
    private YouTubeTitleFetcher(Context context) {
        this.context = context.getApplicationContext();
        this.extractor = new YouTubeVideoExtractor();
        this.titleCache = new ConcurrentHashMap<>();
        this.cacheTimestamps = new ConcurrentHashMap<>();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }
    
    /**
     * Get singleton instance
     * @param context Application context
     * @return YouTubeTitleFetcher instance
     */
    public static synchronized YouTubeTitleFetcher getInstance(Context context) {
        if (instance == null) {
            instance = new YouTubeTitleFetcher(context);
        }
        return instance;
    }
    
    /**
     * Simple interface for title callback
     */
    public interface TitleCallback {
        void onSuccess(String title);
        void onError(String error);
    }
    
    /**
     * Simple interface for title callback with URL
     */
    public interface TitleWithUrlCallback {
        void onSuccess(String url, String title);
        void onError(String url, String error);
    }
    
    /**
     * Get video title - Main method
     * @param youtubeUrl YouTube video URL
     * @param callback Callback for result
     */
    public void getTitle(String youtubeUrl, TitleCallback callback) {
        if (callback == null) {
            return;
        }
        
        // Validate URL
        if (!isValidUrl(youtubeUrl)) {
            callback.onError("Invalid YouTube URL");
            return;
        }
        
        // Clean URL for caching
        String cleanUrl = cleanUrl(youtubeUrl);
        
        // Check cache first
        String cachedTitle = getCachedTitle(cleanUrl);
        if (cachedTitle != null) {
            callback.onSuccess(cachedTitle);
            return;
        }
        
        // Extract title from YouTube
        extractor.getVideoTitle(cleanUrl, new YouTubeVideoExtractor.OnTitleListener() {
            @Override
            public void onTitleReceived(String title) {
                // Cache the title
                cacheTitle(cleanUrl, title);
                
                // Return result on main thread
                mainHandler.post(() -> callback.onSuccess(title));
            }
            
            @Override
            public void onError(String error) {
                // Return error on main thread
                mainHandler.post(() -> callback.onError(error));
            }
        });
    }
    
    /**
     * Get video title with URL in callback
     * @param youtubeUrl YouTube video URL
     * @param callback Callback for result with URL
     */
    public void getTitleWithUrl(String youtubeUrl, TitleWithUrlCallback callback) {
        if (callback == null) {
            return;
        }
        
        getTitle(youtubeUrl, new TitleCallback() {
            @Override
            public void onSuccess(String title) {
                callback.onSuccess(youtubeUrl, title);
            }
            
            @Override
            public void onError(String error) {
                callback.onError(youtubeUrl, error);
            }
        });
    }
    
    /**
     * Get video title synchronously (for background threads only)
     * WARNING: Do not call this on the main thread!
     * @param youtubeUrl YouTube video URL
     * @return Title or null if error
     */
    public String getTitleSync(String youtubeUrl) {
        if (!isValidUrl(youtubeUrl)) {
            return null;
        }
        
        String cleanUrl = cleanUrl(youtubeUrl);
        
        // Check cache first
        String cachedTitle = getCachedTitle(cleanUrl);
        if (cachedTitle != null) {
            return cachedTitle;
        }
        
        // Use a blocking approach (only for background threads)
        final String[] result = new String[1];
        final boolean[] finished = new boolean[1];
        
        extractor.getVideoTitle(cleanUrl, new YouTubeVideoExtractor.OnTitleListener() {
            @Override
            public void onTitleReceived(String title) {
                result[0] = title;
                cacheTitle(cleanUrl, title);
                finished[0] = true;
            }
            
            @Override
            public void onError(String error) {
                result[0] = null;
                finished[0] = true;
            }
        });
        
        // Wait for result (with timeout)
        long startTime = System.currentTimeMillis();
        while (!finished[0] && (System.currentTimeMillis() - startTime) < 30000) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            }
        }
        
        return result[0];
    }
    
    /**
     * Get video title with Toast notification
     * @param youtubeUrl YouTube video URL
     * @param showToast Whether to show toast messages
     */
    public void getTitleWithToast(String youtubeUrl, boolean showToast) {
        getTitle(youtubeUrl, new TitleCallback() {
            @Override
            public void onSuccess(String title) {
                if (showToast && context != null) {
                    Toast.makeText(context, "Title: " + title, Toast.LENGTH_LONG).show();
                }
            }
            
            @Override
            public void onError(String error) {
                if (showToast && context != null) {
                    Toast.makeText(context, "Error: " + error, Toast.LENGTH_LONG).show();
                }
            }
        });
    }
    
    /**
     * Get multiple video titles at once
     * @param urls Array of YouTube URLs
     * @param callback Callback for each result
     */
    public void getMultipleTitles(String[] urls, TitleWithUrlCallback callback) {
        if (urls == null || callback == null) {
            return;
        }
        
        for (String url : urls) {
            getTitleWithUrl(url, callback);
        }
    }
    
    /**
     * Get video title from video ID
     * @param videoId YouTube video ID
     * @param callback Callback for result
     */
    public void getTitleFromVideoId(String videoId, TitleCallback callback) {
        if (videoId == null || videoId.trim().isEmpty()) {
            callback.onError("Invalid video ID");
            return;
        }
        
        String youtubeUrl = "https://www.youtube.com/watch?v=" + videoId;
        getTitle(youtubeUrl, callback);
    }
    
    /**
     * Check if title is cached
     * @param url YouTube URL
     * @return true if cached and not expired
     */
    public boolean isCached(String url) {
        String cleanUrl = cleanUrl(url);
        return getCachedTitle(cleanUrl) != null;
    }
    
    /**
     * Get cached title if available and not expired
     * @param url YouTube URL
     * @return Cached title or null
     */
    public String getCachedTitleIfAvailable(String url) {
        String cleanUrl = cleanUrl(url);
        return getCachedTitle(cleanUrl);
    }
    
    /**
     * Clear all cached titles
     */
    public void clearCache() {
        titleCache.clear();
        cacheTimestamps.clear();
    }
    
    /**
     * Clear expired cache entries
     */
    public void clearExpiredCache() {
        long currentTime = System.currentTimeMillis();
        
        for (Map.Entry<String, Long> entry : cacheTimestamps.entrySet()) {
            if (currentTime - entry.getValue() > CACHE_EXPIRY_TIME) {
                String url = entry.getKey();
                titleCache.remove(url);
                cacheTimestamps.remove(url);
            }
        }
    }
    
    /**
     * Get cache size
     * @return Number of cached titles
     */
    public int getCacheSize() {
        return titleCache.size();
    }
    
    /**
     * Check if extractor is busy
     * @return true if busy
     */
    public boolean isBusy() {
        return extractor.isBusy();
    }
    
    /**
     * Cancel all ongoing operations
     */
    public void cancelAll() {
        extractor.cancelAll();
    }
    
    /**
     * Cleanup resources
     */
    public void cleanup() {
        extractor.cleanup();
        titleCache.clear();
        cacheTimestamps.clear();
    }
    
    // Private helper methods
    
    private boolean isValidUrl(String url) {
        return url != null && !url.trim().isEmpty() && YouTubeVideoExtractor.isValidYouTubeUrl(url);
    }
    
    private String cleanUrl(String url) {
        if (url == null) return "";
        
        // Remove unnecessary parameters for caching
        if (url.contains("&")) {
            String[] parts = url.split("&");
            for (String part : parts) {
                if (part.contains("v=")) {
                    return "https://www.youtube.com/watch?" + part;
                }
            }
        }
        
        return url;
    }
    
    private String getCachedTitle(String url) {
        if (!titleCache.containsKey(url)) {
            return null;
        }
        
        // Check if cache is expired
        Long timestamp = cacheTimestamps.get(url);
        if (timestamp == null || (System.currentTimeMillis() - timestamp) > CACHE_EXPIRY_TIME) {
            titleCache.remove(url);
            cacheTimestamps.remove(url);
            return null;
        }
        
        return titleCache.get(url);
    }
    
    private void cacheTitle(String url, String title) {
        if (url != null && title != null && !title.trim().isEmpty()) {
            titleCache.put(url, title);
            cacheTimestamps.put(url, System.currentTimeMillis());
        }
    }
    
    // Static utility methods for quick access
    
    /**
     * Quick method to get title with default instance
     * @param context Application context
     * @param youtubeUrl YouTube URL
     * @param callback Callback for result
     */
    public static void fetchTitle(Context context, String youtubeUrl, TitleCallback callback) {
        getInstance(context).getTitle(youtubeUrl, callback);
    }
    
    /**
     * Quick method to get title with Toast
     * @param context Application context
     * @param youtubeUrl YouTube URL
     */
    public static void fetchTitleWithToast(Context context, String youtubeUrl) {
        getInstance(context).getTitleWithToast(youtubeUrl, true);
    }
    
    /**
     * Quick method to get title from video ID
     * @param context Application context
     * @param videoId YouTube video ID
     * @param callback Callback for result
     */
    public static void fetchTitleFromVideoId(Context context, String videoId, TitleCallback callback) {
        getInstance(context).getTitleFromVideoId(videoId, callback);
    }
}