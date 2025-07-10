package com.nidoham.hdstreamztv;

import android.content.Intent;
// REMOVED: Unnecessary permission-related imports
// import android.content.pm.PackageManager;
// import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
// REMOVED: Unnecessary permission-related imports
// import androidx.core.app.ActivityCompat;
// import androidx.core.content.ContextCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.nidoham.hdstreamztv.adapter.MainViewPagerAdapter;
import com.nidoham.hdstreamztv.databinding.ActivityMainBinding;
import com.nidoham.hdstreamztv.util.NetworkUtils;

/**
 * Professional Main Activity with Material Design 3 Layout
 *
 * @author Professional Enhanced Version
 * @version 3.2
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    // Tab Configuration
    private static final int TAB_COUNT = 4;
    private static final int TAB_HOME = 0;
    private static final int TAB_TV = 1;
    private static final int TAB_CLOUD = 2;
    private static final int TAB_GUEST = 3;

    // State Keys
    private static final String KEY_CURRENT_TAB = "current_tab";
    private static final String KEY_FIRST_LAUNCH = "first_launch";
    private static final String KEY_TOOLBAR_TITLE = "toolbar_title";

    // REMOVED: Permission request codes are no longer needed
    // private static final int REQUEST_PERMISSIONS = 1001;
    private static final int REQUEST_SEARCH = 1002;
    private static final int REQUEST_NOTIFICATIONS = 1003;

    // Core Components
    private ActivityMainBinding binding;
    private MainViewPagerAdapter viewPagerAdapter;
    private TabLayoutMediator tabLayoutMediator;

    // State Management
    private final ActivityStateManager stateManager = new ActivityStateManager();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    // Debug flag from App class
    public static boolean DEBUG = App.DEBUG;

    // ========================================================================================
    // State Management Class
    // ========================================================================================
    private static class ActivityStateManager {
        private volatile int currentTabPosition = TAB_HOME;
        private volatile boolean isInitialized = false;
        private volatile boolean isFirstLaunch = true;
        // REMOVED: hasRequiredPermissions is no longer needed as there are no runtime permissions
        private volatile String toolbarTitle = "";
        private volatile boolean hasNewNotifications = false;

        public synchronized int getCurrentTabPosition() { return currentTabPosition; }
        public synchronized void setCurrentTabPosition(int position) { this.currentTabPosition = position; }

        public synchronized boolean isInitialized() { return isInitialized; }
        public synchronized void setInitialized(boolean initialized) { this.isInitialized = initialized; }

        public synchronized boolean isFirstLaunch() { return isFirstLaunch; }
        public synchronized void setFirstLaunch(boolean firstLaunch) { this.isFirstLaunch = firstLaunch; }
        
        // REMOVED: hasRequiredPermissions getter/setter
        
        public synchronized String getToolbarTitle() { return toolbarTitle; }
        public synchronized void setToolbarTitle(String title) { this.toolbarTitle = title; }

        public synchronized boolean hasNewNotifications() { return hasNewNotifications; }
        public synchronized void setHasNewNotifications(boolean hasNew) { this.hasNewNotifications = hasNew; }

        public synchronized void reset() {
            currentTabPosition = TAB_HOME;
            isInitialized = false;
            isFirstLaunch = true;
            toolbarTitle = "";
            hasNewNotifications = false;
        }
    }

    // ========================================================================================
    // Activity Lifecycle
    // ========================================================================================

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (DEBUG) {
            Log.d(TAG, "onCreate: Starting MainActivity initialization");
        }

        try {
            if (!initializeBinding()) {
                Log.e(TAG, "onCreate: Failed to initialize binding");
                return;
            }

            restoreState(savedInstanceState);

            // FIXED: No runtime permissions needed, so we can initialize directly.
            initializeActivity();

        } catch (Exception e) {
            Log.e(TAG, "onCreate: Critical error during initialization", e);
            handleCriticalError("Failed to initialize application", e);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (DEBUG) Log.d(TAG, "onStart: Activity starting");
        checkNetworkConnectivity();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (DEBUG) Log.d(TAG, "onResume: Activity resuming");
        updateUIState();
        checkForNewNotifications();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (DEBUG) Log.d(TAG, "onPause: Activity pausing");
        saveCurrentState();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (DEBUG) Log.d(TAG, "onStop: Activity stopping");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (DEBUG) Log.d(TAG, "onDestroy: Cleaning up resources");
        cleanup();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (binding != null && binding.viewPager != null) {
            outState.putInt(KEY_CURRENT_TAB, binding.viewPager.getCurrentItem());
        }
        outState.putBoolean(KEY_FIRST_LAUNCH, stateManager.isFirstLaunch());
        outState.putString(KEY_TOOLBAR_TITLE, stateManager.getToolbarTitle());
        if (DEBUG) Log.d(TAG, "onSaveInstanceState: State saved");
    }

    // ========================================================================================
    // Initialization Methods
    // ========================================================================================

    private boolean initializeBinding() {
        try {
            binding = ActivityMainBinding.inflate(getLayoutInflater());
            setContentView(binding.getRoot());
            if (DEBUG) Log.d(TAG, "View binding initialized successfully");
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize view binding", e);
            return false;
        }
    }

    private void restoreState(@Nullable Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            int savedTabPosition = savedInstanceState.getInt(KEY_CURRENT_TAB, TAB_HOME);
            boolean wasFirstLaunch = savedInstanceState.getBoolean(KEY_FIRST_LAUNCH, true);
            String savedTitle = savedInstanceState.getString(KEY_TOOLBAR_TITLE, getString(R.string.app_name));
            stateManager.setCurrentTabPosition(savedTabPosition);
            stateManager.setFirstLaunch(wasFirstLaunch);
            stateManager.setToolbarTitle(savedTitle);
            if (DEBUG) Log.d(TAG, "State restored - Tab: " + savedTabPosition + ", First launch: " + wasFirstLaunch);
        } else {
            stateManager.setToolbarTitle(getString(R.string.app_name));
        }
    }

    private void initializeActivity() {
        try {
            setupToolbar();
            setupTabs();
            setupActionButtons();
            setupAdditionalUI();
            updateActionButtonsForTab(stateManager.getCurrentTabPosition());
            stateManager.setInitialized(true);
            if (DEBUG) Log.d(TAG, "Activity initialization completed successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error during activity initialization", e);
            handleCriticalError("Failed to setup application interface", e);
        }
    }

    private void setupToolbar() {
        try {
            if (binding.toolbar == null) {
                Log.e(TAG, "Toolbar is null, cannot setup");
                return;
            }
            setSupportActionBar(binding.toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayShowTitleEnabled(false);
            }
            updateToolbarTitle(stateManager.getToolbarTitle());
            if (binding.toolbarAppIcon != null) {
                binding.toolbarAppIcon.setOnClickListener(v -> onAppIconClicked());
            }
            if (DEBUG) Log.d(TAG, "Toolbar setup completed");
        } catch (Exception e) {
            Log.e(TAG, "Error setting up toolbar", e);
        }
    }

    private void setupTabs() {
        if (binding == null) {
            Log.e(TAG, "Cannot setup tabs - binding is null");
            return;
        }
        try {
            viewPagerAdapter = new MainViewPagerAdapter(this);
            binding.viewPager.setAdapter(viewPagerAdapter);
            configureViewPager();
            setupTabLayoutMediator();
            restoreTabPosition();
            if (DEBUG) Log.d(TAG, "Tabs setup completed successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error setting up tabs", e);
            handleCriticalError("Failed to setup navigation tabs", e);
        }
    }

    private void configureViewPager() {
        if (binding.viewPager == null) return;
        try {
            binding.viewPager.setUserInputEnabled(false);
            binding.viewPager.setOffscreenPageLimit(TAB_COUNT);
            binding.viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
                @Override
                public void onPageSelected(int position) {
                    super.onPageSelected(position);
                    handleTabSelected(position);
                }
            });
            if (DEBUG) Log.d(TAG, "ViewPager configured successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error configuring ViewPager", e);
        }
    }

    private void setupTabLayoutMediator() {
        if (binding.tabLayout == null || binding.viewPager == null) {
            Log.e(TAG, "Cannot setup TabLayoutMediator - required views are null");
            return;
        }
        try {
            tabLayoutMediator = new TabLayoutMediator(binding.tabLayout, binding.viewPager, this::configureTab);
            tabLayoutMediator.attach();
            setupTabSelectionListener();
            if (DEBUG) Log.d(TAG, "TabLayoutMediator setup completed");
        } catch (Exception e) {
            Log.e(TAG, "Error setting up TabLayoutMediator", e);
        }
    }

    private void configureTab(@NonNull TabLayout.Tab tab, int position) {
        try {
            TabInfo tabInfo = getTabInfo(position);
            tab.setIcon(tabInfo.iconResId);
            tab.setContentDescription(tabInfo.contentDescription);
        } catch (Exception e) {
            Log.e(TAG, "Error configuring tab at position: " + position, e);
        }
    }

    private TabInfo getTabInfo(int position) {
        switch (position) {
            case TAB_HOME: return new TabInfo(R.drawable.ic_home, getString(R.string.tab_home_title), getString(R.string.tab_home_description));
            case TAB_TV: return new TabInfo(R.drawable.ic_tv, getString(R.string.tab_tv_title), getString(R.string.tab_tv_description));
            case TAB_CLOUD: return new TabInfo(R.drawable.ic_cloud, getString(R.string.tab_cloud_title), getString(R.string.tab_cloud_description));
            case TAB_GUEST: return new TabInfo(R.drawable.ic_guest, getString(R.string.tab_guest_title), getString(R.string.tab_guest_description));
            default: return new TabInfo(R.drawable.ic_home, "Unknown", "Unknown tab");
        }
    }

    private void setupTabSelectionListener() {
        if (binding.tabLayout == null) return;
        try {
            binding.tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
                @Override public void onTabSelected(TabLayout.Tab tab) {}
                @Override public void onTabUnselected(TabLayout.Tab tab) {}
                @Override public void onTabReselected(TabLayout.Tab tab) {
                    handleTabReselected(tab.getPosition());
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error setting up tab selection listener", e);
        }
    }

    private void setupActionButtons() {
        try {
            if (binding.actionSearch != null) {
                binding.actionSearch.setOnClickListener(v -> onSearchClicked());
                binding.actionSearch.setContentDescription(getString(R.string.action_search_description));
            }
            if (binding.actionNotifications != null) {
                binding.actionNotifications.setOnClickListener(v -> onNotificationsClicked());
                binding.actionNotifications.setContentDescription(getString(R.string.action_notifications_description));
            }
            if (DEBUG) Log.d(TAG, "Action buttons setup completed");
        } catch (Exception e) {
            Log.e(TAG, "Error setting up action buttons", e);
        }
    }

    private void setupAdditionalUI() {
        if (DEBUG) Log.d(TAG, "Additional UI setup completed");
    }

    // ========================================================================================
    // Action Button Handlers & UI Logic
    // ========================================================================================
    private void onAppIconClicked() {
        Toast.makeText(this, R.string.app_name, Toast.LENGTH_SHORT).show();
        scrollCurrentTabToTop();
    }
    private void onSearchClicked() {
        showSearchDialog();
    }
    private void onNotificationsClicked() {
        showNotificationsDialog();
        stateManager.setHasNewNotifications(false);
        updateNotificationIndicator();
    }
    private void showSearchDialog() {
        Toast.makeText(this, "Search functionality - Coming Soon", Toast.LENGTH_SHORT).show();
    }
    private void showNotificationsDialog() {
        Toast.makeText(this, "Notifications - Coming Soon", Toast.LENGTH_SHORT).show();
    }
    private void scrollCurrentTabToTop() {
        if (DEBUG) Log.d(TAG, "Scroll to top requested for tab: " + stateManager.getCurrentTabPosition());
    }

    // ========================================================================================
    // Tab Management
    // ========================================================================================
    private void handleTabSelected(int position) {
        stateManager.setCurrentTabPosition(position);
        updateToolbarTitleForTab(position);
        updateActionButtonsForTab(position);
    }
    private void handleTabReselected(int position) {
        if (DEBUG) Log.d(TAG, "Tab reselected: " + position);
        scrollCurrentTabToTop();
    }

    private void updateToolbarTitleForTab(int position) {
        String title;
        switch (position) {
            case TAB_TV: title = getString(R.string.tab_tv_title); break;
            case TAB_CLOUD: title = getString(R.string.tab_cloud_title); break;
            case TAB_GUEST: title = getString(R.string.tab_guest_title); break;
            default: title = getString(R.string.app_name); break;
        }
        updateToolbarTitle(title);
    }

    private void updateToolbarTitle(String title) {
        if (binding != null && binding.toolbarTitle != null && title != null) {
            binding.toolbarTitle.setText(title);
            stateManager.setToolbarTitle(title);
        }
    }

    private void updateActionButtonsForTab(int position) {
        boolean isSearchVisible;
        boolean isNotificationsVisible;
        switch (position) {
            case TAB_TV: isSearchVisible = true; isNotificationsVisible = true; break;
            case TAB_CLOUD: isSearchVisible = true; isNotificationsVisible = false; break;
            case TAB_GUEST: isSearchVisible = false; isNotificationsVisible = false; break;
            default: isSearchVisible = true; isNotificationsVisible = true; break;
        }
        setSearchButtonVisible(isSearchVisible);
        setNotificationsButtonVisible(isNotificationsVisible);
    }

    private void restoreTabPosition() {
        if (binding != null && binding.viewPager != null) {
            int pos = stateManager.getCurrentTabPosition();
            if (pos >= 0 && pos < TAB_COUNT) {
                binding.viewPager.setCurrentItem(pos, false);
            }
        }
    }

    // ========================================================================================
    // Notification Management
    // ========================================================================================
    private void checkForNewNotifications() {
        boolean hasNew = false; // Placeholder
        stateManager.setHasNewNotifications(hasNew);
        updateNotificationIndicator();
    }
    private void updateNotificationIndicator() {
        if (binding != null && binding.actionNotifications != null) {
            binding.actionNotifications.setAlpha(stateManager.hasNewNotifications() ? 1.0f : 0.7f);
        }
    }

    // ========================================================================================
    // REMOVED: Permission Management Section
    // The entire block for getRequiredPermissions, checkAndRequestPermissions,
    // hasAllRequiredPermissions, requestRequiredPermissions, onRequestPermissionsResult,
    // handlePermissionResults, and handlePermissionDenied has been removed as it is not needed.
    // ========================================================================================

    // ========================================================================================
    // Activity Result Handling
    // ========================================================================================
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null) {
            if (requestCode == REQUEST_SEARCH) {
                handleSearchResult(data);
            } else if (requestCode == REQUEST_NOTIFICATIONS) {
                handleNotificationsResult(data);
            }
        }
    }
    private void handleSearchResult(Intent data) {
        String query = data.getStringExtra("query");
        if (query != null) {
            if (DEBUG) Log.d(TAG, "Search result received: " + query);
            Toast.makeText(this, "Searching for: " + query, Toast.LENGTH_SHORT).show();
        }
    }
    private void handleNotificationsResult(Intent data) {
        boolean hasNew = data.getBooleanExtra("has_new_notifications", false);
        if (DEBUG) Log.d(TAG, "Notifications result received: " + hasNew);
        stateManager.setHasNewNotifications(hasNew);
        updateNotificationIndicator();
    }

    // ========================================================================================
    // Utility & Resource Management
    // ========================================================================================
    private void checkNetworkConnectivity() {
        if (!NetworkUtils.isNetworkAvailable(this)) {
            Toast.makeText(this, R.string.no_network_connection, Toast.LENGTH_SHORT).show();
        }
    }
    private void updateUIState() {
        if (stateManager.isInitialized() && binding != null) {
            updateToolbarTitle(stateManager.getToolbarTitle());
            updateNotificationIndicator();
        }
    }
    private void saveCurrentState() {
        if (binding != null && binding.viewPager != null) {
            stateManager.setCurrentTabPosition(binding.viewPager.getCurrentItem());
        }
    }
    private void handleCriticalError(String message, Exception e) {
        Log.e(TAG, "Critical error: " + message, e);
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }
    private void cleanup() {
        if (tabLayoutMediator != null) tabLayoutMediator.detach();
        if (binding != null && binding.viewPager != null) binding.viewPager.setAdapter(null);
        if (mainHandler != null) mainHandler.removeCallbacksAndMessages(null);
        binding = null;
        viewPagerAdapter = null;
        tabLayoutMediator = null;
        stateManager.reset();
        if (DEBUG) Log.d(TAG, "Resource cleanup completed");
    }

    // ========================================================================================
    // Public API Methods
    // ========================================================================================
    public int getCurrentTabPosition() { return stateManager.getCurrentTabPosition(); }
    public void switchToTab(int position) {
        if (position >= 0 && position < TAB_COUNT && binding != null && binding.viewPager != null) {
            binding.viewPager.setCurrentItem(position, true);
        }
    }
    public void setToolbarTitle(String title) { updateToolbarTitle(title); }
    public String getToolbarTitle() { return stateManager.getToolbarTitle(); }
    public boolean isInitialized() { return stateManager.isInitialized(); }
    public void setSearchButtonVisible(boolean visible) {
        if (binding != null && binding.actionSearch != null) {
            binding.actionSearch.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }
    public void setNotificationsButtonVisible(boolean visible) {
        if (binding != null && binding.actionNotifications != null) {
            binding.actionNotifications.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }
    public void setHasNewNotifications(boolean hasNew) {
        stateManager.setHasNewNotifications(hasNew);
        updateNotificationIndicator();
    }
    public void triggerSearch() { onSearchClicked(); }
    public void triggerNotifications() { onNotificationsClicked(); }

    // ========================================================================================
    // Helper Classes
    // ========================================================================================
    private static class TabInfo {
        final int iconResId;
        final String title;
        final String contentDescription;

        TabInfo(int iconResId, String title, String contentDescription) {
            this.iconResId = iconResId;
            this.title = title;
            this.contentDescription = contentDescription;
        }
    }
}