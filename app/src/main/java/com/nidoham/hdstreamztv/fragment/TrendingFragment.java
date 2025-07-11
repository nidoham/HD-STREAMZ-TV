package com.nidoham.hdstreamztv.fragment;

import android.content.Context;
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

import bd.nidoham.youtube.home.TrendingVideosExecutor;
import com.nidoham.hdstreamztv.R;
import com.nidoham.hdstreamztv.adapter.VideoAdapter;
import com.nidoham.hdstreamztv.databinding.FragmentTrendingBinding;
import com.nidoham.hdstreamztv.model.VideoItem;
import com.nidoham.hdstreamztv.util.NetworkUtils;

import org.schabi.newpipe.extractor.Image;
import org.schabi.newpipe.extractor.stream.StreamInfoItem;

import java.util.ArrayList;
import java.util.List;

public class TrendingFragment extends Fragment implements VideoAdapter.OnVideoItemClickListener, TrendingVideosExecutor.Listener {

    private static final String TAG = "TrendingFragment";

    // View Binding object to safely access views
    private FragmentTrendingBinding binding;

    // UI Components
    private VideoAdapter videoAdapter;

    // Data & Logic Components
    private TrendingVideosExecutor executor;

    // State Management
    private final ArrayList<VideoItem> videoList = new ArrayList<>();
    private boolean isLoading = false;

    // --- Lifecycle Methods ---

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            // Initialize TrendingVideosExecutor with the listener (this Fragment)
            executor = new TrendingVideosExecutor(requireContext(), this);
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize executor", e);
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
        // Nullify the binding object to prevent memory leaks
        binding = null;
    }

    // --- Initialization ---

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
                if (dy > 0) { // Only load more when scrolling down
                    LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                    if (layoutManager != null && layoutManager.findLastCompletelyVisibleItemPosition() >= videoAdapter.getItemCount() - 2) {
                        loadMoreVideos();
                    }
                }
            }
        });
    }

    // --- Data Loading ---

    private void loadInitialVideos(boolean forceRefresh) {
        if (isLoading || !isNetworkAvailable()) return;
        executor.fetchTrendingVideos(forceRefresh);
    }

    private void loadMoreVideos() {
        if (isLoading || !isNetworkAvailable()) return;
        executor.fetchMoreVideos();
    }

    // --- TrendingVideosExecutor.Listener Callbacks ---

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
        // Since no empty state container exists, show a Toast
        Toast.makeText(getContext(), "No videos available", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void showError(String message) {
        if (binding == null) return;
        binding.swipeRefreshLayout.setRefreshing(false);
        binding.progressBar.setVisibility(View.GONE);
        if (videoList.isEmpty()) {
            binding.recyclerViewTrending.setVisibility(View.GONE);
            Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
        } else {
            binding.recyclerViewTrending.setVisibility(View.VISIBLE);
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        }
    }

    // --- UI State Management ---

    private void showContentState() {
        if (binding == null) return;
        binding.swipeRefreshLayout.setRefreshing(false);
        binding.progressBar.setVisibility(View.GONE);
        binding.recyclerViewTrending.setVisibility(View.VISIBLE);
    }

    // --- Helper Methods ---

    private boolean isNetworkAvailable() {
        Context context = getContext();
        if (context != null && !NetworkUtils.isNetworkAvailable(context)) {
            showError("No network connection available");
            return false;
        }
        return true;
    }

    private List<VideoItem> processItems(List<StreamInfoItem> items) {
        List<VideoItem> processedVideos = new ArrayList<>();
        if (items == null) return processedVideos;

        for (StreamInfoItem item : items) {
            // Extract fields from StreamInfoItem and create VideoItem using the provided constructor
            String title = item.getName() != null ? item.getName() : "";
            String uploader = item.getUploaderName() != null ? item.getUploaderName() : "";
            // Use getThumbnails() to get the first thumbnail URL, if available
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

    @Override
    public void onVideoItemClick(@NonNull VideoItem videoItem) {
        Toast.makeText(getContext(), "Opening: " + videoItem.getTitle(), Toast.LENGTH_SHORT).show();
        // TODO: Implement navigation logic here
    }
}