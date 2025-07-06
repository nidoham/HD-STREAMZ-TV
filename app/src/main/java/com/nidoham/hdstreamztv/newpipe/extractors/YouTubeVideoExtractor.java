package com.nidoham.hdstreamztv.newpipe.extractors;

import android.widget.Toast;
import com.nidoham.hdstreamztv.App;
import com.nidoham.hdstreamztv.newpipe.ExtractorHelper;
import org.schabi.newpipe.extractor.ServiceList;
import org.schabi.newpipe.extractor.stream.StreamInfo;
import org.schabi.newpipe.extractor.stream.VideoStream;
import org.schabi.newpipe.extractor.stream.AudioStream;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;

import java.util.List;

/**
 * Professional YouTube video extractor using NewPipe ExtractorHelper
 * Handles video information extraction, stream URLs, and metadata
 */
public final class YouTubeVideoExtractor {
    
    private static final String TAG = "YouTubeVideoExtractor";
    private final CompositeDisposable disposables = new CompositeDisposable();
    
    /*//////////////////////////////////////////////////////////////////////////
    // Callback Interfaces
    //////////////////////////////////////////////////////////////////////////*/
    
    /**
     * Interface for complete video information callbacks
     */
    public interface OnVideoInfoListener {
        void onVideoInfoReceived(VideoInfo videoInfo);
        void onError(String error, Throwable throwable);
        void onProgress(String message);
    }
    
    /**
     * Interface for simple title extraction
     */
    public interface OnTitleListener {
        void onTitleReceived(String title);
        void onError(String error);
    }
    
    /**
     * Interface for video streams extraction
     */
    public interface OnVideoStreamsListener {
        void onVideoStreamsReceived(List<VideoStream> videoStreams);
        void onAudioStreamsReceived(List<AudioStream> audioStreams);
        void onError(String error);
    }
    
    /*//////////////////////////////////////////////////////////////////////////
    // Data Classes
    //////////////////////////////////////////////////////////////////////////*/
    
    /**
     * Immutable data class to hold video information
     */
    public static final class VideoInfo {
        private final String title;
        private final String uploader;
        private final String description;
        private final long duration;
        private final long viewCount;
        private final String thumbnailUrl;
        private final String uploadDate;
        private final List<VideoStream> videoStreams;
        private final List<AudioStream> audioStreams;
        private final String url;
        
        public VideoInfo(StreamInfo streamInfo) {
            this.title = streamInfo.getName() != null ? streamInfo.getName() : "Unknown Title";
            this.uploader = streamInfo.getUploaderName() != null ? streamInfo.getUploaderName() : "Unknown Uploader";
            this.description = streamInfo.getDescription() != null && streamInfo.getDescription().getContent() != null
                    ? streamInfo.getDescription().getContent() : "No description available";
            this.duration = streamInfo.getDuration();
            this.viewCount = streamInfo.getViewCount();
            
            // Safely get thumbnail URL
            this.thumbnailUrl = streamInfo.getThumbnails() != null && !streamInfo.getThumbnails().isEmpty()
                    ? streamInfo.getThumbnails().get(0).getUrl() : "";
            
            // Safely get upload date
            this.uploadDate = streamInfo.getUploadDate() != null && streamInfo.getUploadDate().date() != null
                    ? streamInfo.getUploadDate().date().toString() : "Unknown Date";
            
            this.videoStreams = streamInfo.getVideoStreams();
            this.audioStreams = streamInfo.getAudioStreams();
            this.url = streamInfo.getUrl();
        }
        
        // Getters
        public String getTitle() { return title; }
        public String getUploader() { return uploader; }
        public String getDescription() { return description; }
        public long getDuration() { return duration; }
        public long getViewCount() { return viewCount; }
        public String getThumbnailUrl() { return thumbnailUrl; }
        public String getUploadDate() { return uploadDate; }
        public List<VideoStream> getVideoStreams() { return videoStreams; }
        public List<AudioStream> getAudioStreams() { return audioStreams; }
        public String getUrl() { return url; }
        
        /**
         * Format duration in HH:MM:SS or MM:SS format
         */
        public String getFormattedDuration() {
            if (duration <= 0) return "Unknown";
            
            long hours = duration / 3600;
            long minutes = (duration % 3600) / 60;
            long seconds = duration % 60;
            
            if (hours > 0) {
                return String.format("%d:%02d:%02d", hours, minutes, seconds);
            } else {
                return String.format("%d:%02d", minutes, seconds);
            }
        }
        
        /**
         * Format view count with K/M suffixes
         */
        public String getFormattedViewCount() {
            if (viewCount <= 0) return "Unknown";
            
            if (viewCount >= 1_000_000) {
                return String.format("%.1fM views", viewCount / 1_000_000.0);
            } else if (viewCount >= 1_000) {
                return String.format("%.1fK views", viewCount / 1_000.0);
            } else {
                return viewCount + " views";
            }
        }
    }
    
    /*//////////////////////////////////////////////////////////////////////////
    // Public Methods
    //////////////////////////////////////////////////////////////////////////*/
    
    /**
     * Extract complete video information
     * @param youtubeUrl YouTube video URL
     * @param listener Callback listener
     */
    public void getVideoInfo(String youtubeUrl, OnVideoInfoListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("Listener cannot be null");
        }
        
        if (!isValidYouTubeUrl(youtubeUrl)) {
            listener.onError("Invalid YouTube URL", new IllegalArgumentException("URL is not a valid YouTube URL"));
            return;
        }
        
        final int serviceId = ServiceList.YouTube.getServiceId();
        
        listener.onProgress("Extracting video information...");
        
