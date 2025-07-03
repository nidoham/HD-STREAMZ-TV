package com.nidoham.hdstreamztv.adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

// Import the fragment classes you are about to create
import com.nidoham.hdstreamztv.fragment.EventsFragment;
import com.nidoham.hdstreamztv.fragment.FavoriteFragment;
import com.nidoham.hdstreamztv.fragment.LiveRadioFragment;
import com.nidoham.hdstreamztv.fragment.LiveTvFragment;

public class MainViewPagerAdapter extends FragmentStateAdapter {

    // The total number of tabs
    private static final int NUM_TABS = 4;

    public MainViewPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        // Return a NEW fragment instance in createFragment(int)
        switch (position) {
            case 0:
                return new EventsFragment();
            case 1:
                return new LiveTvFragment();
            case 2:
                return new LiveRadioFragment();
            case 3:
                return new FavoriteFragment();
            default:
                // This should never happen, but it's good practice to have a default
                return new EventsFragment();
        }
    }

    @Override
    public int getItemCount() {
        return NUM_TABS;
    }
}