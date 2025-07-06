package com.nidoham.hdstreamztv.utils.device;

import android.content.ClipData;
import android.content.ClipDescription; // <-- Added this necessary import
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Build;
import android.os.PersistableBundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.StringRes;

/**
 * A modern and optimized clipboard utility class for Android.
 * This class provides static methods for easy clipboard manipulation.
 *
 * Key Features:
 * - Prevents memory leaks by avoiding static context.
 * - Handles API-specific behaviors for Android 12+ (automatic toast) and Android 13+ (sensitive content).
 * - Provides robust and safe methods for copying, pasting, and clearing the clipboard.
 * - Compatible with Java 17 and targets Android 8 (API 26) to Android 15+.
 */
public final class Clipboard {

    private static final String TAG = "ClipboardUtils";

    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private Clipboard() {
    }

    /**
     * Copies text to the clipboard with a default label.
     *
     * @param context The context to use for accessing system services.
     * @param text    The text to copy. Can be null or empty, in which case the operation fails.
     * @return true if the text was copied successfully, false otherwise.
     */
    public static boolean copyText(@NonNull Context context, @Nullable String text) {
        return copyText(context, "Copied Text", text, false);
    }

    /**
     * Copies text to the clipboard with a custom label and allows marking it as sensitive.
     * On Android 13+ (API 33), sensitive content will be hidden from previews.
     *
     * @param context     The context to use for accessing system services.
     * @param label       A user-visible label for the clip data.
     * @param text        The text to copy. Can be null or empty, in which case the operation fails.
     * @param isSensitive If true, marks the content as sensitive (requires API 33+).
     * @return true if the text was copied successfully, false otherwise.
     */
    public static boolean copyText(@NonNull Context context, @NonNull String label, @Nullable String text, boolean isSensitive) {
        if (text == null || text.trim().isEmpty()) {
            Log.w(TAG, "Attempted to copy null or empty text to clipboard.");
            return false;
        }

        try {
            ClipboardManager clipboardManager = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboardManager == null) {
                Log.e(TAG, "ClipboardManager not available.");
                return false;
            }

            ClipData clipData = ClipData.newPlainText(label, text);

            // Mark content as sensitive on Android 13 (API 33) and higher
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && isSensitive) {
                setSensitiveFlag(clipData);
            }

            clipboardManager.setPrimaryClip(clipData);
            Log.i(TAG, "Text copied to clipboard successfully.");
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Failed to copy text to clipboard", e);
            return false;
        }
    }

    /**
     * Copies text to the clipboard and shows a toast message on success or failure.
     * Note: On Android 12 (API 31) and higher, the system shows its own "copied" toast.
     * This method avoids showing a second toast on these versions.
     *
     * @param context The context to use for showing the toast and accessing services.
     * @param text    The text to copy.
     */
    public static void copyWithToast(@NonNull Context context, @Nullable String text) {
        boolean success = copyText(context, text);

        // Avoid showing a double toast on Android 12 (S) and above.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            String message = success ? "Text copied to clipboard" : "Failed to copy text";
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
        }
        // For Android 12+, the OS provides feedback, so we do nothing here.
        // A failure log is already present in copyText().
    }

    /**
     * An overload for copyWithToast that allows using a string resource for the message.
     *
     * @param context The context to use.
     * @param text The text to copy.
     * @param successMessageRes The string resource ID for the success message.
     * @param failureMessageRes The string resource ID for the failure message.
     */
    public static void copyWithToast(@NonNull Context context, @Nullable String text, @StringRes int successMessageRes, @StringRes int failureMessageRes) {
        boolean success = copyText(context, text);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            Toast.makeText(context, success ? successMessageRes : failureMessageRes, Toast.LENGTH_SHORT).show();
        }
    }


    /**
     * Retrieves the text currently on the clipboard.
     *
     * @param context The context to use for accessing system services.
     * @return The text from the clipboard as a String, or null if the clipboard is empty or contains non-text data.
     */
    @Nullable
    public static String getClipboardText(@NonNull Context context) {
        try {
            ClipboardManager clipboardManager = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboardManager == null || !clipboardManager.hasPrimaryClip()) {
                return null;
            }

            ClipData clipData = clipboardManager.getPrimaryClip();
            if (clipData != null && clipData.getItemCount() > 0) {
                CharSequence text = clipData.getItemAt(0).getText();
                return text != null ? text.toString() : null;
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to retrieve text from clipboard", e);
        }
        return null;
    }

    /**
     * Checks if the clipboard contains plain text.
     *
     * @param context The context to use for accessing system services.
     * @return true if the clipboard has a clip and its MIME type is "text/plain", false otherwise.
     */
    public static boolean hasClipboardText(@NonNull Context context) {
        try {
            ClipboardManager clipboardManager = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboardManager == null || !clipboardManager.hasPrimaryClip()) {
                return false;
            }
            // *** THIS IS THE CORRECTED LINE ***
            return clipboardManager.getPrimaryClipDescription().hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN);
        } catch (Exception e) {
            Log.e(TAG, "Failed to check clipboard content", e);
            return false;
        }
    }

    /**
     * Clears the clipboard by setting an empty clip.
     * <p>
     * WARNING: Starting from Android 10 (API 29), an app can only clear the clipboard
     * if it is the default Input Method Editor (IME) or if it currently has focus.
     * This method may fail silently or throw a SecurityException on newer devices.
     *
     * @param context The context to use for accessing system services.
     * @return true if the clear operation was attempted successfully, false if an error occurred.
     *         Note: A return value of true does not guarantee the clipboard was cleared on API 29+.
     */
    public static boolean clearClipboard(@NonNull Context context) {
        try {
            ClipboardManager clipboardManager = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboardManager == null) {
                Log.e(TAG, "ClipboardManager not available.");
                return false;
            }
            // Setting an empty clip is the way to "clear" the clipboard.
            ClipData clipData = ClipData.newPlainText("", "");
            clipboardManager.setPrimaryClip(clipData);
            return true;
        } catch (SecurityException e) {
            Log.w(TAG, "Failed to clear clipboard. App may not be default IME or have focus.", e);
            return false;
        } catch (Exception e) {
            Log.e(TAG, "An unexpected error occurred while clearing clipboard", e);
            return false;
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private static void setSensitiveFlag(ClipData clipData) {
        // Use ClipDescription.EXTRA_IS_SENSITIVE for Android 13+
        PersistableBundle extras = new PersistableBundle();
        extras.putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true);
        clipData.getDescription().setExtras(extras);
    }
}