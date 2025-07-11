package com.nidoham.hdstreamztv.model;

import java.io.Serializable;

public class VideoQuality implements Serializable {
    private String quality;
    private String url;

    public VideoQuality(String quality, String url) {
        this.quality = quality;
        this.url = url;
    }

    public String getQuality() {
        return quality;
    }

    public String getUrl() {
        return url;
    }
}