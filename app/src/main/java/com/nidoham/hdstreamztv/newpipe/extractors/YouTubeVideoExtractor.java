package com.nidoham.hdstreamztv.newpipe.extractors;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.nidoham.hdstreamztv.newpipe.ExtractorHelper;

import org.schabi.newpipe.extractor.ServiceList;
import org.schabi.newpipe.extractor.stream.AudioStream;
import org.schabi.newpipe.extractor.stream.StreamInfo;
import org.schabi.newpipe.extractor.stream.VideoStream;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * A professional extractor for YouTube video data, utilizing the NewPipe Extractor library.
 * This class provides a clean, asynchronous, and callback-based API to fetch video metadata,
 * stream URLs, and other details. It is designed to be robust, efficient, and easy to integrate.
 *
 * It correctly handles various YouTube URL formats, including standard, shortened (youtu.be),
 * and embed URLs.
 *
 * <p><b>Usage:</b>
 * <pre>
 * YouTubeVideoExtractor extractor = new YouTubeVideoExtractor();
 * extractor.getVideoInfo("YOUTUBE_URL", new YouTubeVideoExtractor.OnVideoInfoListener() {
 *     @Override
 *     public void onVideoInfoReceived(VideoInfo videoInfo) {
 *         // Use videoInfo object
 *     }
 *
 *     @Override
 *     public void onError(String message, Throwable throwable) {
 *         // Handle error
 *     }
 *
 *     @Override
 *     public void onProgress(String message) {
 *         // Show progress
 *     }
 * });
 *
 * // Remember to call extractor.cleanup() when done to release resources.
 * </pre>
 * </p>
 */
public final class YouTubeVideoExtractor {

    private static final String TAG = "YouTubeVideoExtractor";

    /**
     * A robust regex pattern to extract the video ID from various YouTube URL formats.
     * Supports:
     * - youtube.com/watch?v=VIDEO_ID
     * - youtu.be/VIDEO_ID
     * - youtube.com/embed/VIDEO_ID
     * - youtube.com/v/VIDEO_ID
     * - m.youtube.com/*
     * - music.youtube.com/*
     * And various query parameters. The video ID is typically 11 characters.
     */
    private static final Pattern YOUTUBE_URL_PATTERN =
            Pattern.compile("(?:youtube(?:-nocookie)?\\.com/(?:[^/]+/.+/|(?:v|e(?:mbed)?)/|.*[?&]v=)|youtu\\.be/)([^\"&?/\\s]{11})");

    private final CompositeDisposable disposables = new CompositeDisposable();

    /*//////////////////////////////////////////////////////////////////////////
    // Callback Interfaces
    //////////////////////////////////////////////////////////////////////////*/

    /**
     * A callback interface for receiving the complete set of extracted video information.
     */
    public interface OnVideoInfoListener {
        /**
         * Called when the video information has been successfully extracted.
         * @param videoInfo An immutable data object containing all video details.
         */
        void onVideoInfoReceived(@NonNull VideoInfo videoInfo);

        /**
         * Called when an error occurs during the extraction process.
         * @param message A user-friendly error message.
         * @param throwable The underlying exception that caused the error.
         */
        void onError(@NonNull String message, @NonNull Throwable throwable);

        /**
         * Called to report progress during the extraction process.
         * @param message A message indicating the current step, e.g., "Extracting video info...".
         */
        void onProgress(@NonNull String message);
    }

    /**
     * A lightweight callback interface for receiving only the video title.
     */
    public interface OnTitleListener {
        void onTitleReceived(@NonNull String title);
        void onError(@NonNull String error);
    }

    /**
     * A callback interface for receiving the lists of available video and audio streams.
     */
    public interface OnVideoStreamsListener {
        void onVideoStreamsReceived(@NonNull List<VideoStream> videoStreams);
        void onAudioStreamsReceived(@NonNull List<AudioStream> audioStreams);
        void onError(@NonNull String error);
    }

    /*//////////////////////////////////////////////////////////////////////////
    // Data Class
    //////////////////////////////////////////////////////////////////////////*/

