package com.nidoham.hdstreamztv.dialog;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.nidoham.hdstreamztv.PlayerActivity;
import com.nidoham.hdstreamztv.R;
import com.nidoham.hdstreamztv.model.ChannelUrl;
import com.nidoham.hdstreamztv.template.model.settings.Template;

import java.util.List;

/**
 * Dialog for displaying multiple channel links to the user.
 * Allows users to select from available streaming links for a channel.
 */
public class MultipleLinkDialog {
    
    private static final int MAX_LINKS = 5;
    private static final int[] LINK_TEXT_VIEW_IDS = {
        R.id.link1, R.id.link2, R.id.link3, R.id.link4, R.id.link5
    };
    
    private final Context context;
    private final List<ChannelUrl> channelUrls;
    private final String channelName;
    private Dialog dialog;
    
    /**
     * Private constructor to enforce builder pattern usage.
     */
    private MultipleLinkDialog(Context context, List<ChannelUrl> channelUrls, String channelName) {
        this.context = context;
        this.channelUrls = channelUrls;
        this.channelName = channelName;
    }
    
    /**
     * Static method to show the dialog.
     * 
     * @param context The context to show the dialog in
     * @param urls List of channel URLs to display
     * @param channelName Name of the channel
     */
    public static void show(Context context, List<ChannelUrl> urls, String channelName) {
        if (context == null || urls == null || urls.isEmpty()) {
            showErrorToast(context, "Invalid channel data");
            return;
        }
        
        MultipleLinkDialog dialogHelper = new MultipleLinkDialog(context, urls, channelName);
        dialogHelper.createAndShowDialog();
    }
    
    /**
     * Creates and displays the dialog.
     */
    private void createAndShowDialog() {
        dialog = new Dialog(context);
        dialog.setContentView(R.layout.dialog_multiple_links);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        
        setupLinkViews();
        dialog.show();
    }
    
    /**
     * Sets up the link TextViews based on available URLs.
     */
    private void setupLinkViews() {
        int urlCount = Math.min(channelUrls.size(), MAX_LINKS);
        
        for (int i = 0; i < MAX_LINKS; i++) {
            TextView linkView = dialog.findViewById(LINK_TEXT_VIEW_IDS[i]);
            
            if (i < urlCount) {
                setupLinkView(linkView, channelUrls.get(i), i);
            } else {
                linkView.setVisibility(View.GONE);
            }
        }
    }
    
    /**
     * Sets up an individual link TextView.
     * 
     * @param linkView The TextView to setup
     * @param channelUrl The channel URL data
     * @param index The index of the link
     */
    private void setupLinkView(TextView linkView, ChannelUrl channelUrl, int index) {
        linkView.setText(channelUrl.getTittle());
        linkView.setVisibility(View.VISIBLE);
        linkView.setOnClickListener(createLinkClickListener(channelUrl));
        
        // Optional: Add styling or additional setup here
        linkView.setTag(index); // Store index for potential future use
    }
    
    /**
     * Creates a click listener for a link.
     * 
     * @param channelUrl The channel URL data
     * @return OnClickListener for the link
     */
    private View.OnClickListener createLinkClickListener(ChannelUrl channelUrl) {
        return v -> {
            try {
                Intent intent = createPlayerIntent(channelUrl);
                context.startActivity(intent);
                dismissDialog();
            } catch (Exception e) {
                showErrorToast(context, "Failed to open stream");
            }
        };
    }
    
    /**
     * Creates the intent for launching the player activity.
     * 
     * @param channelUrl The selected channel URL
     * @return Intent for PlayerActivity
     */
    private Intent createPlayerIntent(ChannelUrl channelUrl) {
        Intent intent = new Intent(context, PlayerActivity.class);
        intent.putExtra("name", channelName);
        
        // TODO: Fix this hardcoded YouTube URL - should use channelUrl.getLink()
        // Currently using hardcoded YouTube.SONG_001 - this should be dynamic
        intent.putExtra("link", channelUrl.getLink()); // Use this instead
        
        intent.putExtra("category", Template.YOUTUBE);
        return intent;
    }
    
    /**
     * Dismisses the dialog safely.
     */
    private void dismissDialog() {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
    }
    
    /**
     * Shows an error toast message.
     * 
     * @param context The context
     * @param message The error message
     */
    private static void showErrorToast(Context context, String message) {
        if (context != null) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * Builder class for creating MultipleLinkDialog instances.
     * This provides a more flexible way to create dialogs in the future.
     */
    public static class Builder {
        private Context context;
        private List<ChannelUrl> channelUrls;
        private String channelName;
        
        public Builder(Context context) {
            this.context = context;
        }
        
        public Builder setChannelUrls(List<ChannelUrl> channelUrls) {
            this.channelUrls = channelUrls;
            return this;
        }
        
        public Builder setChannelName(String channelName) {
            this.channelName = channelName;
            return this;
        }
        
        public void show() {
            MultipleLinkDialog.show(context, channelUrls, channelName);
        }
    }
}