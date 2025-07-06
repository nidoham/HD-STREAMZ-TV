package com.nidoham.hdstreamztv.newpipe.extractors.helper;

import com.nidoham.hdstreamztv.newpipe.ExtractorHelper;
import org.schabi.newpipe.extractor.ServiceList;
import org.schabi.newpipe.extractor.stream.StreamInfo;
import org.schabi.newpipe.extractor.stream.VideoStream;
import org.schabi.newpipe.extractor.stream.AudioStream;
import org.schabi.newpipe.extractor.exceptions.ExtractionException;

import android.util.Log;
import android.os.Handler;
import android.os.Looper;

import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * Professional YouTube Stream Link Fetcher
 * 
 * This class provides functionality to extract direct stream URLs from YouTube videos
 * using the NewPipe ExtractorHelper with proper error handling and callback mechanisms.
 * 
 * Features:
 * - Uses ExtractorHelper for reliable extraction
 * - Handles both video and audio streams
 * - Quality-based stream selection
 * - Comprehensive error handling
 * - Memory leak prevention
 * 
 * @author HDStreamzTV
 * @version 2.0
 */
public final class YouTubeStreamLinkFetcher {
    
    private static final String TAG = "YouTubeStreamFetcher";
    private static final int YOUTUBE_SERVICE_ID = ServiceList.YouTube.getServiceId();
    
    private final CompositeDisposable compositeDisposable;
    private final Handler mainHandler;
    
    /*//////////////////////////////////////////////////////////////////////////
    // Constructor
    //////////////////////////////////////////////////////////////////////////*/
    
    /**
     * Constructor initializes the disposable container and main thread handler
     */
    public YouTubeStreamLinkFetcher() {
        this.compositeDisposable = new CompositeDisposable();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }
    
    /*//////////////////////////////////////////////////////////////////////////
    // Interfaces
    //////////////////////////////////////////////////////////////////////////*/
    
    /**
     * Callback interface for stream URL extraction
     */
    public interface OnStreamLinkListener {
        /**
         * Called when video stream URL is successfully extracted
         * @param streamData Container with extracted stream information
         */
        void onStreamLinkExtracted(StreamData streamData);
        
        /**
         * Called when extraction fails
         * @param error Error message describing the failure
         * @param throwable Original exception (optional)
         */
        void onError(String error, Throwable throwable);
        
        /**
         * Called when extraction starts (optional)
         */
        default void onExtractionStarted() {}
        
        /**
         * Called to show progress (optional)
         * @param message Progress message
         */
        default void onProgress(String message) {}
    }
    
    /*//////////////////////////////////////////////////////////////////////////
    // Data Classes
    //////////////////////////////////////////////////////////////////////////*/
    
    /**
     * Immutable data class to hold extracted stream information
     */
    public static final class StreamData {
        private final String videoUrl;
        private final String audioUrl;
        private final String title;
        private final String uploader;
        private final long duration;
        private final String thumbnailUrl;
        private final int videoWidth;
        private final int videoHeight;
        private final String videoFormat;
        private final String audioFormat;
        private final String videoQuality;
        private final List<VideoStream> allVideoStreams;
        private final List<AudioStream> allAudioStreams;
        
        private StreamData(Builder builder) {
            this.videoUrl = builder.videoUrl;
            this.audioUrl = builder.audioUrl;
            this.title = builder.title != null ? builder.title : "Unknown Title";
            this.uploader = builder.uploader != null ? builder.uploader : "Unknown Uploader";
            this.duration = builder.duration;
            this.thumbnailUrl = builder.thumbnailUrl;
            this.videoWidth = builder.videoWidth;
            this.videoHeight = builder.videoHeight;
            this.videoFormat = builder.videoFormat;
            this.audioFormat = builder.audioFormat;
            this.videoQuality = builder.videoQuality;
            this.allVideoStreams = builder.allVideoStreams != null ? 
                new ArrayList<>(builder.allVideoStreams) : new ArrayList<>();
            this.allAudioStreams = builder.allAudioStreams != null ? 
                new ArrayList<>(builder.allAudioStreams) : new ArrayList<>();
        }
        
