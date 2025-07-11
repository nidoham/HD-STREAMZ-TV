package com.nidoham.hdstreamztv;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.nidoham.hdstreamztv.adapter.MainViewPagerAdapter;
import com.nidoham.hdstreamztv.databinding.ActivityMainBinding;
import com.nidoham.hdstreamztv.util.NetworkUtils;

/**
 * MainActivity serves as the primary screen, managing top-level navigation and UI.
 * It uses a ViewPager2 with a TabLayout for a modern, swipeable tab interface.
 *
 * @version 4.0
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final String KEY_CURRENT_TAB = "current_tab";

    // Tab Definitions
    private static final int TAB_HOME = 0;
    private static final int TAB_TV = 1;
    private static final int TAB_YOUTUBE = 2;
    private static final int TAB_TRADING = 3;

    private ActivityMainBinding binding;
    private MainViewPagerAdapter viewPagerAdapter;
    private TabLayoutMediator tabLayoutMediator;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        initToolbar();
        initViewPagerAndTabs();
        initActionButtons();

        if (savedInstanceState == null) {
            checkNetworkConnectivity();
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (binding != null) {
            outState.putInt(KEY_CURRENT_TAB, binding.viewPager.getCurrentItem());
        }
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        int savedTab = savedInstanceState.getInt(KEY_CURRENT_TAB, TAB_HOME);
        if (binding != null) {
            binding.viewPager.setCurrentItem(savedTab, false);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (tabLayoutMediator != null) {
            tabLayoutMediator.detach();
        }
        binding = null; // Release binding to prevent memory leaks
    }

    /**
     * Initializes the Toolbar, setting it as the support action bar.
     */
    private void initToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
    }

    /**
     * Sets up the ViewPager, Adapter, and TabLayout, linking them together.
     */
    private void initViewPagerAndTabs() {
        viewPagerAdapter = new MainViewPagerAdapter(this);
        binding.viewPager.setAdapter(viewPagerAdapter);
        binding.viewPager.setUserInputEnabled(false); // Disable swipe gestures

        tabLayoutMediator = new TabLayoutMediator(binding.tabLayout, binding.viewPager, (tab, position) -> {
            switch (position) {
                case TAB_HOME:
                    tab.setIcon(R.drawable.ic_home);
                    tab.setContentDescription(R.string.desc_home_icon);
                    break;
                case TAB_TV:
                    tab.setIcon(R.drawable.ic_tv);
                    tab.setContentDescription(R.string.desc_tv_icon);
                    break;
                case TAB_YOUTUBE:
                    tab.setIcon(R.drawable.ic_youtube);
                    tab.setContentDescription(R.string.desc_youtube_icon);
                    break;
                case TAB_TRADING:
                    tab.setIcon(R.drawable.ic_cloud);
                    tab.setContentDescription(R.string.desc_events_icon);
                    break;
            }
        });
        tabLayoutMediator.attach();

        binding.viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                updateUiForTab(position);
            }
        });

        // Set initial state for the first tab
        updateUiForTab(binding.viewPager.getCurrentItem());
    }

    /**
     * Initializes click listeners for toolbar action buttons.
     */
    private void initActionButtons() {
        binding.actionSearch.setOnClickListener(v ->
                Toast.makeText(this, "Search coming soon!", Toast.LENGTH_SHORT).show());
        binding.actionNotifications.setOnClickListener(v ->
                Toast.makeText(this, "Notifications coming soon!", Toast.LENGTH_SHORT).show());
    }

    /**
     * Updates the toolbar title and button visibility based on the selected tab.
     *
     * @param position The position of the newly selected tab.
     */
    private void updateUiForTab(int position) {
        switch (position) {
            case TAB_HOME:
                binding.toolbarTitle.setText(R.string.tab_home);
                binding.actionSearch.setVisibility(View.VISIBLE);
                binding.actionNotifications.setVisibility(View.VISIBLE);
                break;
            case TAB_TV:
                binding.toolbarTitle.setText(R.string.tab_tv);
                binding.actionSearch.setVisibility(View.VISIBLE);
                binding.actionNotifications.setVisibility(View.VISIBLE);
                break;
            case TAB_YOUTUBE:
                binding.toolbarTitle.setText(R.string.tab_youtube);
                binding.actionSearch.setVisibility(View.VISIBLE);
                binding.actionNotifications.setVisibility(View.GONE);
                break;
            case TAB_TRADING:
                binding.toolbarTitle.setText(R.string.tab_events);
                binding.actionSearch.setVisibility(View.GONE);
                binding.actionNotifications.setVisibility(View.GONE);
                break;
        }
    }

    /**
     * Checks for an active network connection and displays a Toast if unavailable.
     */
    private void checkNetworkConnectivity() {
        if (!NetworkUtils.isNetworkAvailable(this)) {
            Toast.makeText(this, R.string.no_network_connection, Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Switches the ViewPager to a specific tab.
     *
     * @param position The tab index to switch to.
     */
    public void switchToTab(int position) {
        if (binding != null && position >= 0 && position < viewPagerAdapter.getItemCount()) {
            binding.viewPager.setCurrentItem(position, true);
        }
    }
}