package com.nidoham.hdstreamztv.utils;

import android.graphics.Rect;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

/**
 * A RecyclerView.ItemDecoration that adds spacing between items in a grid layout.
 * Supports both edge-inclusive and edge-exclusive spacing, suitable for Streamly's
 * channel grid on Android Phone and Android TV.
 */
public class GridSpacingItemDecoration extends RecyclerView.ItemDecoration {

    private final int spanCount;
    private final int spacing;
    private final boolean includeEdge;

    /**
     * Constructs a GridSpacingItemDecoration for a RecyclerView grid.
     *
     * @param spanCount   The number of columns in the grid.
     * @param spacing     The spacing between items in pixels.
     * @param includeEdge If true, includes spacing on the grid's outer edges; if false,
     *                    spacing is applied only between items.
     */
    public GridSpacingItemDecoration(int spanCount, int spacing, boolean includeEdge) {
        this.spanCount = spanCount;
        this.spacing = spacing;
        this.includeEdge = includeEdge;
    }

    @Override
    public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        int position = parent.getChildAdapterPosition(view); // Item position
        int column = position % spanCount; // Item column

        if (includeEdge) {
            // Add spacing to left and right edges of the grid
            outRect.left = spacing - column * spacing / spanCount;
            outRect.right = (column + 1) * spacing / spanCount;
            // Add top spacing only for the first row
            if (position < spanCount) {
                outRect.top = spacing;
            }
            outRect.bottom = spacing; // Bottom spacing for all items
        } else {
            // Add spacing only between items, not on outer edges
            outRect.left = column * spacing / spanCount;
            outRect.right = spacing - (column + 1) * spacing / spanCount;
            // Add top spacing for all rows except the first
            if (position >= spanCount) {
                outRect.top = spacing;
            }
        }
    }
}