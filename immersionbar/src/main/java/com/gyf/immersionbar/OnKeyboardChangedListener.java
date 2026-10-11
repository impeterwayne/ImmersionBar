package com.gyf.immersionbar;

import androidx.annotation.NonNull;

/**
 * Listener for soft keyboard visibility and height changes.
 *
 * @author geyifeng
 * @date 2026/7/26
 */
public interface OnKeyboardChangedListener {
    /**
     * Called when the soft keyboard state changes.
     *
     * @param keyboard the keyboard
     */
    void onKeyboardChanged(@NonNull Keyboard keyboard);
}
