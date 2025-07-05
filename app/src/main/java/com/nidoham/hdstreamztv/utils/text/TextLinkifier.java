// PASTE THIS ENTIRE CODE INTO YOUR TextLinkifier.java FILE

package com.nidoham.hdstreamztv.utils.text;

import android.content.Context;
import android.text.SpannableStringBuilder;
import android.text.method.LinkMovementMethod; // --- ADD THIS IMPORT ---
import android.text.style.URLSpan;
import android.text.util.Linkify;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.text.HtmlCompat;

import org.schabi.newpipe.extractor.StreamingService;
import org.schabi.newpipe.extractor.stream.Description;

import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.noties.markwon.Markwon;
import io.noties.markwon.linkify.LinkifyPlugin;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public final class TextLinkifier {
    public static final String TAG = TextLinkifier.class.getSimpleName();

    // --- MODIFIED --- This no longer relies on the missing HashtagLongPressClickableSpan
    private static final Pattern HASHTAGS_PATTERN = Pattern.compile("(#[\\p{L}0-9_]+)");

    public static final Consumer<TextView> SET_LINK_MOVEMENT_METHOD =
            // --- MODIFIED --- Replaced LongPressLinkMovementMethod with the standard Android one
            v -> v.setMovementMethod(LinkMovementMethod.getInstance());

    private TextLinkifier() {}

    public static void fromDescription(@NonNull final TextView textView,
                                       @NonNull final Description description,
                                       final int htmlCompatFlag,
                                       @Nullable final StreamingService relatedInfoService,
                                       @Nullable final String relatedStreamUrl,
                                       @NonNull final CompositeDisposable disposables,
                                       @Nullable final Consumer<TextView> onCompletion) {
        switch (description.getType()) {
            case Description.HTML:
                fromHtml(textView, description.getContent(), htmlCompatFlag,
                        relatedInfoService, relatedStreamUrl, disposables, onCompletion);
                break;
            case Description.MARKDOWN:
                fromMarkdown(textView, description.getContent(),
                        relatedInfoService, relatedStreamUrl, disposables, onCompletion);
                break;
            case Description.PLAIN_TEXT:
            default:
                fromPlainText(textView, description.getContent(),
                        relatedInfoService, relatedStreamUrl, disposables, onCompletion);
                break;
        }
    }

    public static void fromHtml(@NonNull final TextView textView,
                                @NonNull final String htmlBlock,
                                final int htmlCompatFlag,
                                @Nullable final StreamingService relatedInfoService,
                                @Nullable final String relatedStreamUrl,
                                @NonNull final CompositeDisposable disposables,
                                @Nullable final Consumer<TextView> onCompletion) {
        changeLinkIntents(
                textView, HtmlCompat.fromHtml(htmlBlock, htmlCompatFlag), relatedInfoService,
                relatedStreamUrl, disposables, onCompletion);
    }

    public static void fromPlainText(@NonNull final TextView textView,
                                     @NonNull final String plainTextBlock,
                                     @Nullable final StreamingService relatedInfoService,
                                     @Nullable final String relatedStreamUrl,
                                     @NonNull final CompositeDisposable disposables,
                                     @Nullable final Consumer<TextView> onCompletion) {
        textView.setAutoLinkMask(Linkify.WEB_URLS);
        textView.setText(plainTextBlock, TextView.BufferType.SPANNABLE);
        changeLinkIntents(textView, textView.getText(), relatedInfoService,
                relatedStreamUrl, disposables, onCompletion);
    }

    public static void fromMarkdown(@NonNull final TextView textView,
                                    @NonNull final String markdownBlock,
                                    @Nullable final StreamingService relatedInfoService,
                                    @Nullable final String relatedStreamUrl,
                                    @NonNull final CompositeDisposable disposables,
                                    @Nullable final Consumer<TextView> onCompletion) {
        final Markwon markwon = Markwon.builder(textView.getContext())
                .usePlugin(LinkifyPlugin.create()).build();
        changeLinkIntents(textView, markwon.toMarkdown(markdownBlock),
                relatedInfoService, relatedStreamUrl, disposables, onCompletion);
    }

    private static void changeLinkIntents(@NonNull final TextView textView,
                                          @NonNull final CharSequence chars,
                                          @Nullable final StreamingService relatedInfoService,
                                          @Nullable final String relatedStreamUrl,
                                          @NonNull final CompositeDisposable disposables,
                                          @Nullable final Consumer<TextView> onCompletion) {

        // --- MODIFIED ---
        // The original code used RxJava to replace standard URLSpans with custom LongPressClickableSpans
        // and to add custom hashtag/timestamp spans.
        // Since those custom classes are missing, we simplify this method to just set the text directly.
        // The CharSequence 'chars' already contains standard, clickable URLSpans from HtmlCompat/Markwon/Linkify.

        setTextViewCharSequence(textView, chars, onCompletion);
    }

    // --- REMOVED --- All logic from this method is gone as it depended on missing classes.
    private static void addClickListenersOnHashtags(@NonNull final Context context,
                                                    @NonNull final SpannableStringBuilder spannableDescription,
                                                    @NonNull final StreamingService relatedInfoService) {
        // This method is now empty because HashtagLongPressClickableSpan is missing.
    }

    // --- REMOVED --- All logic from this method is gone as it depended on missing classes.
    private static void addClickListenersOnTimestamps(@NonNull final Context context,
                                                      @NonNull final SpannableStringBuilder spannableDescription,
                                                      @NonNull final StreamingService relatedInfoService,
                                                      @NonNull final String relatedStreamUrl,
                                                      @NonNull final CompositeDisposable disposables) {
        // This method is now empty because TimestampExtractor and TimestampLongPressClickableSpan are missing.
    }

    private static void setTextViewCharSequence(@NonNull final TextView textView,
                                                @Nullable final CharSequence charSequence,
                                                @Nullable final Consumer<TextView> onCompletion) {
        textView.setText(charSequence);
        textView.setVisibility(View.VISIBLE);
        if (onCompletion != null) {
            onCompletion.accept(textView);
        }
    }
}