        // Getters
        public String getVideoUrl() { return videoUrl; }
        public String getAudioUrl() { return audioUrl; }
        public String getTitle() { return title; }
        public String getUploader() { return uploader; }
        public long getDuration() { return duration; }
        public String getThumbnailUrl() { return thumbnailUrl; }
        public int getVideoWidth() { return videoWidth; }
        public int getVideoHeight() { return videoHeight; }
        public String getVideoFormat() { return videoFormat; }
        public String getAudioFormat() { return audioFormat; }
        public String getVideoQuality() { return videoQuality; }
        public List<VideoStream> getAllVideoStreams() { return new ArrayList<>(allVideoStreams); }
        public List<AudioStream> getAllAudioStreams() { return new ArrayList<>(allAudioStreams); }
        
        /**
         * Get formatted duration string
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
         * Get video resolution string
         */
        public String getResolution() {
            if (videoWidth > 0 && videoHeight > 0) {
                return videoWidth + "x" + videoHeight;
            }
            return "Unknown";
        }
        
        /**
         * Builder pattern for StreamData
         */
        public static final class Builder {
            private String videoUrl;
            private String audioUrl;
            private String title;
            private String uploader;
            private long duration;
            private String thumbnailUrl;
            private int videoWidth;
            private int videoHeight;
            private String videoFormat;
            private String audioFormat;
            private String videoQuality;
            private List<VideoStream> allVideoStreams;
            private List<AudioStream> allAudioStreams;
            
            public Builder setVideoUrl(String videoUrl) {
                this.videoUrl = videoUrl;
                return this;
            }
            
            public Builder setAudioUrl(String audioUrl) {
                this.audioUrl = audioUrl;
                return this;
            }
            
            public Builder setTitle(String title) {
                this.title = title;
                return this;
            }
            
            public Builder setUploader(String uploader) {
                this.uploader = uploader;
                return this;
            }
            
            public Builder setDuration(long duration) {
                this.duration = duration;
                return this;
            }
            
            public Builder setThumbnailUrl(String thumbnailUrl) {
                this.thumbnailUrl = thumbnailUrl;
                return this;
            }
            
            public Builder setVideoResolution(int width, int height) {
                this.videoWidth = width;
                this.videoHeight = height;
                return this;
            }
            
            public Builder setVideoFormat(String format) {
                this.videoFormat = format;
                return this;
            }
            
            public Builder setAudioFormat(String format) {
                this.audioFormat = format;
                return this;
            }
            
            public Builder setVideoQuality(String quality) {
                this.videoQuality = quality;
                return this;
            }
            
            public Builder setAllStreams(List<VideoStream> videoStreams, List<AudioStream> audioStreams) {
                this.allVideoStreams = videoStreams;
                this.allAudioStreams = audioStreams;
                return this;
            }
            
            public StreamData build() {
                return new StreamData(this);
            }
        }
    }
    
    /*//////////////////////////////////////////////////////////////////////////
    // Enums
    //////////////////////////////////////////////////////////////////////////*/
    
    /**
     * Quality preference enum for video streams
     */
    public enum VideoQuality {
        HIGHEST("Highest Available"),
        HIGH("High Quality"),
        MEDIUM("Medium Quality"),
        LOW("Low Quality"),
        LOWEST("Lowest Available"),
        ADAPTIVE("Adaptive Quality");
        
        private final String displayName;
        
