package com.gyf.immersionbar;

import android.graphics.Color;
import androidx.annotation.ColorInt;
import androidx.annotation.FloatRange;
import androidx.annotation.NonNull;

import android.view.View;
import android.view.WindowManager;

import java.util.HashMap;
import java.util.Map;

/**
 * Immersion bar configuration parameters.
 *
 * @author geyifeng
 * @date 2017 /5/9
 */
public class BarParams implements Cloneable {
    /**
     * Status bar color.
     */
    @ColorInt
    public int statusBarColor = Color.TRANSPARENT;
    /**
     * Navigation bar color.
     */
    @ColorInt
    public int navigationBarColor = Color.BLACK;
    /**
     * Whether the user explicitly specified the navigation bar color, used internally by ImmersionBar to distinguish default styling from explicit configuration.
     */
    boolean navigationBarColorSpecified = false;

    /**
     * The Default navigation bar color.
     */
    public int defaultNavigationBarColor = Color.BLACK;
    /**
     * Status bar alpha.
     */
    @FloatRange(from = 0f, to = 1f)
    public float statusBarAlpha = 0.0f;

    @FloatRange(from = 0f, to = 1f)
    public float statusBarTempAlpha = 0.0f;
    /**
     * Navigation bar alpha.
     */
    @FloatRange(from = 0f, to = 1f)
    public float navigationBarAlpha = 0.0f;
    @FloatRange(from = 0f, to = 1f)
    public float navigationBarTempAlpha = 0.0f;
    /**
     * Full screen display when navigation bar exists.
     */
    public boolean fullScreen = false;
    /**
     * Whether navigation bar is hidden.
     */
    public boolean hideNavigationBar = false;
    /**
     * System bar hide mode.
     */
    public BarHide barHide = BarHide.FLAG_SHOW_BAR;
    /**
     * Status bar dark/light font flag.
     */
    public boolean statusBarDarkFont = false;

    /**
     * Navigation bar dark/light icon flag.
     */
    public boolean navigationBarDarkIcon = false;
    /**
     * Whether the user explicitly specified the navigation bar icon mode, used internally by ImmersionBar to distinguish default styling from explicit configuration.
     */
    boolean navigationBarDarkIconSpecified = false;
    /**
     * Whether to automatically adjust dark/light status bar mode based on status bar color.
     */
    public boolean autoStatusBarDarkModeEnable = false;

    /**
     * Whether to automatically adjust dark/light navigation bar mode based on navigation bar color.
     */
    public boolean autoNavigationBarDarkModeEnable = false;

    /**
     * The Auto status bar dark mode alpha.
     */
    @FloatRange(from = 0f, to = 1f)
    public float autoStatusBarDarkModeAlpha = 0.0f;

    /**
     * The Auto navigation bar dark mode alpha.
     */
    @FloatRange(from = 0f, to = 1f)
    public float autoNavigationBarDarkModeAlpha = 0.0f;

    /**
     * Whether status bar color modification is enabled.
     */
    public boolean statusBarColorEnabled = true;
    /**
     * Transformed status bar color.
     */
    @ColorInt
    public int statusBarColorTransform = Color.BLACK;
    /**
     * Transformed navigation bar color.
     */
    @ColorInt
    public int navigationBarColorTransform = Color.BLACK;
    /**
     * View color transformation mapping.
     */
    Map<View, Map<Integer, Integer>> viewMap = new HashMap<>();
    /**
     * The View alpha.
     */
    @FloatRange(from = 0f, to = 1f)
    public float viewAlpha = 0.0f;
    /**
     * The Status bar color content view.
     */
    @ColorInt
    public int contentColor = Color.TRANSPARENT;
    /**
     * The Status bar color content view transform.
     */
    @ColorInt
    public int contentColorTransform = Color.BLACK;
    /**
     * The Status bar content view alpha.
     */
    @FloatRange(from = 0f, to = 1f)
    public float contentAlpha = 0.0f;
    /**
     * Resolves layout overlap between title bar and status bar.
     */
    public boolean fits = false;
    /**
     * Title bar view for resolving status bar overlap.
     */
    public View titleBarView;
    /**
     * Status bar placeholder view for resolving overlap.
     */
    public View statusBarView;
    /**
     * Whether layout overlap adjustment is enabled.
     */
    public boolean fitsLayoutOverlapEnable = true;
    /**
     * Flyme OS status bar font color.
     */
    @ColorInt
    public int flymeOSStatusBarFontColor;
    @ColorInt
    public int flymeOSStatusBarFontTempColor;
    /**
     * Whether used with SupportActionBar.
     */
    public boolean isSupportActionBar = false;
    /**
     * Resolves soft keyboard and input field conflict.
     */
    public boolean keyboardEnable = false;

