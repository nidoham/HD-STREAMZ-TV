package org.schabi.newpipe.util;

import android.content.Context;
import android.text.TextUtils;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Locale;

import org.schabi.newpipe.extractor.kiosk.KioskInfo;
import org.schabi.newpipe.extractor.exceptions.ExtractionException;

import io.reactivex.rxjava3.core.Single;

/**
 * Utility class for fetching trending/home page videos from YouTube
 * based on the device's geographical location.
 */
public final class TrendingVideoExecutor {
    
    private static final String YOUTUBE_BASE_URL = "https://www.youtube.com/";
    private static final String COUNTRY_PARAM = "?gl=";
    private static final String DEFAULT_COUNTRY_CODE = "BD";
    
    // Private constructor to prevent instantiation
    private TrendingVideoExecutor() {
        throw new AssertionError("Utility class should not be instantiated");
    }
    
    /**
     * Fetches YouTube trending/home page videos for the device's country.
     * 
     * @param context Android context, must not be null
     * @param serviceId Service ID for the extractor (YouTube is typically 0)
     * @param forceLoad Whether to force network fetch instead of using cache
     * @return Single<KioskInfo> containing trending videos information
     * @throws IllegalArgumentException if context is null or serviceId is invalid
     */
    @NonNull
    public static Single<KioskInfo> getTrendingVideos(@NonNull Context context, 
                                                     int serviceId, 
                                                     boolean forceLoad) {
        if (context == null) {
            return Single.error(new IllegalArgumentException("Context cannot be null"));
        }
        
        if (serviceId < 0) {
            return Single.error(new IllegalArgumentException("Service ID must be non-negative"));
        }
        
        return Single.fromCallable(() -> {
            String countryCode = getDeviceCountryCode(context);
            String url = buildYouTubeUrl(countryCode);
            return url;
        })
        .flatMap(url -> ExtractorHelper.getKioskInfo(serviceId, url, forceLoad))
        .onErrorResumeNext(throwable -> {
            // Fallback to default country if country detection fails
            if (throwable instanceof ExtractionException) {
                String fallbackUrl = buildYouTubeUrl(DEFAULT_COUNTRY_CODE);
                return ExtractorHelper.getKioskInfo(serviceId, fallbackUrl, forceLoad);
            }
            return Single.error(throwable);
        });
    }
    
    /**
     * Detects the device's country code using system locale.
     * 
     * @param context Android context, must not be null
     * @return ISO 3166-1 alpha-2 country code (2 letters), defaults to "US" if detection fails
     */
    @NonNull
    public static String getDeviceCountryCode(@NonNull Context context) {
        if (context == null) {
            return DEFAULT_COUNTRY_CODE;
        }
        
        try {
            Locale locale = getSystemLocale(context);
            String countryCode = locale.getCountry();
            
            // Validate country code format (should be 2 uppercase letters)
            if (isValidCountryCode(countryCode)) {
                return countryCode.toUpperCase(Locale.ROOT);
            }
        } catch (Exception e) {
            // Log the exception in a real implementation
            // Log.w("TrendingVideoExecutor", "Failed to get device country", e);
        }
        
        return DEFAULT_COUNTRY_CODE;
    }
    
    /**
     * Gets the system locale from the context.
     * 
     * @param context Android context
     * @return Current system locale
     */
    @NonNull
    private static Locale getSystemLocale(@NonNull Context context) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            return context.getResources().getConfiguration().getLocales().get(0);
        } else {
            return context.getResources().getConfiguration().locale;
        }
    }
    
    /**
     * Validates if the provided country code is in the correct format.
     * 
     * @param countryCode Country code to validate
     * @return true if valid (2 letters), false otherwise
     */
    private static boolean isValidCountryCode(@Nullable String countryCode) {
        return !TextUtils.isEmpty(countryCode) && 
               countryCode.length() == 2 && 
               countryCode.matches("[A-Za-z]{2}");
    }
    
    /**
     * Builds the YouTube URL with the specified country parameter.
     * 
     * @param countryCode ISO 3166-1 alpha-2 country code
     * @return Complete YouTube URL with country parameter
     */
    @NonNull
    private static String buildYouTubeUrl(@NonNull String countryCode) {
        return YOUTUBE_BASE_URL + COUNTRY_PARAM + countryCode.toUpperCase(Locale.ROOT);
    }
}