    /**
     * An immutable data class that encapsulates all extracted information about a video.
     * Provides safe access to properties and convenient formatting methods.
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

        /**
         * Constructs a VideoInfo object from a NewPipe {@link StreamInfo} object.
         * This constructor performs null-safe operations to prevent crashes.
         *
         * @param streamInfo The source object from the extractor.
         */
        public VideoInfo(@NonNull final StreamInfo streamInfo) {
            this.title = Objects.toString(streamInfo.getName(), "Unknown Title");
            this.uploader = Objects.toString(streamInfo.getUploaderName(), "Unknown Uploader");
            this.description = streamInfo.getDescription() != null
                    ? Objects.toString(streamInfo.getDescription().getContent(), "No description available.")
                    : "No description available.";
            this.duration = streamInfo.getDuration();
            this.viewCount = streamInfo.getViewCount();
            this.url = Objects.toString(streamInfo.getUrl(), "");

            // Safely get the highest resolution thumbnail URL
            this.thumbnailUrl = streamInfo.getThumbnails() != null && !streamInfo.getThumbnails().isEmpty()
                    ? streamInfo.getThumbnails().get(streamInfo.getThumbnails().size() - 1).getUrl()
                    : "";

            // Safely get and format the upload date
            this.uploadDate = streamInfo.getUploadDate() != null
                    ? Objects.toString(streamInfo.getUploadDate().toString(), "Unknown Date")
                    : "Unknown Date";

            this.videoStreams = streamInfo.getVideoStreams() != null ? streamInfo.getVideoStreams() : Collections.emptyList();
            this.audioStreams = streamInfo.getAudioStreams() != null ? streamInfo.getAudioStreams() : Collections.emptyList();
        }

        //<editor-fold desc="Getters">
        @NonNull public String getTitle() { return title; }
        @NonNull public String getUploader() { return uploader; }
        @NonNull public String getDescription() { return description; }
        public long getDuration() { return duration; }
        public long getViewCount() { return viewCount; }
        @NonNull public String getThumbnailUrl() { return thumbnailUrl; }
        @NonNull public String getUploadDate() { return uploadDate; }
        @NonNull public List<VideoStream> getVideoStreams() { return videoStreams; }
        @NonNull public List<AudioStream> getAudioStreams() { return audioStreams; }
        @NonNull public String getUrl() { return url; }
        //</editor-fold>

        /**
         * Formats the duration in seconds into a human-readable HH:MM:SS or MM:SS string.
         * @return The formatted duration string, or "Live" if duration is not positive.
         */
        @NonNull
        public String getFormattedDuration() {
            if (duration <= 0) return "Live";

            final long hours = duration / 3600;
            final long minutes = (duration % 3600) / 60;
            final long seconds = duration % 60;

            if (hours > 0) {
                return String.format("%d:%02d:%02d", hours, minutes, seconds);
            } else {
                return String.format("%d:%02d", minutes, seconds);
            }
        }

