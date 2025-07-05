package com.nidoham.hdstreamztv.fragment.model;


/**
 * Interface for handling back press events in fragments
 */
public interface BackPressHandler {
    /**
     * Handle back press event
     * @return true if the event was handled, false otherwise
     */
    boolean onBackPressed();
}
