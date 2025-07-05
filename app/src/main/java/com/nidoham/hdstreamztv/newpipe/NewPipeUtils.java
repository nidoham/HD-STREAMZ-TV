//package com.nidoham.hdstreamztv.newpipe;
//
//import android.os.Handler;
//import android.os.Looper;
//import android.util.Log;
//
//import org.schabi.newpipe.extractor.InfoItem;
//import org.schabi.newpipe.extractor.ListExtractor;
//import org.schabi.newpipe.extractor.NewPipe;
//import org.schabi.newpipe.extractor.SearchExtractor;
//import org.schabi.newpipe.extractor.ServiceList;
//import org.schabi.newpipe.extractor.StreamingService;
//import org.schabi.newpipe.extractor.channel.ChannelInfo;
//import org.schabi.newpipe.extractor.channel.ChannelInfoItem;
//import org.schabi.newpipe.extractor.downloader.Downloader;
//import org.schabi.newpipe.extractor.downloader.okhttp.OkHttpDownloader;
//import org.schabi.newpipe.extractor.kiosk.KioskList;
//import org.schabi.newpipe.extractor.playlist.PlaylistInfo;
//import org.schabi.newpipe.extractor.playlist.PlaylistInfoItem;
//import org.schabi.newpipe.extractor.search.SearchQueryHandler;
//import org.schabi.newpipe.extractor.search.SearchQueryHandlerFactory;
//import org.schabi.newpipe.extractor.stream.AudioStream;
//import org.schabi.newpipe.extractor.stream.StreamInfo;
//import org.schabi.newpipe.extractor.stream.StreamInfoItem;
//import org.schabi.newpipe.extractor.stream.StreamType;
//import org.schabi.newpipe.extractor.stream.VideoStream;
//
//import java.util.ArrayList;
//import java.util.Collections;
//import java.util.List;
//import java.util.concurrent.CompletableFuture;
//import java.util.concurrent.ExecutorService;
//import java.util.concurrent.Executors;
//
//public class NewPipeUtils {
//    private static final String TAG = "NewPipeUtils";
//    private static final ExecutorService executor = Executors.newCachedThreadPool();
//    private static final Handler mainHandler = new Handler(Looper.getMainLooper());
//
//    /**
//     * Initializes the NewPipe extractor. This MUST be called once before using any
//     * other methods in this class, typically in your Application's onCreate().
//     */
//    public static void init() {
//        // You can also provide your own custom Downloader implementation
//        init(OkHttpDownloader.getInstance());
//    }
//
//    public static void init(Downloader downloader) {
//        NewPipe.init(downloader);
//    }
//
//    // Callback interfaces
//    public interface OnVideoInfoListener {
//        void onSuccess(VideoInfo videoInfo);
//        void onError(Exception error);
//    }
//
//    public interface OnChannelInfoListener {
//        void onSuccess(ChannelInfo channelInfo);
//        void onError(Exception error);
//    }
//
//    public interface OnPlaylistInfoListener {
//        void onSuccess(PlaylistInfo playlistInfo);
//        void onError(Exception error);
//    }
//
//    public interface OnSearchResultListener {
//        void onSuccess(List<SearchResult> results);
//        void onError(Exception error);
//    }
//
//    public interface OnSuggestionListener {
//        void onSuccess(List<String> suggestions);
//        void onError(Exception error);
//    }
//
//    // Data classes
//    public static class VideoInfo {
//        public final String title;
//        public final String uploader;
//        public final String uploaderUrl;
//        public final String thumbnailUrl;
//        public final long duration;
//        public final long viewCount;
//        public final String description;
//        public final String uploadDate;
//        public final List<VideoStream> videoStreams;
//        public final List<AudioStream> audioStreams;
//        public final List<InfoItem> relatedStreams;
//        public final String category;
//        public final List<String> tags;
//        public final boolean isLiveStream;
//        public final String hlsUrl;
//        public final String dashMpdUrl;
//
//        public VideoInfo(StreamInfo streamInfo) {
//            this.title = streamInfo.getName();
//            this.uploader = streamInfo.getUploaderName();
//            this.uploaderUrl = streamInfo.getUploaderUrl();
//            this.thumbnailUrl = streamInfo.getThumbnailUrl();
//            this.duration = streamInfo.getDuration();
//            this.viewCount = streamInfo.getViewCount();
//            this.description = streamInfo.getDescription() != null ?
//                streamInfo.getDescription().getContent() : "";
//            this.uploadDate = streamInfo.getUploadDate() != null ?
//                streamInfo.getUploadDate().offsetDateTime().toString() : "";
//            this.videoStreams = streamInfo.getVideoStreams();
//            this.audioStreams = streamInfo.getAudioStreams();
//            this.relatedStreams = new ArrayList<>(streamInfo.getRelatedStreams());
//            this.category = streamInfo.getCategory();
//            this.tags = streamInfo.getTags();
//            this.isLiveStream = streamInfo.getStreamType() == StreamType.LIVE_STREAM;
//            this.hlsUrl = streamInfo.getHlsUrl();
//            this.dashMpdUrl = streamInfo.getDashMpdUrl();
//        }
//    }
//
//    public static class SearchResult {
//        public final String title;
//        public final String url;
//        public final String thumbnailUrl;
//        public final String uploader;
//        public final String uploaderUrl;
//        public final String duration;
//        public final long viewCount;
//        public final String uploadDate;
//        public final InfoItem.InfoType type;
//
//        public SearchResult(InfoItem item) {
//            this.title = item.getName();
//            this.url = item.getUrl();
//            this.thumbnailUrl = item.getThumbnailUrl();
//            this.type = item.getInfoType();
//
//            if (item instanceof StreamInfoItem) {
//                StreamInfoItem streamItem = (StreamInfoItem) item;
//                this.uploader = streamItem.getUploaderName();
//                this.uploaderUrl = streamItem.getUploaderUrl();
//                this.duration = formatDuration(streamItem.getDuration());
//                this.viewCount = streamItem.getViewCount();
//                this.uploadDate = streamItem.getUploadDate() != null ?
//                    streamItem.getUploadDate().offsetDateTime().toString() : "";
//            } else if (item instanceof ChannelInfoItem) {
//                ChannelInfoItem channelItem = (ChannelInfoItem) item;
//                this.uploader = channelItem.getName();
//                this.uploaderUrl = channelItem.getUrl();
//                this.duration = "";
//                this.viewCount = channelItem.getSubscriberCount();
//                this.uploadDate = "";
//            } else if (item instanceof PlaylistInfoItem) {
//                PlaylistInfoItem playlistItem = (PlaylistInfoItem) item;
//                this.uploader = playlistItem.getUploaderName();
//                this.uploaderUrl = playlistItem.getUploaderUrl();
//                this.duration = String.valueOf(playlistItem.getStreamCount()); // Duration field used for stream count
//                this.viewCount = playlistItem.getStreamCount();
//                this.uploadDate = "";
//            } else {
//                this.uploader = "";
//                this.uploaderUrl = "";
//                this.duration = "";
//                this.viewCount = 0;
//                this.uploadDate = "";
//            }
//        }
//    }
//
//    // Video Information Extraction
//    public static void fetchVideoInfo(String url, OnVideoInfoListener listener) {
//        executeAsync(() -> {
//            StreamInfo info = StreamInfo.getInfo(url);
//            return new VideoInfo(info);
//        }, listener::onSuccess, listener::onError);
//    }
//
//    // Channel Information Extraction
//    public static void fetchChannelInfo(String channelUrl, OnChannelInfoListener listener) {
//        executeAsync(() -> ChannelInfo.getInfo(channelUrl), listener::onSuccess, listener::onError);
//    }
//
//    // Playlist Information Extraction
//    public static void fetchPlaylistInfo(String playlistUrl, OnPlaylistInfoListener listener) {
//        executeAsync(() -> PlaylistInfo.getInfo(playlistUrl), listener::onSuccess, listener::onError);
//    }
//
//    // Search Functionality
//    public static void searchContent(String query, OnSearchResultListener listener) {
//        searchContent(query, Collections.emptyList(), listener);
//    }
//
//    // Search with content filter
//    public static void searchContent(String query, List<String> contentFilters, OnSearchResultListener listener) {
//        executeAsync(() -> {
//            StreamingService service = ServiceList.YouTube; // Or make this a parameter
//            SearchQueryHandler queryHandler = service.getSearchQueryHandlerFactory().fromQuery(query, contentFilters, "");
//            
//            SearchExtractor extractor = service.getSearchExtractor(queryHandler);
//            extractor.fetchPage();
//
//            List<SearchResult> results = new ArrayList<>();
//            for (InfoItem item : extractor.getInitialPage().getItems()) {
//                results.add(new SearchResult(item));
//            }
//            return results;
//        }, listener::onSuccess, listener::onError);
//    }
//
//    // Search Suggestions
//    public static void getSearchSuggestions(String query, OnSuggestionListener listener) {
//        executeAsync(() -> {
//            StreamingService service = ServiceList.YouTube;
//            return service.getSuggestionExtractor().suggestionList(query);
//        }, listener::onSuccess, listener::onError);
//    }
//
//    // Get trending videos
//    public static void getTrendingVideos(OnSearchResultListener listener) {
//        executeAsync(() -> {
//            StreamingService service = ServiceList.YouTube;
//            KioskList kioskList = service.getKioskList();
//            String trendingId = kioskList.getDefaultKioskId();
//
//            ListExtractor kioskExtractor = kioskList.getExtractor(trendingId);
//            kioskExtractor.fetchPage();
//
//            List<SearchResult> results = new ArrayList<>();
//            for (InfoItem item : kioskExtractor.getInitialPage().getItems()) {
//                if (item instanceof StreamInfoItem) {
//                    results.add(new SearchResult(item));
//                }
//            }
//            return results;
//        }, listener::onSuccess, listener::onError);
//    }
//
//    // Quality selection helpers
//    public static VideoStream getBestVideoStream(List<VideoStream> streams) {
//        if (streams == null || streams.isEmpty()) return null;
//        
//        return streams.stream()
//            .filter(stream -> stream.getResolution() != null && !stream.getResolution().isEmpty())
//            .max((s1, s2) -> {
//                int res1 = parseResolution(s1.getResolution());
//                int res2 = parseResolution(s2.getResolution());
//                return Integer.compare(res1, res2);
//            })
//            .orElse(streams.get(0));
//    }
//
//    public static AudioStream getBestAudioStream(List<AudioStream> streams) {
//        if (streams == null || streams.isEmpty()) return null;
//        
//        return streams.stream()
//            .max((s1, s2) -> Integer.compare(s1.getAverageBitrate(), s2.getAverageBitrate()))
//            .orElse(streams.get(0));
//    }
//
//    // Synchronous methods
//    public static VideoInfo fetchVideoInfoSync(String url) throws Exception {
//        StreamInfo info = StreamInfo.getInfo(url);
//        return new VideoInfo(info);
//    }
//
//    public static List<SearchResult> searchContentSync(String query) throws Exception {
//        StreamingService service = ServiceList.YouTube;
//        SearchQueryHandler queryHandler = service.getSearchQueryHandlerFactory().fromQuery(query);
//        SearchExtractor extractor = service.getSearchExtractor(queryHandler);
//        extractor.fetchPage();
//
//        List<SearchResult> results = new ArrayList<>();
//        for (InfoItem item : extractor.getInitialPage().getItems()) {
//            results.add(new SearchResult(item));
//        }
//        return results;
//    }
//
//    // Utility Methods
//    private static String formatDuration(long seconds) {
//        if (seconds < 0) return "Live";
//        if (seconds == 0) return "0:00";
//        
//        long hours = seconds / 3600;
//        long minutes = (seconds % 3600) / 60;
//        long secs = seconds % 60;
//        
//        if (hours > 0) {
//            return String.format("%d:%02d:%02d", hours, minutes, secs);
//        } else {
//            return String.format("%d:%02d", minutes, secs);
//        }
//    }
//
//    private static int parseResolution(String resolution) {
//        try {
//            return Integer.parseInt(resolution.replaceAll("\\D+", ""));
//        } catch (NumberFormatException e) {
//            return 0;
//        }
//    }
//
//    private static <T> void executeAsync(SupplierWithException<T> supplier,
//                                       java.util.function.Consumer<T> onSuccess,
//                                       java.util.function.Consumer<Exception> onError) {
//        CompletableFuture.supplyAsync(() -> {
//            try {
//                return supplier.get();
//            } catch (Exception e) {
//                Log.e(TAG, "Async execution error", e);
//                throw new RuntimeException(e); // Wrap checked exceptions
//            }
//        }, executor).whenComplete((result, throwable) -> {
//            mainHandler.post(() -> {
//                if (throwable != null) {
//                    // Unwrap the CompletionException/RuntimeException to get the original cause
//                    Throwable cause = throwable.getCause();
//                    if (cause instanceof Exception) {
//                        onError.accept((Exception) cause);
//                    } else {
//                        onError.accept(new Exception(throwable)); // Fallback
//                    }
//                } else {
//                    onSuccess.accept(result);
//                }
//            });
//        });
//    }
//
//    @FunctionalInterface
//    private interface SupplierWithException<T> {
//        T get() throws Exception;
//    }
//
//    // Cleanup
//    public static void cleanup() {
//        if (executor != null && !executor.isShutdown()) {
//            executor.shutdown();
//        }
//    }
//}