        VideoQuality(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    /*//////////////////////////////////////////////////////////////////////////
    // Public Methods
    //////////////////////////////////////////////////////////////////////////*/
    
    /**
     * Extracts stream links from YouTube URL with default highest quality
     * 
     * @param youtubeUrl The YouTube video URL
     * @param listener Callback listener for results
     */
    public void extractStreamLink(String youtubeUrl, OnStreamLinkListener listener) {
        extractStreamLink(youtubeUrl, VideoQuality.HIGHEST, listener);
    }
    
    /**
     * Extracts stream links from YouTube URL with specified quality preference
     * 
     * @param youtubeUrl The YouTube video URL
     * @param quality Preferred video quality
     * @param listener Callback listener for results
     */
    public void extractStreamLink(String youtubeUrl, VideoQuality quality, OnStreamLinkListener listener) {
        if (listener == null) {
            Log.e(TAG, "Listener cannot be null");
            return;
        }
        
        if (!isValidYouTubeUrl(youtubeUrl)) {
            listener.onError("Invalid YouTube URL format", new IllegalArgumentException("Invalid URL"));
            return;
        }
        
        // Notify extraction started
        listener.onExtractionStarted();
        listener.onProgress("Initializing extraction...");
        
        Log.d(TAG, "Starting extraction for URL: " + youtubeUrl + " with quality: " + quality.getDisplayName());
        
        // Use ExtractorHelper for reliable extraction
        compositeDisposable.add(
            ExtractorHelper.getStreamInfo(YOUTUBE_SERVICE_ID, youtubeUrl, false)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    streamInfo -> {
                        try {
                            listener.onProgress("Processing stream information...");
                            processStreamInfo(streamInfo, quality, listener);
                        } catch (Exception e) {
                            Log.e(TAG, "Error processing stream info", e);
                            listener.onError("Error processing stream information", e);
                        }
                    },
                    throwable -> {
                        Log.e(TAG, "Failed to extract stream info", throwable);
                        String errorMessage = getErrorMessage(throwable);
                        listener.onError(errorMessage, throwable);
                    }
                )
        );
    }
    
    /**
     * Extracts only video streams without audio
     * 
     * @param youtubeUrl The YouTube video URL
     * @param quality Preferred video quality
     * @param listener Callback listener for results
     */
    public void extractVideoStreamOnly(String youtubeUrl, VideoQuality quality, OnStreamLinkListener listener) {
        extractStreamLink(youtubeUrl, quality, new OnStreamLinkListener() {
            @Override
            public void onStreamLinkExtracted(StreamData streamData) {
                // Create new StreamData with only video stream
                StreamData videoOnlyData = new StreamData.Builder()
                    .setVideoUrl(streamData.getVideoUrl())
                    .setTitle(streamData.getTitle())
                    .setUploader(streamData.getUploader())
                    .setDuration(streamData.getDuration())
                    .setThumbnailUrl(streamData.getThumbnailUrl())
                    .setVideoResolution(streamData.getVideoWidth(), streamData.getVideoHeight())
                    .setVideoFormat(streamData.getVideoFormat())
                    .setVideoQuality(streamData.getVideoQuality())
                    .setAllStreams(streamData.getAllVideoStreams(), new ArrayList<>())
                    .build();
                
                listener.onStreamLinkExtracted(videoOnlyData);
            }
            
            @Override
            public void onError(String error, Throwable throwable) {
                listener.onError(error, throwable);
            }
            
            @Override
            public void onExtractionStarted() {
                listener.onExtractionStarted();
            }
            
            @Override
            public void onProgress(String message) {
                listener.onProgress(message);
            }
        });
    }
    
    /*//////////////////////////////////////////////////////////////////////////
    // Private Methods
    //////////////////////////////////////////////////////////////////////////*/
    
