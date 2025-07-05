package com.nidoham.hdstreamztv.newpipe.extractors.helper;

import com.nidoham.hdstreamztv.newpipe.extractors.YouTubeVideoExtractor;
import org.schabi.newpipe.extractor.stream.VideoStream;
import org.schabi.newpipe.extractor.stream.AudioStream;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/**
 * Professional YouTube link extractor for retrieving direct stream URLs
 * This class provides a clean interface to extract video and audio URLs from YouTube videos
 * for use with ExoPlayer and other media players.
 * 
 * @author HDStreamz TV
 * @version 2.0
 */
public class YouTubeLinkExtractor {
    
    private final YouTubeVideoExtractor extractor;
    
    // Constants for quality thresholds
    private static final int QUALITY_4K = 2160;
    private static final int QUALITY_1080P = 1080;
    private static final int QUALITY_720P = 720;
    private static final int QUALITY_480P = 480;
    private static final int QUALITY_360P = 360;
    private static final int QUALITY_240P = 240;
    
    /**
     * Video quality preference enumeration
     */
    public enum Quality {
        BEST,        // Highest quality available
        UHD_4K,      // 4K (2160p)
        FULL_HD,     // 1080p
        HD,          // 720p
        MEDIUM,      // 480p
        LOW,         // 360p
        LOWEST,      // 240p or lower
        AUDIO_ONLY   // Audio stream only
    }
    
    /**
     * Callback interface for video and audio stream extraction
     */
    public interface OnLinkExtractedListener {
        /**
         * Called when both video and audio URLs are successfully extracted
         * @param videoUrl Direct video stream URL
         * @param audioUrl Direct audio stream URL
         * @param title Video title
         */
        void onVideoLinkExtracted(String videoUrl, String audioUrl, String title);
        
        /**
         * Called when extraction fails
         * @param error Error message
         */
        void onError(String error);
    }
    
    /**
     * Callback interface for single video URL extraction
     */
    public interface OnVideoLinkListener {
        /**
         * Called when video URL is successfully extracted
         * @param videoUrl Direct video stream URL
         * @param title Video title
         */
        void onVideoLinkExtracted(String videoUrl, String title);
        
        /**
         * Called when extraction fails
         * @param error Error message
         */
        void onError(String error);
    }
    
    /**
     * Callback interface for all available links
     */
    public interface OnAllLinksListener {
        /**
         * Called when all video links are successfully extracted
         * @param videoLinks List of all available video links with qualities
         * @param audioUrl Best audio stream URL
         * @param title Video title
         */
        void onAllLinksExtracted(List<VideoLinkInfo> videoLinks, String audioUrl, String title);
        
        /**
         * Called when extraction fails
         * @param error Error message
         */
        void onError(String error);
    }
    
    /**
     * Data class representing video link information
     */
    public static class VideoLinkInfo {
        private final String url;
        private final String quality;
        private final int height;
        private final String format;
        
        public VideoLinkInfo(String url, String quality, int height, String format) {
            this.url = url;
            this.quality = quality;
            this.height = height;
            this.format = format != null ? format : "unknown";
        }
        
        public VideoLinkInfo(String url, String quality, int height) {
            this(url, quality, height, null);
        }
        
        // Getters
        public String getUrl() { return url; }
        public String getQuality() { return quality; }
        public int getHeight() { return height; }
        public String getFormat() { return format; }
        
        @Override
        public String toString() {
            return String.format("%s (%s) - %s", quality, format, url);
        }
    }
    
    /**
     * Constructor
     */
    public YouTubeLinkExtractor() {
        this.extractor = new YouTubeVideoExtractor();
    }
    
    /**
     * Extract direct video and audio URLs from YouTube link
     * @param youtubeUrl YouTube video URL
     * @param quality Preferred video quality
     * @param listener Callback listener
     */
    public void extractLink(String youtubeUrl, Quality quality, OnLinkExtractedListener listener) {
        validateListener(listener);
        
        if (!isValidYouTubeUrl(youtubeUrl)) {
            listener.onError("Invalid YouTube URL: " + youtubeUrl);
            return;
        }
        
        extractor.getVideoInfo(youtubeUrl, new YouTubeVideoExtractor.OnVideoInfoListener() {
            @Override
            public void onVideoInfoReceived(YouTubeVideoExtractor.VideoInfo videoInfo) {
                processVideoInfo(videoInfo, quality, listener);
            }
            
            @Override
            public void onError(String error, Throwable throwable) {
                listener.onError(formatError(error, throwable));
            }
            
            @Override
            public void onProgress(String message) {
                // Progress updates - can be extended for UI feedback
            }
        });
    }
    
