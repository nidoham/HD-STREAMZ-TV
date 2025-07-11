package bd.nidoham.youtube.home;

import android.content.Context;
import bd.nidoham.kiosk.KioskContentLoader;
import org.schabi.newpipe.extractor.ListExtractor;
import org.schabi.newpipe.extractor.NewPipe;
import org.schabi.newpipe.extractor.Page;
import org.schabi.newpipe.extractor.ServiceList;
import org.schabi.newpipe.extractor.StreamingService;
import org.schabi.newpipe.extractor.exceptions.ExtractionException;
import org.schabi.newpipe.extractor.kiosk.KioskInfo;
import org.schabi.newpipe.extractor.kiosk.KioskList;
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandlerFactory;
import org.schabi.newpipe.extractor.stream.StreamInfoItem;
import org.schabi.newpipe.util.Localization;

import java.util.List;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * Manages the fetching, state, and pagination of trending videos.
 * This class is designed to be used by an Activity or a custom View,
 * handling all business logic and leaving the UI updates to a listener.
 */
public class TrendingVideosExecutor {

    /**
     * Listener interface for receiving callbacks from the executor.
     * The UI layer (e.g., an Activity) will implement this.
     */
    public interface Listener {
        void showLoading(boolean isLoading);
        void showInitialVideos(List<StreamInfoItem> items);
        void showMoreVideos(List<StreamInfoItem> items);
        void showEmptyState();
        void showError(String message);
    }

    private final Listener listener;
    private final KioskContentLoader contentLoader;
    private final CompositeDisposable disposables = new CompositeDisposable();

    private Page nextPage;
    private boolean isLoading = false;

    public TrendingVideosExecutor(Context context, Listener listener) throws ExtractionException {
        this.listener = listener;

        // Configure for YouTube's Trending "Kiosk"
        final int serviceId = ServiceList.YouTube.getServiceId();
        final StreamingService service = NewPipe.getService(serviceId);
        final KioskList kioskList = service.getKioskList();
        final String trendingKioskId = kioskList.getDefaultKioskId(); // "Trending" is default
        final ListLinkHandlerFactory factory = kioskList.getListLinkHandlerFactoryByType(trendingKioskId);
        final String trendingUrl = factory.fromId(trendingKioskId).getUrl();

        this.contentLoader = new KioskContentLoader(serviceId, trendingUrl, context);
    }

    /**
     * Starts the initial fetch for trending videos.
     *
     * @param forceReload if true, bypasses the cache.
     */
    public void fetchTrendingVideos(boolean forceReload) {
        if (isLoading) return;
        setLoading(true);

        disposables.add(contentLoader.loadInitialInfo(forceReload)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::handleInitialResult, this::handleError)
        );
    }

    /**
     * Fetches the next page of trending videos if available.
     */
    public void fetchMoreVideos() {
        if (isLoading || nextPage == null) return;
        setLoading(true);

        disposables.add(contentLoader.loadMoreItems(nextPage)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::handleMoreResults, this::handleError)
        );
    }

    private void handleInitialResult(KioskInfo result) {
        setLoading(false);
        this.nextPage = result.getNextPage();

        if (result.getRelatedItems().isEmpty()) {
            listener.showEmptyState();
        } else {
            listener.showInitialVideos(result.getRelatedItems());
        }
    }

    private void handleMoreResults(ListExtractor.InfoItemsPage<StreamInfoItem> result) {
        setLoading(false);
        this.nextPage = result.getNextPage();
        listener.showMoreVideos(result.getItems());
    }

    private void handleError(Throwable error) {
        setLoading(false);
        this.nextPage = null; // Stop pagination on error
        listener.showError("An error occurred: " + error.getMessage());
    }

    private void setLoading(boolean loading) {
        this.isLoading = loading;
        listener.showLoading(loading);
    }

    /**
     * Must be called to clean up subscriptions and prevent memory leaks.
     * Typically called in an Activity's onDestroy().
     */
    public void dispose() {
        disposables.clear();
    }
}