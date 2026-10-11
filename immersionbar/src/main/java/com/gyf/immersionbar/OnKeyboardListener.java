package com.gyf.immersionbar;

/**
 * Soft keyboard listener.
 *
 * @author geyifeng
 * @date 2017/8/28
 * @deprecated Use {@link OnKeyboardChangedListener} instead.
 */
@Deprecated
public interface OnKeyboardListener {
    /**
     * On keyboard change.
     *
     * @param isPopup        whether the keyboard is shown
     * @param keyboardHeight keyboard height
     */
    void onKeyboardChange(boolean isPopup, int keyboardHeight);
}
