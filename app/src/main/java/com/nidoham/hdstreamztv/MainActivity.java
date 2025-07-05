package com.nidoham.hdstreamztv;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.nidoham.hdstreamztv.adapter.MainViewPagerAdapter; // You will create this adapter
import com.nidoham.hdstreamztv.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;
    private MainViewPagerAdapter viewPagerAdapter;
    
    public static boolean DEBUG = App.DEBUG;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Call the method to set up our tabs and viewpager
        setupTabs();
    }

    private void setupTabs() {
        // Create the adapter that will return a fragment for each of the four tabs
        viewPagerAdapter = new MainViewPagerAdapter(this);
        binding.viewPager.setAdapter(viewPagerAdapter);
        
        binding.viewPager.setUserInputEnabled(false);

        // Connect the TabLayout and the ViewPager2
        // This will also handle setting the tab titles
        new TabLayoutMediator(binding.tabLayout, binding.viewPager,
            (tab, position) -> {
                // Get the string resource for each tab position
                switch (position) {
                    case 0:
                        tab.setText(getString(R.string.tab_events));
                        break;
                    case 1:
                        tab.setText(getString(R.string.tab_live_tv));
                        break;
                    case 2:
                        tab.setText(getString(R.string.tab_live_radio));
                        break;
                    case 3:
                        tab.setText(getString(R.string.tab_favorite));
                        break;
                }
            }
        ).attach(); // Don't forget to call attach()!
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        this.binding = null;
    }
}