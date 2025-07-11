package com.nidoham.hdstreamztv.model;

public class VideoItem {
    private String title;
    private String uploader;
    private String thumbnailUrl;
    private String videoUrl;
    private long duration;
    private long viewCount;
    
    public VideoItem(String title, String uploader, String thumbnailUrl, 
                    String videoUrl, long duration, long viewCount) {
        this.title = title;
        this.uploader = uploader;
        this.thumbnailUrl = thumbnailUrl;
        this.videoUrl = videoUrl;
        this.duration = duration;
        this.viewCount = viewCount;
    }
    
    // Getters
    public String getTitle() { return title; }
    public String getUploader() { return uploader; }
    public String getThumbnailUrl() { return thumbnailUrl; }
    public String getVideoUrl() { return videoUrl; }
    public long getDuration() { return duration; }
    public long getViewCount() { return viewCount; }
    
    // Setters
    public void setTitle(String title) { this.title = title; }
    public void setUploader(String uploader) { this.uploader = uploader; }
    public void setThumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; }
    public void setVideoUrl(String videoUrl) { this.videoUrl = videoUrl; }
    public void setDuration(long duration) { this.duration = duration; }
    public void setViewCount(long viewCount) { this.viewCount = viewCount; }
}