    /**
     * Extract only video URL (combines video and audio or uses video-only stream)
     * @param youtubeUrl YouTube video URL
     * @param quality Preferred video quality
     * @param listener Callback listener
     */
    public void extractVideoLink(String youtubeUrl, Quality quality, OnVideoLinkListener listener) {
        validateListener(listener);
        
        extractLink(youtubeUrl, quality, new OnLinkExtractedListener() {
            @Override
            public void onVideoLinkExtracted(String videoUrl, String audioUrl, String title) {
                // For audio-only quality, return audio URL
                String finalUrl = (quality == Quality.AUDIO_ONLY) ? audioUrl : videoUrl;
                if (finalUrl != null) {
                    listener.onVideoLinkExtracted(finalUrl, title);
                } else {
                    listener.onError("No suitable stream found for the requested quality");
                }
            }
            
            @Override
            public void onError(String error) {
                listener.onError(error);
            }
        });
    }
    
    /**
     * Get all available video links with their qualities
     * @param youtubeUrl YouTube video URL
     * @param listener Callback listener
     */
    public void getAllVideoLinks(String youtubeUrl, OnAllLinksListener listener) {
        validateListener(listener);
        
        if (!isValidYouTubeUrl(youtubeUrl)) {
            listener.onError("Invalid YouTube URL: " + youtubeUrl);
            return;
        }
        
        extractor.getVideoInfo(youtubeUrl, new YouTubeVideoExtractor.OnVideoInfoListener() {
            @Override
            public void onVideoInfoReceived(YouTubeVideoExtractor.VideoInfo videoInfo) {
                processAllLinks(videoInfo, listener);
            }
            
            @Override
            public void onError(String error, Throwable throwable) {
                listener.onError(formatError(error, throwable));
            }
            
            @Override
            public void onProgress(String message) {
                // Progress updates
            }
        });
    }
    
    /**
     * Quick method to get best quality video link
     * @param youtubeUrl YouTube video URL
     * @param listener Callback listener
     */
    public void getBestVideoLink(String youtubeUrl, OnVideoLinkListener listener) {
        extractVideoLink(youtubeUrl, Quality.BEST, listener);
    }
    
    /**
     * Quick method to get audio-only link
     * @param youtubeUrl YouTube video URL
     * @param listener Callback listener
     */
    public void getAudioLink(String youtubeUrl, OnVideoLinkListener listener) {
        extractVideoLink(youtubeUrl, Quality.AUDIO_ONLY, listener);
    }
    
    /**
     * Quick method to get HD quality video link
     * @param youtubeUrl YouTube video URL
     * @param listener Callback listener
     */
    public void getHDVideoLink(String youtubeUrl, OnVideoLinkListener listener) {
        extractVideoLink(youtubeUrl, Quality.HD, listener);
    }
    
    // Private helper methods
    
    private void processVideoInfo(YouTubeVideoExtractor.VideoInfo videoInfo, Quality quality, OnLinkExtractedListener listener) {
        try {
            if (quality == Quality.AUDIO_ONLY) {
                String audioUrl = getBestAudioUrl(videoInfo.getAudioStreams());
                if (audioUrl != null) {
                    listener.onVideoLinkExtracted(null, audioUrl, videoInfo.getTitle());
                } else {
                    listener.onError("No audio stream available");
                }
            } else {
                String videoUrl = getBestVideoUrl(videoInfo.getVideoStreams(), quality);
                String audioUrl = getBestAudioUrl(videoInfo.getAudioStreams());
                
                if (videoUrl != null) {
                    listener.onVideoLinkExtracted(videoUrl, audioUrl, videoInfo.getTitle());
                } else {
                    listener.onError("No suitable video stream found for quality: " + quality);
                }
            }
        } catch (Exception e) {
            listener.onError("Error processing video information: " + e.getMessage());
        }
    }
    
    private void processAllLinks(YouTubeVideoExtractor.VideoInfo videoInfo, OnAllLinksListener listener) {
        try {
            List<VideoLinkInfo> videoLinks = new ArrayList<>();
            
            for (VideoStream stream : videoInfo.getVideoStreams()) {
                if (isValidStreamUrl(stream.getUrl())) {
                    String quality = formatQuality(stream.getHeight());
                    String format = stream.getFormat() != null ? stream.getFormat().getName() : "unknown";
                    videoLinks.add(new VideoLinkInfo(stream.getUrl(), quality, stream.getHeight(), format));
                }
            }
            
            // Sort by quality (highest first)
            Collections.sort(videoLinks, new Comparator<VideoLinkInfo>() {
                @Override
                public int compare(VideoLinkInfo a, VideoLinkInfo b) {
                    return Integer.compare(b.getHeight(), a.getHeight());
                }
            });
            
            String audioUrl = getBestAudioUrl(videoInfo.getAudioStreams());
            
            if (!videoLinks.isEmpty() || audioUrl != null) {
                listener.onAllLinksExtracted(videoLinks, audioUrl, videoInfo.getTitle());
            } else {
                listener.onError("No valid streams found");
            }
            
        } catch (Exception e) {
            listener.onError("Error processing all links: " + e.getMessage());
        }
    }
    
