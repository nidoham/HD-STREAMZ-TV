package com.nidoham.hdstreamztv.repository;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.nidoham.hdstreamztv.model.ChannelUrl;

import java.util.ArrayList;
import java.util.List;

public class ChannelRepository {

    public interface ChannelCallback {
        void onSuccess(List<ChannelUrl> urls);
        void onFailure(String error);
    }

    public void fetchChannelStreams(String channelId, ChannelCallback callback) {
        FirebaseDatabase.getInstance()
                .getReference("channels")
                .child(channelId)
                .child("streams")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        List<ChannelUrl> streamList = new ArrayList<>();
                        for (DataSnapshot streamSnap : snapshot.getChildren()) {
                            ChannelUrl url = streamSnap.getValue(ChannelUrl.class);
                            if (url != null) {
                                streamList.add(url);
                            }
                        }
                        callback.onSuccess(streamList);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        callback.onFailure(error.getMessage());
                    }
                });
    }
}