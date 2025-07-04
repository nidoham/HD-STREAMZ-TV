package com.nidoham.hdstreamztv.viewmodel;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.nidoham.hdstreamztv.repository.ChannelRetriever;
import com.nidoham.hdstreamztv.model.Channel;
import com.nidoham.hdstreamztv.network.ChannelCallback;
import java.util.ArrayList;
import java.util.List;

/**
 * ViewModel for managing and providing live TV channel data to the Streamly UI.
 * Fetches data from the ChannelRetriever repository and exposes it via LiveData,
 * ensuring data survives configuration changes like screen rotations.
 */
public class LiveTvViewModel extends ViewModel {

    private static final String TAG = "LiveTvViewModel";

    private final ChannelRetriever channelRetriever;

    // LiveData to hold the list of channels
    private final MutableLiveData<List<Channel>> _channelList = new MutableLiveData<>();
    public final LiveData<List<Channel>> channelList = _channelList;

    // LiveData to signal the loading state (e.g., for showing a ProgressBar)
    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>();
    public final LiveData<Boolean> isLoading = _isLoading;

    // LiveData to signal that no channels were found
    private final MutableLiveData<Boolean> _isChannelListEmpty = new MutableLiveData<>();
    public final LiveData<Boolean> isChannelListEmpty = _isChannelListEmpty;

    // LiveData to hold error messages for the UI
    private final MutableLiveData<String> _errorMessage = new MutableLiveData<>();
    public final LiveData<String> errorMessage = _errorMessage;

    /**
     * Constructs the ViewModel with a default ChannelRetriever.
     */
    public LiveTvViewModel() {
        this.channelRetriever = new ChannelRetriever();
    }

    /**
     * Constructs the ViewModel with a custom ChannelRetriever.
     * Useful for dependency injection in testing.
     *
     * @param channelRetriever The ChannelRetriever to use for fetching channels.
     */
    public LiveTvViewModel(@NonNull ChannelRetriever channelRetriever) {
        this.channelRetriever = channelRetriever;
    }

    /**
     * Triggers fetching all channels from the repository.
     * Results are posted to LiveData and observed by the UI (Activity/Fragment).
     */
    public void loadChannels() {
        _isLoading.setValue(true);
        _isChannelListEmpty.setValue(false);
        _errorMessage.setValue(null);

        channelRetriever.fetchAllChannels(new ChannelCallback() {
            @Override
            public void onChannelsRetrieved(@NonNull List<Channel> channelList) {
                Log.d(TAG, "Successfully fetched " + channelList.size() + " channels.");
                _isLoading.postValue(false);
                _isChannelListEmpty.postValue(false);
                _channelList.postValue(channelList);
            }

            @Override
            public void onChannelsNotFound() {
                Log.w(TAG, "No channels found in the database.");
                _isLoading.postValue(false);
                _isChannelListEmpty.postValue(true);
                _channelList.postValue(new ArrayList<>());
            }

            @Override
            public void onRetrievalFailed(@NonNull Exception exception) {
                Log.e(TAG, "Failed to fetch channels.", exception);
                _isLoading.postValue(false);
                String message = exception instanceof java.net.UnknownHostException
                        ? "No internet connection. Please check your network."
                        : "Failed to load channels: " + exception.getMessage();
                _errorMessage.postValue(message);
            }
        });
    }
}