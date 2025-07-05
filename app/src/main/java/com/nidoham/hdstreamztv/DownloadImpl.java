package com.nidoham.hdstreamztv;

import org.schabi.newpipe.extractor.downloader.Downloader;
import org.schabi.newpipe.extractor.downloader.Request;
import org.schabi.newpipe.extractor.downloader.Response;
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class DownloadImpl extends Downloader {
    
    private static final int DEFAULT_CONNECT_TIMEOUT = 15000; // 15 seconds
    private static final int DEFAULT_READ_TIMEOUT = 30000;    // 30 seconds
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36";
    
    @Override
    public Response execute(Request request) throws IOException, ReCaptchaException {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(request.url());
            connection = (HttpURLConnection) url.openConnection();
            
            // Set request method
            connection.setRequestMethod(request.httpMethod());
            
            // Set timeouts
            connection.setConnectTimeout(DEFAULT_CONNECT_TIMEOUT);
            connection.setReadTimeout(DEFAULT_READ_TIMEOUT);
            
            // Set default headers
            connection.setRequestProperty("User-Agent", USER_AGENT);
            connection.setRequestProperty("Accept", "*/*");
            connection.setRequestProperty("Accept-Language", "en-US,en;q=0.9");
            connection.setRequestProperty("Accept-Encoding", "gzip, deflate");
            connection.setRequestProperty("Connection", "keep-alive");
            
            // Add custom headers from request
            Map<String, List<String>> headers = request.headers();
            if (headers != null) {
                for (Map.Entry<String, List<String>> header : headers.entrySet()) {
                    if (header.getValue() != null && !header.getValue().isEmpty()) {
                        connection.setRequestProperty(header.getKey(), header.getValue().get(0));
                    }
                }
            }
            
            // Handle POST data if present
            byte[] dataToSend = request.dataToSend();
            if (dataToSend != null && dataToSend.length > 0) {
                connection.setDoOutput(true);
                connection.getOutputStream().write(dataToSend);
                connection.getOutputStream().flush();
            }
            
            // Follow redirects
            connection.setInstanceFollowRedirects(true);
            
            // Get response
            int responseCode = connection.getResponseCode();
            String responseMessage = connection.getResponseMessage();
            
            // Get response headers
            Map<String, List<String>> responseHeaders = new HashMap<>();
            for (Map.Entry<String, List<String>> entry : connection.getHeaderFields().entrySet()) {
                if (entry.getKey() != null && entry.getValue() != null && !entry.getValue().isEmpty()) {
                    responseHeaders.put(entry.getKey(), entry.getValue());
                }
            }
            
            // Get response body as String
            String responseBody = "";
            String latestUrl = connection.getURL().toString();
            
            try {
                InputStream inputStream;
                if (responseCode >= 400) {
                    inputStream = connection.getErrorStream();
                } else {
                    inputStream = connection.getInputStream();
                }
                
                if (inputStream != null) {
                    java.util.Scanner scanner = new java.util.Scanner(inputStream, "UTF-8");
                    responseBody = scanner.useDelimiter("\\A").hasNext() ? scanner.next() : "";
                    scanner.close();
                    inputStream.close();
                }
            } catch (Exception e) {
                // If reading fails, leave responseBody as empty string
            }
            
            return new Response(responseCode, responseMessage, responseHeaders, responseBody, latestUrl);
            
        } catch (IOException e) {
            if (connection != null) {
                connection.disconnect();
            }
            throw e;
        }
    }
    
    /**
     * Legacy method for backward compatibility if needed
     */
    public InputStream get(String url, boolean stream) throws IOException {
        try {
            Request request = new Request.Builder()
                    .get(url)
                    .build();
            
            Response response = execute(request);
            
            if (response.responseCode() >= 400) {
                throw new IOException("HTTP " + response.responseCode() + ": " + response.responseMessage());
            }
            
            // Convert String response body back to InputStream
            return new java.io.ByteArrayInputStream(response.responseBody().getBytes("UTF-8"));
            
        } catch (ReCaptchaException e) {
            throw new IOException("ReCaptcha challenge encountered", e);
        }
    }
    
    /**
     * Get response body as InputStream directly (optimized for streaming)
     */
    public InputStream getStream(String url) throws IOException {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(url).openConnection();
            
            // Set request method
            connection.setRequestMethod("GET");
            
            // Set timeouts
            connection.setConnectTimeout(DEFAULT_CONNECT_TIMEOUT);
            connection.setReadTimeout(DEFAULT_READ_TIMEOUT);
            
            // Set default headers
            connection.setRequestProperty("User-Agent", USER_AGENT);
            connection.setRequestProperty("Accept", "*/*");
            connection.setRequestProperty("Accept-Language", "en-US,en;q=0.9");
            connection.setRequestProperty("Accept-Encoding", "gzip, deflate");
            connection.setRequestProperty("Connection", "keep-alive");
            
            // Follow redirects
            connection.setInstanceFollowRedirects(true);
            
            // Get response code
            int responseCode = connection.getResponseCode();
            
            // Check for successful response
            if (responseCode >= 400) {
                throw new IOException("HTTP " + responseCode + ": " + connection.getResponseMessage());
            }
            
            return connection.getInputStream();
            
        } catch (IOException e) {
            if (connection != null) {
                connection.disconnect();
            }
            throw e;
        }
    }
    public InputStream getWithRetry(String url, int maxRetries) throws IOException {
        IOException lastException = null;
        
        for (int attempt = 0; attempt < maxRetries; attempt++) {
            try {
                return get(url, false);
            } catch (IOException e) {
                lastException = e;
                
                if (attempt < maxRetries - 1) {
                    try {
                        // Exponential backoff: wait 1s, 2s, 4s, etc.
                        TimeUnit.SECONDS.sleep(1L << attempt);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new IOException("Download interrupted", ie);
                    }
                }
            }
        }
        
        throw new IOException("Failed to download after " + maxRetries + " attempts", lastException);
    }
    
    /**
     * Method to check if URL is reachable
     */
    public boolean isUrlReachable(String url) {
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("HEAD");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.setRequestProperty("User-Agent", USER_AGENT);
            
            int responseCode = connection.getResponseCode();
            connection.disconnect();
            
            return responseCode >= 200 && responseCode < 400;
        } catch (IOException e) {
            return false;
        }
    }
    
    /**
     * Execute request with retry logic
     */
    public Response executeWithRetry(Request request, int maxRetries) throws IOException, ReCaptchaException {
        IOException lastException = null;
        ReCaptchaException lastReCaptchaException = null;
        
        for (int attempt = 0; attempt < maxRetries; attempt++) {
            try {
                return execute(request);
            } catch (IOException e) {
                lastException = e;
                
                if (attempt < maxRetries - 1) {
                    try {
                        // Exponential backoff: wait 1s, 2s, 4s, etc.
                        TimeUnit.SECONDS.sleep(1L << attempt);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new IOException("Request interrupted", ie);
                    }
                }
            } catch (ReCaptchaException e) {
                lastReCaptchaException = e;
                break; // Don't retry on ReCaptcha
            }
        }
        
        if (lastReCaptchaException != null) {
            throw lastReCaptchaException;
        }
        
        throw new IOException("Failed to execute request after " + maxRetries + " attempts", lastException);
    }
}