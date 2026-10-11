package com.gyf.immersionbar;

import android.app.Activity;
import android.app.Dialog;
import android.content.res.Configuration;
import android.os.Build;
import android.view.View;

import androidx.core.view.ViewCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;

/**
 * ImmersionBar lifecycle delegate.
 *
 * @author geyifeng
 * @date 2019/4/12 4:01 PM
 */
@SuppressWarnings("deprecation")
class ImmersionDelegate {

    private ImmersionBar mImmersionBar;
    private BarProperties mLastBarProperties;
    /**
     * Whether the host page is in the foreground. Callbacks are not dispatched and the last dispatched snapshot is not updated while invisible.
     */
    private boolean mResumed;
    /**
     * When resuming on Android 15+, the result of WindowInsetsController.hide() may take effect after onResume.
     * Intermediate transition snapshots are not read and dispatched before the configured hide state is truly applied.
     */
    private boolean mAwaitingResumeBarState;
    /**
     * One-time navigation bar visibility hint for the next BarProperties snapshot.
     * Belongs only to the current Delegate/Window, retained across asynchronous posts and cleared immediately upon read; cannot serve as persistent visibility state.
     */
    private Boolean mPendingNavigationBarVisible;
    private final Runnable mDispatchBarPropertiesRunnable = this::dispatchBarProperties;
    private final Runnable mDispatchBarPropertiesAfterFrameRunnable = () -> {
        ImmersionBar immersionBar = mImmersionBar;
        if (!canDispatchBarProperties(immersionBar)) {
            return;
        }
        View decorView = immersionBar.getWindow().getDecorView();
        decorView.removeCallbacks(mDispatchBarPropertiesRunnable);
        ViewCompat.postOnAnimation(decorView, mDispatchBarPropertiesRunnable);
    };

    ImmersionDelegate(Object o) {
        if (o instanceof Activity) {
            if (mImmersionBar == null) {
                mImmersionBar = new ImmersionBar((Activity) o);
            }
        } else if (o instanceof Fragment) {
            if (mImmersionBar == null) {
                if (o instanceof DialogFragment) {
                    mImmersionBar = new ImmersionBar((DialogFragment) o);
                } else {
                    mImmersionBar = new ImmersionBar((Fragment) o);
                }
            }
        } else if (o instanceof android.app.Fragment) {
            if (mImmersionBar == null) {
                if (o instanceof android.app.DialogFragment) {
                    mImmersionBar = new ImmersionBar((android.app.DialogFragment) o);
                } else {
                    mImmersionBar = new ImmersionBar((android.app.Fragment) o);
                }
            }
        }
        if (mImmersionBar != null) {
            mImmersionBar.setImmersionDelegate(this);
        }
    }

    ImmersionDelegate(Activity activity, Dialog dialog) {
        if (mImmersionBar == null) {
            mImmersionBar = new ImmersionBar(activity, dialog);
        }
        // Pass back delegate reference so runtime system bar visibility changes can trigger BarProperties snapshot refreshes
        mImmersionBar.setImmersionDelegate(this);
    }

    public ImmersionBar get() {
        return mImmersionBar;
    }

    void onResume() {
        mResumed = true;
        if (mImmersionBar != null) {
            mAwaitingResumeBarState = shouldAwaitResumeBarState(mImmersionBar);
            mImmersionBar.onResume();
            // System bar changes may have occurred while invisible; perform differential comparison against the last dispatched snapshot upon resume.
            refreshBarProperties();
        }
    }

    void onPause() {
        mResumed = false;
        mAwaitingResumeBarState = false;
        mPendingNavigationBarVisible = null;
        cancelPendingBarPropertiesRefresh();
    }

    void onDestroy() {
        mResumed = false;
        mAwaitingResumeBarState = false;
        mPendingNavigationBarVisible = null;
        cancelPendingBarPropertiesRefresh();
        mLastBarProperties = null;
        if (mImmersionBar != null) {
            mImmersionBar.onDestroy();
            mImmersionBar = null;
        }
    }

    void onConfigurationChanged(Configuration newConfig) {
        if (mImmersionBar != null) {
            mImmersionBar.onConfigurationChanged(newConfig);
            refreshBarProperties();
        }
    }

    /**
     * Called when runtime system bar visibility or navigation mode changes.
     *
     * <p>Handles any runtime events that alter insets (hideBar/showBar, temporary gesture show/hide, 3-button vs gesture navigation switch).
     *
     * <p>Only queues a refresh when the host page is in the foreground and initialized; does not dispatch callbacks or advance snapshots while invisible.
     * When the page resumes, {@link #onResume()} re-reads current properties and dispatches differences.
     */
    void refreshBarProperties() {
        refreshBarProperties(null);
    }