        disposables.add(
            ExtractorHelper.getStreamInfo(serviceId, youtubeUrl, false)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    streamInfo -> {
                        try {
                            VideoInfo videoInfo = new VideoInfo(streamInfo);
                            listener.onVideoInfoReceived(videoInfo);
                        } catch (Exception e) {
                            listener.onError("Error processing video info", e);
                        }
                    },
                    throwable -> {
                        String errorMessage = getErrorMessage(throwable);
                        listener.onError(errorMessage, throwable);
                    }
                )
        );
    }
    
    /**
     * Extract only video title (lightweight operation)
     * @param youtubeUrl YouTube video URL
     * @param listener Callback listener
     */
    public void getVideoTitle(String youtubeUrl, OnTitleListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("Listener cannot be null");
        }
        
        getVideoInfo(youtubeUrl, new OnVideoInfoListener() {
            @Override
            public void onVideoInfoReceived(VideoInfo videoInfo) {
                listener.onTitleReceived(videoInfo.getTitle());
            }
            
            @Override
            public void onError(String error, Throwable throwable) {
                listener.onError(error);
            }
            
            @Override
            public void onProgress(String message) {
                // Progress updates for title extraction (optional)
            }
        });
    }
    
    /**
     * Extract video streams for playback
     * @param youtubeUrl YouTube video URL
     * @param listener Callback listener
     */
    public void getVideoStreams(String youtubeUrl, OnVideoStreamsListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("Listener cannot be null");
        }
        
        getVideoInfo(youtubeUrl, new OnVideoInfoListener() {
            @Override
            public void onVideoInfoReceived(VideoInfo videoInfo) {
                listener.onVideoStreamsReceived(videoInfo.getVideoStreams());
                listener.onAudioStreamsReceived(videoInfo.getAudioStreams());
            }
            
            @Override
            public void onError(String error, Throwable throwable) {
                listener.onError(error);
            }
            
            @Override
            public void onProgress(String message) {
                // Progress updates for stream extraction (optional)
            }
        });
    }
    
    /*//////////////////////////////////////////////////////////////////////////
    // Static Utility Methods
    //////////////////////////////////////////////////////////////////////////*/
    
    /**
     * Check if URL is valid YouTube URL
     * @param url URL to check
     * @return true if valid YouTube URL
     */
    public static boolean isValidYouTubeUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }
        
        String lowerUrl = url.toLowerCase();
        return lowerUrl.contains("youtube.com/watch") || 
               lowerUrl.contains("youtu.be/") || 
               lowerUrl.contains("youtube.com/v/") ||
               lowerUrl.contains("youtube.com/embed/") ||
               lowerUrl.contains("m.youtube.com/watch");
    }
    
    /**
     * Extract video ID from YouTube URL
     * @param url YouTube URL
     * @return Video ID or null if not found
     */
    public static String extractVideoId(String url) {
        if (!isValidYouTubeUrl(url)) {
            return null;
        }
        
        String videoId = null;
        
        try {
            if (url.contains("youtube.com/watch")) {
                String[] parts = url.split("v=");
                if (parts.length > 1) {
                    videoId = parts[1].split("&")[0];
                }
            } else if (url.contains("youtu.be/")) {
                String[] parts = url.split("youtu.be/");
                if (parts.length > 1) {
                    videoId = parts[1].split("\\?")[0];
                }
            } else if (url.contains("youtube.com/v/")) {
                String[] parts = url.split("v/");
                if (parts.length > 1) {
                    videoId = parts[1].split("\\?")[0];
                }
            } else if (url.contains("youtube.com/embed/")) {
                String[] parts = url.split("embed/");
                if (parts.length > 1) {
                    videoId = parts[1].split("\\?")[0];
                }
            }
            
            // Clean up video ID (remove any trailing characters)
            if (videoId != null && videoId.length() > 11) {
                videoId = videoId.substring(0, 11);
            }
        } catch (Exception e) {
            return null;
        }
        
        return videoId;
    }
    
    /*//////////////////////////////////////////////////////////////////////////
    // Resource Management
    //////////////////////////////////////////////////////////////////////////*/
    
    /**
     * Clean up resources - call this when done
     */
    public void cleanup() {
        if (disposables != null && !disposables.isDisposed()) {
            disposables.dispose();
        }
    }
    
    /**
     * Check if extractor is currently working
     * @return true if there are active operations
     */
    public boolean isBusy() {
        return disposables != null && disposables.size() > 0;
    }
    
    /**
     * Cancel all ongoing operations
     */
    public void cancelAll() {
        if (disposables != null) {
            disposables.clear();
        }
    }
    
    /*//////////////////////////////////////////////////////////////////////////
    // Error Handling
    //////////////////////////////////////////////////////////////////////////*/
    
    private String getErrorMessage(Throwable throwable) {
        if (throwable == null) {
            return "Unknown error occurred";
        }
        
        String message = throwable.getMessage();
        if (message == null || message.trim().isEmpty()) {
            return "Error: " + throwable.getClass().getSimpleName();
        }
        
        // Common error messages for better user experience
        String lowerMessage = message.toLowerCase();
        if (lowerMessage.contains("video unavailable") || lowerMessage.contains("not available")) {
            return "Video is unavailable or private";
        } else if (lowerMessage.contains("network") || lowerMessage.contains("connection")) {
            return "Network connection error";
        } else if (lowerMessage.contains("timeout")) {
            return "Request timeout - please try again";
        } else if (lowerMessage.contains("blocked") || lowerMessage.contains("restricted")) {
            return "Video is blocked in your region";
        } else if (lowerMessage.contains("copyright")) {
            return "Video is not available due to copyright restrictions";
        } else if (lowerMessage.contains("age")) {
            return "Video requires age verification";
        } else {
            return "Error: " + message;
        }
    }
}