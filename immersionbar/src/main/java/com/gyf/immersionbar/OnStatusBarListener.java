package com.gyf.immersionbar;

/**
 * The interface On status bar listener.
 * Status bar visibility listener.
 *
 * @author geyifeng
 * @deprecated Use {@link OnStatusBarChangedListener} instead.
 */
@Deprecated
public interface OnStatusBarListener {
    /**
     * On status bar change.
     *
     * @param show whether the status bar is visible
     */
    void onStatusBarChange(boolean show);
}
