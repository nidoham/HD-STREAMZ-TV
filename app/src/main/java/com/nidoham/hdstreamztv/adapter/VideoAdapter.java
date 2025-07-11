package com.nidoham.hdstreamztv.adapter;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.nidoham.hdstreamztv.R;
import com.nidoham.hdstreamztv.model.VideoItem;
import java.util.List;
import java.util.Locale;

/**
 * RecyclerView Adapter for displaying video items in a list
 */
public class VideoAdapter extends RecyclerView.Adapter<VideoAdapter.VideoViewHolder> {
    
    private List<VideoItem> videoList;
    private OnVideoItemClickListener clickListener;
    private Context context;
    
    /**
     * Interface for handling video item clicks
     */
    public interface OnVideoItemClickListener {
        void onVideoItemClick(VideoItem videoItem);
    }
    
    /**
     * Constructor
     * @param videoList List of video items to display
     * @param clickListener Click listener for video items
     */
    public VideoAdapter(List<VideoItem> videoList, OnVideoItemClickListener clickListener) {
        this.videoList = videoList;
        this.clickListener = clickListener;
    }
    
    @NonNull
    @Override
    public VideoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_video, parent, false);
        return new VideoViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull VideoViewHolder holder, int position) {
        VideoItem videoItem = videoList.get(position);
        holder.bind(videoItem);
    }
    
    @Override
    public int getItemCount() {
        return videoList != null ? videoList.size() : 0;
    }
    
    /**
     * Update the video list and refresh the adapter
     * @param newVideoList New list of videos
     */
    public void updateVideoList(List<VideoItem> newVideoList) {
        this.videoList = newVideoList;
        notifyDataSetChanged();
    }
    
    /**
     * ViewHolder class for video items
     */
    public class VideoViewHolder extends RecyclerView.ViewHolder {
        
        private ImageView thumbnailImageView;
        private TextView titleTextView;
        private TextView uploaderTextView;
        private TextView durationTextView;
        private TextView viewCountTextView;
        private View itemContainer;
        
        public VideoViewHolder(@NonNull View itemView) {
            super(itemView);
            initializeViews(itemView);
            setupClickListener();
        }
        
        /**
         * Initialize view components
         */
        private void initializeViews(View itemView) {
            thumbnailImageView = itemView.findViewById(R.id.iv_thumbnail);
            titleTextView = itemView.findViewById(R.id.tv_title);
            uploaderTextView = itemView.findViewById(R.id.tv_uploader);
            durationTextView = itemView.findViewById(R.id.tv_duration);
            viewCountTextView = itemView.findViewById(R.id.tv_view_count);
            itemContainer = itemView.findViewById(R.id.item_container);
        }
        
        /**
         * Setup click listener for the item
         */
        private void setupClickListener() {
            itemContainer.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && clickListener != null) {
                    clickListener.onVideoItemClick(videoList.get(position));
                }
            });
        }
        
        /**
         * Bind video data to views
         * @param videoItem Video item to bind
         */
        public void bind(VideoItem videoItem) {
            if (videoItem == null) {
                return;
            }
            
            // Set title with null check
            titleTextView.setText(videoItem.getTitle() != null ? videoItem.getTitle() : "Unknown Title");
            
            // Set uploader with null check
            uploaderTextView.setText(videoItem.getUploader() != null ? videoItem.getUploader() : "Unknown Uploader");
            
            // Set duration
            durationTextView.setText(formatDuration(videoItem.getDuration()));
            
            // Set view count
            viewCountTextView.setText(formatViewCount(videoItem.getViewCount()));
            
            // Load thumbnail image
            loadThumbnail(videoItem.getThumbnailUrl());
        }
        
        /**
         * Load thumbnail image using Glide with better error handling
         * @param thumbnailUrl URL of the thumbnail image
         */
        private void loadThumbnail(String thumbnailUrl) {
            RequestOptions requestOptions = new RequestOptions()
                    .placeholder(R.drawable.ic_video_placeholder)
                    .error(R.drawable.ic_video_error)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .centerCrop();
            
            // Check if thumbnail URL is valid
            if (!TextUtils.isEmpty(thumbnailUrl) && thumbnailUrl.startsWith("http")) {
                Glide.with(context)
                        .load(thumbnailUrl)
                        .apply(requestOptions)
                        .into(thumbnailImageView);
            } else {
                // Load placeholder if no valid URL
                Glide.with(context)
                        .load(R.drawable.ic_video_placeholder)
                        .apply(requestOptions)
                        .into(thumbnailImageView);
            }
        }
        
        /**
         * Format duration from seconds to MM:SS or HH:MM:SS format
         * @param durationInSeconds Duration in seconds
         * @return Formatted duration string
         */
        private String formatDuration(long durationInSeconds) {
            if (durationInSeconds <= 0) {
                return "00:00";
            }
            
            long hours = durationInSeconds / 3600;
            long minutes = (durationInSeconds % 3600) / 60;
            long seconds = durationInSeconds % 60;
            
            if (hours > 0) {
                return String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds);
            } else {
                return String.format(Locale.getDefault(), "%d:%02d", minutes, seconds);
            }
        }
        
        /**
         * Format view count to readable format (e.g., 1.2K, 1.5M)
         * @param viewCount Number of views
         * @return Formatted view count string
         */
        private String formatViewCount(long viewCount) {
            if (viewCount < 0) {
                return "No views";
            } else if (viewCount < 1000) {
                return viewCount + " views";
            } else if (viewCount < 1000000) {
                return String.format(Locale.getDefault(), "%.1fK views", viewCount / 1000.0);
            } else if (viewCount < 1000000000) {
                return String.format(Locale.getDefault(), "%.1fM views", viewCount / 1000000.0);
            } else {
                return String.format(Locale.getDefault(), "%.1fB views", viewCount / 1000000000.0);
            }
        }
    }
}