    /**
     * Processes the extracted StreamInfo and builds StreamData
     */
    private void processStreamInfo(StreamInfo streamInfo, VideoQuality quality, OnStreamLinkListener listener) {
        try {
            if (streamInfo == null) {
                listener.onError("Stream information is null", new IllegalStateException("StreamInfo is null"));
                return;
            }
            
            Log.d(TAG, "Processing stream info for: " + streamInfo.getName());
            
            // Get video and audio streams
            List<VideoStream> videoStreams = streamInfo.getVideoStreams();
            List<AudioStream> audioStreams = streamInfo.getAudioStreams();
            
            // Log available streams
            Log.d(TAG, "Available video streams: " + (videoStreams != null ? videoStreams.size() : 0));
            Log.d(TAG, "Available audio streams: " + (audioStreams != null ? audioStreams.size() : 0));
            
            // Get best streams
            VideoStream bestVideoStream = getBestVideoStream(videoStreams, quality);
            AudioStream bestAudioStream = getBestAudioStream(audioStreams);
            
            // Get thumbnail URL
            String thumbnailUrl = getThumbnailUrl(streamInfo);
            
            // Determine video quality string
            String qualityString = getQualityString(bestVideoStream, quality);
            
            // Build stream data
            StreamData streamData = new StreamData.Builder()
                .setVideoUrl(bestVideoStream != null ? bestVideoStream.getUrl() : null)
                .setAudioUrl(bestAudioStream != null ? bestAudioStream.getUrl() : null)
                .setTitle(streamInfo.getName())
                .setUploader(streamInfo.getUploaderName())
                .setDuration(streamInfo.getDuration())
                .setThumbnailUrl(thumbnailUrl)
                .setVideoResolution(
                    bestVideoStream != null ? bestVideoStream.getWidth() : 0,
                    bestVideoStream != null ? bestVideoStream.getHeight() : 0
                )
                .setVideoFormat(bestVideoStream != null ? getFormatName(bestVideoStream) : null)
                .setAudioFormat(bestAudioStream != null ? getFormatName(bestAudioStream) : null)
                .setVideoQuality(qualityString)
                .setAllStreams(videoStreams, audioStreams)
                .build();
            
            // Validate that we have at least one stream
            if (streamData.getVideoUrl() == null && streamData.getAudioUrl() == null) {
                listener.onError("No playable streams found", new IllegalStateException("No streams available"));
                return;
            }
            
            listener.onStreamLinkExtracted(streamData);
            Log.d(TAG, "Stream extraction successful for: " + streamData.getTitle());
            
        } catch (Exception e) {
            Log.e(TAG, "Error processing stream info", e);
            listener.onError("Error processing stream information: " + e.getMessage(), e);
        }
    }
    
