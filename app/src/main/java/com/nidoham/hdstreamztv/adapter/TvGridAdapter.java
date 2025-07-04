package com.nidoham.hdstreamztv.adapter;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.nidoham.hdstreamztv.R;
import com.nidoham.hdstreamztv.dialog.MultipleLinkDialog;
import com.nidoham.hdstreamztv.model.Channel;

import com.nidoham.hdstreamztv.model.ChannelUrl;
import com.nidoham.hdstreamztv.repository.ChannelRepository;
import java.util.List;
import java.util.Objects;

/**
 * A RecyclerView adapter for displaying a grid of TV channels in the Streamly app.
 * Uses ListAdapter for efficient data updates and animations, suitable for both
 * Android Phone and Android TV.
 */
public class TvGridAdapter extends ListAdapter<Channel, TvGridAdapter.ViewHolder> {

    private final Context context;

    /**
     * Constructs the TvGridAdapter.
     *
     * @param context The context from the calling Fragment or Activity.
     */
    public TvGridAdapter(Context context) {
        super(DIFF_CALLBACK);
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_grid_card_tv, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Channel channel = getItem(position);

        // Set the channel name
        holder.titleTextView.setText(channel.getChannelName());

        // Load the channel logo using Glide
        Glide.with(context)
                .load(channel.getLogoUrl())
                .placeholder(R.drawable.placeholder_image)
                .error(R.drawable.error_image)
                .into(holder.imageView);

        // Set click listener for the item
        holder.itemView.setOnClickListener(v -> {
            setRepository(channel);
        });
    }
    
    public void setRepository(Channel channel){
        ChannelRepository repository = new ChannelRepository();

        repository.fetchChannelStreams(channel.getChannelId() , new ChannelRepository.ChannelCallback() {
            @Override
            public void onSuccess(List<ChannelUrl> urls) {
                MultipleLinkDialog.show(context, urls, channel.getChannelName());
            }

            @Override
            public void onFailure(String error) {
                Log.e("FirebaseError", error);
            }
        });
    }

    /**
     * ViewHolder for each grid item, holding references to the channel logo and name views.
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {
        final ImageView imageView;
        final TextView titleTextView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.iv_channel_logo);
            titleTextView = itemView.findViewById(R.id.tv_channel_name);
        }
    }

    /**
     * DiffUtil.ItemCallback for efficient list updates.
     * Compares Channel objects based on their channelId and contents.
     */
    private static final DiffUtil.ItemCallback<Channel> DIFF_CALLBACK = new DiffUtil.ItemCallback<Channel>() {
        @Override
        public boolean areItemsTheSame(@NonNull Channel oldItem, @NonNull Channel newItem) {
            return Objects.equals(oldItem.getChannelId(), newItem.getChannelId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull Channel oldItem, @NonNull Channel newItem) {
            return oldItem.equals(newItem);
        }
    };
}