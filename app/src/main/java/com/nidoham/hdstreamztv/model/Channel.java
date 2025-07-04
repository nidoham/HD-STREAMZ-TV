package com.nidoham.hdstreamztv.model;

public class Channel {
    private String channelId = "";
    private String channelName = "";
    private boolean published = false;
    private String category = "";
    private String logoUrl = "";
    private String country = "";

    /**
     * A public, no-argument constructor is REQUIRED for Firebase to automatically deserialize
     * database objects.
     */
    public Channel() {}

    public Channel(
            String channelId,
            String channelName,
            boolean published,
            String category,
            String logoUrl,
            String country) {
        this.channelId = channelId;
        this.channelName = channelName;
        this.published = published;
        this.category = category;
        this.logoUrl = logoUrl;
        this.country = country;
    }

    public String getChannelId() {
        return this.channelId;
    }

    public void setChannelId(String channelId) {
        this.channelId = channelId;
    }

    public String getChannelName() {
        return this.channelName;
    }

    public void setChannelName(String channelName) {
        this.channelName = channelName;
    }

    public boolean getPublished() {
        return this.published;
    }

    public void setPublished(boolean published) {
        this.published = published;
    }

    public String getCategory() {
        return this.category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getLogoUrl() {
        return this.logoUrl;
    }

    public void setLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
    }

    public String getCountry() {
        return this.country;
    }

    public void setCountry(String country) {
        this.country = country;
    }
}
