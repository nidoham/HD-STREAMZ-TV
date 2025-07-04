package com.nidoham.hdstreamztv.repository;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;


import com.nidoham.hdstreamztv.model.Channel;
import com.nidoham.hdstreamztv.network.ChannelCallback;
import java.util.ArrayList;
import java.util.List;

/**
 * Repository class for retrieving channels from the Firebase Realtime Database.
 * Follows the Repository pattern to abstract data access for the Streamly video player.
 */
public class ChannelRetriever {

    // Firebase Realtime Database node for channels
    private static final String CHANNELS_NODE = "channels";
    private final DatabaseReference databaseReference;

    /**
     * Constructs a ChannelRetriever with a reference to the Firebase Realtime Database.
     */
    public ChannelRetriever() {
        this.databaseReference = FirebaseDatabase.getInstance().getReference(CHANNELS_NODE);
    }

    /**
     * Constructs a ChannelRetriever with a custom Firebase database reference.
     * Useful for testing or using a different database node.
     *
     * @param databaseReference The Firebase DatabaseReference to use.
     */
    public ChannelRetriever(@NonNull DatabaseReference databaseReference) {
        this.databaseReference = databaseReference;
    }

    /**
     * Asynchronously fetches all channels from the Firebase Realtime Database.
     * Results are delivered on the main thread via the provided callback.
     *
     * @param callback The callback to handle the result of the channel retrieval.
     *                 Must not be null.
     * @throws IllegalArgumentException if callback is null.
     */
    public void fetchAllChannels(@NonNull ChannelCallback callback) {
        if (callback == null) {
            throw new IllegalArgumentException("Callback must not be null");
        }

        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<Channel> channelList = new ArrayList<>();
                for (DataSnapshot channelSnapshot : dataSnapshot.getChildren()) {
                    Channel channel = channelSnapshot.getValue(Channel.class);
                    if (channel != null && channel.getChannelId() != null) {
                        channelList.add(channel);
                    }
                }
                if (channelList.isEmpty()) {
                    callback.onChannelsNotFound();
                } else {
                    callback.onChannelsRetrieved(channelList);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                callback.onRetrievalFailed(databaseError.toException());
            }
        });
    }
}