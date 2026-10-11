package com.gyf.immersionbar;

/**
 * @author geyifeng
 * @date 2019/4/12 6:39 PM
 */
class Constants {
    /**
     * View ID for status bar placeholder view on Android 4.4 or EMUI 3.
     */
    static final int IMMERSION_STATUS_BAR_VIEW_ID = R.id.immersion_status_bar_view;
    /**
     * View ID for navigation bar placeholder view on Android 4.4 or EMUI 3.
     */
    static final int IMMERSION_NAVIGATION_BAR_VIEW_ID = R.id.immersion_navigation_bar_view;
    /**
     * Status bar height identifier.
     */
    static final String IMMERSION_STATUS_BAR_HEIGHT = "status_bar_height";
    /**
     * Navigation bar portrait height identifier.
     */
    static final String IMMERSION_NAVIGATION_BAR_HEIGHT = "navigation_bar_height";
    /**
     * Navigation bar landscape height identifier.
     */
    static final String IMMERSION_NAVIGATION_BAR_HEIGHT_LANDSCAPE = "navigation_bar_height_landscape";
    /**
     * Navigation bar width identifier.
     */
    static final String IMMERSION_NAVIGATION_BAR_WIDTH = "navigation_bar_width";
    /**
     * Xiaomi navigation bar mode identifier: 0 for 3-button navigation, 1 for gesture navigation.
     */
    static final String IMMERSION_NAVIGATION_BAR_MODE_MIUI = "force_fsg_nav_bar";
    /**
     * Xiaomi gesture navigation hint line visibility: 0 for show, 1 for hide.
     */
    static final String IMMERSION_NAVIGATION_BAR_MODE_MIUI_HIDE = "hide_gesture_line";
    /**
     * Huawei EMUI navigation bar visibility identifier: 0 for 3-button navigation, 1 for gesture navigation / hidden.
     */
    static final String IMMERSION_NAVIGATION_BAR_MODE_EMUI = "navigationbar_is_min";
    /**
     * Vivo navigation bar mode identifier: 0 for 3-button navigation, 1 for classic 3-stage gestures, 2 for full-screen gestures.
     */
    static final String IMMERSION_NAVIGATION_BAR_MODE_VIVO = "navigation_gesture_on";
    /**
     * Oppo ColorOS navigation bar mode identifier: 0 for 3-button navigation, 1 for gesture navigation, 2 for swipe-up gestures, 3 for swipe-from-sides gestures.
     */
    static final String IMMERSION_NAVIGATION_BAR_MODE_OPPO = "hide_navigationbar_enable";
    /**
     * Samsung navigation bar mode identifier: 0 for 3-button navigation, 1 for gesture navigation.
     */
    static final String IMMERSION_NAVIGATION_BAR_MODE_SAMSUNG = "navigation_bar_gesture_while_hidden";
    /**
     * Samsung gesture navigation subtype: 0 for 3-stage swipe-up lines, 1 for single swipe line.
     */
    static final String IMMERSION_NAVIGATION_BAR_MODE_SAMSUNG_GESTURE_TYPE = "navigation_bar_gesture_detail_type";
    /**
     * Samsung gesture navigation hint line visibility: 0 for hide, 1 for show.
     */
    static final String IMMERSION_NAVIGATION_BAR_MODE_SAMSUNG_GESTURE = "navigation_bar_gesture_hint";
    /**
     * Samsung legacy navigation bar mode identifier: 0 for 3-button navigation, 1 for gesture navigation.
     */
    static final String IMMERSION_NAVIGATION_BAR_MODE_SAMSUNG_OLD = "navigationbar_hide_bar_enabled";
    /**
     * Default navigation mode identifier: 0 for 3-button navigation, 1 for 2-button navigation, 2 for gesture navigation.
     */
    static final String IMMERSION_NAVIGATION_BAR_MODE_DEFAULT = "navigation_mode";
    /**
     * MIUI status bar dark/light icon extra flag identifier.
     */
    static final String IMMERSION_STATUS_BAR_DARK_MIUI = "EXTRA_FLAG_STATUS_BAR_DARK_MODE";
    /**
     * MIUI navigation bar dark/light icon extra flag identifier.
     */
    static final String IMMERSION_NAVIGATION_BAR_DARK_MIUI = "EXTRA_FLAG_NAVIGATION_BAR_DARK_MODE";

    /**
     * Boundary color threshold for automatic dark/light font color switching.
     */
    static final int IMMERSION_BOUNDARY_COLOR = 0xFFBABABA;

    /**
     * Status bar layout overlap adjustment flag: default (none).
     */
    static final int FLAG_FITS_DEFAULT = 0X00;
    /**
     * Status bar layout overlap adjustment flag: using titleBar method.
     */
    static final int FLAG_FITS_TITLE = 0X01;
    /**
     * Status bar layout overlap adjustment flag: using titleBarMarginTop method.
     */
    static final int FLAG_FITS_TITLE_MARGIN_TOP = 0X02;
    /**
     * Status bar layout overlap adjustment flag: using StatusBarView method.
     */
    static final int FLAG_FITS_STATUS = 0X03;
    /**
     * Status bar layout overlap adjustment flag: using fitsSystemWindows method.
     */
    static final int FLAG_FITS_SYSTEM_WINDOWS = 0X04;
}
