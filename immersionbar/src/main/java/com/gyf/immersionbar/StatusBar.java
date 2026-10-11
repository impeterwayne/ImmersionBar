package com.gyf.immersionbar;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Status bar information snapshot.
 *
 * @author geyifeng
 * @date 2026 /7/19
 */
public class StatusBar {

    public static final int CHANGE_NONE = 0;
    public static final int CHANGE_VISIBILITY = 1;
    public static final int CHANGE_HEIGHT = 1 << 1;
    public static final int CHANGE_HEIGHT_IGNORING_VISIBILITY = 1 << 2;
    public static final int CHANGE_ALL = CHANGE_VISIBILITY | CHANGE_HEIGHT | CHANGE_HEIGHT_IGNORING_VISIBILITY;

    @IntDef(flag = true, value = {
            CHANGE_NONE,
            CHANGE_VISIBILITY,
            CHANGE_HEIGHT,
            CHANGE_HEIGHT_IGNORING_VISIBILITY,
            CHANGE_ALL
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface Change {
    }

    /**
     * Whether the status bar is currently visible.
     */
    private final boolean visible;
    /**
     * Current status bar height in pixels, or 0 when hidden.
     */
    private final int height;
    /**
     * Status bar height ignoring visibility (returns actual size even when hidden) in pixels.
     */
    private final int heightIgnoringVisibility;
    /**
     * Whether this is the first callback dispatched to the listener.
     */
    private final boolean firstCallback;
    /**
     * Bitmask flags indicating properties that changed relative to the previous callback.
     */
    @Change
    private final int changedFlags;

    StatusBar(boolean visible, int height, int heightIgnoringVisibility, boolean firstCallback,
            @Change int changedFlags) {
        this.visible = visible;
        this.height = height;
        this.heightIgnoringVisibility = heightIgnoringVisibility;
        this.firstCallback = firstCallback;
        this.changedFlags = changedFlags;
    }

    static StatusBar from(BarProperties barProperties, boolean firstCallback, @Change int changedFlags) {
        return new StatusBar(barProperties.isStatusBarVisible(), barProperties.getStatusBarHeight(),
                barProperties.getStatusBarHeightIgnoringVisibility(), firstCallback, changedFlags);
    }

    @Change
    static int calculateChanges(@BarProperties.Change int barPropertiesChanges) {
        int changes = CHANGE_NONE;
        if ((barPropertiesChanges & BarProperties.CHANGE_STATUS_BAR_VISIBILITY) != 0) {
            changes |= CHANGE_VISIBILITY;
        }
        if ((barPropertiesChanges & BarProperties.CHANGE_STATUS_BAR_HEIGHT) != 0) {
            changes |= CHANGE_HEIGHT;
        }
        if ((barPropertiesChanges & BarProperties.CHANGE_STATUS_BAR_HEIGHT_IGNORING_VISIBILITY) != 0) {
            changes |= CHANGE_HEIGHT_IGNORING_VISIBILITY;
        }
        return changes;
    }

    /**
     * Whether the status bar is currently visible.
     *
     * @return true if visible, false otherwise
     */
    public boolean isVisible() {
        return visible;
    }

    /**
     * Current status bar height in pixels, or 0 when hidden.
     *
     * @return the current status bar height
     */
    public int getHeight() {
        return height;
    }

    /**
     * Status bar height ignoring visibility (returns actual size even when hidden) in pixels.
     *
     * @return the status bar height ignoring visibility
     */
    public int getHeightIgnoringVisibility() {
        return heightIgnoringVisibility;
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
        return "StatusBar{" +
                "visible=" + visible +
                ", height=" + height +
                ", heightIgnoringVisibility=" + heightIgnoringVisibility +
                ", firstCallback=" + firstCallback +
                ", changedFlags=" + changedFlags +
                '}';
    }
}
