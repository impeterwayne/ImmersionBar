package com.gyf.immersionbar;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * System bar properties snapshot.
 *
 * @author geyifeng
 * @date 2019-05-10 18:43
 */
public class BarProperties {

    public static final int CHANGE_NONE = 0;
    public static final int CHANGE_PORTRAIT = 1;
    public static final int CHANGE_LANDSCAPE_LEFT = 1 << 1;
    public static final int CHANGE_LANDSCAPE_RIGHT = 1 << 2;
    public static final int CHANGE_NOTCH_SCREEN = 1 << 3;
    public static final int CHANGE_HAS_NAVIGATION_BAR = 1 << 4;
    public static final int CHANGE_NAVIGATION_AT_BOTTOM = 1 << 5;
    public static final int CHANGE_NAVIGATION_BAR_TYPE = 1 << 6;
    public static final int CHANGE_GESTURE_NAVIGATION = 1 << 7;
    public static final int CHANGE_STATUS_BAR_VISIBILITY = 1 << 8;
    public static final int CHANGE_NAVIGATION_BAR_VISIBILITY = 1 << 9;
    public static final int CHANGE_STATUS_BAR_HEIGHT = 1 << 10;
    public static final int CHANGE_STATUS_BAR_HEIGHT_IGNORING_VISIBILITY = 1 << 11;
    public static final int CHANGE_NAVIGATION_BAR_HEIGHT = 1 << 12;
    public static final int CHANGE_NAVIGATION_BAR_HEIGHT_IGNORING_VISIBILITY = 1 << 13;
    public static final int CHANGE_NAVIGATION_BAR_WIDTH = 1 << 14;
    public static final int CHANGE_NOTCH_HEIGHT = 1 << 15;
    public static final int CHANGE_ACTION_BAR_HEIGHT = 1 << 16;
    public static final int CHANGE_ALL = (1 << 17) - 1;

