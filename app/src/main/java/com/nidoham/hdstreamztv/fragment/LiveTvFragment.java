package com.nidoham.hdstreamztv.fragment;

import android.content.res.Resources;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.nidoham.hdstreamztv.R;
import com.nidoham.hdstreamztv.adapter.TvGridAdapter;
import com.nidoham.hdstreamztv.utils.GridSpacingItemDecoration;
import com.nidoham.hdstreamztv.viewmodel.LiveTvViewModel;

/**
 * A fragment that displays a grid of live TV channels in the Streamly app.
 * Follows the MVVM architecture, observing data from LiveTvViewModel.
 * Supports both Android Phone and Android TV (leanback-compatible).
 */
public class LiveTvFragment extends Fragment {

    private static final int GRID_COLUMN_COUNT = 3;
    private static final int GRID_SPACING_DP = 16;

    private LiveTvViewModel viewModel;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView errorTextView;
    private TvGridAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_tv, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize ViewModel
        viewModel = new ViewModelProvider(this).get(LiveTvViewModel.class);

        // Set up UI components
        setupViews(view);

        // Configure RecyclerView
        setupRecyclerView();

        // Set up LiveData observers
        setupObservers();

        // Trigger channel loading
        viewModel.loadChannels();
    }

    private void setupViews(View view) {
        recyclerView = view.findViewById(R.id.tv_grid_recyclerview);
        progressBar = view.findViewById(R.id.progress_bar);
        errorTextView = view.findViewById(R.id.error_message);
    }

    private void setupRecyclerView() {
        adapter = new TvGridAdapter(requireContext());
        recyclerView.setLayoutManager(new GridLayoutManager(requireContext(), GRID_COLUMN_COUNT));
        int spacingInPixels = dpToPx(GRID_SPACING_DP);
        recyclerView.addItemDecoration(new GridSpacingItemDecoration(GRID_COLUMN_COUNT, spacingInPixels, true));
        recyclerView.setAdapter(adapter);
    }

    private void setupObservers() {
        // Observe loading state
        viewModel.isLoading.observe(getViewLifecycleOwner(), isLoading -> {
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            recyclerView.setVisibility(isLoading ? View.GONE : View.VISIBLE);
            errorTextView.setVisibility(View.GONE);
        });

        // Observe channel list
        viewModel.channelList.observe(getViewLifecycleOwner(), channels -> {
            adapter.submitList(channels);
            recyclerView.setVisibility(channels.isEmpty() ? View.GONE : View.VISIBLE);
            errorTextView.setVisibility(View.GONE);
        });

        // Observe empty state
        viewModel.isChannelListEmpty.observe(getViewLifecycleOwner(), isEmpty -> {
            if (isEmpty) {
                recyclerView.setVisibility(View.GONE);
                errorTextView.setVisibility(View.VISIBLE);
                errorTextView.setText(R.string.no_channels_available);
            }
        });

        // Observe error messages
        viewModel.errorMessage.observe(getViewLifecycleOwner(), message -> {
            if (message != null) {
                recyclerView.setVisibility(View.GONE);
                errorTextView.setVisibility(View.VISIBLE);
                errorTextView.setText(message);
                Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
            }
        });
    }
    
    

    private int dpToPx(int dp) {
        Resources r = getResources();
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, r.getDisplayMetrics()));
    }
}