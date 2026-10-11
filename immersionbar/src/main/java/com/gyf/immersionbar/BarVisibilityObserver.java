package com.gyf.immersionbar;

import android.annotation.SuppressLint;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsets;

import androidx.annotation.RequiresApi;

/**
 * Per-window runtime system bar (status bar / navigation bar) visibility observer.
 *
 * <p>Runtime visibility changes (such as {@link ImmersionBar#hideBar} or temporary system gesture shows/hides)
 * do not flow through ContentProvider, so the {@link android.database.ContentObserver} in {@link NavigationBarObserver}
 * cannot detect them. They must be detected via runtime signals:
 * <ul>
 *   <li>API 30+ (R): {@link View#setOnApplyWindowInsetsListener} + {@link WindowInsets#isVisible(int)}</li>
 *   <li>API 16~29: {@link View#setOnSystemUiVisibilityChangeListener} + SYSTEM_UI_FLAG_* bits</li>
 *   <li>API &lt;16: No available runtime API, no-op</li>
 * </ul>
 *
 * <p><b>Why use an invisible anchor view instead of DecorView directly:</b>
 * The insets and systemUi listener slots on each view are single-slot (setting a new listener replaces the previous one).
 * If attached directly to DecorView, it would conflict with and overwrite any listener the app developer set on DecorView.
 * By creating a 0x0 anchor child view and attaching the listener to it, we leverage the framework's dispatch mechanism
 * from DecorView down to child views to receive signals without interfering with the developer's DecorView listener.
 * The anchor is inserted to index 0, ensuring it receives insets before sibling views that might consume them.
 *
 * <p>This class only monitors "when visibility changes" and forwards the actual visibility state to
 * {@link ImmersionBar#onBarVisibilityChange(boolean, boolean)}.
 *
 * @author geyifeng
 */
@SuppressWarnings("deprecation")
final class BarVisibilityObserver {

    private final ImmersionBar mImmersionBar;
    private final ViewGroup mDecorView;

    /**
     * Invisible anchor view (0x0) added to DecorView to host the listener, avoiding slot conflicts with developer's DecorView listener.
     */
    private View mAnchorView;

    /**
     * Previously dispatched visibility states for deduplication; null indicates not yet seeded.
     */
    private Boolean mLastStatusVisible = null;
    private Boolean mLastNavigationVisible = null;

    BarVisibilityObserver(ImmersionBar immersionBar) {
        mImmersionBar = immersionBar;
        Window window = immersionBar.getWindow();
        mDecorView = (ViewGroup) window.getDecorView();
    }

    /**
     * Creates the anchor view, registers the runtime visibility listener according to API version,
     * and seeds the initial state with the current true visibility.
     * Safe to call repeatedly (anchor is created only once, and listeners re-bind to the same slot).
     */
    @SuppressLint("InflateParams")
    void enable() {
        if (Build.VERSION.SDK_INT < Version.JELLY_BEAN) {
            // API <16: No runtime detection API available, no-op (URI mode switch observer only)
            return;
        }
        if (mAnchorView == null) {
            mAnchorView = new View(mDecorView.getContext());
            mDecorView.addView(mAnchorView, new ViewGroup.LayoutParams(0, 0));
        }
        if (Build.VERSION.SDK_INT >= Version.R) {
            enableInsetsListener();
        } else {
            enableSystemUiListener();
        }
        seed();
    }

    /**
     * Removes the anchor view, unregisters listeners, and resets deduplication state.
     */
    void disable() {
        if (mAnchorView != null) {
            // Symmetric with enable(): unregister listener by API level before removing anchor view
            if (Build.VERSION.SDK_INT >= Version.R) {
                mAnchorView.setOnApplyWindowInsetsListener(null);
            } else if (Build.VERSION.SDK_INT >= Version.JELLY_BEAN) {
                mAnchorView.setOnSystemUiVisibilityChangeListener(null);
            }
            mDecorView.removeView(mAnchorView);
            mAnchorView = null;
        }
        mLastStatusVisible = null;
        mLastNavigationVisible = null;
    }

    @RequiresApi(api = Version.R)
    private void enableInsetsListener() {
        mAnchorView.setOnApplyWindowInsetsListener((v, insets) -> {
            dispatch(insets.isVisible(WindowInsets.Type.statusBars()),
                    insets.isVisible(WindowInsets.Type.navigationBars()));
            // Anchor view does not consume insets: return default dispatch result so sibling content views receive them
            return v.onApplyWindowInsets(insets);
        });
    }

    private void enableSystemUiListener() {
        // The callback parameter is the new visibility after change; re-reading DecorView on some older devices may yield stale values.
        mAnchorView.setOnSystemUiVisibilityChangeListener(this::dispatchFromSystemUiFlags);
    }

    /**
     * Seeds the observer once with the current real state.
     * SystemUi listener only fires on subsequent changes; without seeding, the first hide after enable
     * would be treated as an initial seed rather than a change event.
     */
    private void seed() {
        if (Build.VERSION.SDK_INT >= Version.R) {
            WindowInsets insets = mDecorView.getRootWindowInsets();
            if (insets != null) {
                dispatch(insets.isVisible(WindowInsets.Type.statusBars()),
                        insets.isVisible(WindowInsets.Type.navigationBars()));
            }
        } else {
            dispatchFromSystemUiFlags(mDecorView.getSystemUiVisibility());
        }
    }

    private void dispatchFromSystemUiFlags(int visibility) {
        boolean fullscreenBySystemUiFlag =
                (visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) != 0;
        boolean fullscreenByWindowFlag = (mImmersionBar.getWindow().getAttributes().flags
                & android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN) != 0;
        boolean statusVisible = !fullscreenBySystemUiFlag && !fullscreenByWindowFlag;
        boolean navigationVisible = (visibility & View.SYSTEM_UI_FLAG_HIDE_NAVIGATION) == 0;
        dispatch(statusVisible, navigationVisible);
    }

    /**
     * Dispatches to ImmersionBar after deduplication: only fires when status/nav visibility changes relative to last state (or initially).
     */
    private void dispatch(boolean statusVisible, boolean navigationVisible) {
        if (mLastStatusVisible != null && mLastNavigationVisible != null
                && mLastStatusVisible == statusVisible && mLastNavigationVisible == navigationVisible) {
            return;
        }
        mLastStatusVisible = statusVisible;
        mLastNavigationVisible = navigationVisible;
        mImmersionBar.onBarVisibilityChange(statusVisible, navigationVisible);
    }
}