        /**
         * Formats the view count into a compact string with K (thousand) or M (million) suffixes.
         * @return The formatted view count string, e.g., "1.2M views", or "No views" if not available.
         */
        @NonNull
        public String getFormattedViewCount() {
            if (viewCount < 0) return "No views"; // Some streams (e.g. premieres) have -1
            if (viewCount < 1000) return viewCount + " views";
            if (viewCount < 1_000_000) return String.format("%.1fK views", viewCount / 1000.0);
            return String.format("%.1fM views", viewCount / 1_000_000.0);
        }
    }

    /*//////////////////////////////////////////////////////////////////////////
    // Public API Methods
    //////////////////////////////////////////////////////////////////////////*/

    /**
     * Asynchronously extracts all available information for a given YouTube video URL.
     * This is the primary method for fetching data. Other methods delegate to this one.
     *
     * @param youtubeUrl The full URL of the YouTube video.
     * @param listener The callback listener to handle success, error, and progress.
     */
    public void getVideoInfo(@NonNull final String youtubeUrl, @NonNull final OnVideoInfoListener listener) {
        if (!isValidYouTubeUrl(youtubeUrl)) {
            listener.onError("Invalid YouTube URL provided.", new IllegalArgumentException("URL does not match YouTube pattern: " + youtubeUrl));
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
                                        final VideoInfo videoInfo = new VideoInfo(streamInfo);
                                        listener.onVideoInfoReceived(videoInfo);
                                    } catch (final Exception e) {
                                        listener.onError("Failed to process video information.", e);
                                    }
                                },
                                throwable -> {
                                    final String errorMessage = getErrorMessage(throwable);
                                    listener.onError(errorMessage, throwable);
                                }
                        )
        );
    }

    /**
     * A convenience method to extract only the video's title.
     *
     * @param youtubeUrl The full URL of the YouTube video.
     * @param listener The callback listener for the title.
     */
    public void getVideoTitle(@NonNull final String youtubeUrl, @NonNull final OnTitleListener listener) {
        getVideoInfo(youtubeUrl, new OnVideoInfoListener() {
            @Override
            public void onVideoInfoReceived(@NonNull final VideoInfo videoInfo) {
                listener.onTitleReceived(videoInfo.getTitle());
            }

            @Override
            public void onError(@NonNull final String error, @NonNull final Throwable throwable) {
                listener.onError(error);
            }

            @Override
            public void onProgress(@NonNull final String message) {
                // Not propagated for this specific listener
            }
        });
    }

    /**
     * A convenience method to extract only the video and audio stream lists.
     *
     * @param youtubeUrl The full URL of the YouTube video.
     * @param listener The callback listener for the stream lists.
     */
    public void getVideoStreams(@NonNull final String youtubeUrl, @NonNull final OnVideoStreamsListener listener) {
        getVideoInfo(youtubeUrl, new OnVideoInfoListener() {
            @Override
            public void onVideoInfoReceived(@NonNull final VideoInfo videoInfo) {
                listener.onVideoStreamsReceived(videoInfo.getVideoStreams());
                listener.onAudioStreamsReceived(videoInfo.getAudioStreams());
            }

            @Override
            public void onError(@NonNull final String error, @NonNull final Throwable throwable) {
                listener.onError(error);
            }

            @Override
            public void onProgress(@NonNull final String message) {
                // Not propagated for this specific listener
            }
        });
    }

    /*//////////////////////////////////////////////////////////////////////////
    // Static Utility Methods
    //////////////////////////////////////////////////////////////////////////*/

    /**
     * Validates if a given URL is a recognizable YouTube video URL.
     *
     * @param url The URL to check.
     * @return {@code true} if the URL is a valid YouTube URL, {@code false} otherwise.
     */
    public static boolean isValidYouTubeUrl(@Nullable final String url) {
        return extractVideoId(url) != null;
    }

    /**
     * Extracts the 11-character video ID from a YouTube URL using a robust regex.
     *
     * @param url The YouTube URL.
     * @return The extracted video ID, or {@code null} if the URL is invalid or no ID is found.
     */
    @Nullable
    public static String extractVideoId(@Nullable final String url) {
        if (url == null || url.trim().isEmpty()) {
            return null;
        }
        final Matcher matcher = YOUTUBE_URL_PATTERN.matcher(url);
        return matcher.find() ? matcher.group(1) : null;
    }

    /*//////////////////////////////////////////////////////////////////////////
    // Resource Management
    //////////////////////////////////////////////////////////////////////////*/

    /**
     * Disposes all active subscriptions. This method should be called when the
     * extractor is no longer needed (e.g., in a Fragment's onDestroyView) to prevent memory leaks.
     */
    public void cleanup() {
        if (!disposables.isDisposed()) {
            disposables.dispose();
        }
    }

    /**
     * Checks if there are any active extraction operations.
     * @return {@code true} if the extractor is busy, {@code false} otherwise.
     */
    public boolean isBusy() {
        return disposables.size() > 0;
    }

    /**
     * Cancels all currently running extraction operations.
     * The listeners for these operations will not be called.
     */
    public void cancelAll() {
        disposables.clear();
    }

    /*//////////////////////////////////////////////////////////////////////////
    // Private Helper Methods
    //////////////////////////////////////////////////////////////////////////*/

    /**
     * Formats a {@link Throwable} into a more user-friendly error message.
     *
     * @param throwable The exception to format.
     * @return A clean, user-facing error string.
     */
    @NonNull
    private String getErrorMessage(@NonNull final Throwable throwable) {
        final String message = throwable.getMessage();
        if (message == null || message.trim().isEmpty()) {
            return "An unknown error occurred: " + throwable.getClass().getSimpleName();
        }

        final String lowerCaseMessage = message.toLowerCase();
        if (lowerCaseMessage.contains("video unavailable")) {
            return "This video is unavailable.";
        } else if (lowerCaseMessage.contains("private video")) {
            return "This is a private video.";
        } else if (lowerCaseMessage.contains("404") || lowerCaseMessage.contains("not found")) {
            return "Video not found. It may have been deleted.";
        } else if (lowerCaseMessage.contains("network") || lowerCaseMessage.contains("connection") || lowerCaseMessage.contains("no address associated")) {
            return "Please check your network connection.";
        } else if (lowerCaseMessage.contains("timeout")) {
            return "The request timed out. Please try again.";
        } else if (lowerCaseMessage.contains("blocked") || lowerCaseMessage.contains("restricted")) {
            return "This video is not available in your country.";
        } else if (lowerCaseMessage.contains("copyright")) {
            return "This video is unavailable due to a copyright claim.";
        } else if (lowerCaseMessage.contains("age-restricted")) {
            return "This video is age-restricted and requires login to view.";
        }

        return "An error occurred: " + message;
    }
}