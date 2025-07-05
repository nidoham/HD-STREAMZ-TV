package com.nidoham.hdstreamztv.newpipe.extractors.helper;

//import android.content.Context;
//import android.widget.Toast;
//import com.google.android.exoplayer2.ExoPlayer;
//import com.google.android.exoplayer2.MediaItem;
//import com.google.android.exoplayer2.source.MediaSource;
//import com.google.android.exoplayer2.source.MergingMediaSource;
//import com.google.android.exoplayer2.source.ProgressiveMediaSource;
//import com.google.android.exoplayer2.upstream.DefaultDataSource;
//import com.google.android.exoplayer2.upstream.DefaultHttpDataSource;
//import com.google.android.exoplayer2.util.Util;
//import com.nidoham.hdstreamztv.newpipe.extractors.YouTubeVideoExtractor;
//import org.schabi.newpipe.extractor.stream.VideoStream;
//import org.schabi.newpipe.extractor.stream.AudioStream;
//import java.util.List;
//
///**
// * Helper class to retrieve YouTube video links for ExoPlayer
// * This class simplifies the process of extracting YouTube streams and preparing them for ExoPlayer
// */
//public class YouTubeExoPlayerHelper {
//    
//    private final Context context;
//    private final YouTubeVideoExtractor extractor;
//    private ExoPlayer exoPlayer;
//    
//    /**
//     * Interface for ExoPlayer media source callbacks
//     */
//    public interface OnMediaSourceReadyListener {
//        void onMediaSourceReady(MediaSource mediaSource, YouTubeVideoExtractor.VideoInfo videoInfo);
//        void onError(String error);
//        void onProgress(String message);
//    }
//    
//    /**
//     * Interface for direct stream URL callbacks
//     */
//    public interface OnStreamUrlListener {
//        void onStreamUrlReady(String videoUrl, String audioUrl, YouTubeVideoExtractor.VideoInfo videoInfo);
//        void onError(String error);
//        void onProgress(String message);
//    }
//    
//    /**
//     * Quality preference for video streams
//     */
//    public enum VideoQuality {
//        BEST_QUALITY,
//        HIGH_QUALITY,    // 720p or higher
//        MEDIUM_QUALITY,  // 480p
//        LOW_QUALITY,     // 360p or lower
//        AUDIO_ONLY
//    }
//    
//    public YouTubeExoPlayerHelper(Context context) {
//        this.context = context;
//        this.extractor = new YouTubeVideoExtractor();
//    }
//    
//    public YouTubeExoPlayerHelper(Context context, ExoPlayer exoPlayer) {
//        this.context = context;
//        this.exoPlayer = exoPlayer;
//        this.extractor = new YouTubeVideoExtractor();
//    }
//    
//    /**
//     * Prepare MediaSource for ExoPlayer from YouTube URL
//     * @param youtubeUrl YouTube video URL
//     * @param quality Preferred video quality
//     * @param listener Callback listener
//     */
//    public void prepareMediaSource(String youtubeUrl, VideoQuality quality, OnMediaSourceReadyListener listener) {
//        if (listener == null) {
//            throw new IllegalArgumentException("Listener cannot be null");
//        }
//        
//        if (!YouTubeVideoExtractor.isValidYouTubeUrl(youtubeUrl)) {
//            listener.onError("Invalid YouTube URL");
//            return;
//        }
//        
//        listener.onProgress("Extracting video streams...");
//        
//        extractor.getVideoInfo(youtubeUrl, new YouTubeVideoExtractor.OnVideoInfoListener() {
//            @Override
//            public void onVideoInfoReceived(YouTubeVideoExtractor.VideoInfo videoInfo) {
//                try {
//                    MediaSource mediaSource = createMediaSource(videoInfo, quality);
//                    if (mediaSource != null) {
//                        listener.onMediaSourceReady(mediaSource, videoInfo);
//                    } else {
//                        listener.onError("No suitable streams found for the selected quality");
//                    }
//                } catch (Exception e) {
//                    listener.onError("Error creating media source: " + e.getMessage());
//                }
//            }
//            
//            @Override
//            public void onError(String error, Throwable throwable) {
//                listener.onError(error);
//            }
//            
//            @Override
//            public void onProgress(String message) {
//                listener.onProgress(message);
//            }
//        });
//    }
//    
//    /**
//     * Get direct stream URLs for custom implementation
//     * @param youtubeUrl YouTube video URL
//     * @param quality Preferred video quality
//     * @param listener Callback listener
//     */
//    public void getStreamUrls(String youtubeUrl, VideoQuality quality, OnStreamUrlListener listener) {
//        if (listener == null) {
//            throw new IllegalArgumentException("Listener cannot be null");
//        }
//        
//        if (!YouTubeVideoExtractor.isValidYouTubeUrl(youtubeUrl)) {
//            listener.onError("Invalid YouTube URL");
//            return;
//        }
//        
//        listener.onProgress("Extracting stream URLs...");
//        
//        extractor.getVideoInfo(youtubeUrl, new YouTubeVideoExtractor.OnVideoInfoListener() {
//            @Override
//            public void onVideoInfoReceived(YouTubeVideoExtractor.VideoInfo videoInfo) {
//                try {
//                    String[] urls = getBestStreamUrls(videoInfo, quality);
//                    if (urls != null && urls.length >= 2) {
//                        listener.onStreamUrlReady(urls[0], urls[1], videoInfo);
//                    } else {
//                        listener.onError("No suitable streams found for the selected quality");
//                    }
//                } catch (Exception e) {
//                    listener.onError("Error extracting stream URLs: " + e.getMessage());
//                }
//            }
//            
//            @Override
//            public void onError(String error, Throwable throwable) {
//                listener.onError(error);
//            }
//            
//            @Override
//            public void onProgress(String message) {
//                listener.onProgress(message);
//            }
//        });
//    }
//    
//    /**
//     * Simple method to load and play YouTube video directly in ExoPlayer
//     * @param youtubeUrl YouTube video URL
//     * @param quality Preferred video quality
//     */
//    public void loadAndPlay(String youtubeUrl, VideoQuality quality) {
//        if (exoPlayer == null) {
//            showError("ExoPlayer instance is null. Use constructor with ExoPlayer parameter.");
//            return;
//        }
//        
//        prepareMediaSource(youtubeUrl, quality, new OnMediaSourceReadyListener() {
//            @Override
//            public void onMediaSourceReady(MediaSource mediaSource, YouTubeVideoExtractor.VideoInfo videoInfo) {
//                exoPlayer.setMediaSource(mediaSource);
//                exoPlayer.prepare();
//                exoPlayer.play();
//                
//                showToast("Now playing: " + videoInfo.getTitle());
//            }
//            
//            @Override
//            public void onError(String error) {
//                showError("Failed to load video: " + error);
//            }
//            
//            @Override
//            public void onProgress(String message) {
//                showToast(message);
//            }
//        });
//    }
//    
//    /**
//     * Create MediaSource from video info
//     */
//    private MediaSource createMediaSource(YouTubeVideoExtractor.VideoInfo videoInfo, VideoQuality quality) {
//        if (quality == VideoQuality.AUDIO_ONLY) {
//            return createAudioOnlySource(videoInfo);
//        }
//        
//        String[] urls = getBestStreamUrls(videoInfo, quality);
//        if (urls == null || urls.length < 2) {
//            return null;
//        }
//        
//        String videoUrl = urls[0];
//        String audioUrl = urls[1];
//        
//        DefaultDataSource.Factory dataSourceFactory = new DefaultDataSource.Factory(
//            context,
//            new DefaultHttpDataSource.Factory()
//                .setUserAgent(Util.getUserAgent(context, "HDStreamzTV"))
//                .setConnectTimeoutMs(30000)
//                .setReadTimeoutMs(30000)
//        );
//        
//        // Create video source
//        MediaSource videoSource = new ProgressiveMediaSource.Factory(dataSourceFactory)
//            .createMediaSource(MediaItem.fromUri(videoUrl));
//        
//        // Create audio source
//        MediaSource audioSource = new ProgressiveMediaSource.Factory(dataSourceFactory)
//            .createMediaSource(MediaItem.fromUri(audioUrl));
//        
//        // Merge video and audio sources
//        return new MergingMediaSource(videoSource, audioSource);
//    }
//    
//    /**
//     * Create audio-only MediaSource
//     */
//    private MediaSource createAudioOnlySource(YouTubeVideoExtractor.VideoInfo videoInfo) {
//        AudioStream bestAudioStream = getBestAudioStream(videoInfo.getAudioStreams());
//        if (bestAudioStream == null) {
//            return null;
//        }
//        
//        DefaultDataSource.Factory dataSourceFactory = new DefaultDataSource.Factory(
//            context,
//            new DefaultHttpDataSource.Factory()
//                .setUserAgent(Util.getUserAgent(context, "HDStreamzTV"))
//        );
//        
//        return new ProgressiveMediaSource.Factory(dataSourceFactory)
//            .createMediaSource(MediaItem.fromUri(bestAudioStream.getUrl()));
//    }
//    
//    /**
//     * Get best stream URLs based on quality preference
//     * @return Array with [videoUrl, audioUrl] or null if not found
//     */
//    private String[] getBestStreamUrls(YouTubeVideoExtractor.VideoInfo videoInfo, VideoQuality quality) {
//        List<VideoStream> videoStreams = videoInfo.getVideoStreams();
//        List<AudioStream> audioStreams = videoInfo.getAudioStreams();
//        
//        if (videoStreams.isEmpty() || audioStreams.isEmpty()) {
//            return null;
//        }
//        
//        VideoStream selectedVideoStream = getBestVideoStream(videoStreams, quality);
//        AudioStream selectedAudioStream = getBestAudioStream(audioStreams);
//        
//        if (selectedVideoStream == null || selectedAudioStream == null) {
//            return null;
//        }
//        
//        return new String[]{selectedVideoStream.getUrl(), selectedAudioStream.getUrl()};
//    }
//    
//    /**
//     * Select best video stream based on quality preference
//     */
//    private VideoStream getBestVideoStream(List<VideoStream> videoStreams, VideoQuality quality) {
//        if (videoStreams.isEmpty()) {
//            return null;
//        }
//        
//        VideoStream bestStream = null;
//        
//        for (VideoStream stream : videoStreams) {
//            if (stream.getUrl() == null || stream.getUrl().isEmpty()) {
//                continue;
//            }
//            
//            switch (quality) {
//                case BEST_QUALITY:
//                    if (bestStream == null || stream.getHeight() > bestStream.getHeight()) {
//                        bestStream = stream;
//                    }
//                    break;
//                    
//                case HIGH_QUALITY:
//                    if (stream.getHeight() >= 720) {
//                        if (bestStream == null || 
//                            (stream.getHeight() < bestStream.getHeight() && bestStream.getHeight() >= 720) ||
//                            (stream.getHeight() >= 720 && bestStream.getHeight() < 720)) {
//                            bestStream = stream;
//                        }
//                    } else if (bestStream == null || bestStream.getHeight() < 720) {
//                        if (stream.getHeight() > bestStream.getHeight()) {
//                            bestStream = stream;
//                        }
//                    }
//                    break;
//                    
//                case MEDIUM_QUALITY:
//                    if (stream.getHeight() <= 480) {
//                        if (bestStream == null || 
//                            (stream.getHeight() > bestStream.getHeight() && stream.getHeight() <= 480)) {
//                            bestStream = stream;
//                        }
//                    } else if (bestStream == null || bestStream.getHeight() > 480) {
//                        bestStream = stream;
//                    }
//                    break;
//                    
//                case LOW_QUALITY:
//                    if (bestStream == null || stream.getHeight() < bestStream.getHeight()) {
//                        bestStream = stream;
//                    }
//                    break;
//            }
//        }
//        
//        return bestStream != null ? bestStream : videoStreams.get(0);
//    }
//    
//    /**
//     * Select best audio stream (highest bitrate)
//     */
//    private AudioStream getBestAudioStream(List<AudioStream> audioStreams) {
//        if (audioStreams.isEmpty()) {
//            return null;
//        }
//        
//        AudioStream bestStream = null;
//        
//        for (AudioStream stream : audioStreams) {
//            if (stream.getUrl() == null || stream.getUrl().isEmpty()) {
//                continue;
//            }
//            
//            if (bestStream == null || stream.getAverageBitrate() > bestStream.getAverageBitrate()) {
//                bestStream = stream;
//            }
//        }
//        
//        return bestStream != null ? bestStream : audioStreams.get(0);
//    }
//    
//    /**
//     * Get available quality options for a YouTube URL
//     */
//    public void getAvailableQualities(String youtubeUrl, OnQualityListListener listener) {
//        if (listener == null) {
//            throw new IllegalArgumentException("Listener cannot be null");
//        }
//        
//        extractor.getVideoInfo(youtubeUrl, new YouTubeVideoExtractor.OnVideoInfoListener() {
//            @Override
//            public void onVideoInfoReceived(YouTubeVideoExtractor.VideoInfo videoInfo) {
//                List<String> qualities = new java.util.ArrayList<>();
//                
//                for (VideoStream stream : videoInfo.getVideoStreams()) {
//                    String quality = stream.getHeight() + "p";
//                    if (!qualities.contains(quality)) {
//                        qualities.add(quality);
//                    }
//                }
//                
//                java.util.Collections.sort(qualities, (a, b) -> {
//                    int heightA = Integer.parseInt(a.replace("p", ""));
//                    int heightB = Integer.parseInt(b.replace("p", ""));
//                    return Integer.compare(heightB, heightA); // Descending order
//                });
//                
//                listener.onQualityListReceived(qualities);
//            }
//            
//            @Override
//            public void onError(String error, Throwable throwable) {
//                listener.onError(error);
//            }
//            
//            @Override
//            public void onProgress(String message) {
//                // Progress updates
//            }
//        });
//    }
//    
//    /**
//     * Interface for quality list callbacks
//     */
//    public interface OnQualityListListener {
//        void onQualityListReceived(List<String> qualities);
//        void onError(String error);
//    }
//    
//    /**
//     * Utility method to show toast messages
//     */
//    private void showToast(String message) {
//        if (context != null) {
//            Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
//        }
//    }
//    
//    /**
//     * Utility method to show error messages
//     */
//    private void showError(String error) {
//        if (context != null) {
//            Toast.makeText(context, error, Toast.LENGTH_LONG).show();
//        }
//    }
//    
//    /**
//     * Set ExoPlayer instance (if not set in constructor)
//     */
//    public void setExoPlayer(ExoPlayer exoPlayer) {
//        this.exoPlayer = exoPlayer;
//    }
//    
//    /**
//     * Get video information without preparing media source
//     */
//    public void getVideoInfo(String youtubeUrl, YouTubeVideoExtractor.OnVideoInfoListener listener) {
//        extractor.getVideoInfo(youtubeUrl, listener);
//    }
//    
//    /**
//     * Check if URL is valid YouTube URL
//     */
//    public static boolean isValidYouTubeUrl(String url) {
//        return YouTubeVideoExtractor.isValidYouTubeUrl(url);
//    }
//    
//    /**
//     * Extract video ID from YouTube URL
//     */
//    public static String extractVideoId(String url) {
//        return YouTubeVideoExtractor.extractVideoId(url);
//    }
//    
//    /**
//     * Clean up resources
//     */
//    public void cleanup() {
//        if (extractor != null) {
//            extractor.cleanup();
//        }
//    }
//    
//    /**
//     * Cancel all ongoing operations
//     */
//    public void cancelAll() {
//        if (extractor != null) {
//            extractor.cancelAll();
//        }
//    }
//    
//    /**
//     * Check if helper is currently busy
//     */
//    public boolean isBusy() {
//        return extractor != null && extractor.isBusy();
//    }
//}