package com.gyf.immersionbar;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Navigation bar information snapshot.
 *
 * @author geyifeng
 * @date 2026 /7/19
 */
public class NavigationBar {

    public static final int CHANGE_NONE = 0;
    public static final int CHANGE_VISIBILITY = 1;
    public static final int CHANGE_HEIGHT = 1 << 1;
    public static final int CHANGE_HEIGHT_IGNORING_VISIBILITY = 1 << 2;
    public static final int CHANGE_WIDTH = 1 << 3;
    public static final int CHANGE_TYPE = 1 << 4;
    public static final int CHANGE_ALL = CHANGE_VISIBILITY | CHANGE_HEIGHT
            | CHANGE_HEIGHT_IGNORING_VISIBILITY | CHANGE_WIDTH | CHANGE_TYPE;

    @IntDef(flag = true, value = {
            CHANGE_NONE,
            CHANGE_VISIBILITY,
            CHANGE_HEIGHT,
            CHANGE_HEIGHT_IGNORING_VISIBILITY,
            CHANGE_WIDTH,
            CHANGE_TYPE,
            CHANGE_ALL
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface Change {
    }

    /**
     * Whether the navigation bar is currently visible.
     */
    private final boolean visible;
    /**
     * Navigation bar height in pixels.
     */
    private final int height;
    /**
     * Navigation bar height ignoring visibility (returns actual size even when hidden) in pixels.
     */
    private final int heightIgnoringVisibility;
    /**
     * Navigation bar width in pixels (valid when navigation bar is placed on the side).
     */
    private final int width;
    /**
     * Navigation bar type (classic 3-button / gestures / 3-stage gestures / 2-button / unknown).
     */
    private final NavigationBarType type;
    /**
     * Whether this is the first callback dispatched to the listener.
     */
    private final boolean firstCallback;
    /**
     * Bitmask flags indicating properties that changed relative to the previous callback.
     */
    @Change
    private final int changedFlags;

    NavigationBar(boolean visible, int height, int heightIgnoringVisibility, int width, NavigationBarType type,
            boolean firstCallback, @Change int changedFlags) {
        this.visible = visible;
        this.height = height;
        this.heightIgnoringVisibility = heightIgnoringVisibility;
        this.width = width;
        this.type = type;
        this.firstCallback = firstCallback;
        this.changedFlags = changedFlags;
    }

    static NavigationBar from(BarProperties barProperties, boolean firstCallback, @Change int changedFlags) {
        return new NavigationBar(barProperties.isNavigationBarVisible(), barProperties.getNavigationBarHeight(),
                barProperties.getNavigationBarHeightIgnoringVisibility(), barProperties.getNavigationBarWidth(),
                barProperties.getNavigationBarType(), firstCallback, changedFlags);
    }

    @Change
    static int calculateChanges(@BarProperties.Change int barPropertiesChanges) {
        int changes = CHANGE_NONE;
        if ((barPropertiesChanges & BarProperties.CHANGE_NAVIGATION_BAR_VISIBILITY) != 0) {
            changes |= CHANGE_VISIBILITY;
        }
        if ((barPropertiesChanges & BarProperties.CHANGE_NAVIGATION_BAR_HEIGHT) != 0) {
            changes |= CHANGE_HEIGHT;
        }
        if ((barPropertiesChanges & BarProperties.CHANGE_NAVIGATION_BAR_HEIGHT_IGNORING_VISIBILITY) != 0) {
            changes |= CHANGE_HEIGHT_IGNORING_VISIBILITY;
        }
        if ((barPropertiesChanges & BarProperties.CHANGE_NAVIGATION_BAR_WIDTH) != 0) {
            changes |= CHANGE_WIDTH;
        }
        if ((barPropertiesChanges & BarProperties.CHANGE_NAVIGATION_BAR_TYPE) != 0) {
            changes |= CHANGE_TYPE;
        }
        return changes;
    }

    /**
     * Whether the navigation bar is currently visible.
     *
     * @return true if visible, false otherwise
     */
    public boolean isVisible() {
        return visible;
    }

    /**
     * Navigation bar height in pixels.
     *
     * @return the navigation bar height in pixels
     */
    public int getHeight() {
        return height;
    }

    /**
     * Navigation bar height ignoring visibility (returns actual size even when hidden) in pixels.
     *
     * @return the navigation bar height ignoring visibility in pixels
     */
    public int getHeightIgnoringVisibility() {
        return heightIgnoringVisibility;
    }

    /**
     * Navigation bar width in pixels (valid when navigation bar is placed on the side).
     *
     * @return the navigation bar width in pixels
     */
    public int getWidth() {
        return width;
    }

    /**
     * Navigation bar type.
     *
     * @return the NavigationBarType
     */
    @NonNull
    public NavigationBarType getType() {
        return type;
    }

    /**
     * Whether this is the first callback dispatched to the listener.
     *
     * @return true if this is the first callback dispatched to the listener
     */
    public boolean isFirstCallback() {
        return firstCallback;
    }

    /**
     * Determines whether the specified property changed relative to the previous callback.
     * When multiple flags are bitwise-ORed, returns true if any property changed.
     * On the first callback without historical snapshot, all properties are considered changed.
     *
     * @param change one or more CHANGE_* constants bitwise-ORed
     * @return true if at least one specified property has changed
     */
    public boolean hasChanged(@Change int change) {
        return (changedFlags & change) != 0;
    }

    @NonNull
    @Override
    public String toString() {
        return "NavigationBar{" +
                "visible=" + visible +
                ", height=" + height +
                ", heightIgnoringVisibility=" + heightIgnoringVisibility +
                ", width=" + width +
                ", type=" + type +
                ", firstCallback=" + firstCallback +
                ", changedFlags=" + changedFlags +
                '}';
    }
}
