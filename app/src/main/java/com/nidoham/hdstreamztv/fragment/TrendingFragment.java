package com.nidoham.hdstreamztv.fragment;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import bd.nidoham.intent.IntentKeys;
import bd.nidoham.youtube.home.TrendingVideosExecutor;
import com.nidoham.hdstreamztv.App;
import com.nidoham.hdstreamztv.PlayerActivity;
import com.nidoham.hdstreamztv.R;
import com.nidoham.hdstreamztv.adapter.VideoAdapter;
import com.nidoham.hdstreamztv.databinding.FragmentTrendingBinding;
import com.nidoham.hdstreamztv.model.VideoItem;
import com.nidoham.hdstreamztv.model.VideoQuality;
import com.nidoham.hdstreamztv.util.NetworkUtils;

import org.schabi.newpipe.extractor.Image;
import org.schabi.newpipe.extractor.ServiceList;
import org.schabi.newpipe.extractor.stream.StreamInfo;
import org.schabi.newpipe.extractor.stream.StreamInfoItem;
import org.schabi.newpipe.extractor.stream.VideoStream;
import org.schabi.newpipe.util.ExtractorHelper;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TrendingFragment extends Fragment implements VideoAdapter.OnVideoItemClickListener, TrendingVideosExecutor.Listener {

    private static final String TAG = "TrendingFragment";

    // View Binding object to safely access views
    private FragmentTrendingBinding binding;

    // UI Components
    private VideoAdapter videoAdapter;

    // Data & Logic Components
    private TrendingVideosExecutor executor;
    private final CompositeDisposable compositeDisposable = new CompositeDisposable();

    // State Management
    private final ArrayList<VideoItem> videoList = new ArrayList<>();
    private boolean isLoading = false;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            // Check if NewPipe is initialized by the App class
            if (!App.getInstance().isNewPipeInitialized()) {
                App.getInstance().reinitializeNewPipe();
            }
            // Initialize TrendingVideosExecutor with the listener (this Fragment)
            executor = new TrendingVideosExecutor(requireContext(), this);
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize executor", e);
            showError("Failed to initialize video service");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the layout using View Binding
        binding = FragmentTrendingBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupRecyclerView();

        if (executor == null) {
            showError("Service is not available. Please restart the app.");
            return;
        }

        // Load data on view creation since state is not saved
        loadInitialVideos(false);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (executor != null) {
            executor.dispose(); // Clean up executor subscriptions
        }
        compositeDisposable.clear(); // Clean up RxJava subscriptions
        binding = null;
    }

    private void setupRecyclerView() {
        videoAdapter = new VideoAdapter(videoList, this);
        binding.recyclerViewTrending.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerViewTrending.setAdapter(videoAdapter);

        // Setup swipe-to-refresh listener
        binding.swipeRefreshLayout.setOnRefreshListener(() -> loadInitialVideos(true));

        binding.recyclerViewTrending.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (dy > 0) {
                    LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                    if (layoutManager != null && layoutManager.findLastCompletelyVisibleItemPosition() >= videoAdapter.getItemCount() - 2) {
                        loadMoreVideos();
                    }
                }
            }
        });
    }

    private void loadInitialVideos(boolean forceRefresh) {
        if (isLoading || !isNetworkAvailable()) return;
        executor.fetchTrendingVideos(forceRefresh);
    }

    private void loadMoreVideos() {
        if (isLoading || !isNetworkAvailable()) return;
        executor.fetchMoreVideos();
    }

    @Override
    public void showLoading(boolean isLoading) {
        this.isLoading = isLoading;
        if (binding == null) return;
        binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        binding.recyclerViewTrending.setVisibility(isLoading ? View.GONE : View.VISIBLE);
        binding.swipeRefreshLayout.setRefreshing(false);
    }

    @Override
    public void showInitialVideos(List<StreamInfoItem> items) {
        videoList.clear();
        videoList.addAll(processItems(items));
        videoAdapter.notifyDataSetChanged();

        if (videoList.isEmpty()) {
            showEmptyState();
        } else {
            showContentState();
        }
    }

    @Override
    public void showMoreVideos(List<StreamInfoItem> items) {
        int startPosition = videoList.size();
        List<VideoItem> newItems = processItems(items);
        if (!newItems.isEmpty()) {
            videoList.addAll(newItems);
            videoAdapter.notifyItemRangeInserted(startPosition, newItems.size());
        }
        showContentState();
    }

    @Override
    public void showEmptyState() {
        if (binding == null) return;
        binding.swipeRefreshLayout.setRefreshing(false);
        binding.progressBar.setVisibility(View.GONE);
        binding.recyclerViewTrending.setVisibility(View.GONE);
        showToast("No videos available");
    }

    @Override
    public void showError(String message) {
        if (binding == null) return;
        binding.swipeRefreshLayout.setRefreshing(false);
        binding.progressBar.setVisibility(View.GONE);
        if (videoList.isEmpty()) {
            binding.recyclerViewTrending.setVisibility(View.GONE);
            showToast(message, Toast.LENGTH_LONG);
        } else {
            binding.recyclerViewTrending.setVisibility(View.VISIBLE);
            showToast(message, Toast.LENGTH_SHORT);
        }
    }

    private void showContentState() {
        if (binding == null) return;
        binding.swipeRefreshLayout.setRefreshing(false);
        binding.progressBar.setVisibility(View.GONE);
        binding.recyclerViewTrending.setVisibility(View.VISIBLE);
    }

    private List<VideoItem> processItems(List<StreamInfoItem> items) {
        List<VideoItem> processedVideos = new ArrayList<>();
        if (items == null) return processedVideos;

        for (StreamInfoItem item : items) {
            String title = item.getName() != null ? item.getName() : "";
            String uploader = item.getUploaderName() != null ? item.getUploaderName() : "";
            String thumbnailUrl = "";
            List<Image> thumbnails = item.getThumbnails();
            if (thumbnails != null && !thumbnails.isEmpty()) {
                thumbnailUrl = thumbnails.get(0).getUrl() != null ? thumbnails.get(0).getUrl() : "";
            }
            String videoUrl = item.getUrl() != null ? item.getUrl() : "";
            long duration = item.getDuration();
            long viewCount = item.getViewCount();

            processedVideos.add(new VideoItem(title, uploader, thumbnailUrl, videoUrl, duration, viewCount));
        }
        return processedVideos;
    }

    private void showToast(String message) {
        showToast(message, Toast.LENGTH_SHORT);
    }

    private void showToast(String message, int duration) {
        Context context = getContext();
        if (context != null) {
            Toast.makeText(context, message, duration).show();
        }
    }

    @Override
    public void onVideoItemClick(@NonNull VideoItem videoItem) {
        handleStreamExtraction(videoItem.getVideoUrl());
    }

    private void handleStreamExtraction(final String url) {
        if (!isAdded() || getContext() == null) {
            Log.w(TAG, "Fragment not active or context is null");
            return;
        }

        if (url == null || url.trim().isEmpty()) {
            showToast("Invalid video URL");
            return;
        }

        Log.d(TAG, "Starting stream extraction for URL: " + url);

        int serviceId = ServiceList.YouTube.getServiceId();

        Disposable disposable = ExtractorHelper.getStreamInfo(serviceId, url, true)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        this::handleStreamInfoReceived,
                        throwable -> {
                            Log.e(TAG, "Failed to extract stream info", throwable);
                            if (isAdded() && getContext() != null) {
                                String errorMessage = "Error: Unable to extract video info. ";
                                if (throwable.getMessage() != null) {
                                    errorMessage += throwable.getMessage();
                                } else {
                                    errorMessage += "Please ensure your app is updated or try a different video.";
                                }
                                showToast(errorMessage, Toast.LENGTH_LONG);
                            }
                        }
                );

        compositeDisposable.add(disposable);
    }

    private void handleStreamInfoReceived(StreamInfo streamInfo) {
        Context context = getContext();
        if (context == null) return;

        if (streamInfo == null) {
            showToast("Stream info is null. This video might not be supported.");
            return;
        }

        List<VideoStream> videoStreams = streamInfo.getVideoStreams();
        if (videoStreams == null || videoStreams.isEmpty()) {
            showToast("No video streams available for this video.");
            return;
        }

        // Create a map to store all quality streams
        Map<String, String> qualityUrlMap = new LinkedHashMap<>();
        
        // Store all video qualities and their URLs
        for (VideoStream stream : videoStreams) {
            String quality = stream.getResolution();
            String streamUrl = stream.getUrl();
            if (quality != null && !quality.isEmpty() && streamUrl != null && !streamUrl.isEmpty()) {
                qualityUrlMap.put(quality, streamUrl);
            }
        }

        if (qualityUrlMap.isEmpty()) {
            showToast("No valid quality options available.");
            return;
        }

        // Create intent and pass video information
        Intent intent = new Intent(context, PlayerActivity.class);
        
        // Basic video information
        String videoTitle = streamInfo.getName() != null ? streamInfo.getName() : "Unknown Title";
        intent.putExtra(IntentKeys.EXTRA_VIDEO_NAME, videoTitle);
        intent.putExtra(IntentKeys.EXTRA_VIDEO_CATEGORY, IntentKeys.EXTRA_KIOSK_YOUTUBE);

        // Convert quality map to ArrayList of VideoQuality objects
        ArrayList<VideoQuality> videoQualities = new ArrayList<>();
        for (Map.Entry<String, String> entry : qualityUrlMap.entrySet()) {
            videoQualities.add(new VideoQuality(entry.getKey(), entry.getValue()));
        }
        
        // Add HLS stream if available
        String hlsUrl = streamInfo.getHlsUrl();
        if (hlsUrl != null && !hlsUrl.isEmpty()) {
            intent.putExtra(IntentKeys.EXTRA_HLS_URL, hlsUrl);
        }

        // Add all video qualities
        intent.putExtra(IntentKeys.EXTRA_VIDEO_QUALITIES, videoQualities);
        
        // Add additional metadata
        if (streamInfo.getDuration() > 0) {
            intent.putExtra(IntentKeys.EXTRA_VIDEO_DURATION, streamInfo.getDuration());
        }
        if (streamInfo.getUploaderName() != null) {
            intent.putExtra(IntentKeys.EXTRA_UPLOADER_NAME, streamInfo.getUploaderName());
        }
        if (streamInfo.getViewCount() > 0) {
            intent.putExtra(IntentKeys.EXTRA_VIEW_COUNT, streamInfo.getViewCount());
        }
        if (streamInfo.getThumbnails() != null && !streamInfo.getThumbnails().isEmpty()) {
            intent.putExtra(IntentKeys.EXTRA_THUMBNAIL_URL, streamInfo.getThumbnails().get(0).getUrl());
        }
        
        context.startActivity(intent);
    }

    private boolean isNetworkAvailable() {
        Context context = getContext();
        if (context != null && !NetworkUtils.isNetworkAvailable(context)) {
            showError("No network connection available");
            return false;
        }
        return true;
    }
}