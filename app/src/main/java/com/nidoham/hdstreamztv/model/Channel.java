package com.nidoham.hdstreamztv.model;

public class Channel {
    private final String channelId; // More specific than just 'id'
    private final String channelName; // More descriptive than 'name'
    private final boolean isVisible; // Unchanged, clear and appropriate
    private final String category; // Unchanged, clear and appropriate
    private final String logoUrl; // Changed from 'avatar' to clarify it's a URL for the channel's logo or thumbnail

    /**
     * A public, no-argument constructor is REQUIRED for Firebase
     * to automatically deserialize database objects.
     */
    public Channel() {
        this.channelId = "";
        this.channelName = "";
        this.isVisible = true; // Default visible
        this.category = "";
        this.logoUrl = "";
    }

    public Channel(String channelId, String channelName, boolean isVisible, String category, String logoUrl) {
        this.channelId = channelId;
        this.channelName = channelName;
        this.isVisible = isVisible;
        this.category = category;
        this.logoUrl = logoUrl;
    }

    public String getChannelId() {
        return channelId;
    }

    public String getChannelName() {
        return channelName;
    }

    public boolean isVisible() {
        return isVisible;
    }

    public String getCategory() {
        return category;
    }

    public String getLogoUrl() {
        return logoUrl;
    }
}