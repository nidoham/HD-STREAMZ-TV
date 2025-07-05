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

public class YouTubeVideoExtractor {
    
    private CompositeDisposable disposables = new CompositeDisposable();
    
    /**
     * Interface for video information callbacks
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
    
    /**
     * Data class to hold video information
     */
    public static class VideoInfo {
        private String title;
        private String uploader;
        private String description;
        private long duration;
        private long viewCount;
        private String thumbnailUrl;
        private String uploadDate;
        private List<VideoStream> videoStreams;
        private List<AudioStream> audioStreams;
        private String url;
        
        public VideoInfo(StreamInfo streamInfo) {
            this.title = streamInfo.getName();
            this.uploader = streamInfo.getUploaderName();
            this.description = streamInfo.getDescription().getContent();
            this.duration = streamInfo.getDuration();
            this.viewCount = streamInfo.getViewCount();
            
            // Fix: Use getThumbnails() instead of getThumbnailUrl()
            this.thumbnailUrl = streamInfo.getThumbnails().isEmpty() ? 
                               "" : streamInfo.getThumbnails().get(0).getUrl();
            
            this.uploadDate = streamInfo.getUploadDate() != null ? 
                             streamInfo.getUploadDate().date().toString() : "";
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
    
    /**
     * Extract complete video information
     * @param youtubeUrl YouTube video URL
     * @param listener Callback listener
     */
    public void getVideoInfo(String youtubeUrl, OnVideoInfoListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("Listener cannot be null");
        }
        
        if (youtubeUrl == null || youtubeUrl.trim().isEmpty()) {
            listener.onError("Invalid URL", new IllegalArgumentException("URL cannot be empty"));
            return;
        }
        
        int serviceId = ServiceList.YouTube.getServiceId();
        
        // Notify progress
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
     * Extract only video title (simple method)
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
                // Progress updates for title extraction
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
                // Progress updates for stream extraction
            }
        });
    }
    
    /**
     * Legacy method for backward compatibility
     * @param youtubeUrl YouTube video URL
     */
    public void getVideoTitle(String youtubeUrl) {
        getVideoTitle(youtubeUrl, new OnTitleListener() {
            @Override
            public void onTitleReceived(String title) {
                onTitleReceived(title);
            }
            
            @Override
            public void onError(String error) {
                // Fix: Pass the error string directly instead of creating RuntimeException
                onError(error);
            }
        });
    }
    
    /**
     * Check if URL is valid YouTube URL
     * @param url URL to check
     * @return true if valid YouTube URL
     */
    public static boolean isValidYouTubeUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }
        
        return url.contains("youtube.com/watch") || 
               url.contains("youtu.be/") || 
               url.contains("youtube.com/v/") ||
               url.contains("youtube.com/embed/");
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
        
        return videoId;
    }
    
    private String getErrorMessage(Throwable throwable) {
        if (throwable == null) {
            return "Unknown error occurred";
        }
        
        String message = throwable.getMessage();
        if (message == null || message.trim().isEmpty()) {
            return "Error: " + throwable.getClass().getSimpleName();
        }
        
        // Common error messages
        if (message.contains("Video unavailable")) {
            return "Video is unavailable or private";
        } else if (message.contains("network")) {
            return "Network connection error";
        } else if (message.contains("timeout")) {
            return "Request timeout - please try again";
        } else if (message.contains("blocked")) {
            return "Video is blocked in your region";
        } else {
            return "Error: " + message;
        }
    }
    
    // Legacy methods for backward compatibility
    private void onTitleReceived(String title) {
        if (App.getContext() != null) {
            Toast.makeText(App.getContext(), title, Toast.LENGTH_SHORT).show();
        }
        System.out.println("Video Title: " + title);
    }
    
    // Fix: Change parameter type from Throwable to String
    private void onError(String error) {
        if (App.getContext() != null) {
            Toast.makeText(App.getContext(), error, Toast.LENGTH_LONG).show();
        }
        System.err.println("Error getting video info: " + error);
    }
    
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
}