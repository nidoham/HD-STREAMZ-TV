package com.nidoham.hdstreamztv.network;

import androidx.annotation.NonNull;
import com.nidoham.hdstreamztv.model.Channel;
import java.util.List;

/**
 * Callback interface for asynchronous retrieval of channels in the Streamly video player.
 * Used to handle success, empty data, or failure scenarios when fetching channel data,
 * typically from a Firebase database or network source.
 */
public interface ChannelCallback {

    /**
     * Called when a list of channels is successfully retrieved from the data source.
     *
     * @param channelList The non-empty list of channels. Never null.
     */
    void onChannelsRetrieved(@NonNull List<Channel> channelList);

    /**
     * Called when the data retrieval operation succeeds but no channels are found
     * (e.g., the database path exists but contains no data).
     */
    void onChannelsNotFound();

    /**
     * Called when an error occurs during channel retrieval, such as a network failure
     * or permission issue.
     *
     * @param exception The exception that caused the failure, such as IOException or
     *                  SecurityException. Never null.
     */
    void onRetrievalFailed(@NonNull Exception exception);
}