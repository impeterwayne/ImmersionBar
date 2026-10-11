package com.gyf.immersionbar;

/**
 * Navigation bar type.
 *
 * @author: ifan
 * @date: 8/12/21
 */
public enum NavigationBarType {
    /**
     * Classic 3-button navigation.
     */
    CLASSIC(0),
    /**
     * Full-screen gesture navigation.
     */
    GESTURES(1),
    /**
     * Three-stage gesture navigation.
     */
    GESTURES_THREE_STAGE(2),
    /**
     * Two-button navigation.
     */
    DOUBLE(3),
    /**
     * Unknown.
     */
    UNKNOWN(-1);

    private final int type;

    NavigationBarType(int type) {
        this.type = type;
    }

    public int getType() {
        return type;
    }
}
