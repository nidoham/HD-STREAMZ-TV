package com.nidoham.hdstreamztv.newpipe.extractors.helper;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.nidoham.hdstreamztv.newpipe.ExtractorHelper;

import org.schabi.newpipe.extractor.ServiceList;
import org.schabi.newpipe.extractor.stream.AudioStream;
import org.schabi.newpipe.extractor.stream.StreamInfo;
import org.schabi.newpipe.extractor.stream.VideoStream;

import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public final class YouTubeStreamLinkFetcher {

    private static final String TAG = "YouTubeStreamFetcher";
    private static final int YOUTUBE_SERVICE_ID = ServiceList.YouTube.getServiceId();

    private static final Pattern YOUTUBE_URL_PATTERN =
            Pattern.compile("(?:youtube(?:-nocookie)?\\.com/(?:[^/]+/.+/|(?:v|e(?:mbed)?)/|.*[?&]v=)|youtu\\.be/)([^\"&?/\\s]{11})");

    private final CompositeDisposable compositeDisposable;

    public YouTubeStreamLinkFetcher() {
        this.compositeDisposable = new CompositeDisposable();
    }

    public interface OnStreamLinkListener {
        void onStreamLinkExtracted(@NonNull StreamData streamData);
        void onError(@NonNull String error, @NonNull Throwable throwable);
    }

    public static final class StreamData {
        @Nullable public final String videoUrl;
        @Nullable public final String audioUrl;
        public final boolean isDashStream;
        @NonNull public final String title;
        @NonNull public final String videoQuality;

        private StreamData(@Nullable String videoUrl, @Nullable String audioUrl, boolean isDashStream, @NonNull String title, @NonNull String videoQuality) {
            this.videoUrl = videoUrl;
            this.audioUrl = audioUrl;
            this.isDashStream = isDashStream;
            this.title = title;
            this.videoQuality = videoQuality;
        }
    }

    public enum VideoQuality {
        BEST, HD_1080P, HD_720P, SD_480P, SD_360P, LOWEST
    }

    public void extractStreamLink(@NonNull String youtubeUrl, @NonNull VideoQuality quality, @NonNull OnStreamLinkListener listener) {
        if (!isValidYouTubeUrl(youtubeUrl)) {
            listener.onError("Invalid YouTube URL format.", new IllegalArgumentException("Invalid URL: " + youtubeUrl));
            return;
        }

        Log.d(TAG, "Starting extraction for URL: " + youtubeUrl + " with quality: " + quality);

        compositeDisposable.add(
                ExtractorHelper.getStreamInfo(YOUTUBE_SERVICE_ID, youtubeUrl, false)
                        .subscribeOn(Schedulers.io())
                        .map(streamInfo -> processStreamInfo(streamInfo, quality))
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                listener::onStreamLinkExtracted,
                                throwable -> {
                                    Log.e(TAG, "Failed to extract or process stream info.", throwable);
                                    String errorMessage = getErrorMessage(throwable);
                                    listener.onError(errorMessage, throwable);
                                }
                        )
        );
    }

    @NonNull
    private StreamData processStreamInfo(@NonNull StreamInfo streamInfo, @NonNull VideoQuality quality) throws Exception {
        List<VideoStream> progressiveStreams = streamInfo.getVideoStreams().stream()
                .filter(s -> !s.isVideoOnly() && s.getUrl() != null && !s.getUrl().isEmpty())
                .sorted(Comparator.comparingInt(VideoStream::getHeight).reversed())
                .collect(Collectors.toList());

        if (!progressiveStreams.isEmpty()) {
            VideoStream bestStream = selectBestStream(progressiveStreams, quality);
            return new StreamData(bestStream.getUrl(), null, false, streamInfo.getName(), bestStream.getResolution());
        }

        List<VideoStream> videoOnlyStreams = streamInfo.getVideoStreams().stream()
                .filter(s -> s.isVideoOnly() && s.getUrl() != null && !s.getUrl().isEmpty())
                .sorted(Comparator.comparingInt(VideoStream::getHeight).reversed())
                .collect(Collectors.toList());

        List<AudioStream> audioStreams = streamInfo.getAudioStreams().stream()
                .filter(s -> s.getUrl() != null && !s.getUrl().isEmpty())
                .sorted(Comparator.comparingInt(AudioStream::getAverageBitrate).reversed())
                .collect(Collectors.toList());

        if (videoOnlyStreams.isEmpty() || audioStreams.isEmpty()) {
            throw new Exception("No playable DASH streams found.");
        }

        VideoStream bestVideoStream = selectBestStream(videoOnlyStreams, quality);
        AudioStream bestAudioStream = audioStreams.get(0);

        return new StreamData(bestVideoStream.getUrl(), bestAudioStream.getUrl(), true, streamInfo.getName(), bestVideoStream.getResolution());
    }

    private VideoStream selectBestStream(@NonNull List<VideoStream> streams, @NonNull VideoQuality quality) {
        if (streams.isEmpty()) {
            throw new IllegalStateException("Cannot select from an empty stream list.");
        }
        switch (quality) {
            case HD_1080P: return streams.stream().filter(s -> s.getHeight() >= 1080).findFirst().orElse(streams.get(0));
            case HD_720P: return streams.stream().filter(s -> s.getHeight() >= 720).findFirst().orElse(streams.get(0));
            case SD_480P: return streams.stream().filter(s -> s.getHeight() >= 480).findFirst().orElse(streams.get(0));
            case SD_360P: return streams.stream().filter(s -> s.getHeight() >= 360).findFirst().orElse(streams.get(0));
            case LOWEST: return streams.get(streams.size() - 1);
            case BEST:
            default: return streams.get(0);
        }
    }

    @Nullable
    public static String extractVideoId(@Nullable final String url) {
        if (url == null || url.trim().isEmpty()) {
            return null;
        }
        final Matcher matcher = YOUTUBE_URL_PATTERN.matcher(url);
        return matcher.find() ? matcher.group(1) : null;
    }

    public static boolean isValidYouTubeUrl(@Nullable final String url) {
        return extractVideoId(url) != null;
    }

    @NonNull
    private String getErrorMessage(@NonNull final Throwable throwable) {
        final String message = throwable.getMessage();
        if (message == null || message.trim().isEmpty()) return "An unknown error occurred: " + throwable.getClass().getSimpleName();
        String lowerCaseMessage = message.toLowerCase();
        if (lowerCaseMessage.contains("video unavailable")) return "This video is unavailable.";
        if (lowerCaseMessage.contains("private video")) return "This is a private video.";
        if (lowerCaseMessage.contains("404")) return "Video not found (404).";
        if (lowerCaseMessage.contains("network") || lowerCaseMessage.contains("connection")) return "Please check your network connection.";
        if (lowerCaseMessage.contains("timeout")) return "The request timed out.";
        if (lowerCaseMessage.contains("blocked") || lowerCaseMessage.contains("restricted")) return "This video is not available in your country.";
        return "Extraction failed: " + message;
    }

    public void cleanup() {
        if (!compositeDisposable.isDisposed()) {
            compositeDisposable.dispose();
            Log.d(TAG, "YouTube Stream Fetcher cleanup completed.");
        }
    }

    public boolean isBusy() {
        return compositeDisposable.size() > 0;
    }
}