    void refreshBarProperties(Boolean navigationBarVisible) {
        ImmersionBar immersionBar = mImmersionBar;
        if (!canDispatchBarProperties(immersionBar)) {
            return;
        }
        if (navigationBarVisible != null) {
            mPendingNavigationBarVisible = navigationBarVisible;
        }
        View decorView = immersionBar.getWindow().getDecorView();
        decorView.removeCallbacks(mDispatchBarPropertiesRunnable);
        decorView.removeCallbacks(mDispatchBarPropertiesAfterFrameRunnable);
        if (mAwaitingResumeBarState) {
            // Wait across at least one full draw frame before reading WindowInsets asynchronously applied on Android 15+.
            ViewCompat.postOnAnimation(decorView, mDispatchBarPropertiesAfterFrameRunnable);
        } else {
            // Merge multiple change signals within the same message loop into a single snapshot read.
            decorView.post(mDispatchBarPropertiesRunnable);
        }
    }

    private boolean canDispatchBarProperties(ImmersionBar immersionBar) {
        return mResumed && immersionBar != null && immersionBar.initialized()
                && Build.VERSION.SDK_INT >= Version.KITKAT;
    }

    private void cancelPendingBarPropertiesRefresh() {
        ImmersionBar immersionBar = mImmersionBar;
        if (immersionBar == null) {
            return;
        }
        View decorView = immersionBar.getWindow().getDecorView();
        decorView.removeCallbacks(mDispatchBarPropertiesRunnable);
        decorView.removeCallbacks(mDispatchBarPropertiesAfterFrameRunnable);
    }

    private boolean shouldAwaitResumeBarState(ImmersionBar immersionBar) {
        if (Build.VERSION.SDK_INT < Version.VANILLA_ICE_CREAM) {
            return false;
        }
        BarParams barParams = immersionBar.getBarParams();
        return barParams != null && barParams.barHide != BarHide.FLAG_SHOW_BAR;
    }

    private boolean isResumeBarStateApplied(ImmersionBar immersionBar, BarProperties barProperties) {
        BarParams barParams = immersionBar.getBarParams();
        if (barParams == null) {
            return true;
        }
        switch (barParams.barHide) {
            case FLAG_HIDE_STATUS_BAR:
                return !barProperties.isStatusBarVisible();
            case FLAG_HIDE_NAVIGATION_BAR:
                return !barProperties.hasNavigationBar() || !barProperties.isNavigationBarVisible();
            case FLAG_HIDE_BAR:
                return !barProperties.isStatusBarVisible() && !barProperties.isNavigationBarVisible();
            case FLAG_SHOW_BAR:
            default:
                return true;
        }
    }

    void dispatchBarProperties() {
        ImmersionBar immersionBar = mImmersionBar;
        // The host may have paused after posting; verify again at execution time to prevent paused pages from receiving callbacks and advancing snapshots.
        if (!canDispatchBarProperties(immersionBar)) {
            return;
        }
        Boolean navigationBarVisibleHint = mPendingNavigationBarVisible;
        mPendingNavigationBarVisible = null;
        BarProperties lastBarProperties = mLastBarProperties;
        BarProperties barProperties = BarPropertiesUtils.getBarProperties(
                immersionBar.getWindow(), navigationBarVisibleHint);
        if (mAwaitingResumeBarState) {
            if (!isResumeBarStateApplied(immersionBar, barProperties)) {
                // Stale insets from Android 15 resume in progress; await the next real insets signal from BarVisibilityObserver.
                return;
            }
            mAwaitingResumeBarState = false;
        }

        // On initial snapshot (no previous snapshot), the three dispatches below fire simultaneously with firstCallback marked true.
        boolean firstCallback = lastBarProperties == null;
        int barPropertiesChanges = barProperties.calculateChanges(lastBarProperties);
        int statusBarChanges = StatusBar.calculateChanges(barPropertiesChanges);
        int navigationBarChanges = NavigationBar.calculateChanges(barPropertiesChanges);
        barProperties.setFirstCallback(firstCallback);
        barProperties.setChangedFlags(barPropertiesChanges);

        if (barPropertiesChanges != BarProperties.CHANGE_NONE) {
            mLastBarProperties = new BarProperties(barProperties);
            immersionBar.dispatchOnBarPropertiesChanged(barProperties);
        }
        if (statusBarChanges != StatusBar.CHANGE_NONE) {
            immersionBar.dispatchOnStatusBarChanged(StatusBar.from(barProperties, firstCallback, statusBarChanges));
        }
        if (navigationBarChanges != NavigationBar.CHANGE_NONE) {
            immersionBar.dispatchOnNavigationBarChanged(
                    NavigationBar.from(barProperties, firstCallback, navigationBarChanges));
        }
    }


}