    private String getBestVideoUrl(List<VideoStream> videoStreams, Quality quality) {
        if (videoStreams == null || videoStreams.isEmpty()) {
            return null;
        }
        
        VideoStream bestStream = null;
        int targetHeight = getTargetHeight(quality);
        
        for (VideoStream stream : videoStreams) {
            if (!isValidStreamUrl(stream.getUrl())) {
                continue;
            }
            
            if (bestStream == null) {
                bestStream = stream;
                continue;
            }
            
            switch (quality) {
                case BEST:
                    if (stream.getHeight() > bestStream.getHeight()) {
                        bestStream = stream;
                    }
                    break;
                    
                case LOWEST:
                    if (stream.getHeight() < bestStream.getHeight()) {
                        bestStream = stream;
                    }
                    break;
                    
                default:
                    // For specific quality targets, find closest match
                    int currentDiff = Math.abs(stream.getHeight() - targetHeight);
                    int bestDiff = Math.abs(bestStream.getHeight() - targetHeight);
                    
                    if (currentDiff < bestDiff) {
                        bestStream = stream;
                    } else if (currentDiff == bestDiff && stream.getHeight() > bestStream.getHeight()) {
                        // If same difference, prefer higher quality
                        bestStream = stream;
                    }
                    break;
            }
        }
        
        return bestStream != null ? bestStream.getUrl() : null;
    }
    
    private String getBestAudioUrl(List<AudioStream> audioStreams) {
        if (audioStreams == null || audioStreams.isEmpty()) {
            return null;
        }
        
        AudioStream bestStream = null;
        
        for (AudioStream stream : audioStreams) {
            if (!isValidStreamUrl(stream.getUrl())) {
                continue;
            }
            
            if (bestStream == null || stream.getAverageBitrate() > bestStream.getAverageBitrate()) {
                bestStream = stream;
            }
        }
        
        return bestStream != null ? bestStream.getUrl() : null;
    }
    
    private int getTargetHeight(Quality quality) {
        switch (quality) {
            case UHD_4K: return QUALITY_4K;
            case FULL_HD: return QUALITY_1080P;
            case HD: return QUALITY_720P;
            case MEDIUM: return QUALITY_480P;
            case LOW: return QUALITY_360P;
            case LOWEST: return QUALITY_240P;
            default: return QUALITY_1080P; // Default to 1080p
        }
    }
    
    private String formatQuality(int height) {
        if (height >= QUALITY_4K) return height + "p (4K)";
        if (height >= QUALITY_1080P) return height + "p (Full HD)";
        if (height >= QUALITY_720P) return height + "p (HD)";
        if (height >= QUALITY_480P) return height + "p (SD)";
        return height + "p";
    }
    
    private boolean isValidStreamUrl(String url) {
        return url != null && !url.trim().isEmpty() && url.startsWith("http");
    }
    
    private String formatError(String error, Throwable throwable) {
        if (error != null && !error.trim().isEmpty()) {
            return error;
        }
        if (throwable != null) {
            return "Error: " + throwable.getMessage();
        }
        return "Unknown error occurred";
    }
    
    private void validateListener(Object listener) {
        if (listener == null) {
            throw new IllegalArgumentException("Listener cannot be null");
        }
    }
    
    // Public utility methods
    
    /**
     * Check if URL is a valid YouTube URL
     * @param url URL to validate
     * @return true if valid YouTube URL
     */
    public static boolean isValidYouTubeUrl(String url) {
        return YouTubeVideoExtractor.isValidYouTubeUrl(url);
    }
    
    /**
     * Extract video ID from YouTube URL
     * @param url YouTube URL
     * @return Video ID or null if not found
     */
    public static String extractVideoId(String url) {
        return YouTubeVideoExtractor.extractVideoId(url);
    }
    
    /**
     * Get quality enum from height value
     * @param height Video height in pixels
     * @return Corresponding Quality enum
     */
    public static Quality getQualityFromHeight(int height) {
        if (height >= QUALITY_4K) return Quality.UHD_4K;
        if (height >= QUALITY_1080P) return Quality.FULL_HD;
        if (height >= QUALITY_720P) return Quality.HD;
        if (height >= QUALITY_480P) return Quality.MEDIUM;
        if (height >= QUALITY_360P) return Quality.LOW;
        return Quality.LOWEST;
    }
    
    // Resource management methods
    
    /**
     * Clean up resources - call this when done with the extractor
     */
    public void cleanup() {
        if (extractor != null) {
            extractor.cleanup();
        }
    }
    
    /**
     * Cancel all ongoing extraction operations
     */
    public void cancelAll() {
        if (extractor != null) {
            extractor.cancelAll();
        }
    }
    
    /**
     * Check if extractor is currently processing requests
     * @return true if there are active operations
     */
    public boolean isBusy() {
        return extractor != null && extractor.isBusy();
    }
}