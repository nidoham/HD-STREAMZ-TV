package bd.nidoham.kiosk;

import android.content.Context;

import org.schabi.newpipe.extractor.ListExtractor;
import org.schabi.newpipe.extractor.Page;
import org.schabi.newpipe.extractor.kiosk.KioskInfo;
import org.schabi.newpipe.extractor.stream.StreamInfoItem;
import org.schabi.newpipe.util.ExtractorHelper;

import io.reactivex.rxjava3.core.Single;

/**
 * A helper class to load Kiosk-like content, including the "Trending" page.
 */
public class KioskContentLoader {

    private final int serviceId;
    private final String url;
    private final Context context;

    public KioskContentLoader(final int serviceId, final String url, final Context context) {
        this.serviceId = serviceId;
        this.url = url;
        this.context = context.getApplicationContext(); // Use application context to prevent leaks
    }

    /**
     * Loads the initial Kiosk/Trending information.
     *
     * @param forceReload whether to force a network reload of the content
     * @return a Single emitting the KioskInfo
     */
    public Single<KioskInfo> loadInitialInfo(final boolean forceReload) {
        return ExtractorHelper.getKioskInfo(serviceId, url, forceReload);
    }

    /**
     * Loads the next page of items.
     *
     * @param nextPage the page object from the previous load
     * @return a Single emitting a page of StreamInfoItems
     */
    public Single<ListExtractor.InfoItemsPage<StreamInfoItem>> loadMoreItems(final Page nextPage) {
        return ExtractorHelper.getMoreKioskItems(serviceId, url, nextPage);
    }
}