    /**
     * Soft input window mode flags.
     */
    public int keyboardMode = WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN
            | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE;
    /**
     * Whether navigation bar color customization is enabled.
     */
    public boolean navigationBarEnable = true;
    /**
     * Whether navigation bar color customization is enabled on Android 4.4 and Huawei EMUI 3.1.
     */
    public boolean navigationBarWithKitkatEnable = true;
    /**
     * Whether navigation bar color customization is enabled on Huawei EMUI 3 series devices.
     */
    public boolean navigationBarWithEMUI3Enable = true;
    /**
     * Whether immersion bar features are enabled.
     */
    public boolean barEnable = true;
    /**
     * Soft keyboard listener.
     */
    OnKeyboardListener onKeyboardListener;

    /**
     * Navigation bar visibility change listener.
     */
    OnNavigationBarListener onNavigationBarListener;

    /**
     * Status bar visibility change listener.
     */
    OnStatusBarListener onStatusBarListener;

    /**
     * Orientation and bar change listener.
     */
    OnBarListener onBarListener;

    /**
     * Clears references that may hold page lifecycle objects.
     */
    void clear() {
        onKeyboardListener = null;
        onNavigationBarListener = null;
        onStatusBarListener = null;
        onBarListener = null;
        titleBarView = null;
        statusBarView = null;
        if (viewMap != null) {
            viewMap.clear();
        }
    }

    @Override
    protected BarParams clone() {
        BarParams barParams = null;
        try {
            barParams = (BarParams) super.clone();
        } catch (CloneNotSupportedException ignored) {
        }
        return barParams;
    }

    @NonNull
    @Override
    public String toString() {
        return "BarParams{" +
                "statusBarColor=" + statusBarColor +
                ", navigationBarColor=" + navigationBarColor +
                ", defaultNavigationBarColor=" + defaultNavigationBarColor +
                ", statusBarAlpha=" + statusBarAlpha +
                ", statusBarTempAlpha=" + statusBarTempAlpha +
                ", navigationBarAlpha=" + navigationBarAlpha +
                ", navigationBarTempAlpha=" + navigationBarTempAlpha +
                ", fullScreen=" + fullScreen +
                ", hideNavigationBar=" + hideNavigationBar +
                ", barHide=" + barHide +
                ", statusBarDarkFont=" + statusBarDarkFont +
                ", navigationBarDarkIcon=" + navigationBarDarkIcon +
                ", autoStatusBarDarkModeEnable=" + autoStatusBarDarkModeEnable +
                ", autoNavigationBarDarkModeEnable=" + autoNavigationBarDarkModeEnable +
                ", autoStatusBarDarkModeAlpha=" + autoStatusBarDarkModeAlpha +
                ", autoNavigationBarDarkModeAlpha=" + autoNavigationBarDarkModeAlpha +
                ", statusBarColorEnabled=" + statusBarColorEnabled +
                ", statusBarColorTransform=" + statusBarColorTransform +
                ", navigationBarColorTransform=" + navigationBarColorTransform +
                ", viewMap=" + viewMap +
                ", viewAlpha=" + viewAlpha +
                ", contentColor=" + contentColor +
                ", contentColorTransform=" + contentColorTransform +
                ", contentAlpha=" + contentAlpha +
                ", fits=" + fits +
                ", titleBarView=" + titleBarView +
                ", statusBarView=" + statusBarView +
                ", fitsLayoutOverlapEnable=" + fitsLayoutOverlapEnable +
                ", flymeOSStatusBarFontColor=" + flymeOSStatusBarFontColor +
                ", flymeOSStatusBarFontTempColor=" + flymeOSStatusBarFontTempColor +
                ", isSupportActionBar=" + isSupportActionBar +
                ", keyboardEnable=" + keyboardEnable +
                ", keyboardMode=" + keyboardMode +
                ", navigationBarEnable=" + navigationBarEnable +
                ", navigationBarWithKitkatEnable=" + navigationBarWithKitkatEnable +
                ", navigationBarWithEMUI3Enable=" + navigationBarWithEMUI3Enable +
                ", barEnable=" + barEnable +
                ", onKeyboardListener=" + onKeyboardListener +
                ", onNavigationBarListener=" + onNavigationBarListener +
                ", onStatusBarListener=" + onStatusBarListener +
                ", onBarListener=" + onBarListener +
                '}';
    }
}
