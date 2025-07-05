# YouTubeLinkExtractor Usage Guide

## Overview
The `YouTubeLinkExtractor` is a professional-grade utility class for extracting direct stream URLs from YouTube videos. It provides a clean, callback-based API for retrieving video and audio URLs that can be used directly with ExoPlayer or other media players.

## Key Features

- **Multiple Quality Options**: Support for 4K, Full HD, HD, Medium, Low, and Audio-only streams
- **Professional Error Handling**: Comprehensive error messages and validation
- **Asynchronous Operations**: Non-blocking extraction using RxJava
- **Resource Management**: Proper cleanup and cancellation methods
- **Flexible Callbacks**: Multiple listener interfaces for different use cases

## Usage Examples

### Basic Video + Audio Extraction

```java
YouTubeLinkExtractor extractor = new YouTubeLinkExtractor();

extractor.extractLink("https://youtube.com/watch?v=dQw4w9WgXcQ", 
    YouTubeLinkExtractor.Quality.HD, 
    new YouTubeLinkExtractor.OnLinkExtractedListener() {
        @Override
        public void onVideoLinkExtracted(String videoUrl, String audioUrl, String title) {
            // Use URLs directly in ExoPlayer
            Log.d("Extractor", "Video: " + videoUrl);
            Log.d("Extractor", "Audio: " + audioUrl);
            Log.d("Extractor", "Title: " + title);
            
            // Setup ExoPlayer with extracted URLs
            setupExoPlayer(videoUrl, audioUrl);
        }
        
        @Override
        public void onError(String error) {
            Log.e("Extractor", "Error: " + error);
        }
    });
```

### Simple Video URL Extraction

```java
extractor.extractVideoLink("https://youtube.com/watch?v=dQw4w9WgXcQ", 
    YouTubeLinkExtractor.Quality.BEST, 
    new YouTubeLinkExtractor.OnVideoLinkListener() {
        @Override
        public void onVideoLinkExtracted(String videoUrl, String title) {
            // Use single URL for playback
            playVideo(videoUrl, title);
        }
        
        @Override
        public void onError(String error) {
            showError(error);
        }
    });
```

### Quick Methods

```java
// Get best quality video
extractor.getBestVideoLink("https://youtube.com/watch?v=dQw4w9WgXcQ", listener);

// Get HD quality video
extractor.getHDVideoLink("https://youtube.com/watch?v=dQw4w9WgXcQ", listener);

// Get audio-only stream
extractor.getAudioLink("https://youtube.com/watch?v=dQw4w9WgXcQ", listener);
```

### Get All Available Qualities

```java
extractor.getAllVideoLinks("https://youtube.com/watch?v=dQw4w9WgXcQ", 
    new YouTubeLinkExtractor.OnAllLinksListener() {
        @Override
        public void onAllLinksExtracted(List<VideoLinkInfo> videoLinks, String audioUrl, String title) {
            // Display quality options to user
            for (VideoLinkInfo link : videoLinks) {
                Log.d("Quality", link.getQuality() + " - " + link.getUrl());
            }
        }
        
        @Override
        public void onError(String error) {
            Log.e("Extractor", error);
        }
    });
```

## Quality Options

| Quality | Description | Target Resolution |
|---------|-------------|------------------|
| `BEST` | Highest available quality | Variable |
| `UHD_4K` | 4K Ultra HD | 2160p |
| `FULL_HD` | Full HD | 1080p |
| `HD` | High Definition | 720p |
| `MEDIUM` | Standard Definition | 480p |
| `LOW` | Low Quality | 360p |
| `LOWEST` | Lowest Available | 240p or lower |
| `AUDIO_ONLY` | Audio stream only | N/A |

## ExoPlayer Integration

```java
private void setupExoPlayer(String videoUrl, String audioUrl) {
    // Create MediaSource for video
    MediaSource videoSource = new ProgressiveMediaSource.Factory(dataSourceFactory)
        .createMediaSource(MediaItem.fromUri(videoUrl));
    
    // Create MediaSource for audio (if separate)
    if (audioUrl != null) {
        MediaSource audioSource = new ProgressiveMediaSource.Factory(dataSourceFactory)
            .createMediaSource(MediaItem.fromUri(audioUrl));
        
        // Merge video and audio
        MediaSource mergedSource = new MergingMediaSource(videoSource, audioSource);
        player.setMediaSource(mergedSource);
    } else {
        player.setMediaSource(videoSource);
    }
    
    player.prepare();
    player.play();
}
```

## Error Handling

The extractor provides detailed error messages for different scenarios:

- **Invalid URL**: "Invalid YouTube URL: [url]"
- **No streams**: "No suitable video stream found for quality: [quality]"
- **Network issues**: "Network connection error"
- **Video unavailable**: "Video is unavailable or private"
- **Processing errors**: "Error processing video information: [details]"

## Best Practices

### 1. Always Clean Up Resources
```java
@Override
protected void onDestroy() {
    super.onDestroy();
    if (extractor != null) {
        extractor.cleanup();
    }
}
```

### 2. Handle Lifecycle Events
```java
@Override
protected void onPause() {
    super.onPause();
    if (extractor != null && extractor.isBusy()) {
        extractor.cancelAll();
    }
}
```

### 3. Validate URLs Before Extraction
```java
if (YouTubeLinkExtractor.isValidYouTubeUrl(url)) {
    extractor.extractLink(url, quality, listener);
} else {
    showError("Please enter a valid YouTube URL");
}
```

### 4. Provide User Feedback
```java
extractor.extractLink(url, quality, new OnLinkExtractedListener() {
    @Override
    public void onVideoLinkExtracted(String videoUrl, String audioUrl, String title) {
        hideProgressBar();
        playVideo(videoUrl, audioUrl);
    }
    
    @Override
    public void onError(String error) {
        hideProgressBar();
        showErrorDialog(error);
    }
});
```

## Advanced Usage

### Custom Quality Selection
```java
// Get all qualities and let user choose
extractor.getAllVideoLinks(url, new OnAllLinksListener() {
    @Override
    public void onAllLinksExtracted(List<VideoLinkInfo> links, String audioUrl, String title) {
        // Create quality selection dialog
        showQualityDialog(links, audioUrl, title);
    }
    
    @Override
    public void onError(String error) {
        showError(error);
    }
});
```

### Batch Processing
```java
private void extractMultipleVideos(List<String> urls) {
    for (String url : urls) {
        if (YouTubeLinkExtractor.isValidYouTubeUrl(url)) {
            extractor.getBestVideoLink(url, new OnVideoLinkListener() {
                @Override
                public void onVideoLinkExtracted(String videoUrl, String title) {
                    // Process each video
                    processVideo(videoUrl, title);
                }
                
                @Override
                public void onError(String error) {
                    Log.e("BatchExtract", "Failed to extract: " + url + " - " + error);
                }
            });
        }
    }
}
```

## Thread Safety

The extractor is designed to be thread-safe and handles all network operations on background threads, with callbacks delivered on the main thread for UI updates.

## Memory Management

The extractor uses `CompositeDisposable` to manage RxJava subscriptions and provides cleanup methods to prevent memory leaks. Always call `cleanup()` when done with the extractor.

## Dependencies

- NewPipe Extractor Library
- RxJava3
- Android Support Libraries

## Notes

- Stream URLs are temporary and may expire after some time
- Always handle errors gracefully as YouTube may change their API
- Some videos may have region restrictions or be unavailable
- Audio-only streams are useful for music playback to save bandwidth