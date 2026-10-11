package com.gyf.immersionbar;

import androidx.annotation.NonNull;

/**
 * Soft keyboard state and height information.
 *
 * @author geyifeng
 * @date 2026/7/26
 */
public class Keyboard {
    /**
     * Whether the soft keyboard is currently visible.
     */
    private final boolean popup;
    /**
     * Current soft keyboard height in pixels.
     */
    private final int height;

    Keyboard(boolean popup, int height) {
        this.popup = popup;
        this.height = height;
    }

    /**
     * Whether the soft keyboard is currently visible.
     *
     * @return true if the soft keyboard is shown, false otherwise
     */
    public boolean isPopup() {
        return popup;
    }

    /**
     * Current soft keyboard height in pixels, or 0 when hidden.
     *
     * @return the current keyboard height
     */
    public int getHeight() {
        return height;
    }

    @NonNull
    @Override
    public String toString() {
        return "Keyboard{" +
                "popup=" + popup +
                ", height=" + height +
                '}';
    }
}