    /**
     * Gets thumbnail URL from StreamInfo
     */
    private String getThumbnailUrl(StreamInfo streamInfo) {
        try {
            if (streamInfo.getThumbnails() != null && !streamInfo.getThumbnails().isEmpty()) {
                return streamInfo.getThumbnails().get(0).getUrl();
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to get thumbnail URL: " + e.getMessage());
        }
        return null;
    }
    
    /**
     * Gets the best video stream based on quality preference
     */
    private VideoStream getBestVideoStream(List<VideoStream> videoStreams, VideoQuality quality) {
        if (videoStreams == null || videoStreams.isEmpty()) {
            Log.w(TAG, "No video streams available");
            return null;
        }
        
        // Filter out streams with null URLs
        List<VideoStream> validStreams = new ArrayList<>();
        for (VideoStream stream : videoStreams) {
            if (stream != null && stream.getUrl() != null && !stream.getUrl().isEmpty()) {
                validStreams.add(stream);
            }
        }
        
        if (validStreams.isEmpty()) {
            Log.w(TAG, "No valid video streams found");
            return null;
        }
        
        // Sort streams by resolution (highest first)
        validStreams.sort((s1, s2) -> {
            int resolution1 = s1.getWidth() * s1.getHeight();
            int resolution2 = s2.getWidth() * s2.getHeight();
            return Integer.compare(resolution2, resolution1);
        });
        
        switch (quality) {
            case HIGHEST:
                return validStreams.get(0);
            case HIGH:
                return validStreams.size() > 1 ? validStreams.get(1) : validStreams.get(0);
            case MEDIUM:
                int midIndex = validStreams.size() / 2;
                return validStreams.get(midIndex);
            case LOW:
                return validStreams.size() > 1 ? 
                    validStreams.get(validStreams.size() - 2) : validStreams.get(0);
            case LOWEST:
                return validStreams.get(validStreams.size() - 1);
            case ADAPTIVE:
                // For adaptive, prefer streams with good balance of quality and compatibility
                return getBestAdaptiveStream(validStreams);
            default:
                return validStreams.get(0);
        }
    }
    
    /**
     * Gets the best audio stream (highest bitrate)
     */
    private AudioStream getBestAudioStream(List<AudioStream> audioStreams) {
        if (audioStreams == null || audioStreams.isEmpty()) {
            Log.w(TAG, "No audio streams available");
            return null;
        }
        
        // Filter out streams with null URLs
        for (AudioStream stream : audioStreams) {
            if (stream != null && stream.getUrl() != null && !stream.getUrl().isEmpty()) {
                return stream; // Return the first valid audio stream
            }
        }
        
        Log.w(TAG, "No valid audio streams found");
        return null;
    }
    
    /**
     * Gets the best adaptive stream (balanced quality and compatibility)
     */
    private VideoStream getBestAdaptiveStream(List<VideoStream> validStreams) {
        // Look for 720p or 1080p streams as they offer good balance
        for (VideoStream stream : validStreams) {
            int height = stream.getHeight();
            if (height == 720 || height == 1080) {
                return stream;
            }
        }
        
        // If no 720p/1080p, return the middle quality stream
        int midIndex = validStreams.size() / 2;
        return validStreams.get(midIndex);
    }
    
    /**
     * Gets format name from stream
     */
    private String getFormatName(VideoStream stream) {
        try {
            return stream.getFormat() != null ? stream.getFormat().getName() : "Unknown";
        } catch (Exception e) {
            return "Unknown";
        }
    }
    
    /**
     * Gets format name from audio stream
     */
    private String getFormatName(AudioStream stream) {
        try {
            return stream.getFormat() != null ? stream.getFormat().getName() : "Unknown";
        } catch (Exception e) {
            return "Unknown";
        }
    }
    
    /**
     * Gets quality string description
     */
    private String getQualityString(VideoStream videoStream, VideoQuality quality) {
        if (videoStream == null) {
            return "Audio Only";
        }
        
        int height = videoStream.getHeight();
        if (height > 0) {
            return height + "p";
        }
        
        return quality.getDisplayName();
    }
    
    /**
     * Gets user-friendly error message
     */
    private String getErrorMessage(Throwable throwable) {
        if (throwable == null) {
            return "Unknown error occurred";
        }
        
        String message = throwable.getMessage();
        if (message == null || message.trim().isEmpty()) {
            return "Error: " + throwable.getClass().getSimpleName();
        }
        
        // Common error messages
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
            return "Video unavailable due to copyright restrictions";
        } else if (lowerMessage.contains("age")) {
            return "Video requires age verification";
        } else if (lowerMessage.contains("stream")) {
            return "Unable to extract stream URLs";
        } else {
            return "Extraction failed: " + message;
        }
    }
    
    /*//////////////////////////////////////////////////////////////////////////
    // Utility Methods
    //////////////////////////////////////////////////////////////////////////*/
    
    /**
     * Validates YouTube URL format
     */
    public boolean isValidYouTubeUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }
        
        String lowerUrl = url.toLowerCase();
        return lowerUrl.contains("youtube.com/watch") || 
               lowerUrl.contains("youtu.be/") || 
               lowerUrl.contains("youtube.com/embed/") ||
               lowerUrl.contains("youtube.com/v/") ||
               lowerUrl.contains("m.youtube.com/watch");
    }
    
    /*//////////////////////////////////////////////////////////////////////////
    // Resource Management
    //////////////////////////////////////////////////////////////////////////*/
    
    /**
     * Cleans up resources and cancels ongoing operations
     */
    public void cleanup() {
        if (compositeDisposable != null && !compositeDisposable.isDisposed()) {
            compositeDisposable.dispose();
            Log.d(TAG, "YouTube Stream Fetcher cleanup completed");
        }
    }
    
    /**
     * Checks if there are active operations
     */
    public boolean isBusy() {
        return compositeDisposable != null && compositeDisposable.size() > 0;
    }
    
    /**
     * Cancels all ongoing operations
     */
    public void cancelAll() {
        if (compositeDisposable != null) {
            compositeDisposable.clear();
        }
    }
}