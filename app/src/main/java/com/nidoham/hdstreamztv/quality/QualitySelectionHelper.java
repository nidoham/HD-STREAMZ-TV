package com.nidoham.hdstreamztv.quality;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.schabi.newpipe.extractor.stream.VideoStream;

import java.util.List;

/**
 * Helper class for managing video quality preferences and selection
 */
public class QualitySelectionHelper {
    
    private static final String TAG = "QualitySelectionHelper";
    private static final String PREFS_NAME = "video_quality_prefs";
    private static final String KEY_PREFERRED_QUALITY = "preferred_quality";
    private static final String KEY_AUTO_SELECT = "auto_select_quality";
    
    /**
     * Get user's preferred quality setting
     */
    public static String getPreferredQuality(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_PREFERRED_QUALITY, "720p"); // Default to 720p
    }
    
    /**
     * Save user's preferred quality setting
     */
    public static void setPreferredQuality(Context context, String quality) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_PREFERRED_QUALITY, quality).apply();
    }
    
    /**
     * Check if auto-select quality is enabled
     */
    public static boolean isAutoSelectEnabled(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_AUTO_SELECT, false);
    }
    
    /**
     * Set auto-select quality preference
     */
    public static void setAutoSelectEnabled(Context context, boolean enabled) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_AUTO_SELECT, enabled).apply();
    }
    
    /**
     * Automatically select best quality based on user preference and available streams
     */
    public static VideoStream selectBestQuality(Context context, List<VideoStream> videoStreams) {
        if (videoStreams == null || videoStreams.isEmpty()) {
            return null;
        }
        
        String preferredQuality = getPreferredQuality(context);
        
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
    
    /**
     * Find closest available quality to preferred quality
     */
    private static VideoStream findClosestQuality(String preferredQuality, List<VideoStream> videoStreams) {
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
    
    /**
     * Parse resolution height from resolution string
     */
    private static int parseResolutionHeight(String resolution) {
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
    
    /**
     * Get quality recommendations based on network conditions
     */
    public static String getRecommendedQuality(Context context) {
        // You can implement network speed detection here
        // For now, return a default recommendation
        return "720p";
    }
}