    @IntDef(flag = true, value = {
            CHANGE_NONE,
            CHANGE_PORTRAIT,
            CHANGE_LANDSCAPE_LEFT,
            CHANGE_LANDSCAPE_RIGHT,
            CHANGE_NOTCH_SCREEN,
            CHANGE_HAS_NAVIGATION_BAR,
            CHANGE_NAVIGATION_AT_BOTTOM,
            CHANGE_NAVIGATION_BAR_TYPE,
            CHANGE_GESTURE_NAVIGATION,
            CHANGE_STATUS_BAR_VISIBILITY,
            CHANGE_NAVIGATION_BAR_VISIBILITY,
            CHANGE_STATUS_BAR_HEIGHT,
            CHANGE_STATUS_BAR_HEIGHT_IGNORING_VISIBILITY,
            CHANGE_NAVIGATION_BAR_HEIGHT,
            CHANGE_NAVIGATION_BAR_HEIGHT_IGNORING_VISIBILITY,
            CHANGE_NAVIGATION_BAR_WIDTH,
            CHANGE_NOTCH_HEIGHT,
            CHANGE_ACTION_BAR_HEIGHT,
            CHANGE_ALL
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface Change {
    }

    /**
     * Whether the orientation is portrait.
     */
    private boolean portrait;
    /**
     * Whether the orientation is landscape-left (rotated 90 degrees).
     */
    private boolean landscapeLeft;
    /**
     * Whether the orientation is landscape-right (rotated 270 degrees).
     */
    private boolean landscapeRight;
    /**
     * Whether the device has a notch / display cutout.
     */
    private boolean notchScreen;
    /**
     * Whether the device has a navigation bar.
     */
    private boolean hasNavigationBar;
    /**
     * Whether the navigation bar is at the bottom (false indicates side placement, e.g. 3-button navigation in landscape).
     */
    private boolean navigationAtBottom;
    /**
     * Navigation bar type (classic 3-button / gestures / 3-stage gestures / 2-button / unknown).
     */
    private NavigationBarType navigationBarType = NavigationBarType.UNKNOWN;
    /**
     * Whether gesture navigation is enabled.
     */
    private boolean gestureNavigation;
    /**
     * Whether the status bar is currently visible (updates dynamically at runtime and dispatched via OnBarPropertiesChangedListener).
     */
    private boolean statusBarVisible = true;
    /**
     * Whether the navigation bar is currently visible (updates dynamically at runtime and dispatched via OnBarPropertiesChangedListener).
     */
    private boolean navigationBarVisible = true;
    /**
     * Current status bar height in pixels, or 0 when hidden; portrait and landscape status bar heights may differ on cutout displays.
     */
    private int statusBarHeight;
    /**
     * Status bar height ignoring visibility (returns actual size even when hidden) in pixels.
     */
    private int statusBarHeightIgnoringVisibility;
    /**
     * Navigation bar height in pixels.
     */
    private int navigationBarHeight;
    /**
     * Navigation bar height ignoring visibility (returns actual size even when hidden) in pixels.
     */
    private int navigationBarHeightIgnoringVisibility;
    /**
     * Navigation bar width in pixels.
     */
    private int navigationBarWidth;
    /**
     * Notch / display cutout height in pixels.
     */
    private int notchHeight;
    /**
     * ActionBar height in pixels.
     */
    private int actionBarHeight;
    /**
     * Whether this is the first callback dispatched (true on initial dispatch). Belongs to dispatch metadata rather than window state:
     * Excluded from equals/hashCode deduplication and copy constructor.
     */
    private boolean firstCallback;
    /**
     * Bitmask flags indicating properties that changed relative to previously dispatched snapshot. Belongs to dispatch metadata; excluded from equals/hashCode or snapshot copying.
     */
    @Change
    private int changedFlags = CHANGE_NONE;

    BarProperties() {
    }

    /**
     * Copy constructor for retaining the last dispatched snapshot for deduplication.
     * Copies window state fields only; firstCallback is per-dispatch metadata and not copied.
     *
     * @param other source object
     */
    BarProperties(BarProperties other) {
        this.portrait = other.portrait;
        this.landscapeLeft = other.landscapeLeft;
        this.landscapeRight = other.landscapeRight;
        this.notchScreen = other.notchScreen;
        this.hasNavigationBar = other.hasNavigationBar;
        this.navigationAtBottom = other.navigationAtBottom;
        this.navigationBarType = other.navigationBarType;
        this.gestureNavigation = other.gestureNavigation;
        this.statusBarVisible = other.statusBarVisible;
        this.navigationBarVisible = other.navigationBarVisible;
        this.statusBarHeight = other.statusBarHeight;
        this.statusBarHeightIgnoringVisibility = other.statusBarHeightIgnoringVisibility;
        this.navigationBarHeight = other.navigationBarHeight;
        this.navigationBarHeightIgnoringVisibility = other.navigationBarHeightIgnoringVisibility;
        this.navigationBarWidth = other.navigationBarWidth;
        this.notchHeight = other.notchHeight;
        this.actionBarHeight = other.actionBarHeight;
    }

    public boolean isPortrait() {
        return portrait;
    }

    void setPortrait(boolean portrait) {
        this.portrait = portrait;
    }

    public boolean isLandscapeLeft() {
        return landscapeLeft;
    }

    void setLandscapeLeft(boolean landscapeLeft) {
        this.landscapeLeft = landscapeLeft;
    }

    public boolean isLandscapeRight() {
        return landscapeRight;
    }

    void setLandscapeRight(boolean landscapeRight) {
        this.landscapeRight = landscapeRight;
    }

    public boolean isNotchScreen() {
        return notchScreen;
    }

    void setNotchScreen(boolean notchScreen) {
        this.notchScreen = notchScreen;
    }

    public boolean hasNavigationBar() {
        return hasNavigationBar;
    }

    void setNavigationBar(boolean hasNavigationBar) {
        this.hasNavigationBar = hasNavigationBar;
    }

    public boolean isNavigationAtBottom() {
        return navigationAtBottom;
    }

    void setNavigationAtBottom(boolean navigationAtBottom) {
        this.navigationAtBottom = navigationAtBottom;
    }

    public NavigationBarType getNavigationBarType() {
        return navigationBarType;
    }

    void setNavigationBarType(NavigationBarType navigationBarType) {
        this.navigationBarType = navigationBarType;
    }

    public boolean isGestureNavigation() {
        return gestureNavigation;
    }

    void setGestureNavigation(boolean gestureNavigation) {
        this.gestureNavigation = gestureNavigation;
    }

    public boolean isStatusBarVisible() {
        return statusBarVisible;
    }

    void setStatusBarVisible(boolean statusBarVisible) {
        this.statusBarVisible = statusBarVisible;
    }

    public boolean isNavigationBarVisible() {
        return navigationBarVisible;
    }

    void setNavigationBarVisible(boolean navigationBarVisible) {
        this.navigationBarVisible = navigationBarVisible;
    }

    /**
     * Current status bar height in pixels, or 0 when hidden.
     *
     * @return the current status bar height
     */
    public int getStatusBarHeight() {
        return statusBarHeight;
    }

    void setStatusBarHeight(int statusBarHeight) {
        this.statusBarHeight = statusBarHeight;
    }

    /**
     * Status bar height ignoring visibility (returns actual size even when hidden) in pixels.
     *
     * @return the status bar height ignoring visibility
     */
    public int getStatusBarHeightIgnoringVisibility() {
        return statusBarHeightIgnoringVisibility;
    }

    void setStatusBarHeightIgnoringVisibility(int statusBarHeightIgnoringVisibility) {
        this.statusBarHeightIgnoringVisibility = statusBarHeightIgnoringVisibility;
    }

    public int getNavigationBarHeight() {
        return navigationBarHeight;
    }

    void setNavigationBarHeight(int navigationBarHeight) {
        this.navigationBarHeight = navigationBarHeight;
    }

    public int getNavigationBarHeightIgnoringVisibility() {
        return navigationBarHeightIgnoringVisibility;
    }

    void setNavigationBarHeightIgnoringVisibility(int navigationBarHeightIgnoringVisibility) {
        this.navigationBarHeightIgnoringVisibility = navigationBarHeightIgnoringVisibility;
    }

    public int getNavigationBarWidth() {
        return navigationBarWidth;
    }

    void setNavigationBarWidth(int navigationBarWidth) {
        this.navigationBarWidth = navigationBarWidth;
    }

    public int getNotchHeight() {
        return notchHeight;
    }

    void setNotchHeight(int notchHeight) {
        this.notchHeight = notchHeight;
    }

    public int getActionBarHeight() {
        return actionBarHeight;
    }

    void setActionBarHeight(int actionBarHeight) {
        this.actionBarHeight = actionBarHeight;
    }

    /**
     * Whether this is the first callback dispatched (initial snapshot dispatch).
     *
     * @return true if this is the first callback received by the listener
     */
    public boolean isFirstCallback() {
        return firstCallback;
    }

    void setFirstCallback(boolean firstCallback) {
        this.firstCallback = firstCallback;
    }

    /**
     * Determines whether the specified property changed relative to the previously dispatched snapshot.
     * When multiple flags are bitwise-ORed, returns true if any property changed.
     * On the first callback without historical snapshot, all properties are considered changed.
     *
     * @param change one or more CHANGE_* constants bitwise-ORed
     * @return true if at least one specified property has changed
     */
    public boolean hasChanged(@Change int change) {
        return (changedFlags & change) != 0;
    }

    void setChangedFlags(@Change int changedFlags) {
        this.changedFlags = changedFlags;
    }

    /**
     * Calculates the bitmask of properties changed in current snapshot relative to previously dispatched snapshot.
     *
     * @param last previously dispatched snapshot, or null for initial snapshot
     * @return one or more CHANGE_* constants bitwise-ORed
     */
    @Change
    int calculateChanges(BarProperties last) {
        if (last == null) {
            return CHANGE_ALL;
        }
        int changes = CHANGE_NONE;
        if (last.portrait != portrait) {
            changes |= CHANGE_PORTRAIT;
        }
        if (last.landscapeLeft != landscapeLeft) {
            changes |= CHANGE_LANDSCAPE_LEFT;
        }
        if (last.landscapeRight != landscapeRight) {
            changes |= CHANGE_LANDSCAPE_RIGHT;
        }
        if (last.notchScreen != notchScreen) {
            changes |= CHANGE_NOTCH_SCREEN;
        }
        if (last.hasNavigationBar != hasNavigationBar) {
            changes |= CHANGE_HAS_NAVIGATION_BAR;
        }
        if (last.navigationAtBottom != navigationAtBottom) {
            changes |= CHANGE_NAVIGATION_AT_BOTTOM;
        }
        if (last.navigationBarType != navigationBarType) {
            changes |= CHANGE_NAVIGATION_BAR_TYPE;
        }
        if (last.gestureNavigation != gestureNavigation) {
            changes |= CHANGE_GESTURE_NAVIGATION;
        }
        if (last.statusBarVisible != statusBarVisible) {
            changes |= CHANGE_STATUS_BAR_VISIBILITY;
        }
        if (last.navigationBarVisible != navigationBarVisible) {
            changes |= CHANGE_NAVIGATION_BAR_VISIBILITY;
        }
        if (last.statusBarHeight != statusBarHeight) {
            changes |= CHANGE_STATUS_BAR_HEIGHT;
        }
        if (last.statusBarHeightIgnoringVisibility != statusBarHeightIgnoringVisibility) {
            changes |= CHANGE_STATUS_BAR_HEIGHT_IGNORING_VISIBILITY;
        }
        if (last.navigationBarHeight != navigationBarHeight) {
            changes |= CHANGE_NAVIGATION_BAR_HEIGHT;
        }
        if (last.navigationBarHeightIgnoringVisibility != navigationBarHeightIgnoringVisibility) {
            changes |= CHANGE_NAVIGATION_BAR_HEIGHT_IGNORING_VISIBILITY;
        }
        if (last.navigationBarWidth != navigationBarWidth) {
            changes |= CHANGE_NAVIGATION_BAR_WIDTH;
        }
        if (last.notchHeight != notchHeight) {
            changes |= CHANGE_NOTCH_HEIGHT;
        }
        if (last.actionBarHeight != actionBarHeight) {
            changes |= CHANGE_ACTION_BAR_HEIGHT;
        }
        return changes;
    }

    @NonNull
    @Override
    public String toString() {
        return "BarProperties{" +
                "portrait=" + portrait +
                ", landscapeLeft=" + landscapeLeft +
                ", landscapeRight=" + landscapeRight +
                ", notchScreen=" + notchScreen +
                ", hasNavigationBar=" + hasNavigationBar +
                ", navigationAtBottom=" + navigationAtBottom +
                ", navigationBarType=" + navigationBarType +
                ", gestureNavigation=" + gestureNavigation +
                ", statusBarVisible=" + statusBarVisible +
                ", navigationBarVisible=" + navigationBarVisible +
                ", statusBarHeight=" + statusBarHeight +
                ", statusBarHeightIgnoringVisibility=" + statusBarHeightIgnoringVisibility +
                ", navigationBarHeight=" + navigationBarHeight +
                ", navigationBarHeightIgnoringVisibility=" + navigationBarHeightIgnoringVisibility +
                ", navigationBarWidth=" + navigationBarWidth +
                ", notchHeight=" + notchHeight +
                ", actionBarHeight=" + actionBarHeight +
                ", firstCallback=" + firstCallback +
                ", changedFlags=" + changedFlags +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        BarProperties that = (BarProperties) o;
        return portrait == that.portrait
                && landscapeLeft == that.landscapeLeft
                && landscapeRight == that.landscapeRight
                && notchScreen == that.notchScreen
                && hasNavigationBar == that.hasNavigationBar
                && navigationAtBottom == that.navigationAtBottom
                && gestureNavigation == that.gestureNavigation
                && statusBarVisible == that.statusBarVisible
                && navigationBarVisible == that.navigationBarVisible
                && statusBarHeight == that.statusBarHeight
                && statusBarHeightIgnoringVisibility == that.statusBarHeightIgnoringVisibility
                && navigationBarHeight == that.navigationBarHeight
                && navigationBarHeightIgnoringVisibility == that.navigationBarHeightIgnoringVisibility
                && navigationBarWidth == that.navigationBarWidth
                && notchHeight == that.notchHeight
                && actionBarHeight == that.actionBarHeight
                && navigationBarType == that.navigationBarType;
    }

    @Override
    public int hashCode() {
        int result = (portrait ? 1 : 0);
        result = 31 * result + (landscapeLeft ? 1 : 0);
        result = 31 * result + (landscapeRight ? 1 : 0);
        result = 31 * result + (notchScreen ? 1 : 0);
        result = 31 * result + (hasNavigationBar ? 1 : 0);
        result = 31 * result + (navigationAtBottom ? 1 : 0);
        result = 31 * result + (gestureNavigation ? 1 : 0);
        result = 31 * result + (statusBarVisible ? 1 : 0);
        result = 31 * result + (navigationBarVisible ? 1 : 0);
        result = 31 * result + (navigationBarType != null ? navigationBarType.hashCode() : 0);
        result = 31 * result + statusBarHeight;
        result = 31 * result + statusBarHeightIgnoringVisibility;
        result = 31 * result + navigationBarHeight;
        result = 31 * result + navigationBarHeightIgnoringVisibility;
        result = 31 * result + navigationBarWidth;
        result = 31 * result + notchHeight;
        result = 31 * result + actionBarHeight;
        return result;
    }
}
