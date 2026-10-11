package com.gyf.immersionbar;

import static com.gyf.immersionbar.Constants.FLAG_FITS_DEFAULT;
import static com.gyf.immersionbar.Constants.FLAG_FITS_STATUS;
import static com.gyf.immersionbar.Constants.FLAG_FITS_SYSTEM_WINDOWS;
import static com.gyf.immersionbar.Constants.FLAG_FITS_TITLE;
import static com.gyf.immersionbar.Constants.FLAG_FITS_TITLE_MARGIN_TOP;
import static com.gyf.immersionbar.Constants.IMMERSION_BOUNDARY_COLOR;
import static com.gyf.immersionbar.Constants.IMMERSION_NAVIGATION_BAR_DARK_MIUI;
import static com.gyf.immersionbar.Constants.IMMERSION_NAVIGATION_BAR_VIEW_ID;
import static com.gyf.immersionbar.Constants.IMMERSION_STATUS_BAR_DARK_MIUI;
import static com.gyf.immersionbar.Constants.IMMERSION_STATUS_BAR_VIEW_ID;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Build;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.widget.FrameLayout;

import androidx.annotation.ChecksSdkIntAtLeast;
import androidx.annotation.ColorInt;
import androidx.annotation.ColorRes;
import androidx.annotation.FloatRange;
import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Immersion and bar management for Android 4.4 and above.
 *
 * @author gyf
 * @date 2017 /05/09
 */
@SuppressWarnings({"deprecation", "SameParameterValue"})
@SuppressLint("UseRequiresApi")
@TargetApi(Version.KITKAT)
public final class ImmersionBar implements Runnable {

    private Activity mActivity;
    private Fragment mSupportFragment;
    private android.app.Fragment mFragment;
    private Dialog mDialog;
    private Window mWindow;
    private ViewGroup mDecorView;
    private ViewGroup mContentView;
    private ImmersionBar mParentBar;

    /**
     * Whether used in a Fragment.
     */
    private boolean mIsFragment = false;
    /**
     * Whether it is a DialogFragment.
     */
    private boolean mIsDialogFragment = false;
    /**
     * Whether used in a Dialog.
     */
    private boolean mIsDialog = false;
    /**
     * User-configured bar parameters.
     */
    private BarParams mBarParams;
    /**
     * Navigation bar color of the current Window prior to being taken over by ImmersionBar.
     */
    private int mDefaultNavigationBarColor = Color.BLACK;
    /**
     * Whether the current Window used dark navigation bar icons prior to being taken over by ImmersionBar.
     */
    private boolean mDefaultNavigationBarDarkIcon = false;
    /**
     * The most recent params instance exposed to callers via getBarParams(), used to support legacy usage that directly modifies public fields.
     */
    private WeakReference<BarParams> mExposedBarParams;
    private int mExposedNavigationBarColor;
    private boolean mExposedNavigationBarDarkIcon;
    /**
     * System bar information.
     */
    private BarConfig mBarConfig;
    /**
     * Height of the ActionBar.
     */
    private int mActionBarHeight = 0;
    /**
     * Soft keyboard listener.
     */
    private FitsKeyboard mFitsKeyboard = null;
    /**
     * System bar (status bar / navigation bar) runtime visibility listener, per-window.
     */
    private BarVisibilityObserver mBarVisibilityObserver = null;
    /**
     * Layout change listener for Dialog fake status bar / fake navigation bar on decorView, used to correct fake bar visibility and padding based on geometric relations after Dialog window size changes.
     * Created only for Dialog on API 30+; may be null.
     */
    private View.OnLayoutChangeListener mFakeBarLayoutListener = null;
    /**
     * The delegate holding this ImmersionBar, used to refresh the BarProperties snapshot when system bar visibility changes at runtime.
     * Passed back by {@link ImmersionDelegate} only in Activity/Fragment scenarios; may be null.
     */
    private ImmersionDelegate mImmersionDelegate = null;
    /**
     * Collection of bar property change listeners. Uses CopyOnWriteArraySet to support safely adding or removing listeners during callbacks.
     */
    private final Set<OnBarPropertiesChangedListener> mOnBarPropertiesChangedListeners =
            new CopyOnWriteArraySet<>();
    /**
     * Collection of soft keyboard change listeners. Uses CopyOnWriteArraySet to support safely adding or removing listeners during callbacks.
     */
    private final Set<OnKeyboardChangedListener> mOnKeyboardChangedListeners = new CopyOnWriteArraySet<>();
    /**
     * Collection of status bar change listeners. Uses CopyOnWriteArraySet to support safely adding or removing listeners during callbacks.
     */
    private final Set<OnStatusBarChangedListener> mOnStatusBarChangedListeners = new CopyOnWriteArraySet<>();
    /**
     * Collection of navigation bar change listeners. Uses CopyOnWriteArraySet to support safely adding or removing listeners during callbacks.
     */
    private final Set<OnNavigationBarChangedListener> mOnNavigationBarChangedListeners = new CopyOnWriteArraySet<>();
    /**
     * Internal default status bar change callback, registered to mOnStatusBarChangedListeners.
     * Synchronizes all views depending on the status bar: fake status bar view visibility and height, as well as views adapted via titleBar/titleBarMarginTop/statusBarView
     * for overlap (the status bar height is variable, e.g. after rotation it must re-adapt to the current height; the three static methods are applied differentially by tag and replayed idempotently).
     */
    private final OnStatusBarChangedListener mStatusBarSync = statusBar -> {
        // Refresh BarConfig first so fake status bar and overlap adaptation views can obtain current status bar height
        mBarConfig = new BarConfig(mActivity);
        syncStatusBarView(statusBar.isVisible());
        fitsWindows();
    };
    /**
     * Internal default navigation bar change callback, registered to mOnNavigationBarChangedListeners,
     * used to synchronize fake navigation bar views (GONE when hidden, re-tinted according to mBarParams when shown).
     */
    private final OnNavigationBarChangedListener mFakeNavigationBarSync =
            navigationBar -> syncNavigationBarView(navigationBar.isVisible());
    /**
     * Internal navigation mode change listener (3-button ⇄ gesture navigation setting changes, etc.), registered to NavigationBarObserver.
     * Only responsible for triggering BarProperties snapshot refresh, dispatched uniformly by snapshot; does not directly invoke user listeners.
     */
    final OnNavigationBarListener mNavigationModeChangeListener = ImmersionBar.this::onNavigationBarListener;

    /**
     * Collection of bar parameters added by users using tags.
     */
    private final Map<String, BarParams> mTagMap = new HashMap<>();
    /**
     * The adaptation method currently used for top layout and status bar overlap.
     */
    private int mFitsStatusBarType = FLAG_FITS_DEFAULT;
    /**
     * Whether init() has already been called.
     */
    private boolean mInitialized = false;
    /**
     * Whether ActionBar is used on devices below LOLLIPOP.
     */
    private boolean mIsActionBarBelowLOLLIPOP = false;

    private boolean mKeyboardTempEnable = false;

    private int mPaddingLeft = 0, mPaddingTop = 0, mPaddingRight = 0, mPaddingBottom = 0;

    /**
     * Used in Activity.
     * With immersion bar.
     *
     * @param activity the activity
     * @return the immersion bar
     */
    public static ImmersionBar with(@NonNull Activity activity) {
        return getRetriever().get(activity, false);
    }

    /**
     * Used in Activity.
     *
     * @param activity the activity
     * @param isOnly   the is only whether the Fragment instance is unique; default is false (not unique); isOnly affects how tags are generated
     * @return the immersion bar
     */
    public static ImmersionBar with(@NonNull Activity activity, boolean isOnly) {
        return getRetriever().get(activity, isOnly);
    }

    /**
     * Used in Fragment.
     * With immersion bar.
     *
     * @param fragment the fragment
     * @return the immersion bar
     */
    public static ImmersionBar with(@NonNull Fragment fragment) {
        return getRetriever().get(fragment, false);
    }

    /**
     * Used in Fragment.
     * With immersion bar.
     *
     * @param fragment the fragment
     * @param isOnly   the is only whether the Fragment instance is unique; default is false (not unique); isOnly affects how tags are generated
     * @return the immersion bar
     */
    public static ImmersionBar with(@NonNull Fragment fragment, boolean isOnly) {
        return getRetriever().get(fragment, isOnly);
    }

    /**
     * Used in Fragment.
     * With immersion bar.
     *
     * @param fragment the fragment
     * @return the immersion bar
     */
    public static ImmersionBar with(@NonNull android.app.Fragment fragment) {
        return getRetriever().get(fragment, false);
    }

    /**
     * Used in Fragment.
     * With immersion bar.
     *
     * @param fragment the fragment
     * @param isOnly   the is only whether the Fragment instance is unique; default is false (not unique); isOnly affects how tags are generated
     * @return the immersion bar
     */
    public static ImmersionBar with(@NonNull android.app.Fragment fragment, boolean isOnly) {
        return getRetriever().get(fragment, isOnly);
    }

    /**
     * Used in DialogFragment.
     * With immersion bar.
     *
     * @param dialogFragment the dialog fragment
     * @return the immersion bar
     */
    public static ImmersionBar with(@NonNull DialogFragment dialogFragment) {
        return getRetriever().get(dialogFragment, false);
    }

    /**
     * Used in DialogFragment.
     *
     * @param dialogFragment the dialog fragment
     * @param isOnly         the is only whether the Fragment instance is unique; default is false (not unique); isOnly affects how tags are generated
     * @return the immersion bar
     */
    public static ImmersionBar with(@NonNull DialogFragment dialogFragment, boolean isOnly) {
        return getRetriever().get(dialogFragment, isOnly);
    }

    /**
     * Used in DialogFragment.
     * With immersion bar.
     *
     * @param dialogFragment the dialog fragment
     * @return the immersion bar
     */
    public static ImmersionBar with(@NonNull android.app.DialogFragment dialogFragment) {
        return getRetriever().get(dialogFragment, false);
    }

    /**
     * Used in DialogFragment.
     *
     * @param dialogFragment the dialog fragment
     * @param isOnly         the is only whether the Fragment instance is unique; default is false (not unique); isOnly affects how tags are generated
     * @return the immersion bar
     */
    public static ImmersionBar with(@NonNull android.app.DialogFragment dialogFragment, boolean isOnly) {
        return getRetriever().get(dialogFragment, isOnly);
    }

    /**
     * Used in Dialog.
     * With immersion bar.
     *
     * @param activity the activity
     * @param dialog   the dialog
     * @return the immersion bar
     */
    public static ImmersionBar with(@NonNull Activity activity, @NonNull Dialog dialog) {
        return getRetriever().get(activity, dialog, false);
    }

    /**
     * Used in Dialog.
     *
     * @param activity the activity
     * @param dialog   the dialog
     * @param isOnly   the is only whether the Fragment instance is unique; default is false (not unique); isOnly affects how tags are generated
     * @return the immersion bar
     */
    public static ImmersionBar with(@NonNull Activity activity, @NonNull Dialog dialog, boolean isOnly) {
        return getRetriever().get(activity, dialog, isOnly);
    }

    /**
     * Used in Dialog, automatically resolving the owning Activity from the Dialog without explicit passing.
     * With immersion bar.
     *
     * @param dialog the dialog
     * @return the immersion bar
     */
    public static ImmersionBar with(@NonNull Dialog dialog) {
        return getRetriever().get(ActivityUtils.getActivity(dialog), dialog, false);
    }

    /**
     * Used in Dialog, automatically resolving the owning Activity from the Dialog without explicit passing.
     *
     * @param dialog the dialog
     * @param isOnly the is only whether the Fragment instance is unique; default is false (not unique); isOnly affects how tags are generated
     * @return the immersion bar
     */
    public static ImmersionBar with(@NonNull Dialog dialog, boolean isOnly) {
        return getRetriever().get(ActivityUtils.getActivity(dialog), dialog, isOnly);
    }

    /**
     * Destroy Fragment.
     *
     * @param fragment the Fragment
     * @deprecated When the Fragment is destroyed, it will automatically be destroyed along with its childFragmentManager; no need to call this manually.
     */
    @Deprecated
    public static void destroy(@NonNull Fragment fragment) {
        getRetriever().destroy(fragment, false);
    }

    /**
     * Destroy Fragment.
     *
     * @param fragment the Fragment
     * @param isOnly   the is only whether the Fragment instance is unique; default is false (not unique); isOnly affects how tags are generated
     * @deprecated When the Fragment is destroyed, it will automatically be destroyed along with its childFragmentManager; no need to call this manually.
     */
    @Deprecated
    public static void destroy(@NonNull Fragment fragment, boolean isOnly) {
        getRetriever().destroy(fragment, isOnly);
    }

    /**
     * Destroy Fragment.
     *
     * @param fragment the android.app.Fragment
     * @deprecated When the Fragment is destroyed, it will automatically be destroyed along with its childFragmentManager; no need to call this manually.
     */
    @Deprecated
    public static void destroy(@NonNull android.app.Fragment fragment) {
        getRetriever().destroy(fragment, false);
    }


    /**
     * Destroy Fragment.
     *
     * @param fragment the android.app.Fragment
     * @param isOnly   the is only whether the Fragment instance is unique; default is false (not unique); isOnly affects how tags are generated
     * @deprecated When the Fragment is destroyed, it will automatically be destroyed along with its childFragmentManager; no need to call this manually.
     */
    @Deprecated
    public static void destroy(@NonNull android.app.Fragment fragment, boolean isOnly) {
        getRetriever().destroy(fragment, isOnly);
    }

    /**
     * Destroy in Dialog, excluding DialogFragment.
     *
     * @param activity the activity
     * @param dialog   the dialog
     * @deprecated Since {@link #with(Activity, Dialog)}, it will be destroyed automatically when the Dialog dismisses; no need to call this manually.
     */
    @Deprecated
    public static void destroy(@NonNull Activity activity, @NonNull Dialog dialog) {
        getRetriever().destroy(activity, dialog, false);
    }

    /**
     * Destroy in Dialog, excluding DialogFragment.
     *
     * @param activity the activity
     * @param dialog   the dialog
     * @param isOnly   the is only whether the Fragment instance is unique; default is false (not unique); isOnly affects how tags are generated
     * @deprecated Since {@link #with(Activity, Dialog, boolean)}, it will be destroyed automatically when the Dialog dismisses; no need to call this manually.
     */
    @Deprecated
    public static void destroy(@NonNull Activity activity, @NonNull Dialog dialog, boolean isOnly) {
        getRetriever().destroy(activity, dialog, isOnly);
    }

    /**
     * Must be initialized after the configuration above before it can be successfully called.
     */
    @SuppressLint("ObsoleteSdkInt")
    public void init() {
        if (mActivity == null || mWindow == null || mDecorView == null
                || mContentView == null || mBarParams == null) {
            return;
        }
        if (Build.VERSION.SDK_INT >= Version.KITKAT && mBarParams.barEnable) {
            // Update Bar parameters
            updateBarParams();
            // Set immersion bar
            setBar();
            // Fix view display
            fitsWindows();
            // Resolve soft keyboard and bottom input view conflict
            fitsKeyboard();
            // Color transform views
            transformView();
            mInitialized = true;
        }
    }

    /**
     * Internal method, no need to call.
     */
    void onDestroy() {
        cancelListener();
        restoreDialogParentBar();
        clearBarParamsReferences();
        clearWindowReferences();
        clearOwnerReferences();
        clearRuntimeReferences();
        mInitialized = false;
    }

    private void restoreDialogParentBar() {
        if (mIsDialog && mParentBar != null && mParentBar.mBarParams != null) {
            BarParams parentParams = mParentBar.mBarParams;
            parentParams.keyboardEnable = mParentBar.mKeyboardTempEnable;
            if (mParentBar.mInitialized && parentParams.barEnable) {
                mParentBar.updateBarConfig();
                mParentBar.setBar();
                mParentBar.fitsKeyboard();
            }
        }
    }

    private void clearBarParamsReferences() {
        if (mBarParams != null) {
            mBarParams.clear();
        }
        for (BarParams barParams : mTagMap.values()) {
            if (barParams != null) {
                barParams.clear();
            }
        }
        mTagMap.clear();
        mBarParams = null;
        mBarConfig = null;
    }

    private void clearWindowReferences() {
        mWindow = null;
        mDecorView = null;
        mContentView = null;
    }

    private void clearOwnerReferences() {
        mParentBar = null;
        mSupportFragment = null;
        mFragment = null;
        mDialog = null;
        mActivity = null;
    }

    private void clearRuntimeReferences() {
        mOnBarPropertiesChangedListeners.clear();
        mOnKeyboardChangedListeners.clear();
        mOnStatusBarChangedListeners.clear();
        mOnNavigationBarChangedListeners.clear();
        mImmersionDelegate = null;
    }

    void onResume() {
        if (mActivity == null || mBarParams == null) {
            return;
        }
        updateBarConfig();
        if (!mIsFragment && mInitialized) {
            if (OSUtils.isEMUI3_x() && mBarParams.navigationBarWithEMUI3Enable) {
                init();
            } else {
                if (mBarParams.barHide != BarHide.FLAG_SHOW_BAR) {
                    setBar();
                }
            }
        }
    }

    void onConfigurationChanged(Configuration newConfig) {
        if (mActivity == null || mDecorView == null || mBarParams == null) {
            return;
        }
        updateBarConfig();
        if (OSUtils.isEMUI3_x() || Build.VERSION.SDK_INT == Version.KITKAT) {
            if (mInitialized && !mIsFragment && mBarParams.navigationBarWithKitkatEnable) {
                init();
            } else {
                fitsWindows();
            }
        } else {
            fitsWindows();
            if (mDecorView.findViewById(IMMERSION_NAVIGATION_BAR_VIEW_ID) != null) {
                setupNavBarView();
            }
        }
    }

    /**
     * Update Bar parameters.
     * Update bar params.
     */
    @SuppressLint("ObsoleteSdkInt")
    private void updateBarParams() {
        applyDefaultNavigationBarStyle();
        adjustDarkModeParams();
        if (Build.VERSION.SDK_INT >= Version.KITKAT) {
            // Obtain Bar related information
            if (!mInitialized || mIsFragment) {
                updateBarConfig();
            }
            if (mParentBar != null) {
                // If used in Fragment, synchronize Fragment's BarParams to Activity
                if (mIsFragment) {
                    mParentBar.mBarParams = mBarParams;
                }
                // If keyboardEnable is set to true in Dialog, keyboardEnable set in Activity becomes false
                if (mIsDialog) {
                    if (mParentBar.mKeyboardTempEnable) {
                        mParentBar.mBarParams.keyboardEnable = false;
                    }
                }
            }
        }
        syncExposedNavigationBarStyle();
    }

    /**
     * Initialize status bar and navigation bar.
     */
    void setBar() {
        // Prevent content area size change when system bars are hidden
        int uiFlags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
        if (isEdgeToEdgeEnforced()) {
            // Adapt to notch / display cutout
            fitsNotchScreen();
            // On Android 15 (targetSdk 35) and above, edge-to-edge is enforced, setXxxBarColor is invalid, use fake bar view for tinting
            initBarAboveAndroid15();
            // Status bar font and navigation bar icon darkness handled by WindowInsetsController on Android 11+
            setBarDarkFontAboveR();
        } else if (Build.VERSION.SDK_INT >= Version.LOLLIPOP && !OSUtils.isEMUI3_x()) {
            // Adapt to notch / display cutout
            fitsNotchScreen();
            // Initialize Android 5.0 and above, including 5.0
            uiFlags = initBarAboveLOLLIPOP(uiFlags);
            // Set dark status bar font on Android 6.0+
            uiFlags = setStatusBarDarkFont(uiFlags);
            // Set dark navigation bar icon on Android 8.0+
            uiFlags = setNavigationIconDark(uiFlags);
            // Adapt to Android 11+
            setBarDarkFontAboveR();
        } else {
            // Initialize below Android 5.0, Android 4.4+ immersion
            initBarBelowLOLLIPOP();
        }
        // Hide status bar or navigation bar
        uiFlags = hideBarBelowR(uiFlags);
        // Apply flags
        mDecorView.setSystemUiVisibility(uiFlags);
        // Adapt status bar dark/light font for Xiaomi and Meizu
        setSpecialBarDarkMode();
        // Adapt to Android 11+
        hideBarAboveR();
        // Navigation mode change listener (3-button ⇄ gesture, etc.): registered internally, only triggers BarProperties snapshot refresh on change, dispatched uniformly by snapshot
        NavigationBarObserver.getInstance().addOnNavigationBarListener(mNavigationModeChangeListener);
        NavigationBarObserver.getInstance().register(mActivity.getApplication());
        // Listen to system bar runtime visibility (hideBar, etc.), register insets/systemUi listener by version, callback onBarVisibilityChange on change
        setupBarVisibilityObserver();
    }

    /**
     * Create and enable {@link BarVisibilityObserver} to listen for system bar runtime visibility.
     * Detection APIs are only available on API 16+; no-op on lower versions (retains URI mode change listener only).
     */
    @SuppressLint("ObsoleteSdkInt")
    private void setupBarVisibilityObserver() {
        if (Build.VERSION.SDK_INT < Version.JELLY_BEAN) {
            return;
        }
        if (mBarVisibilityObserver == null) {
            mBarVisibilityObserver = new BarVisibilityObserver(this);
        }
        mBarVisibilityObserver.enable();
    }

    private void setBarDarkFontAboveR() {
        if (Build.VERSION.SDK_INT >= Version.R) {
            setStatusBarDarkFontAboveR();
            setNavigationIconDarkAboveR();
        }
    }

    private void setSpecialBarDarkMode() {
        if (OSUtils.isMIUI6Later()) {
            // Change MIUI status bar font color
            SpecialBarFontUtils.setMIUIBarDark(mWindow, IMMERSION_STATUS_BAR_DARK_MIUI, mBarParams.statusBarDarkFont);
            // Change MIUI navigation bar icons to dark
            if (mBarParams.navigationBarEnable) {
                SpecialBarFontUtils.setMIUIBarDark(mWindow, IMMERSION_NAVIGATION_BAR_DARK_MIUI, mBarParams.navigationBarDarkIcon);
            }
        }
        // Change Flyme OS status bar font color
        if (OSUtils.isFlymeOS4Later()) {
            if (mBarParams.flymeOSStatusBarFontColor != 0) {
                SpecialBarFontUtils.setStatusBarDarkIcon(mActivity, mBarParams.flymeOSStatusBarFontColor);
            } else {
                SpecialBarFontUtils.setStatusBarDarkIcon(mActivity, mBarParams.statusBarDarkFont);
            }
        }
    }

    /**
     * Adapt to notch / display cutout.
     * Fits notch screen.
     */
    private void fitsNotchScreen() {
        if (Build.VERSION.SDK_INT >= Version.P && !mInitialized) {
            try {
                WindowManager.LayoutParams lp = mWindow.getAttributes();
                lp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
                mWindow.setAttributes(lp);
            } catch (Exception ignored) {

            }
        }
    }

    /**
     * Determine whether current window is enforced to edge-to-edge by the system.
     * On Android 15 (targetSdk 35) and above, edge-to-edge is enforced by the system, and legacy APIs like {@link Window#setStatusBarColor(int)} and
     * {@link Window#setNavigationBarColor(int)} become no-ops, requiring fake bar views for tinting.
     * Whether it is enforced is determined by three factors: device API, host app targetSdk, and windowOptOutEdgeToEdgeEnforcement attribute.
     *
     * @return true if enforced edge-to-edge and needs to go through {@link #initBarAboveAndroid15()}
     */
    @ChecksSdkIntAtLeast(api = Version.VANILLA_ICE_CREAM)
    private boolean isEdgeToEdgeEnforced() {
        // Device must be Android 15 (API 35) and above
        if (Build.VERSION.SDK_INT < Version.VANILLA_ICE_CREAM) {
            return false;
        }
        // Host app targetSdk must be 35 and above; below 35, system will not enforce edge-to-edge
        int target = mActivity.getApplicationInfo().targetSdkVersion;
        if (target < Version.VANILLA_ICE_CREAM) {
            return false;
        }
        if (isEdgeToEdgeOptedOut()) {
            // windowOptOutEdgeToEdgeEnforcement is only ignored when targetSdk 36+ and device is also 36+; in other cases opting out remains effective
            return target >= Version.BAKLAVA && Build.VERSION.SDK_INT >= Version.BAKLAVA;
        }
        return true;
    }

    /**
     * Read android:windowOptOutEdgeToEdgeEnforcement attribute from theme (added in API 35).
     *
     * @return true if the app actively opted out of edge-to-edge enforcement
     */
    private boolean isEdgeToEdgeOptedOut() {
        try {
            TypedValue tv = new TypedValue();
            if (mActivity.getTheme().resolveAttribute(
                    android.R.attr.windowOptOutEdgeToEdgeEnforcement, tv, true)) {
                return tv.data != 0;
            }
        } catch (Throwable ignored) {
            // Lower versions do not have this attribute or error occurred while parsing; treat as not opted out
        }
        return false;
    }

    /**
     * Initialize status bar and navigation bar on Android 15 (targetSdk 35) and above.
     * On this version the system enforces edge-to-edge, {@link Window#setStatusBarColor(int)} and
     * {@link Window#setNavigationBarColor(int)} are no-ops; system bars are set to transparent and tinted using custom fake bar views.
     */
    @RequiresApi(api = Version.VANILLA_ICE_CREAM)
    private void initBarAboveAndroid15() {
        // Obtain default navigation bar color
        if (!mInitialized) {
            mBarParams.defaultNavigationBarColor = mWindow.getNavigationBarColor();
        }
        // Disable system contrast enforcement, and make system bars transparent so custom fake bar views show through
        // (under edge-to-edge, setXxxBarColor is a no-op, setting transparent has no side effects)
        mWindow.setStatusBarContrastEnforced(false);
        mWindow.setNavigationBarContrastEnforced(false);
        mWindow.setStatusBarColor(Color.TRANSPARENT);
        mWindow.setNavigationBarColor(Color.TRANSPARENT);
        // Create fake status bar and tint according to mBarParams
        setupStatusBarView();
        // Create fake navigation bar and tint according to mBarParams
        setupNavBarView();
        // Fake bars follow real system bar visibility, observed by BarVisibilityObserver and dispatched to onBarVisibilityChange;
        // Synchronize once immediately using current real insets to prevent fake bar from staying in wrong state when setupXxxBarView just set it VISIBLE but the real system bar is already hidden
        // (e.g. repeated clicks on "hide all", hide() is a no-op and won't trigger listener callback to correct it).
        WindowInsets rootInsets = mDecorView.getRootWindowInsets();
        if (rootInsets != null) {
            onBarVisibilityChange(rootInsets.isVisible(WindowInsets.Type.statusBars()),
                    rootInsets.isVisible(WindowInsets.Type.navigationBars()));
        }
    }

    /**
     * Initialize status bar and navigation bar on Android 5.0 and above.
     *
     * @param uiFlags the ui flags
     * @return the int
     */
    @RequiresApi(api = Version.LOLLIPOP)
    private int initBarAboveLOLLIPOP(int uiFlags) {
        // Obtain default navigation bar color
        if (!mInitialized) {
            mBarParams.defaultNavigationBarColor = mWindow.getNavigationBarColor();
        }
        // Activity is displayed full screen, but the status bar will not be hidden or covered; it remains visible, and the top layout of the Activity will be covered by the status bar.
        uiFlags |= View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
        if (mBarParams.fullScreen && mBarParams.navigationBarEnable) {
            // Activity is displayed full screen, but the navigation bar will not be hidden or covered; it remains visible, and the bottom layout of the Activity will be covered by the navigation bar.
            uiFlags |= View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;
        }
        mWindow.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        // Check if navigation bar exists
        if (mBarConfig.hasNavigationBar()) {
            mWindow.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
        }
        // This must be set to configure status bar and navigation bar colors
        mWindow.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        // Set status bar color
        if (mBarParams.statusBarColorEnabled) {
            if (Build.VERSION.SDK_INT >= Version.Q) {
                mWindow.setStatusBarContrastEnforced(false);
            }
            mWindow.setStatusBarColor(ColorUtils.blendARGB(mBarParams.statusBarColor,
                    mBarParams.statusBarColorTransform, mBarParams.statusBarAlpha));
        } else {
            mWindow.setStatusBarColor(ColorUtils.blendARGB(mBarParams.statusBarColor,
                    Color.TRANSPARENT, mBarParams.statusBarAlpha));
        }
        // Set navigation bar color
        if (mBarParams.navigationBarEnable) {
            if (Build.VERSION.SDK_INT >= Version.Q) {
                mWindow.setNavigationBarContrastEnforced(false);
            }
            mWindow.setNavigationBarColor(ColorUtils.blendARGB(mBarParams.navigationBarColor,
                    mBarParams.navigationBarColorTransform, mBarParams.navigationBarAlpha));
        } else {
            mWindow.setNavigationBarColor(mBarParams.defaultNavigationBarColor);
        }
        return uiFlags;
    }

    /**
     * Initialize status bar and navigation bar on Android 4.4 and EMUI 3.1.
     */
    private void initBarBelowLOLLIPOP() {
        // Translucent status bar
        mWindow.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        // Create a fake status bar
        setupStatusBarView();
        // Check whether navigation bar exists and whether configuring navigation bar is disabled
        if (mBarConfig.hasNavigationBar() || OSUtils.isEMUI3_x()) {
            if (mBarParams.navigationBarEnable && mBarParams.navigationBarWithKitkatEnable) {
                // Translucent navigation bar; with this set, if navigation bar exists, bottom layout will be covered by navigation bar
                mWindow.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
            } else {
                mWindow.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
            }
            // Create a fake navigation bar
            setupNavBarView();
        }
    }

    /**
     * Set a custom color status bar.
     */
    private void setupStatusBarView() {
        View statusBarView = mDecorView.findViewById(IMMERSION_STATUS_BAR_VIEW_ID);
        if (statusBarView == null) {
            statusBarView = new View(mActivity);
            statusBarView.setId(IMMERSION_STATUS_BAR_VIEW_ID);
            mDecorView.addView(statusBarView);
        }
        // Status bar height may change with rotation, etc. (e.g. notch screen portrait/landscape differences), re-apply using current mBarConfig each time
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,
                mBarConfig.getStatusBarHeight());
        params.gravity = Gravity.TOP;
        statusBarView.setLayoutParams(params);
        if (mBarParams.statusBarColorEnabled) {
            statusBarView.setBackgroundColor(ColorUtils.blendARGB(mBarParams.statusBarColor,
                    mBarParams.statusBarColorTransform, mBarParams.statusBarAlpha));
        } else {
            statusBarView.setBackgroundColor(ColorUtils.blendARGB(mBarParams.statusBarColor,
                    Color.TRANSPARENT, mBarParams.statusBarAlpha));
        }
        if (needDrawFakeStatusBar()) {
            statusBarView.setVisibility(View.VISIBLE);
            statusBarView.bringToFront();
        } else {
            statusBarView.setVisibility(View.GONE);
        }
        // Dialog window size may only be determined at onStart after init (e.g. bottom half-screen Dialog); re-evaluate fake status bar visibility after layout changes
        registerFakeBarLayoutListener();
    }

    /**
     * Set a custom color navigation bar.
     */
    private void setupNavBarView() {
        View navigationBarView = mDecorView.findViewById(IMMERSION_NAVIGATION_BAR_VIEW_ID);
        if (navigationBarView == null) {
            navigationBarView = new View(mActivity);
            navigationBarView.setId(IMMERSION_NAVIGATION_BAR_VIEW_ID);
            mDecorView.addView(navigationBarView);
        }

        FrameLayout.LayoutParams params;
        if (mBarConfig.isNavigationAtBottom()) {
            params = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, mBarConfig.getNavigationBarHeight());
            params.gravity = Gravity.BOTTOM;
        } else {
            params = new FrameLayout.LayoutParams(mBarConfig.getNavigationBarWidth(), FrameLayout.LayoutParams.MATCH_PARENT);
            params.gravity = Gravity.END;
        }
        navigationBarView.setLayoutParams(params);
        navigationBarView.setBackgroundColor(ColorUtils.blendARGB(mBarParams.navigationBarColor,
                mBarParams.navigationBarColorTransform, mBarParams.navigationBarAlpha));

        boolean navBarViewEnable = mBarParams.navigationBarEnable && mBarParams.navigationBarWithKitkatEnable && !mBarParams.hideNavigationBar;
        if (navBarViewEnable && needDrawFakeNavBar()) {
            navigationBarView.setVisibility(View.VISIBLE);
            navigationBarView.bringToFront();
        } else {
            navigationBarView.setVisibility(View.GONE);
        }
        // Dialog window size may only be determined at onStart after init (e.g. top/side half-screen Dialog); re-evaluate fake navigation bar visibility after layout changes
        registerFakeBarLayoutListener();
    }

    /**
     * Determine whether a fake navigation bar needs to be drawn for the current window.
     * <p>The fake navigation bar is a child view added to {@link #mDecorView} with Gravity.BOTTOM/END; this is fine for full-screen Activity/Dialog;
     * however, for top or side Dialogs that do not cover the system navigation bar, their window does not overlap with the system navigation bar.
     * If a fake bar is still drawn, Gravity.BOTTOM/END would place it on the edge of the Dialog window itself (e.g. middle of screen), displaying an incorrect fake navigation bar.
     * <p>Dialog's {@code getRootWindowInsets()} returns display-level navigation bar insets, which cannot reflect the actual geometry of a half-screen window.
     * Therefore, geometric determination is used here: whether the window decorView edge actually reaches the screen area where the system navigation bar resides.
     * Non-Dialog (Activity) windows always fill the screen and directly return true to preserve original behavior.
     *
     * @return true if fake navigation bar needs to be drawn
     */
    @SuppressLint("ObsoleteSdkInt")
    private boolean needDrawFakeNavBar() {
        if (!mIsDialog || Build.VERSION.SDK_INT < Version.R) {
            return true;
        }
        return windowReachesNavigationBar();
    }

    /**
     * Use geometric relations to determine whether the window decorView reaches the screen area where the system navigation bar resides.
     * <p>The fake navigation bar is attached to the edge of decorView with Gravity.BOTTOM (bottom navigation) or Gravity.END (side navigation).
     * Only when that edge of decorView falls within the corresponding system navigation bar region on screen will the fake bar correctly cover the real navigation bar;
     * otherwise (e.g. bottom edge of a top half-screen Dialog in the middle of screen), a misplaced fake bar will appear.
     *
     * @return true if the window reaches the system navigation bar area and fake bar should be drawn; returns true conservatively if geometry is unavailable
     */
    @RequiresApi(api = Version.R)
    private boolean windowReachesNavigationBar() {
        int[] screenSize = getRealScreenSize();
        if (screenSize == null) {
            // Unable to get screen size, draw conservatively and let subsequent layout callbacks correct it
            return true;
        }
        int width = mDecorView.getWidth();
        int height = mDecorView.getHeight();
        if (width == 0 || height == 0) {
            // decorView not yet laid out, geometry unavailable, draw conservatively and let layout callbacks correct it
            return true;
        }
        int[] location = new int[2];
        mDecorView.getLocationOnScreen(location);
        int windowBottom = location[1] + height;
        int windowRight = location[0] + width;
        if (mBarConfig.isNavigationAtBottom()) {
            int navHeight = mBarConfig.getNavigationBarHeight();
            // Window bottom edge must pass (screen bottom - navigation bar height) to be considered covering bottom navigation bar
            return windowBottom > screenSize[1] - navHeight;
        }
        // Lateral navigation fake bar drawn with Gravity.END; window right edge must reach screen right navigation bar area
        int navWidth = mBarConfig.getNavigationBarWidth();
        return windowRight > screenSize[0] - navWidth;
    }

    /**
     * Determine whether a fake status bar needs to be drawn for the current window.
     * <p>Symmetric with {@link #needDrawFakeNavBar()}: fake status bar is attached to {@link #mDecorView} top edge with Gravity.TOP.
     * A bottom half-screen Dialog's top edge falls in the middle of the screen and does not overlap with the real status bar;
     * drawing a fake status bar would show an erroneous color strip at the window top edge. Non-Dialog (Activity) windows always reach screen top, directly returning true to preserve original behavior.
     *
     * @return true if fake status bar needs to be drawn
     */
    @SuppressLint("ObsoleteSdkInt")
    private boolean needDrawFakeStatusBar() {
        if (!mIsDialog || Build.VERSION.SDK_INT < Version.R) {
            return true;
        }
        return windowReachesStatusBar();
    }

    /**
     * Use geometric relations to determine whether the window decorView top edge reaches the screen top status bar region.
     * Status bar is always at screen top [0, statusBarHeight), so only need to check if decorView top edge falls in this interval, no real screen size needed.
     *
     * @return true if the window reaches the status bar area and fake bar should be drawn; returns true conservatively if geometry is unavailable
     */
    @RequiresApi(api = Version.R)
    private boolean windowReachesStatusBar() {
        int width = mDecorView.getWidth();
        int height = mDecorView.getHeight();
        if (width == 0 || height == 0) {
            // decorView not yet laid out, geometry unavailable, draw conservatively and let layout callbacks correct it
            return true;
        }
        int[] location = new int[2];
        mDecorView.getLocationOnScreen(location);
        int windowTop = location[1];
        // Window top edge falling within screen top status bar area is considered covering status bar
        return windowTop < mBarConfig.getStatusBarHeight();
    }

    /**
     * Get real screen size (including system bar areas).
     *
     * @return int[]{width, height}, or null if unavailable
     */
    @SuppressLint("ObsoleteSdkInt")
    private int[] getRealScreenSize() {
        try {
            android.util.DisplayMetrics metrics = new android.util.DisplayMetrics();
            if (Build.VERSION.SDK_INT >= Version.JELLY_BEAN_MR1) {
                mWindow.getWindowManager().getDefaultDisplay().getRealMetrics(metrics);
            } else {
                mWindow.getWindowManager().getDefaultDisplay().getMetrics(metrics);
            }
            return new int[]{metrics.widthPixels, metrics.heightPixels};
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Register layout listener for Dialog decorView to re-evaluate fake status bar / fake navigation bar visibility and padding after Dialog window size changes (e.g. Gravity/size set in onStart).
     * <p>{@link BarVisibilityObserver} only callbacks when system bar visibility flips. When Dialog window resizes, system bar visibility does not change and deduplication skips it;
     * also during init (onViewCreated), Dialog window has not resized yet and geometry is inaccurate, so decorView layout callback corrects it after resize.
     * <p>Only needed for Dialog on API 30+; Activity window always fills screen and does not need listening. Listener is created once, safe for repeated calls.
     */
    @SuppressLint("ObsoleteSdkInt")
    private void registerFakeBarLayoutListener() {
        if (!mIsDialog || Build.VERSION.SDK_INT < Version.R) {
            return;
        }
        if (mFakeBarLayoutListener == null) {
            mFakeBarLayoutListener = (v, l, t, r, b, ol, ot, or, ob) -> updateFakeBarByGeometry();
            mDecorView.addOnLayoutChangeListener(mFakeBarLayoutListener);
        }
    }

    /**
     * Correct fake status bar / fake navigation bar view visibility and content area padding based on current geometric relations.
     * <p>During init, Dialog window has not yet resized (decorView is 0x0), {@link #needDrawFakeStatusBar()}/{@link #needDrawFakeNavBar()}
     * can only return true conservatively, drawing fake bars and reserving padding for status bar / navigation bar height; window resize and layout completion makes geometry valid, so re-evaluate here:
     * when half-screen Dialog does not reach real system bar (e.g. bottom half-screen Dialog top edge in middle of screen, top half-screen Dialog bottom edge in middle of screen),
     * hide corresponding fake bar view and revoke extra padding via {@link #fitsWindows()} (otherwise misplaced color bar or erroneous blank space will appear).
     * <p>{@link View#setPadding} and {@link View#setVisibility} are no-ops for identical values, so repeated execution converges without looping.
     */
    private void updateFakeBarByGeometry() {
        View statusBarView = mDecorView.findViewById(IMMERSION_STATUS_BAR_VIEW_ID);
        if (statusBarView != null) {
            boolean shouldShow = needDrawFakeStatusBar();
            if (shouldShow) {
                if (statusBarView.getVisibility() != View.VISIBLE) {
                    statusBarView.setVisibility(View.VISIBLE);
                    statusBarView.bringToFront();
                }
            } else {
                if (statusBarView.getVisibility() != View.GONE) {
                    statusBarView.setVisibility(View.GONE);
                }
            }
        }
        View navigationBarView = mDecorView.findViewById(IMMERSION_NAVIGATION_BAR_VIEW_ID);
        if (navigationBarView != null) {
            boolean navBarViewEnable = mBarParams.navigationBarEnable && mBarParams.navigationBarWithKitkatEnable && !mBarParams.hideNavigationBar;
            boolean shouldShow = navBarViewEnable && needDrawFakeNavBar();
            if (shouldShow) {
                if (navigationBarView.getVisibility() != View.VISIBLE) {
                    navigationBarView.setVisibility(View.VISIBLE);
                    navigationBarView.bringToFront();
                }
            } else {
                if (navigationBarView.getVisibility() != View.GONE) {
                    navigationBarView.setVisibility(View.GONE);
                }
            }
        }
        // Geometry is determined after window resize; recompute content area padding, revoking conservatively reserved status bar / navigation bar padding from init
        fitsWindows();
    }

    /**
     * When the user has not explicitly configured the navigation bar, uniformly apply a theme-matching default appearance on systems supporting dark icons:
     * white background with dark icons for light themes, and black background with light icons for dark themes; older systems retain the Window's original appearance.
     * Retains legacy behavior when the user explicitly sets the color; automatically selects a contrasting background when only the icon mode is explicitly set.
     */
    private void applyDefaultNavigationBarStyle() {
        detectExposedNavigationBarStyleChanges();
        boolean navigationIconDarkSupported = isSupportNavigationIconDark();
        boolean lightTheme = isLightTheme();
        boolean explicitNavigationIcon = mBarParams.navigationBarDarkIconSpecified
                && !mBarParams.autoNavigationBarDarkModeEnable;
        if (!mBarParams.navigationBarColorSpecified) {
            if (explicitNavigationIcon && navigationIconDarkSupported) {
                mBarParams.navigationBarColor = mBarParams.navigationBarDarkIcon
                        ? Color.WHITE : Color.BLACK;
            } else if (navigationIconDarkSupported) {
                mBarParams.navigationBarColor = lightTheme ? Color.WHITE : Color.BLACK;
            } else {
                mBarParams.navigationBarColor = mDefaultNavigationBarColor;
            }
        }
        if (!mBarParams.navigationBarDarkIconSpecified
                && !mBarParams.autoNavigationBarDarkModeEnable) {
            if (!mBarParams.navigationBarColorSpecified && navigationIconDarkSupported) {
                mBarParams.navigationBarDarkIcon = lightTheme;
            } else if (!mBarParams.navigationBarColorSpecified) {
                mBarParams.navigationBarDarkIcon = mDefaultNavigationBarDarkIcon;
            } else {
                mBarParams.navigationBarDarkIcon = false;
            }
        }
    }

    /**
     * Detects compatibility usage where callers directly modify navigation bar color or icon mode via getBarParams().
     */
    private void detectExposedNavigationBarStyleChanges() {
        if (mExposedBarParams == null || mExposedBarParams.get() != mBarParams) {
            return;
        }
        if (!mBarParams.navigationBarColorSpecified
                && mBarParams.navigationBarColor != mExposedNavigationBarColor) {
            mBarParams.navigationBarColorSpecified = true;
        }
        if (!mBarParams.navigationBarDarkIconSpecified
                && mBarParams.navigationBarDarkIcon != mExposedNavigationBarDarkIcon) {
            mBarParams.navigationBarDarkIconSpecified = true;
        }
    }

    /**
     * Updates internal snapshot of exposed parameters to avoid misinterpreting ImmersionBar's own default values or auto-mode updates as user modifications.
     */
    private void syncExposedNavigationBarStyle() {
        if (mExposedBarParams != null && mExposedBarParams.get() == mBarParams) {
            mExposedNavigationBarColor = mBarParams.navigationBarColor;
            mExposedNavigationBarDarkIcon = mBarParams.navigationBarDarkIcon;
        }
    }

    private boolean isLightTheme() {
        TypedValue typedValue = new TypedValue();
        if (mWindow.getContext().getTheme()
                .resolveAttribute(android.R.attr.isLightTheme, typedValue, true)) {
            return typedValue.data != 0;
        }
        int nightMode = mActivity.getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK;
        return nightMode != Configuration.UI_MODE_NIGHT_YES;
    }

    /**
     * Adjust dark/light mode parameters.
     */
    private void adjustDarkModeParams() {
        int statusBarColor = ColorUtils.blendARGB(mBarParams.statusBarColor,
                mBarParams.statusBarColorTransform, mBarParams.statusBarAlpha);
        if (mBarParams.autoStatusBarDarkModeEnable && statusBarColor != Color.TRANSPARENT) {
            boolean statusBarDarkFont = statusBarColor > IMMERSION_BOUNDARY_COLOR;
            statusBarDarkFont(statusBarDarkFont, mBarParams.autoStatusBarDarkModeAlpha);
        }
        int navigationBarColor = ColorUtils.blendARGB(mBarParams.navigationBarColor,
                mBarParams.navigationBarColorTransform, mBarParams.navigationBarAlpha);
        if (mBarParams.autoNavigationBarDarkModeEnable && navigationBarColor != Color.TRANSPARENT) {
            boolean navigationBarDarkIcon = navigationBarColor > IMMERSION_BOUNDARY_COLOR;
            setNavigationBarDarkIconInternal(
                    navigationBarDarkIcon, mBarParams.autoNavigationBarDarkModeAlpha);
        }
    }

    /**
     * Hide bar.
     * Hide or show status bar and navigation bar.
     *
     * @param uiFlags the ui flags
     * @return the int
     */
    @SuppressLint("ObsoleteSdkInt")
    private int hideBarBelowR(int uiFlags) {
        if (Build.VERSION.SDK_INT >= Version.R) {
            return uiFlags;
        }
        if (Build.VERSION.SDK_INT >= Version.JELLY_BEAN) {
            int currentUiFlags = mDecorView.getSystemUiVisibility();
            boolean statusBarHidden =
                    (mWindow.getAttributes().flags & WindowManager.LayoutParams.FLAG_FULLSCREEN) != 0 ||
                            (currentUiFlags & View.SYSTEM_UI_FLAG_FULLSCREEN) != 0;
            boolean navigationBarHidden =
                    (currentUiFlags & View.SYSTEM_UI_FLAG_HIDE_NAVIGATION) != 0;
            switch (mBarParams.barHide) {
                case FLAG_HIDE_BAR:
                    uiFlags |= View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN;
                    break;
                case FLAG_HIDE_STATUS_BAR:
                    // When target bar changes from visible to hidden, preserve navigation bar's current native hidden state;
                    // When target bar is already hidden, preserve existing "hide status bar only" toggle semantics.
                    if (!statusBarHidden && navigationBarHidden) {
                        uiFlags |= View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
                    }
                    uiFlags |= View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_FULLSCREEN;
                    break;
                case FLAG_HIDE_NAVIGATION_BAR:
                    // When target bar changes from visible to hidden, preserve status bar's current native hidden state;
                    // When target bar is already hidden, preserve existing "hide navigation bar only" toggle semantics.
                    if (!navigationBarHidden && statusBarHidden) {
                        uiFlags |= View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                | View.SYSTEM_UI_FLAG_FULLSCREEN;
                    }
                    uiFlags |= View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
                    break;
                case FLAG_SHOW_BAR:
                    uiFlags |= View.SYSTEM_UI_FLAG_VISIBLE;
                    break;
                default:
                    break;
            }
        }
        return uiFlags | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
    }

    /**
     * Fix view display.
     */
    @SuppressLint("ObsoleteSdkInt")
    private void fitsWindows() {
        if (Build.VERSION.SDK_INT >= Version.KITKAT) {
            if (Build.VERSION.SDK_INT >= Version.LOLLIPOP && !OSUtils.isEMUI3_x()) {
                // Resolve status bar and layout overlap on Android 5.0 and above
                fitsWindowsAboveLOLLIPOP();
            } else {
                // Resolve status bar and layout overlap below Android 5.0
                fitsWindowsBelowLOLLIPOP();
            }
            // Adapt to status bar and layout overlap
            fitsLayoutOverlap();
        }
    }

    /**
     * Resolve status bar and layout overlap below Android 5.0.
     */
    private void fitsWindowsBelowLOLLIPOP() {
        if (mBarParams.isSupportActionBar) {
            mIsActionBarBelowLOLLIPOP = true;
            mContentView.post(this);
        } else {
            mIsActionBarBelowLOLLIPOP = false;
            postFitsWindowsBelowLOLLIPOP();
        }
    }

    @Override
    public void run() {
        if (mContentView == null || mBarParams == null) {
            return;
        }
        postFitsWindowsBelowLOLLIPOP();
    }

    private void postFitsWindowsBelowLOLLIPOP() {
        // Resolve bottom of activity being obscured by navigation bar when navigation bar exists on Android 4.4, and resolve status bar and layout overlap below Android 5.0
        fitsWindowsKITKAT();
        // Resolve Huawei EMUI 3.1 or 3.0 navigation bar manual hiding issue
        if (!mIsFragment && OSUtils.isEMUI3_x()) {
            fitsWindowsEMUI();
        }
    }

    /**
     * Resolve status bar and layout overlap on Android 5.0 and above.
     * Fits windows above lollipop.
     */
    private void fitsWindowsAboveLOLLIPOP() {
        if (checkFitsSystemWindows(mDecorView.findViewById(android.R.id.content))) {
            setPadding(0, 0, 0, 0);
            return;
        }
        int top = 0;
        if (mBarParams.fits && mFitsStatusBarType == FLAG_FITS_SYSTEM_WINDOWS) {
            top = mBarConfig.getStatusBarHeight();
        }
        if (mBarParams.isSupportActionBar) {
            top = mBarConfig.getStatusBarHeight() + mActionBarHeight;
        }
        if (isEdgeToEdgeEnforced()) {
            // Under edge-to-edge on Android 15+, content extends behind the navigation bar; additional padding in navigation bar direction is required
            int right = 0, bottom = 0;
            // hasNavigationBar is a display-level check and returns true even if half-screen Dialog does not reach system navigation bar; use needDrawFakeNavBar geometric check,
            // avoiding reserving navigation bar height padding for top/side half-screen Dialogs and leaving erroneous blank space at bottom/side
            if (mBarConfig.hasNavigationBar() && mBarParams.navigationBarEnable && !mBarParams.fullScreen
                    && needDrawFakeNavBar()) {
                //noinspection StatementWithEmptyBody
                if (mBarParams.hideNavigationBar) {
                    // Navigation bar is hidden, no need to yield space
                } else if (mBarConfig.isNavigationAtBottom()) {
                    bottom = mBarConfig.getNavigationBarHeight();
                } else {
                    right = mBarConfig.getNavigationBarWidth();
                }
            }
            setPadding(0, top, right, bottom);
            return;
        }
        setPadding(0, top, 0, 0);
    }

    /**
     * Resolve bottom of activity being obscured by navigation bar when navigation bar exists on Android 4.4, and resolve status bar and layout overlap below Android 5.0.
     * Fits windows below lollipop.
     */
    private void fitsWindowsKITKAT() {
        if (checkFitsSystemWindows(mDecorView.findViewById(android.R.id.content))) {
            setPadding(0, 0, 0, 0);
            return;
        }
        int top = 0, right = 0, bottom = 0;
        if (mBarParams.fits && mFitsStatusBarType == FLAG_FITS_SYSTEM_WINDOWS) {
            top = mBarConfig.getStatusBarHeight();
        }
        if (mBarParams.isSupportActionBar) {
            top = mBarConfig.getStatusBarHeight() + mActionBarHeight;
        }
        if (mBarConfig.hasNavigationBar() && mBarParams.navigationBarEnable && mBarParams.navigationBarWithKitkatEnable) {
            if (!mBarParams.fullScreen) {
                if (mBarConfig.isNavigationAtBottom()) {
                    bottom = mBarConfig.getNavigationBarHeight();
                } else {
                    right = mBarConfig.getNavigationBarWidth();
                }
            }
            if (mBarParams.hideNavigationBar) {
                if (mBarConfig.isNavigationAtBottom()) {
                    bottom = 0;
                } else {
                    right = 0;
                }
            } else {
                if (!mBarConfig.isNavigationAtBottom()) {
                    right = mBarConfig.getNavigationBarWidth();
                }
            }
        }
        setPadding(0, top, right, bottom);
    }

    /**
     * Register EMUI 3.x navigation bar listener.
     * Register emui 3 x.
     */
    private void fitsWindowsEMUI() {
        View navigationBarView = mDecorView.findViewById(IMMERSION_NAVIGATION_BAR_VIEW_ID);
        if (mBarParams.navigationBarEnable && mBarParams.navigationBarWithKitkatEnable) {
            if (navigationBarView != null) {
                EMUI3NavigationBarObserver.getInstance().addOnNavigationBarListener(mNavigationModeChangeListener);
                EMUI3NavigationBarObserver.getInstance().register(mActivity.getApplication());
            }
        } else {
            EMUI3NavigationBarObserver.getInstance().removeOnNavigationBarListener(mNavigationModeChangeListener);
            navigationBarView.setVisibility(View.GONE);
        }
    }

    /**
     * Update BarConfig.
     */
    private void updateBarConfig() {
        mBarConfig = new BarConfig(mActivity);
        if (!mInitialized || mIsActionBarBelowLOLLIPOP) {
            mActionBarHeight = mBarConfig.getActionBarHeight();
        }
    }

    /**
     * Unified callback when real system bar (status bar / navigation bar) visibility changes, invoked by {@link BarVisibilityObserver} according to version.
     * Only responsible for refreshing BarProperties snapshot: observer deduplicates by flip, and snapshot layer deduplicates again before uniform dispatch;
     * fake bar views and overlap adaptation view synchronization are handled during dispatch by internal default callbacks (mStatusBarSync/mFakeNavigationBarSync) registered in listener sets,
     * and various listeners (including deprecated listeners) are also invoked during the same dispatch.
     *
     * @param statusVisible     whether status bar is currently visible
     * @param navigationVisible whether navigation bar is currently visible
     */
    void onBarVisibilityChange(boolean statusVisible, boolean navigationVisible) {
        refreshBarProperties(navigationVisible);
    }

    void onNavigationBarListener(boolean navigationVisible, NavigationBarType type) {
        refreshBarProperties(navigationVisible);
    }

    private void refreshBarProperties(boolean navigationVisible) {
        if (mActivity == null || mDecorView == null || mContentView == null || mBarParams == null || mImmersionDelegate == null) {
            return;
        }
        mImmersionDelegate.refreshBarProperties(navigationVisible);
    }

    /**
     * Passed back by {@link ImmersionDelegate} after construction, enabling runtime system bar visibility changes to inversely refresh BarProperties snapshot.
     *
     * @param delegate the delegate holding this ImmersionBar
     */
    void setImmersionDelegate(ImmersionDelegate delegate) {
        mImmersionDelegate = delegate;
    }

    private void syncStatusBarView(boolean statusVisible) {
        View statusBarView = mDecorView.findViewById(IMMERSION_STATUS_BAR_VIEW_ID);
        if (statusBarView == null) return;
        if (statusVisible) {
            setupStatusBarView();
        } else {
            statusBarView.setVisibility(View.GONE);
        }
    }

    private void syncNavigationBarView(boolean show) {
        View navigationBarView = mDecorView.findViewById(IMMERSION_NAVIGATION_BAR_VIEW_ID);
        if (navigationBarView == null) return;
        mBarConfig = new BarConfig(mActivity);
        // When Dialog window does not cover system navigation bar direction (e.g. top/side half-screen Dialog), do not show fake navigation bar, otherwise it falls on window edge showing an erroneous strip
        if (show && !needDrawFakeNavBar()) {
            show = false;
        }
        int bottom = mContentView.getPaddingBottom(), right = mContentView.getPaddingRight();
        if (!show) {
            // Navigation keys hidden
            navigationBarView.setVisibility(View.GONE);
            bottom = 0;
            right = 0;
        } else {
            // Navigation keys shown
            navigationBarView.setVisibility(View.VISIBLE);
            if (checkFitsSystemWindows(mDecorView.findViewById(android.R.id.content))) {
                bottom = 0;
                right = 0;
            } else {
                if (!mBarParams.hideNavigationBar) {
                    FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) navigationBarView.getLayoutParams();
                    // Navigation bar height/width may change with rotation or navigation mode switch; get current value from freshly refreshed mBarConfig above
                    if (mBarConfig.isNavigationAtBottom()) {
                        params.gravity = Gravity.BOTTOM;
                        params.height = mBarConfig.getNavigationBarHeight();
                        bottom = !mBarParams.fullScreen ? mBarConfig.getNavigationBarHeight() : 0;
                        right = 0;
                    } else {
                        params.gravity = Gravity.END;
                        params.width = mBarConfig.getNavigationBarWidth();
                        bottom = 0;
                        right = !mBarParams.fullScreen ? mBarConfig.getNavigationBarWidth() : 0;
                    }
                    navigationBarView.setLayoutParams(params);
                }
            }
        }
        setPadding(0, mContentView.getPaddingTop(), right, bottom);
    }

    /**
     * Sets status bar dark font.
     * Set status bar font color, Android 6.0 and above.
     */
    private int setStatusBarDarkFont(int uiFlags) {
        if (Build.VERSION.SDK_INT >= Version.M) {
            if (mBarParams.statusBarDarkFont) {
                return uiFlags | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            } else {
                return uiFlags;
            }
        } else {
            return uiFlags;
        }
    }

    /**
     * Set navigation bar icons to light or dark.
     * Sets dark navigation icon.
     */
    private int setNavigationIconDark(int uiFlags) {
        if (Build.VERSION.SDK_INT >= Version.O) {
            if (mBarParams.navigationBarDarkIcon) {
                return uiFlags | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
            } else {
                return uiFlags;
            }
        } else {
            return uiFlags;
        }
    }

    @RequiresApi(api = Version.R)
    private void setStatusBarDarkFontAboveR() {
        WindowInsetsController controller = mContentView.getWindowInsetsController();
        if (controller == null) {
            return;
        }
        if (mBarParams.statusBarDarkFont) {
            if (mWindow != null) {
                unsetSystemUiFlag(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            }
            controller.setSystemBarsAppearance(
                    WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                    WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS);
        } else {
            controller.setSystemBarsAppearance(
                    0,
                    WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS);
        }
    }

    @RequiresApi(api = Version.R)
    private void setNavigationIconDarkAboveR() {
        WindowInsetsController controller = mContentView.getWindowInsetsController();
        if (controller == null) {
            return;
        }
        if (mBarParams.navigationBarDarkIcon) {
            controller.setSystemBarsAppearance(
                    WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS,
                    WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS);
        } else {
            controller.setSystemBarsAppearance(
                    0,
                    WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS);
        }
    }

    private void hideBarAboveR() {
        if (Build.VERSION.SDK_INT >= Version.R) {
            WindowInsetsController controller = mContentView.getWindowInsetsController();
            if (controller != null) {
                WindowInsets insets = mDecorView.getRootWindowInsets();
                boolean statusBarHidden = insets == null ||
                        !insets.isVisible(WindowInsets.Type.statusBars());
                boolean navigationBarHidden = insets == null ||
                        !insets.isVisible(WindowInsets.Type.navigationBars());
                switch (mBarParams.barHide) {
                    case FLAG_HIDE_BAR:
                        controller.hide(WindowInsets.Type.statusBars());
                        controller.hide(WindowInsets.Type.navigationBars());
                        break;
                    case FLAG_HIDE_STATUS_BAR:
                        if (statusBarHidden) {
                            controller.show(WindowInsets.Type.navigationBars());
                        }
                        controller.hide(WindowInsets.Type.statusBars());
                        break;
                    case FLAG_HIDE_NAVIGATION_BAR:
                        if (navigationBarHidden) {
                            controller.show(WindowInsets.Type.statusBars());
                        }
                        controller.hide(WindowInsets.Type.navigationBars());
                        break;
                    case FLAG_SHOW_BAR:
                        controller.show(WindowInsets.Type.statusBars());
                        controller.show(WindowInsets.Type.navigationBars());
                        break;
                    default:
                        break;
                }
                controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        }
    }

    private void unsetSystemUiFlag(int systemUiFlag) {
        View decorView = mWindow.getDecorView();
        decorView.setSystemUiVisibility(decorView.getSystemUiVisibility() & ~systemUiFlag);
    }

    /**
     * Adapt to status bar and layout overlap.
     * Fits layout overlap.
     */
    private void fitsLayoutOverlap() {
        int fixHeight = 0;
        if (mBarParams.fitsLayoutOverlapEnable) {
            fixHeight = mBarConfig.getStatusBarHeight();
        }
        switch (mFitsStatusBarType) {
            case FLAG_FITS_TITLE:
                // Redraw title bar height by setting paddingTop
                setTitleBar(fixHeight, mBarParams.titleBarView);
                break;
            case FLAG_FITS_TITLE_MARGIN_TOP:
                // Redraw title bar height by setting marginTop
                setTitleBarMarginTop(fixHeight, mBarParams.titleBarView);
                break;
            case FLAG_FITS_STATUS:
                // Dynamically configure status bar layout with status bar height
                setStatusBarView(fixHeight, mBarParams.statusBarView);
                break;
            default:
                break;
        }
    }

    /**
     * Color transform views.
     * Transform view.
     */
    private void transformView() {
        if (mBarParams != null && !mBarParams.viewMap.isEmpty()) {
            Set<Map.Entry<View, Map<Integer, Integer>>> entrySet = mBarParams.viewMap.entrySet();
            for (Map.Entry<View, Map<Integer, Integer>> entry : entrySet) {
                View view = entry.getKey();
                Map<Integer, Integer> map = entry.getValue();
                Integer colorBefore = mBarParams.statusBarColor;
                Integer colorAfter = mBarParams.statusBarColorTransform;
                for (Map.Entry<Integer, Integer> integerEntry : map.entrySet()) {
                    colorBefore = integerEntry.getKey();
                    colorAfter = integerEntry.getValue();
                }
                if (view != null) {
                    if (Math.abs(mBarParams.viewAlpha - 0.0f) == 0) {
                        view.setBackgroundColor(ColorUtils.blendARGB(colorBefore, colorAfter, mBarParams.statusBarAlpha));
                    } else {
                        view.setBackgroundColor(ColorUtils.blendARGB(colorBefore, colorAfter, mBarParams.viewAlpha));
                    }
                }
            }
        }
    }

    /**
     * Unregister EMUI 3.x navigation bar listener and soft keyboard listener.
     * Cancel listener.
     */
    private void cancelListener() {
        if (mFitsKeyboard != null) {
            mFitsKeyboard.cancel();
            mFitsKeyboard = null;
        }
        if (mBarVisibilityObserver != null) {
            mBarVisibilityObserver.disable();
            mBarVisibilityObserver = null;
        }
        if (mContentView != null) {
            mContentView.removeCallbacks(this);
        }
        if (mDecorView != null) {
            mDecorView.removeCallbacks(this);
            if (mFakeBarLayoutListener != null) {
                mDecorView.removeOnLayoutChangeListener(mFakeBarLayoutListener);
            }
        }
        mFakeBarLayoutListener = null;
        EMUI3NavigationBarObserver.getInstance().removeOnNavigationBarListener(mNavigationModeChangeListener);
        NavigationBarObserver.getInstance().removeOnNavigationBarListener(mNavigationModeChangeListener);
    }

    /**
     * Resolve bottom input view and soft keyboard conflict.
     * Keyboard enable.
     */
    @SuppressLint("ObsoleteSdkInt")
    private void fitsKeyboard() {
        if (Build.VERSION.SDK_INT >= Version.KITKAT) {
            if (!mIsFragment) {
                if (mBarParams.keyboardEnable) {
                    if (mFitsKeyboard == null) {
                        mFitsKeyboard = new FitsKeyboard(this);
                    }
                    mFitsKeyboard.enable(mBarParams.keyboardMode);
                } else {
                    if (mFitsKeyboard != null) {
                        mFitsKeyboard.disable();
                    }
                }
            } else {
                if (mParentBar != null) {
                    if (mParentBar.mBarParams.keyboardEnable) {
                        if (mParentBar.mFitsKeyboard == null) {
                            mParentBar.mFitsKeyboard = new FitsKeyboard(mParentBar);
                        }
                        mParentBar.mFitsKeyboard.enable(mParentBar.mBarParams.keyboardMode);
                    } else {
                        if (mParentBar.mFitsKeyboard != null) {
                            mParentBar.mFitsKeyboard.disable();
                        }
                    }
                }
            }
        }
    }

    void fitsParentBarKeyboard() {
        if (mParentBar != null && mParentBar.mFitsKeyboard != null) {
            mParentBar.mFitsKeyboard.disable();
            mParentBar.mFitsKeyboard.resetKeyboardHeight();
        }
    }

    /**
     * Gets bar params.
     *
     * @return the bar params
     */
    public BarParams getBarParams() {
        if (mBarParams == null) {
            return null;
        }
        if (mExposedBarParams == null || mExposedBarParams.get() != mBarParams) {
            mExposedBarParams = new WeakReference<>(mBarParams);
            syncExposedNavigationBarStyle();
        }
        return mBarParams;
    }

    private void setPadding(int left, int top, int right, int bottom) {
        if (mContentView != null) {
            mContentView.setPadding(left, top, right, bottom);
        }
        mPaddingLeft = left;
        mPaddingTop = top;
        mPaddingRight = right;
        mPaddingBottom = bottom;
    }

    /**
     * Gets padding left.
     *
     * @return the padding left
     */
    int getPaddingLeft() {
        return mPaddingLeft;
    }

    /**
     * Gets padding top.
     *
     * @return the padding top
     */
    int getPaddingTop() {
        return mPaddingTop;
    }

    /**
     * Gets padding right.
     *
     * @return the padding right
     */
    int getPaddingRight() {
        return mPaddingRight;
    }

    /**
     * Gets padding bottom.
     *
     * @return the padding bottom
     */
    int getPaddingBottom() {
        return mPaddingBottom;
    }

    @Nullable
    Activity getActivity() {
        return mActivity;
    }

    Window getWindow() {
        return mWindow;
    }

    Fragment getSupportFragment() {
        return mSupportFragment;
    }

    android.app.Fragment getFragment() {
        return mFragment;
    }

    /**
     * Whether used in a Fragment.
     *
     * @return the boolean
     */
    boolean isFragment() {
        return mIsFragment;
    }

    boolean isDialogFragment() {
        return mIsDialogFragment;
    }

    /**
     * Whether init() has already been called.
     */
    boolean initialized() {
        return mInitialized;
    }

    BarConfig getBarConfig() {
        if (mBarConfig == null) {
            mBarConfig = new BarConfig(mActivity);
        }
        return mBarConfig;
    }


    int getActionBarHeight() {
        return mActionBarHeight;
    }

    /**
     * Check whether the device supports status bar font color change.
     * Is support status bar dark font boolean.
     *
     * @return the boolean
     */
    public static boolean isSupportStatusBarDarkFont() {
        return OSUtils.isMIUI6Later() || OSUtils.isFlymeOS4Later()
                || (Build.VERSION.SDK_INT >= Version.M);
    }

    /**
     * Check whether navigation bar icons support color change.
     * Is support navigation icon dark boolean.
     *
     * @return the boolean
     */
    public static boolean isSupportNavigationIconDark() {
        return OSUtils.isMIUI6Later() || Build.VERSION.SDK_INT >= Version.O;
    }

    @SuppressLint("ObsoleteSdkInt")
    public static void setTitleBar(int fixHeight, View... view) {
        if (Build.VERSION.SDK_INT >= Version.KITKAT) {
            if (fixHeight < 0) {
                fixHeight = 0;
            }
            for (final View v : view) {
                if (v == null) {
                    continue;
                }
                final int statusBarHeight = fixHeight;
                Integer fitsHeight = (Integer) v.getTag(R.id.immersion_fits_layout_overlap);
                if (fitsHeight == null) {
                    fitsHeight = 0;
                }
                if (fitsHeight != statusBarHeight) {
                    v.setTag(R.id.immersion_fits_layout_overlap, statusBarHeight);
                    ViewGroup.LayoutParams layoutParams = v.getLayoutParams();
                    if (layoutParams == null) {
                        layoutParams = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    }
                    if (layoutParams.height == ViewGroup.LayoutParams.WRAP_CONTENT ||
                            layoutParams.height == ViewGroup.LayoutParams.MATCH_PARENT) {
                        final ViewGroup.LayoutParams finalLayoutParams = layoutParams;
                        final int finalFitsHeight = fitsHeight;
                        v.post(() -> {
                            finalLayoutParams.height = v.getHeight() + statusBarHeight - finalFitsHeight;
                            v.setPadding(v.getPaddingLeft(),
                                    v.getPaddingTop() + statusBarHeight - finalFitsHeight,
                                    v.getPaddingRight(),
                                    v.getPaddingBottom());
                            v.setLayoutParams(finalLayoutParams);
                        });
                    } else {
                        layoutParams.height += statusBarHeight - fitsHeight;
                        v.setPadding(v.getPaddingLeft(), v.getPaddingTop() + statusBarHeight - fitsHeight,
                                v.getPaddingRight(), v.getPaddingBottom());
                        v.setLayoutParams(layoutParams);
                    }
                }
            }
        }
    }

    /**
     * Increase paddingTop and height of title bar by the height of status bar.
     * Sets title bar.
     *
     * @param activity the activity
     * @param view     the view
     */
    public static void setTitleBar(@NonNull Activity activity, View... view) {
        setTitleBar(getStatusBarHeight(activity), view);
    }

    public static void setTitleBar(@NonNull Fragment fragment, View... view) {
        setTitleBar(getStatusBarHeight(fragment), view);
    }

    public static void setTitleBar(@NonNull android.app.Fragment fragment, View... view) {
        setTitleBar(getStatusBarHeight(fragment), view);
    }

    public static void setTitleBar(@NonNull Context context, View... view) {
        setTitleBar(getStatusBarHeight(context), view);
    }

    public static void setTitleBar(@NonNull Window window, View... view) {
        setTitleBar(getStatusBarHeight(window), view);
    }

    public static void setTitleBar(@NonNull View target, View... view) {
        setTitleBar(getStatusBarHeight(target), view);
    }

    public static void setTitleBar(@NonNull Dialog dialog, View... view) {
        setTitleBar(getStatusBarHeight(dialog), view);
    }

    @SuppressLint("ObsoleteSdkInt")
    public static void setTitleBarMarginTop(int fixHeight, View... view) {
        if (Build.VERSION.SDK_INT >= Version.KITKAT) {
            if (fixHeight < 0) {
                fixHeight = 0;
            }
            for (View v : view) {
                if (v == null) {
                    continue;
                }
                Integer fitsHeight = (Integer) v.getTag(R.id.immersion_fits_layout_overlap);
                if (fitsHeight == null) {
                    fitsHeight = 0;
                }
                if (fitsHeight != fixHeight) {
                    v.setTag(R.id.immersion_fits_layout_overlap, fixHeight);
                    ViewGroup.LayoutParams lp = v.getLayoutParams();
                    if (lp == null) {
                        lp = new ViewGroup.MarginLayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    }
                    ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) lp;
                    layoutParams.setMargins(layoutParams.leftMargin,
                            layoutParams.topMargin + fixHeight - fitsHeight,
                            layoutParams.rightMargin,
                            layoutParams.bottomMargin);
                    v.setLayoutParams(layoutParams);
                }
            }
        }
    }


    /**
     * Increase marginTop of title bar by the height of status bar.
     * Sets title bar margin top.
     *
     * @param activity the activity
     * @param view     the view
     */
    public static void setTitleBarMarginTop(@NonNull Activity activity, View... view) {
        setTitleBarMarginTop(getStatusBarHeight(activity), view);
    }

    public static void setTitleBarMarginTop(@NonNull Fragment fragment, View... view) {
        setTitleBarMarginTop(getStatusBarHeight(fragment), view);
    }

    public static void setTitleBarMarginTop(@NonNull android.app.Fragment fragment, View... view) {
        setTitleBarMarginTop(getStatusBarHeight(fragment), view);
    }

    public static void setTitleBarMarginTop(@NonNull Context context, View... view) {
        setTitleBarMarginTop(getStatusBarHeight(context), view);
    }

    public static void setTitleBarMarginTop(@NonNull Window window, View... view) {
        setTitleBarMarginTop(getStatusBarHeight(window), view);
    }

    public static void setTitleBarMarginTop(@NonNull View target, View... view) {
        setTitleBarMarginTop(getStatusBarHeight(target), view);
    }

    public static void setTitleBarMarginTop(@NonNull Dialog dialog, View... view) {
        setTitleBarMarginTop(getStatusBarHeight(dialog), view);
    }

    @SuppressLint("ObsoleteSdkInt")
    public static void setStatusBarView(int fixHeight, View... view) {
        if (Build.VERSION.SDK_INT >= Version.KITKAT) {
            if (fixHeight < 0) {
                fixHeight = 0;
            }
            for (View v : view) {
                if (v == null) {
                    continue;
                }
                Integer fitsHeight = (Integer) v.getTag(R.id.immersion_fits_layout_overlap);
                if (fitsHeight == null) {
                    fitsHeight = 0;
                }
                if (fitsHeight != fixHeight) {
                    v.setTag(R.id.immersion_fits_layout_overlap, fixHeight);
                    ViewGroup.LayoutParams lp = v.getLayoutParams();
                    if (lp == null) {
                        lp = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0);
                    }
                    lp.height = fixHeight;
                    v.setLayoutParams(lp);
                }
            }
        }
    }

    /**
     * Add a view separately at the title bar position with height equal to status bar height.
     * Sets status bar view.
     *
     * @param activity the activity
     * @param view     the view
     */
    public static void setStatusBarView(@NonNull Activity activity, View... view) {
        setStatusBarView(getStatusBarHeight(activity), view);
    }

    public static void setStatusBarView(@NonNull Fragment fragment, View... view) {
        setStatusBarView(getStatusBarHeight(fragment), view);
    }

    public static void setStatusBarView(@NonNull android.app.Fragment fragment, View... view) {
        setStatusBarView(getStatusBarHeight(fragment), view);
    }

    public static void setStatusBarView(@NonNull Context context, View... view) {
        setStatusBarView(getStatusBarHeight(context), view);
    }

    public static void setStatusBarView(@NonNull Window window, View... view) {
        setStatusBarView(getStatusBarHeight(window), view);
    }

    public static void setStatusBarView(@NonNull View target, View... view) {
        setStatusBarView(getStatusBarHeight(target), view);
    }

    public static void setStatusBarView(@NonNull Dialog dialog, View... view) {
        setStatusBarView(getStatusBarHeight(dialog), view);
    }

    /**
     * Call setFitsSystemWindows method of the system view.
     * Sets fits system windows.
     *
     * @param activity        the activity
     * @param applySystemFits the apply system fits
     */
    public static void setFitsSystemWindows(@NonNull Activity activity, boolean applySystemFits) {
        Window window = WindowUtils.getWindow(activity);
        setFitsSystemWindows(window, applySystemFits);
    }

    public static void setFitsSystemWindows(@NonNull Activity activity) {
        setFitsSystemWindows(activity, true);
    }

    public static void setFitsSystemWindows(@NonNull Fragment fragment, boolean applySystemFits) {
        Window window = WindowUtils.getWindow(fragment);
        if (window == null) {
            return;
        }
        setFitsSystemWindows(window, applySystemFits);
    }

    public static void setFitsSystemWindows(@NonNull Fragment fragment) {
        setFitsSystemWindows(fragment, true);
    }

    public static void setFitsSystemWindows(@NonNull android.app.Fragment fragment, boolean applySystemFits) {
        Window window = WindowUtils.getWindow(fragment);
        if (window == null) {
            return;
        }
        setFitsSystemWindows(window, applySystemFits);
    }

    public static void setFitsSystemWindows(@NonNull android.app.Fragment fragment) {
        setFitsSystemWindows(fragment, true);
    }

    public static void setFitsSystemWindows(@NonNull Context context, boolean applySystemFits) {
        Window window = WindowUtils.getWindow(context);
        if (window == null) {
            return;
        }
        setFitsSystemWindows(window, applySystemFits);
    }

    public static void setFitsSystemWindows(@NonNull Context context) {
        setFitsSystemWindows(context, true);
    }

    public static void setFitsSystemWindows(@NonNull Window window, boolean applySystemFits) {
        View contentView = window.getDecorView().findViewById(android.R.id.content);
        if (contentView instanceof ViewGroup) {
            setFitsSystemWindows(((ViewGroup) contentView).getChildAt(0), applySystemFits);
        } else {
            setFitsSystemWindows(contentView, applySystemFits);
        }
    }

    public static void setFitsSystemWindows(@NonNull Window window) {
        setFitsSystemWindows(window, true);
    }

    public static void setFitsSystemWindows(@NonNull View view) {
        setFitsSystemWindows(view, true);
    }

    public static void setFitsSystemWindows(@NonNull Dialog dialog, boolean applySystemFits) {
        Window window = WindowUtils.getWindow(dialog);
        if (window == null) {
            return;
        }
        setFitsSystemWindows(window, applySystemFits);
    }

    public static void setFitsSystemWindows(@NonNull Dialog dialog) {
        setFitsSystemWindows(dialog, true);
    }

    public static void setFitsSystemWindows(@NonNull View view, boolean applySystemFits) {
        if (view instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) view;
            if (viewGroup instanceof DrawerLayout) {
                setFitsSystemWindows(viewGroup.getChildAt(0), applySystemFits);
            } else {
                viewGroup.setFitsSystemWindows(applySystemFits);
                viewGroup.setClipToPadding(true);
            }
        } else {
            view.setFitsSystemWindows(applySystemFits);
        }
    }

    /**
     * Check if layout root node uses android:fitsSystemWindows="true" attribute.
     * Check fits system windows boolean.
     *
     * @param view the view
     * @return the boolean
     */
    public static boolean checkFitsSystemWindows(View view) {
        if (view == null) {
            return false;
        }
        if (view.getFitsSystemWindows()) {
            return true;
        }
        if (view instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) view;
            for (int i = 0, count = viewGroup.getChildCount(); i < count; i++) {
                View childView = viewGroup.getChildAt(i);
                if (childView instanceof DrawerLayout) {
                    if (checkFitsSystemWindows(childView)) {
                        return true;
                    }
                }
                if (childView.getFitsSystemWindows()) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Has navigtion bar boolean.
     * Check if navigation bar exists.
     *
     * @param activity the activity
     * @return the boolean
     */
    @SuppressLint("ObsoleteSdkInt")
    @TargetApi(Version.ICE_CREAM_SANDWICH)
    public static boolean hasNavigationBar(@NonNull Activity activity) {
        return getBarProperties(activity).hasNavigationBar();
    }

    @SuppressLint("ObsoleteSdkInt")
    @TargetApi(Version.ICE_CREAM_SANDWICH)
    public static boolean hasNavigationBar(@NonNull Window window) {
        return getBarProperties(window).hasNavigationBar();
    }

    @SuppressLint("ObsoleteSdkInt")
    @TargetApi(Version.ICE_CREAM_SANDWICH)
    public static boolean hasNavigationBar(@NonNull View view) {
        return getBarProperties(view).hasNavigationBar();
    }

    @SuppressLint("ObsoleteSdkInt")
    @TargetApi(Version.ICE_CREAM_SANDWICH)
    public static boolean hasNavigationBar(@NonNull Dialog dialog) {
        return getBarProperties(dialog).hasNavigationBar();
    }

    @SuppressLint("ObsoleteSdkInt")
    @TargetApi(Version.ICE_CREAM_SANDWICH)
    public static boolean hasNavigationBar(@NonNull Fragment fragment) {
        return getBarProperties(fragment).hasNavigationBar();
    }

    @SuppressLint("ObsoleteSdkInt")
    @TargetApi(Version.ICE_CREAM_SANDWICH)
    public static boolean hasNavigationBar(@NonNull android.app.Fragment fragment) {
        return getBarProperties(fragment).hasNavigationBar();
    }

    @SuppressLint("ObsoleteSdkInt")
    @TargetApi(Version.ICE_CREAM_SANDWICH)
    public static boolean hasNavigationBar(@NonNull Context context) {
        return getBarProperties(context).hasNavigationBar();
    }

    /**
     * Gets navigation bar height.
     * Get navigation bar height.
     *
     * @param activity the activity
     * @return the navigation bar height
     */
    @SuppressLint("ObsoleteSdkInt")
    @TargetApi(Version.ICE_CREAM_SANDWICH)
    public static int getNavigationBarHeight(@NonNull Activity activity) {
        return getBarProperties(activity).getNavigationBarHeight();
    }

    @SuppressLint("ObsoleteSdkInt")
    @TargetApi(Version.ICE_CREAM_SANDWICH)
    public static int getNavigationBarHeight(@NonNull Window window) {
        return getBarProperties(window).getNavigationBarHeight();
    }

    @SuppressLint("ObsoleteSdkInt")
    @TargetApi(Version.ICE_CREAM_SANDWICH)
    public static int getNavigationBarHeight(@NonNull View view) {
        return getBarProperties(view).getNavigationBarHeight();
    }

    @SuppressLint("ObsoleteSdkInt")
    @TargetApi(Version.ICE_CREAM_SANDWICH)
    public static int getNavigationBarHeight(@NonNull Dialog dialog) {
        return getBarProperties(dialog).getNavigationBarHeight();
    }

    @SuppressLint("ObsoleteSdkInt")
    @TargetApi(Version.ICE_CREAM_SANDWICH)
    public static int getNavigationBarHeight(@NonNull Fragment fragment) {
        return getBarProperties(fragment).getNavigationBarHeight();
    }

    @SuppressLint("ObsoleteSdkInt")
    @TargetApi(Version.ICE_CREAM_SANDWICH)
    public static int getNavigationBarHeight(@NonNull android.app.Fragment fragment) {
        return getBarProperties(fragment).getNavigationBarHeight();
    }

    @SuppressLint("ObsoleteSdkInt")
    @TargetApi(Version.ICE_CREAM_SANDWICH)
    public static int getNavigationBarHeight(@NonNull Context context) {
        return getBarProperties(context).getNavigationBarHeight();
    }

    /**
     * Gets navigation bar width.
     * Get navigation bar width.
     *
     * @param activity the activity
     * @return the navigation bar width
     */
    @SuppressLint("ObsoleteSdkInt")
    @TargetApi(Version.ICE_CREAM_SANDWICH)
    public static int getNavigationBarWidth(@NonNull Activity activity) {
        return getBarProperties(activity).getNavigationBarWidth();
    }

    @SuppressLint("ObsoleteSdkInt")
    @TargetApi(Version.ICE_CREAM_SANDWICH)
    public static int getNavigationBarWidth(@NonNull Window window) {
        return getBarProperties(window).getNavigationBarWidth();
    }

    @SuppressLint("ObsoleteSdkInt")
    @TargetApi(Version.ICE_CREAM_SANDWICH)
    public static int getNavigationBarWidth(@NonNull View view) {
        return getBarProperties(view).getNavigationBarWidth();
    }

    @SuppressLint("ObsoleteSdkInt")
    @TargetApi(Version.ICE_CREAM_SANDWICH)
    public static int getNavigationBarWidth(@NonNull Dialog dialog) {
        return getBarProperties(dialog).getNavigationBarWidth();
    }

    @SuppressLint("ObsoleteSdkInt")
    @TargetApi(Version.ICE_CREAM_SANDWICH)
    public static int getNavigationBarWidth(@NonNull Fragment fragment) {
        return getBarProperties(fragment).getNavigationBarWidth();
    }

    @SuppressLint("ObsoleteSdkInt")
    @TargetApi(Version.ICE_CREAM_SANDWICH)
    public static int getNavigationBarWidth(@NonNull android.app.Fragment fragment) {
        return getBarProperties(fragment).getNavigationBarWidth();
    }

    @SuppressLint("ObsoleteSdkInt")
    @TargetApi(Version.ICE_CREAM_SANDWICH)
    public static int getNavigationBarWidth(@NonNull Context context) {
        return getBarProperties(context).getNavigationBarWidth();
    }

    /**
     * Is navigation at bottom boolean.
     * Check if navigation bar is at the bottom.
     *
     * @param activity the activity
     * @return the boolean
     */
    @SuppressLint("ObsoleteSdkInt")
    @TargetApi(Version.ICE_CREAM_SANDWICH)
    public static boolean isNavigationAtBottom(@NonNull Activity activity) {
        return getBarProperties(activity).isNavigationAtBottom();
    }

    @SuppressLint("ObsoleteSdkInt")
    @TargetApi(Version.ICE_CREAM_SANDWICH)
    public static boolean isNavigationAtBottom(@NonNull Window window) {
        return getBarProperties(window).isNavigationAtBottom();
    }

    @SuppressLint("ObsoleteSdkInt")
    @TargetApi(Version.ICE_CREAM_SANDWICH)
    public static boolean isNavigationAtBottom(@NonNull View view) {
        return getBarProperties(view).isNavigationAtBottom();
    }

    @SuppressLint("ObsoleteSdkInt")
    @TargetApi(Version.ICE_CREAM_SANDWICH)
    public static boolean isNavigationAtBottom(@NonNull Dialog dialog) {
        return getBarProperties(dialog).isNavigationAtBottom();
    }

    @SuppressLint("ObsoleteSdkInt")
    @TargetApi(Version.ICE_CREAM_SANDWICH)
    public static boolean isNavigationAtBottom(@NonNull Fragment fragment) {
        return getBarProperties(fragment).isNavigationAtBottom();
    }

    @SuppressLint("ObsoleteSdkInt")
    @TargetApi(Version.ICE_CREAM_SANDWICH)
    public static boolean isNavigationAtBottom(@NonNull android.app.Fragment fragment) {
        return getBarProperties(fragment).isNavigationAtBottom();
    }

    @SuppressLint("ObsoleteSdkInt")
    @TargetApi(Version.ICE_CREAM_SANDWICH)
    public static boolean isNavigationAtBottom(@NonNull Context context) {
        return getBarProperties(context).isNavigationAtBottom();
    }

    /**
     * Get current BarProperties snapshot.
     *
     * @param activity the activity
     * @return BarProperties snapshot
     */
    @NonNull
    public static BarProperties getBarProperties(@NonNull Activity activity) {
        return BarPropertiesUtils.getBarProperties(activity);
    }

    /**
     * Get current BarProperties snapshot.
     *
     * @param fragment the fragment
     * @return BarProperties snapshot
     */
    @NonNull
    public static BarProperties getBarProperties(@NonNull Fragment fragment) {
        return BarPropertiesUtils.getBarProperties(fragment);
    }

    /**
     * Get current BarProperties snapshot.
     *
     * @param fragment the fragment
     * @return BarProperties snapshot
     */
    @NonNull
    public static BarProperties getBarProperties(@NonNull android.app.Fragment fragment) {
        return BarPropertiesUtils.getBarProperties(fragment);
    }

    /**
     * Get current BarProperties snapshot.
     *
     * @param dialog the dialog
     * @return BarProperties snapshot
     */
    @NonNull
    public static BarProperties getBarProperties(@NonNull Dialog dialog) {
        return BarPropertiesUtils.getBarProperties(dialog);
    }

    /**
     * Get current BarProperties snapshot.
     *
     * @param view the view
     * @return BarProperties snapshot
     */
    @NonNull
    public static BarProperties getBarProperties(@NonNull View view) {
        return BarPropertiesUtils.getBarProperties(view);
    }

    /**
     * Get current BarProperties snapshot.
     *
     * @param context the context
     * @return BarProperties snapshot
     */
    @NonNull
    public static BarProperties getBarProperties(@NonNull Context context) {
        return BarPropertiesUtils.getBarProperties(context);
    }

    /**
     * Get current BarProperties snapshot.
     *
     * @param window the window
     * @return BarProperties snapshot
     */
    @NonNull
    public static BarProperties getBarProperties(@NonNull Window window) {
        return BarPropertiesUtils.getBarProperties(window);
    }

    /**
     * Get current status bar snapshot.
     *
     * @param activity the activity
     * @return StatusBar snapshot
     */
    @NonNull
    public static StatusBar getStatusBar(@NonNull Activity activity) {
        return StatusBar.from(getBarProperties(activity), false, StatusBar.CHANGE_NONE);
    }

    /**
     * Get current status bar snapshot.
     *
     * @param fragment the fragment
     * @return StatusBar snapshot
     */
    @NonNull
    public static StatusBar getStatusBar(@NonNull Fragment fragment) {
        return StatusBar.from(getBarProperties(fragment), false, StatusBar.CHANGE_NONE);
    }

    /**
     * Get current status bar snapshot.
     *
     * @param fragment the fragment
     * @return StatusBar snapshot
     */
    @NonNull
    public static StatusBar getStatusBar(@NonNull android.app.Fragment fragment) {
        return StatusBar.from(getBarProperties(fragment), false, StatusBar.CHANGE_NONE);
    }

    /**
     * Get current status bar snapshot.
     *
     * @param dialog the dialog
     * @return StatusBar snapshot
     */
    @NonNull
    public static StatusBar getStatusBar(@NonNull Dialog dialog) {
        return StatusBar.from(getBarProperties(dialog), false, StatusBar.CHANGE_NONE);
    }

    /**
     * Get current status bar snapshot.
     *
     * @param view the view
     * @return StatusBar snapshot
     */
    @NonNull
    public static StatusBar getStatusBar(@NonNull View view) {
        return StatusBar.from(getBarProperties(view), false, StatusBar.CHANGE_NONE);
    }

    /**
     * Get current status bar snapshot.
     *
     * @param context the context
     * @return StatusBar snapshot
     */
    @NonNull
    public static StatusBar getStatusBar(@NonNull Context context) {
        return StatusBar.from(getBarProperties(context), false, StatusBar.CHANGE_NONE);
    }

    /**
     * Get current status bar snapshot.
     *
     * @param window the window
     * @return StatusBar snapshot
     */
    @NonNull
    public static StatusBar getStatusBar(@NonNull Window window) {
        return StatusBar.from(getBarProperties(window), false, StatusBar.CHANGE_NONE);
    }

    /**
     * Get current navigation bar snapshot.
     *
     * @param activity the activity
     * @return NavigationBar snapshot
     */
    @NonNull
    public static NavigationBar getNavigationBar(@NonNull Activity activity) {
        return NavigationBar.from(getBarProperties(activity), false, NavigationBar.CHANGE_NONE);
    }

    /**
     * Get current navigation bar snapshot.
     *
     * @param fragment the fragment
     * @return NavigationBar snapshot
     */
    @NonNull
    public static NavigationBar getNavigationBar(@NonNull Fragment fragment) {
        return NavigationBar.from(getBarProperties(fragment), false, NavigationBar.CHANGE_NONE);
    }

    /**
     * Get current navigation bar snapshot.
     *
     * @param fragment the fragment
     * @return NavigationBar snapshot
     */
    @NonNull
    public static NavigationBar getNavigationBar(@NonNull android.app.Fragment fragment) {
        return NavigationBar.from(getBarProperties(fragment), false, NavigationBar.CHANGE_NONE);
    }

    /**
     * Get current navigation bar snapshot.
     *
     * @param dialog the dialog
     * @return NavigationBar snapshot
     */
    @NonNull
    public static NavigationBar getNavigationBar(@NonNull Dialog dialog) {
        return NavigationBar.from(getBarProperties(dialog), false, NavigationBar.CHANGE_NONE);
    }

    /**
     * Get current navigation bar snapshot.
     *
     * @param view the view
     * @return NavigationBar snapshot
     */
    @NonNull
    public static NavigationBar getNavigationBar(@NonNull View view) {
        return NavigationBar.from(getBarProperties(view), false, NavigationBar.CHANGE_NONE);
    }

    /**
     * Get current navigation bar snapshot.
     *
     * @param context the context
     * @return NavigationBar snapshot
     */
    @NonNull
    public static NavigationBar getNavigationBar(@NonNull Context context) {
        return NavigationBar.from(getBarProperties(context), false, NavigationBar.CHANGE_NONE);
    }

    /**
     * Get current navigation bar snapshot.
     *
     * @param window the window
     * @return NavigationBar snapshot
     */
    @NonNull
    public static NavigationBar getNavigationBar(@NonNull Window window) {
        return NavigationBar.from(getBarProperties(window), false, NavigationBar.CHANGE_NONE);
    }

    /**
     * Gets status bar height, regardless of its current visibility.
     * Get actual height of status bar (ignores current visibility, returns actual size even when hidden).
     *
     * @param activity the activity
     * @return the status bar height
     */
    @SuppressLint("ObsoleteSdkInt")
    @TargetApi(Version.ICE_CREAM_SANDWICH)
    public static int getStatusBarHeight(@NonNull Activity activity) {
        return getBarProperties(activity).getStatusBarHeightIgnoringVisibility();
    }

    @SuppressLint("ObsoleteSdkInt")
    @TargetApi(Version.ICE_CREAM_SANDWICH)
    public static int getStatusBarHeight(@NonNull Window window) {
        return getBarProperties(window).getStatusBarHeightIgnoringVisibility();
    }

    @SuppressLint("ObsoleteSdkInt")
    @TargetApi(Version.ICE_CREAM_SANDWICH)
    public static int getStatusBarHeight(@NonNull View view) {
        return getBarProperties(view).getStatusBarHeightIgnoringVisibility();
    }

    @SuppressLint("ObsoleteSdkInt")
    @TargetApi(Version.ICE_CREAM_SANDWICH)
    public static int getStatusBarHeight(@NonNull Dialog dialog) {
        return getBarProperties(dialog).getStatusBarHeightIgnoringVisibility();
    }

    @SuppressLint("ObsoleteSdkInt")
    @TargetApi(Version.ICE_CREAM_SANDWICH)
    public static int getStatusBarHeight(@NonNull Fragment fragment) {
        return getBarProperties(fragment).getStatusBarHeightIgnoringVisibility();
    }

    @SuppressLint("ObsoleteSdkInt")
    @TargetApi(Version.ICE_CREAM_SANDWICH)
    public static int getStatusBarHeight(@NonNull android.app.Fragment fragment) {
        return getBarProperties(fragment).getStatusBarHeightIgnoringVisibility();
    }

    @SuppressLint("ObsoleteSdkInt")
    @TargetApi(Version.ICE_CREAM_SANDWICH)
    public static int getStatusBarHeight(@NonNull Context context) {
        return getBarProperties(context).getStatusBarHeightIgnoringVisibility();
    }

    /**
     * Gets action bar height.
     * Get ActionBar height.
     *
     * @param activity the activity
     * @return the action bar height
     */
    @SuppressLint("ObsoleteSdkInt")
    @TargetApi(Version.ICE_CREAM_SANDWICH)
    public static int getActionBarHeight(@NonNull Activity activity) {
        return getBarProperties(activity).getActionBarHeight();
    }

    @SuppressLint("ObsoleteSdkInt")
    @TargetApi(Version.ICE_CREAM_SANDWICH)
    public static int getActionBarHeight(@NonNull Window window) {
        return getBarProperties(window).getActionBarHeight();
    }

    @SuppressLint("ObsoleteSdkInt")
    @TargetApi(Version.ICE_CREAM_SANDWICH)
    public static int getActionBarHeight(@NonNull View view) {
        return getBarProperties(view).getActionBarHeight();
    }

    @SuppressLint("ObsoleteSdkInt")
    @TargetApi(Version.ICE_CREAM_SANDWICH)
    public static int getActionBarHeight(@NonNull Dialog dialog) {
        return getBarProperties(dialog).getActionBarHeight();
    }

    @SuppressLint("ObsoleteSdkInt")
    @TargetApi(Version.ICE_CREAM_SANDWICH)
    public static int getActionBarHeight(@NonNull Fragment fragment) {
        return getBarProperties(fragment).getActionBarHeight();
    }

    @SuppressLint("ObsoleteSdkInt")
    @TargetApi(Version.ICE_CREAM_SANDWICH)
    public static int getActionBarHeight(@NonNull android.app.Fragment fragment) {
        return getBarProperties(fragment).getActionBarHeight();
    }

    @SuppressLint("ObsoleteSdkInt")
    @TargetApi(Version.ICE_CREAM_SANDWICH)
    public static int getActionBarHeight(@NonNull Context context) {
        return getBarProperties(context).getActionBarHeight();
    }

    /**
     * Whether the display has a notch / cutout.
     * Has notch screen boolean.
     * e.g:getWindow().getDecorView().post(() -> ImmersionBar.hasNotchScreen(this));
     *
     * @param activity the activity
     * @return the boolean
     */
    public static boolean hasNotchScreen(@NonNull Activity activity) {
        Window window = WindowUtils.getWindow(activity);
        return hasNotchScreen(window);
    }

    public static boolean hasNotchScreen(@NonNull Fragment fragment) {
        Window window = WindowUtils.getWindow(fragment);
        if (window == null) {
            return false;
        }
        return hasNotchScreen(window);
    }

    public static boolean hasNotchScreen(@NonNull android.app.Fragment fragment) {
        Window window = WindowUtils.getWindow(fragment);
        if (window == null) {
            return false;
        }
        return hasNotchScreen(window);
    }

    public static boolean hasNotchScreen(@NonNull Context context) {
        Window window = WindowUtils.getWindow(context);
        if (window == null) {
            return false;
        }
        return hasNotchScreen(window);
    }

    /**
     * Whether the display has a notch / cutout.
     * Has notch screen boolean.
     *
     * @param window the window
     * @return the boolean
     */
    public static boolean hasNotchScreen(@NonNull Window window) {
        return NotchUtils.hasNotchScreen(window);
    }

    /**
     * Whether the display has a notch / cutout.
     * Has notch screen boolean.
     *
     * @param view the view
     * @return the boolean
     */
    public static boolean hasNotchScreen(@NonNull View view) {
        Window window = WindowUtils.getWindow(view);
        if (window == null) {
            return false;
        }
        return hasNotchScreen(window);
    }

    public static boolean hasNotchScreen(@NonNull Dialog dialog) {
        Window window = WindowUtils.getWindow(dialog);
        if (window == null) {
            return false;
        }
        return hasNotchScreen(window);
    }

    /**
     * Notch / display cutout height.
     * Notch height int.
     * e.g: getWindow().getDecorView().post(() -> ImmersionBar.getNotchHeight(this));
     *
     * @param activity the activity
     * @return the int
     */
    public static int getNotchHeight(@NonNull Activity activity) {
        Window window = WindowUtils.getWindow(activity);
        return getNotchHeight(window);
    }

    public static int getNotchHeight(@NonNull Fragment fragment) {
        Window window = WindowUtils.getWindow(fragment);
        if (window == null) {
            return 0;
        }
        return getNotchHeight(window);
    }

    public static int getNotchHeight(@NonNull android.app.Fragment fragment) {
        Window window = WindowUtils.getWindow(fragment);
        if (window == null) {
            return 0;
        }
        return getNotchHeight(window);
    }

    public static int getNotchHeight(@NonNull Context context) {
        Window window = WindowUtils.getWindow(context);
        if (window == null) {
            return 0;
        }
        return getNotchHeight(window);
    }

    public static int getNotchHeight(@NonNull Window window) {
        return NotchUtils.getNotchHeight(window);
    }

    public static int getNotchHeight(@NonNull View view) {
        Window window = WindowUtils.getWindow(view);
        if (window == null) {
            return 0;
        }
        return getNotchHeight(window);
    }

    public static int getNotchHeight(@NonNull Dialog dialog) {
        Window window = WindowUtils.getWindow(dialog);
        if (window == null) {
            return 0;
        }
        return getNotchHeight(window);
    }

    public static void getNotchHeight(@NonNull Activity activity, NotchCallback callback) {
        Window window = WindowUtils.getWindow(activity);
        getNotchHeight(window, callback);
    }

    public static void getNotchHeight(@NonNull Fragment fragment, NotchCallback callback) {
        Window window = WindowUtils.getWindow(fragment);
        if (window == null) {
            if (callback != null) {
                callback.onNotchHeight(0);
            }
            return;
        }
        getNotchHeight(window, callback);
    }

    public static void getNotchHeight(@NonNull android.app.Fragment fragment, NotchCallback callback) {
        Window window = WindowUtils.getWindow(fragment);
        if (window == null) {
            if (callback != null) {
                callback.onNotchHeight(0);
            }
            return;
        }
        getNotchHeight(window, callback);
    }

    public static void getNotchHeight(@NonNull Context context, NotchCallback callback) {
        Window window = WindowUtils.getWindow(context);
        if (window == null) {
            if (callback != null) {
                callback.onNotchHeight(0);
            }
            return;
        }
        getNotchHeight(window, callback);
    }

    public static void getNotchHeight(@NonNull Window window, NotchCallback callback) {
        NotchUtils.getNotchHeight(window, callback);
    }

    public static void getNotchHeight(@NonNull View view, NotchCallback callback) {
        Window window = WindowUtils.getWindow(view);
        if (window == null) {
            if (callback != null) {
                callback.onNotchHeight(0);
            }
            return;
        }
        getNotchHeight(window, callback);
    }

    public static void getNotchHeight(@NonNull Dialog dialog, NotchCallback callback) {
        Window window = WindowUtils.getWindow(dialog);
        if (window == null) {
            if (callback != null) {
                callback.onNotchHeight(0);
            }
            return;
        }
        getNotchHeight(window, callback);
    }

    /**
     * Hide status bar.
     * Hide status bar.
     *
     * @param activity the activity
     */
    public static void hideStatusBar(@NonNull Activity activity) {
        Window window = WindowUtils.getWindow(activity);
        hideStatusBar(window);
    }

    public static void hideStatusBar(@NonNull Fragment fragment) {
        Window window = WindowUtils.getWindow(fragment);
        if (window == null) {
            return;
        }
        hideStatusBar(window);
    }

    public static void hideStatusBar(@NonNull android.app.Fragment fragment) {
        Window window = WindowUtils.getWindow(fragment);
        if (window == null) {
            return;
        }
        hideStatusBar(window);
    }

    public static void hideStatusBar(@NonNull Context context) {
        Window window = WindowUtils.getWindow(context);
        if (window == null) {
            return;
        }
        hideStatusBar(window);
    }

    /**
     * Hide status bar via window flag {@link WindowManager.LayoutParams#FLAG_FULLSCREEN};
     * on API 16~29, also synchronizes {@link View#SYSTEM_UI_FLAG_FULLSCREEN} bit,
     * allowing visibility callbacks such as {@link OnStatusBarChangedListener} to trigger normally.
     * Hide status bar.
     *
     * @param window the window
     */
    public static void hideStatusBar(@NonNull Window window) {
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        if (Build.VERSION.SDK_INT >= Version.JELLY_BEAN && Build.VERSION.SDK_INT < Version.R) {
            // Visibility listeners on API 16~29 observe systemUiVisibility; synchronizing this bit triggers runtime callbacks.
            View decorView = window.getDecorView();
            int oldVisibility = decorView.getSystemUiVisibility();
            int newVisibility = oldVisibility | View.SYSTEM_UI_FLAG_FULLSCREEN;
            if (newVisibility != oldVisibility) {
                decorView.setSystemUiVisibility(newVisibility);
            }
        }
    }

    public static void hideStatusBar(@NonNull View view) {
        Window window = WindowUtils.getWindow(view);
        if (window == null) {
            return;
        }
        hideStatusBar(window);
    }

    public static void hideStatusBar(@NonNull Dialog dialog) {
        Window window = WindowUtils.getWindow(dialog);
        if (window == null) {
            return;
        }
        hideStatusBar(window);
    }

    /**
     * Show status bar.
     * Show status bar.
     *
     * @param activity the activity
     */
    public static void showStatusBar(@NonNull Activity activity) {
        Window window = WindowUtils.getWindow(activity);
        showStatusBar(window);
    }

    public static void showStatusBar(@NonNull Fragment fragment) {
        Window window = WindowUtils.getWindow(fragment);
        if (window == null) {
            return;
        }
        showStatusBar(window);
    }

    public static void showStatusBar(@NonNull android.app.Fragment fragment) {
        Window window = WindowUtils.getWindow(fragment);
        if (window == null) {
            return;
        }
        showStatusBar(window);
    }

    public static void showStatusBar(@NonNull Context context) {
        Window window = WindowUtils.getWindow(context);
        if (window == null) {
            return;
        }
        showStatusBar(window);
    }

    /**
     * Show status bar, clearing window flag {@link WindowManager.LayoutParams#FLAG_FULLSCREEN};
     * on API 16~29, also clears {@link View#SYSTEM_UI_FLAG_FULLSCREEN} bit (preserving other bits),
     * symmetric with {@link #hideStatusBar(Window)}, allowing visibility callbacks to trigger normally.
     * Show status bar.
     *
     * @param window the window
     */
    public static void showStatusBar(@NonNull Window window) {
        window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        if (Build.VERSION.SDK_INT >= Version.JELLY_BEAN && Build.VERSION.SDK_INT < Version.R) {
            // Symmetric with hideStatusBar, preserving other systemUiVisibility bits set by caller.
            View decorView = window.getDecorView();
            int oldVisibility = decorView.getSystemUiVisibility();
            int newVisibility = oldVisibility & ~View.SYSTEM_UI_FLAG_FULLSCREEN;
            if (newVisibility != oldVisibility) {
                decorView.setSystemUiVisibility(newVisibility);
            }
        }
    }

    public static void showStatusBar(@NonNull View view) {
        Window window = WindowUtils.getWindow(view);
        if (window == null) {
            return;
        }
        showStatusBar(window);
    }

    public static void showStatusBar(@NonNull Dialog dialog) {
        Window window = WindowUtils.getWindow(dialog);
        if (window == null) {
            return;
        }
        showStatusBar(window);
    }

    /**
     * Whether gesture navigation is enabled.
     *
     * @param activity Activity
     * @return the boolean
     */
    public static boolean isGesture(@NonNull Activity activity) {
        Window window = WindowUtils.getWindow(activity);
        return isGesture(window);
    }

    public static boolean isGesture(@NonNull Fragment fragment) {
        Window window = WindowUtils.getWindow(fragment);
        if (window == null) {
            return false;
        }
        return isGesture(window);
    }

    public static boolean isGesture(@NonNull android.app.Fragment fragment) {
        Window window = WindowUtils.getWindow(fragment);
        if (window == null) {
            return false;
        }
        return isGesture(window);
    }

    /**
     * Whether gesture navigation is enabled.
     *
     * @param context Context
     * @return the boolean
     */
    public static boolean isGesture(@NonNull Context context) {
        return GestureUtils.getGestureBean(context).isGesture;
    }

    public static boolean isGesture(@NonNull Window window) {
        return isGesture(window.getContext());
    }

    public static boolean isGesture(@NonNull View view) {
        return isGesture(view.getContext());
    }

    public static boolean isGesture(@NonNull Dialog dialog) {
        Window window = WindowUtils.getWindow(dialog);
        if (window == null) {
            return false;
        }
        return isGesture(window);
    }

    /**
     * Initialize in Activity.
     * Instantiates a new Immersion bar.
     *
     * @param activity the activity
     */
    ImmersionBar(Activity activity) {
        mActivity = activity;
        initCommonParameter(mActivity.getWindow());
    }

    /**
     * Initialize in Fragment.
     * Instantiates a new Immersion bar.
     *
     * @param fragment the fragment
     */
    ImmersionBar(Fragment fragment) {
        mIsFragment = true;
        Activity activity = fragment.getActivity();
        if (activity == null) {
            throw new IllegalArgumentException("Fragment must be attached to an Activity.");
        }
        mActivity = activity;
        mSupportFragment = fragment;
        checkInitWithActivity();
        initCommonParameter(mActivity.getWindow());
    }

    /**
     * Initialize in Fragment.
     * Instantiates a new Immersion bar.
     *
     * @param fragment the fragment
     */
    ImmersionBar(android.app.Fragment fragment) {
        mIsFragment = true;
        mActivity = fragment.getActivity();
        mFragment = fragment;
        checkInitWithActivity();
        initCommonParameter(mActivity.getWindow());
    }

    /**
     * Used in DialogFragment.
     * Instantiates a new Immersion bar.
     *
     * @param dialogFragment the dialog fragment
     */
    ImmersionBar(DialogFragment dialogFragment) {
        mIsDialog = true;
        mIsDialogFragment = true;
        mActivity = dialogFragment.getActivity();
        mSupportFragment = dialogFragment;
        mDialog = dialogFragment.getDialog();
        checkInitWithActivity();
        initCommonParameter(mDialog.getWindow());
    }

    /**
     * Used in DialogFragment.
     * Instantiates a new Immersion bar.
     *
     * @param dialogFragment the dialog fragment
     */
    ImmersionBar(android.app.DialogFragment dialogFragment) {
        mIsDialog = true;
        mIsDialogFragment = true;
        mActivity = dialogFragment.getActivity();
        mFragment = dialogFragment;
        mDialog = dialogFragment.getDialog();
        checkInitWithActivity();
        initCommonParameter(mDialog.getWindow());
    }

    /**
     * Initialize in Dialog.
     * Instantiates a new Immersion bar.
     *
     * @param activity the activity
     * @param dialog   the dialog
     */
    ImmersionBar(Activity activity, Dialog dialog) {
        mIsDialog = true;
        mActivity = activity;
        mDialog = dialog;
        checkInitWithActivity();
        initCommonParameter(mDialog.getWindow());
    }

    /**
     * Check whether initialized in Activity; if not, initialize automatically.
     */
    private void checkInitWithActivity() {
        if (mParentBar == null) {
            mParentBar = with(mActivity);
        }
        if (mParentBar != null && !mParentBar.mInitialized) {
            mParentBar.init();
        }
    }

    /**
     * Initialize common parameters.
     *
     * @param window window
     */
    private void initCommonParameter(Window window) {
        mWindow = window;
        mDecorView = (ViewGroup) mWindow.getDecorView();
        mContentView = mDecorView.findViewById(android.R.id.content);
        captureDefaultNavigationBarStyle();
        mBarParams = createDefaultBarParams();
        // Register internal default callback: synchronize fake status bar / fake navigation bar views and overlap adaptation views, participating in uniform dispatch along with listener sets
        mOnStatusBarChangedListeners.add(mStatusBarSync);
        mOnNavigationBarChangedListeners.add(mFakeNavigationBarSync);
    }

    /**
     * Captures the current Window's navigation bar appearance prior to being taken over by ImmersionBar.
     * When the user has not actively configured navigation bar color or icon mode, this appearance is preserved
     * to avoid being overwritten by BarParams' compatibility default of black.
     */
    @SuppressLint("ObsoleteSdkInt")
    private void captureDefaultNavigationBarStyle() {
        if (Build.VERSION.SDK_INT >= Version.LOLLIPOP) {
            mDefaultNavigationBarColor = mWindow.getNavigationBarColor();
        }
        if (Build.VERSION.SDK_INT < Version.O) {
            return;
        }
        mDefaultNavigationBarDarkIcon =
                (mDecorView.getSystemUiVisibility() & View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR) != 0;
        if (Build.VERSION.SDK_INT >= Version.R) {
            WindowInsetsController controller = mDecorView.getWindowInsetsController();
            if (controller != null) {
                mDefaultNavigationBarDarkIcon = (controller.getSystemBarsAppearance()
                        & WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS) != 0;
            }
        }
    }

    /**
     * Creates default parameters inheriting the current Window's original navigation bar appearance.
     *
     * @return the default bar params
     */
    private BarParams createDefaultBarParams() {
        BarParams barParams = new BarParams();
        barParams.navigationBarColor = mDefaultNavigationBarColor;
        barParams.defaultNavigationBarColor = mDefaultNavigationBarColor;
        barParams.navigationBarDarkIcon = mDefaultNavigationBarDarkIcon;
        return barParams;
    }

    /**
     * Transparent status bar, default is transparent.
     *
     * @return the immersion bar
     */
    public ImmersionBar transparentStatusBar() {
        mBarParams.statusBarColor = Color.TRANSPARENT;
        return this;
    }

    /**
     * Transparent navigation bar, default is black.
     *
     * @return the immersion bar
     */
    public ImmersionBar transparentNavigationBar() {
        mBarParams.navigationBarColorSpecified = true;
        mBarParams.navigationBarColor = Color.TRANSPARENT;
        mBarParams.fullScreen = true;
        return this;
    }

    /**
     * Transparent status bar and navigation bar.
     *
     * @return the immersion bar
     */
    public ImmersionBar transparentBar() {
        mBarParams.statusBarColor = Color.TRANSPARENT;
        mBarParams.navigationBarColorSpecified = true;
        mBarParams.navigationBarColor = Color.TRANSPARENT;
        mBarParams.fullScreen = true;
        return this;
    }

    /**
     * Status bar color.
     *
     * @param statusBarColor status bar color resource (R.color.xxx)
     * @return the immersion bar
     */
    public ImmersionBar statusBarColor(@ColorRes int statusBarColor) {
        return this.statusBarColorInt(ContextCompat.getColor(mActivity, statusBarColor));
    }

    /**
     * Status bar color.
     *
     * @param statusBarColor status bar color resource (R.color.xxx)
     * @param alpha          the alpha transparency
     * @return the immersion bar
     */
    public ImmersionBar statusBarColor(@ColorRes int statusBarColor,
                                       @FloatRange(from = 0f, to = 1f) float alpha) {
        return this.statusBarColorInt(ContextCompat.getColor(mActivity, statusBarColor), alpha);
    }

    /**
     * Status bar color.
     *
     * @param statusBarColor          status bar color resource (R.color.xxx)
     * @param statusBarColorTransform the status bar color transform transformed color of status bar
     * @param alpha                   the alpha transparency
     * @return the immersion bar
     */
    public ImmersionBar statusBarColor(@ColorRes int statusBarColor,
                                       @ColorRes int statusBarColorTransform,
                                       @FloatRange(from = 0f, to = 1f) float alpha) {
        return this.statusBarColorInt(ContextCompat.getColor(mActivity, statusBarColor),
                ContextCompat.getColor(mActivity, statusBarColorTransform),
                alpha);
    }

    /**
     * Status bar color.
     * Status bar color int immersion bar.
     *
     * @param statusBarColor the status bar color
     * @return the immersion bar
     */
    public ImmersionBar statusBarColor(String statusBarColor) {
        return this.statusBarColorInt(Color.parseColor(statusBarColor));
    }

    /**
     * Status bar color.
     *
     * @param statusBarColor status bar color
     * @param alpha          the alpha transparency
     * @return the immersion bar
     */
    public ImmersionBar statusBarColor(String statusBarColor,
                                       @FloatRange(from = 0f, to = 1f) float alpha) {
        return this.statusBarColorInt(Color.parseColor(statusBarColor), alpha);
    }

    /**
     * Status bar color.
     *
     * @param statusBarColor          status bar color
     * @param statusBarColorTransform the status bar color transform transformed color of status bar
     * @param alpha                   the alpha transparency
     * @return the immersion bar
     */
    public ImmersionBar statusBarColor(String statusBarColor,
                                       String statusBarColorTransform,
                                       @FloatRange(from = 0f, to = 1f) float alpha) {
        return this.statusBarColorInt(Color.parseColor(statusBarColor),
                Color.parseColor(statusBarColorTransform),
                alpha);
    }

    /**
     * Status bar color.
     *
     * @param statusBarColor status bar color resource (R.color.xxx)
     * @return the immersion bar
     */
    public ImmersionBar statusBarColorInt(@ColorInt int statusBarColor) {
        mBarParams.statusBarColor = statusBarColor;
        return this;
    }

    /**
     * Status bar color.
     *
     * @param statusBarColor status bar color resource (R.color.xxx)
     * @param alpha          the alpha transparency
     * @return the immersion bar
     */
    public ImmersionBar statusBarColorInt(@ColorInt int statusBarColor,
                                          @FloatRange(from = 0f, to = 1f) float alpha) {
        mBarParams.statusBarColor = statusBarColor;
        mBarParams.statusBarAlpha = alpha;
        return this;
    }

    /**
     * Status bar color.
     *
     * @param statusBarColor          status bar color resource (R.color.xxx)
     * @param statusBarColorTransform the status bar color transform transformed color of status bar
     * @param alpha                   the alpha transparency
     * @return the immersion bar
     */
    public ImmersionBar statusBarColorInt(@ColorInt int statusBarColor,
                                          @ColorInt int statusBarColorTransform,
                                          @FloatRange(from = 0f, to = 1f) float alpha) {
        mBarParams.statusBarColor = statusBarColor;
        mBarParams.statusBarColorTransform = statusBarColorTransform;
        mBarParams.statusBarAlpha = alpha;
        return this;
    }

    /**
     * Navigation bar color.
     *
     * @param navigationBarColor the navigation bar color
     * @return the immersion bar
     */
    public ImmersionBar navigationBarColor(@ColorRes int navigationBarColor) {
        return this.navigationBarColorInt(ContextCompat.getColor(mActivity, navigationBarColor));
    }

    /**
     * Navigation bar color.
     *
     * @param navigationBarColor the navigation bar color
     * @param navigationAlpha    the navigation alpha transparency
     * @return the immersion bar
     */
    public ImmersionBar navigationBarColor(@ColorRes int navigationBarColor,
                                           @FloatRange(from = 0f, to = 1f) float navigationAlpha) {
        return this.navigationBarColorInt(ContextCompat.getColor(mActivity, navigationBarColor), navigationAlpha);
    }

    /**
     * Navigation bar color.
     *
     * @param navigationBarColor          the navigation bar color
     * @param navigationBarColorTransform the navigation bar color transform transformed color of navigation bar
     * @param navigationAlpha             the navigation alpha transparency
     * @return the immersion bar
     */
    public ImmersionBar navigationBarColor(@ColorRes int navigationBarColor,
                                           @ColorRes int navigationBarColorTransform,
                                           @FloatRange(from = 0f, to = 1f) float navigationAlpha) {
        return this.navigationBarColorInt(ContextCompat.getColor(mActivity, navigationBarColor),
                ContextCompat.getColor(mActivity, navigationBarColorTransform), navigationAlpha);
    }

    /**
     * Navigation bar color.
     *
     * @param navigationBarColor the navigation bar color
     * @return the immersion bar
     */
    public ImmersionBar navigationBarColor(String navigationBarColor) {
        return this.navigationBarColorInt(Color.parseColor(navigationBarColor));
    }

    /**
     * Navigation bar color.
     *
     * @param navigationBarColor the navigation bar color
     * @param navigationAlpha    the navigation alpha transparency
     * @return the immersion bar
     */
    public ImmersionBar navigationBarColor(String navigationBarColor,
                                           @FloatRange(from = 0f, to = 1f) float navigationAlpha) {
        return this.navigationBarColorInt(Color.parseColor(navigationBarColor), navigationAlpha);
    }

    /**
     * Navigation bar color.
     *
     * @param navigationBarColor          the navigation bar color
     * @param navigationBarColorTransform the navigation bar color transform transformed color of navigation bar
     * @param navigationAlpha             the navigation alpha transparency
     * @return the immersion bar
     */
    public ImmersionBar navigationBarColor(String navigationBarColor,
                                           String navigationBarColorTransform,
                                           @FloatRange(from = 0f, to = 1f) float navigationAlpha) {
        return this.navigationBarColorInt(Color.parseColor(navigationBarColor),
                Color.parseColor(navigationBarColorTransform), navigationAlpha);
    }

    /**
     * Navigation bar color.
     *
     * @param navigationBarColor the navigation bar color
     * @return the immersion bar
     */
    public ImmersionBar navigationBarColorInt(@ColorInt int navigationBarColor) {
        mBarParams.navigationBarColorSpecified = true;
        mBarParams.navigationBarColor = navigationBarColor;
        return this;
    }

    /**
     * Navigation bar color.
     *
     * @param navigationBarColor the navigation bar color
     * @param navigationAlpha    the navigation alpha transparency
     * @return the immersion bar
     */
    public ImmersionBar navigationBarColorInt(@ColorInt int navigationBarColor,
                                              @FloatRange(from = 0f, to = 1f) float navigationAlpha) {
        mBarParams.navigationBarColorSpecified = true;
        mBarParams.navigationBarColor = navigationBarColor;
        mBarParams.navigationBarAlpha = navigationAlpha;
        return this;
    }

    /**
     * Navigation bar color.
     *
     * @param navigationBarColor          the navigation bar color
     * @param navigationBarColorTransform the navigation bar color transform transformed color of navigation bar
     * @param navigationAlpha             the navigation alpha transparency
     * @return the immersion bar
     */
    public ImmersionBar navigationBarColorInt(@ColorInt int navigationBarColor,
                                              @ColorInt int navigationBarColorTransform,
                                              @FloatRange(from = 0f, to = 1f) float navigationAlpha) {
        mBarParams.navigationBarColorSpecified = true;
        mBarParams.navigationBarColor = navigationBarColor;
        mBarParams.navigationBarColorTransform = navigationBarColorTransform;
        mBarParams.navigationBarAlpha = navigationAlpha;
        return this;
    }

    /**
     * Status bar and navigation bar color.
     *
     * @param barColor the bar color
     * @return the immersion bar
     */
    public ImmersionBar barColor(@ColorRes int barColor) {
        return this.barColorInt(ContextCompat.getColor(mActivity, barColor));
    }

    /**
     * Status bar and navigation bar color.
     *
     * @param barColor the bar color
     * @param barAlpha the bar alpha
     * @return the immersion bar
     */
    public ImmersionBar barColor(@ColorRes int barColor, @FloatRange(from = 0f, to = 1f) float barAlpha) {
        return this.barColorInt(ContextCompat.getColor(mActivity, barColor), barColor);
    }

    /**
     * Status bar and navigation bar color.
     *
     * @param barColor          the bar color
     * @param barColorTransform the bar color transform
     * @param barAlpha          the bar alpha
     * @return the immersion bar
     */
    public ImmersionBar barColor(@ColorRes int barColor,
                                 @ColorRes int barColorTransform,
                                 @FloatRange(from = 0f, to = 1f) float barAlpha) {
        return this.barColorInt(ContextCompat.getColor(mActivity, barColor),
                ContextCompat.getColor(mActivity, barColorTransform), barAlpha);
    }

    /**
     * Status bar and navigation bar color.
     *
     * @param barColor the bar color
     * @return the immersion bar
     */
    public ImmersionBar barColor(String barColor) {
        return this.barColorInt(Color.parseColor(barColor));
    }

    /**
     * Status bar and navigation bar color.
     *
     * @param barColor the bar color
     * @param barAlpha the bar alpha
     * @return the immersion bar
     */
    public ImmersionBar barColor(String barColor, @FloatRange(from = 0f, to = 1f) float barAlpha) {
        return this.barColorInt(Color.parseColor(barColor), barAlpha);
    }

    /**
     * Status bar and navigation bar color.
     *
     * @param barColor          the bar color
     * @param barColorTransform the bar color transform
     * @param barAlpha          the bar alpha
     * @return the immersion bar
     */
    public ImmersionBar barColor(String barColor,
                                 String barColorTransform,
                                 @FloatRange(from = 0f, to = 1f) float barAlpha) {
        return this.barColorInt(Color.parseColor(barColor), Color.parseColor(barColorTransform), barAlpha);
    }

    /**
     * Status bar and navigation bar color.
     *
     * @param barColor the bar color
     * @return the immersion bar
     */
    public ImmersionBar barColorInt(@ColorInt int barColor) {
        mBarParams.statusBarColor = barColor;
        mBarParams.navigationBarColorSpecified = true;
        mBarParams.navigationBarColor = barColor;
        return this;
    }

    /**
     * Status bar and navigation bar color.
     *
     * @param barColor the bar color
     * @param barAlpha the bar alpha
     * @return the immersion bar
     */
    public ImmersionBar barColorInt(@ColorInt int barColor, @FloatRange(from = 0f, to = 1f) float barAlpha) {
        mBarParams.statusBarColor = barColor;
        mBarParams.navigationBarColorSpecified = true;
        mBarParams.navigationBarColor = barColor;
        mBarParams.statusBarAlpha = barAlpha;
        mBarParams.navigationBarAlpha = barAlpha;
        return this;
    }

    /**
     * Status bar and navigation bar color.
     *
     * @param barColor          the bar color
     * @param barColorTransform the bar color transform
     * @param barAlpha          the bar alpha
     * @return the immersion bar
     */
    public ImmersionBar barColorInt(@ColorInt int barColor,
                                    @ColorInt int barColorTransform,
                                    @FloatRange(from = 0f, to = 1f) float barAlpha) {
        mBarParams.statusBarColor = barColor;
        mBarParams.navigationBarColorSpecified = true;
        mBarParams.navigationBarColor = barColor;

        mBarParams.statusBarColorTransform = barColorTransform;
        mBarParams.navigationBarColorTransform = barColorTransform;

        mBarParams.statusBarAlpha = barAlpha;
        mBarParams.navigationBarAlpha = barAlpha;
        return this;
    }


    /**
     * Color that status bar transforms into based on alpha.
     *
     * @param statusBarColorTransform the status bar color transform
     * @return the immersion bar
     */
    public ImmersionBar statusBarColorTransform(@ColorRes int statusBarColorTransform) {
        return this.statusBarColorTransformInt(ContextCompat.getColor(mActivity, statusBarColorTransform));
    }

    /**
     * Color that status bar transforms into based on alpha.
     *
     * @param statusBarColorTransform the status bar color transform
     * @return the immersion bar
     */
    public ImmersionBar statusBarColorTransform(String statusBarColorTransform) {
        return this.statusBarColorTransformInt(Color.parseColor(statusBarColorTransform));
    }

    /**
     * Color that status bar transforms into based on alpha.
     *
     * @param statusBarColorTransform the status bar color transform
     * @return the immersion bar
     */
    public ImmersionBar statusBarColorTransformInt(@ColorInt int statusBarColorTransform) {
        mBarParams.statusBarColorTransform = statusBarColorTransform;
        return this;
    }

    /**
     * Color that navigation bar transforms into based on alpha.
     *
     * @param navigationBarColorTransform the m navigation bar color transform
     * @return the immersion bar
     */
    public ImmersionBar navigationBarColorTransform(@ColorRes int navigationBarColorTransform) {
        return this.navigationBarColorTransformInt(ContextCompat.getColor(mActivity, navigationBarColorTransform));
    }

    /**
     * Color that navigation bar transforms into based on alpha.
     *
     * @param navigationBarColorTransform the m navigation bar color transform
     * @return the immersion bar
     */
    public ImmersionBar navigationBarColorTransform(String navigationBarColorTransform) {
        return this.navigationBarColorTransformInt(Color.parseColor(navigationBarColorTransform));
    }

    /**
     * Color that navigation bar transforms into based on alpha.
     *
     * @param navigationBarColorTransform the m navigation bar color transform
     * @return the immersion bar
     */
    public ImmersionBar navigationBarColorTransformInt(@ColorInt int navigationBarColorTransform) {
        mBarParams.navigationBarColorTransform = navigationBarColorTransform;
        return this;
    }

    /**
     * Color that status bar and navigation bar transform into based on alpha.
     *
     * @param barColorTransform the bar color transform
     * @return the immersion bar
     */
    public ImmersionBar barColorTransform(@ColorRes int barColorTransform) {
        return this.barColorTransformInt(ContextCompat.getColor(mActivity, barColorTransform));
    }

    /**
     * Color that status bar and navigation bar transform into based on alpha.
     *
     * @param barColorTransform the bar color transform
     * @return the immersion bar
     */
    public ImmersionBar barColorTransform(String barColorTransform) {
        return this.barColorTransformInt(Color.parseColor(barColorTransform));
    }

    /**
     * Color that status bar and navigation bar transform into based on alpha.
     *
     * @param barColorTransform the bar color transform
     * @return the immersion bar
     */
    public ImmersionBar barColorTransformInt(@ColorInt int barColorTransform) {
        mBarParams.statusBarColorTransform = barColorTransform;
        mBarParams.navigationBarColorTransform = barColorTransform;
        return this;
    }

    /**
     * Add view supporting color transformation.
     *
     * @param view the view
     * @return the immersion bar
     */
    public ImmersionBar addViewSupportTransformColor(View view) {
        return this.addViewSupportTransformColorInt(view, mBarParams.statusBarColorTransform);
    }

    /**
     * Add view supporting color transformation.
     *
     * @param view                    the view
     * @param viewColorAfterTransform the view color after transform
     * @return the immersion bar
     */
    public ImmersionBar addViewSupportTransformColor(View view, @ColorRes int viewColorAfterTransform) {
        return this.addViewSupportTransformColorInt(view, ContextCompat.getColor(mActivity, viewColorAfterTransform));
    }

    /**
     * Add view supporting color transformation.
     *
     * @param view                     the view
     * @param viewColorBeforeTransform the view color before transform
     * @param viewColorAfterTransform  the view color after transform
     * @return the immersion bar
     */
    public ImmersionBar addViewSupportTransformColor(View view, @ColorRes int viewColorBeforeTransform,
                                                     @ColorRes int viewColorAfterTransform) {
        return this.addViewSupportTransformColorInt(view,
                ContextCompat.getColor(mActivity, viewColorBeforeTransform),
                ContextCompat.getColor(mActivity, viewColorAfterTransform));
    }

    /**
     * Add view supporting color transformation.
     *
     * @param view                    the view
     * @param viewColorAfterTransform the view color after transform
     * @return the immersion bar
     */
    public ImmersionBar addViewSupportTransformColor(View view, String viewColorAfterTransform) {
        return this.addViewSupportTransformColorInt(view, Color.parseColor(viewColorAfterTransform));
    }

    /**
     * Add view supporting color transformation.
     *
     * @param view                     the view
     * @param viewColorBeforeTransform the view color before transform
     * @param viewColorAfterTransform  the view color after transform
     * @return the immersion bar
     */
    public ImmersionBar addViewSupportTransformColor(View view, String viewColorBeforeTransform,
                                                     String viewColorAfterTransform) {
        return this.addViewSupportTransformColorInt(view,
                Color.parseColor(viewColorBeforeTransform),
                Color.parseColor(viewColorAfterTransform));
    }

    /**
     * Add view supporting color transformation.
     *
     * @param view                    the view
     * @param viewColorAfterTransform the view color after transform
     * @return the immersion bar
     */
    public ImmersionBar addViewSupportTransformColorInt(View view, @ColorInt int viewColorAfterTransform) {
        if (view == null) {
            throw new IllegalArgumentException("View parameter cannot be null");
        }
        Map<Integer, Integer> map = new HashMap<>();
        map.put(mBarParams.statusBarColor, viewColorAfterTransform);
        mBarParams.viewMap.put(view, map);
        return this;
    }

    /**
     * Add view supporting color transformation.
     *
     * @param view                     the view
     * @param viewColorBeforeTransform the view color before transform
     * @param viewColorAfterTransform  the view color after transform
     * @return the immersion bar
     */
    public ImmersionBar addViewSupportTransformColorInt(View view, @ColorInt int viewColorBeforeTransform,
                                                        @ColorInt int viewColorAfterTransform) {
        if (view == null) {
            throw new IllegalArgumentException("View parameter cannot be null");
        }
        Map<Integer, Integer> map = new HashMap<>();
        map.put(viewColorBeforeTransform, viewColorAfterTransform);
        mBarParams.viewMap.put(view, map);
        return this;
    }

    /**
     * View alpha.
     * View alpha immersion bar.
     *
     * @param viewAlpha the view alpha
     * @return the immersion bar
     */
    public ImmersionBar viewAlpha(@FloatRange(from = 0f, to = 1f) float viewAlpha) {
        mBarParams.viewAlpha = viewAlpha;
        return this;
    }

    /**
     * Remove support view immersion bar.
     *
     * @param view the view
     * @return the immersion bar
     */
    public ImmersionBar removeSupportView(View view) {
        if (view == null) {
            throw new IllegalArgumentException("View parameter cannot be null");
        }
        Map<Integer, Integer> map = mBarParams.viewMap.get(view);
        if (map != null && !map.isEmpty()) {
            mBarParams.viewMap.remove(view);
        }
        return this;
    }

    /**
     * Remove support all view immersion bar.
     *
     * @return the immersion bar
     */
    public ImmersionBar removeSupportAllView() {
        if (!mBarParams.viewMap.isEmpty()) {
            mBarParams.viewMap.clear();
        }
        return this;
    }

    /**
     * Whether Activity is displayed full screen when navigation bar exists.
     *
     * @param isFullScreen the is full screen
     * @return the immersion bar
     */
    public ImmersionBar fullScreen(boolean isFullScreen) {
        mBarParams.fullScreen = isFullScreen;
        return this;
    }

    /**
     * Status bar alpha.
     *
     * @param statusAlpha the status alpha
     * @return the immersion bar
     */
    public ImmersionBar statusBarAlpha(@FloatRange(from = 0f, to = 1f) float statusAlpha) {
        mBarParams.statusBarAlpha = statusAlpha;
        mBarParams.statusBarTempAlpha = statusAlpha;
        return this;
    }

    /**
     * Navigation bar alpha.
     *
     * @param navigationAlpha the navigation alpha
     * @return the immersion bar
     */
    public ImmersionBar navigationBarAlpha(@FloatRange(from = 0f, to = 1f) float navigationAlpha) {
        mBarParams.navigationBarAlpha = navigationAlpha;
        mBarParams.navigationBarTempAlpha = navigationAlpha;
        return this;
    }

    /**
     * Status bar and navigation bar alpha.
     *
     * @param barAlpha the bar alpha
     * @return the immersion bar
     */
    public ImmersionBar barAlpha(@FloatRange(from = 0f, to = 1f) float barAlpha) {
        mBarParams.statusBarAlpha = barAlpha;
        mBarParams.statusBarTempAlpha = barAlpha;
        mBarParams.navigationBarAlpha = barAlpha;
        mBarParams.navigationBarTempAlpha = barAlpha;
        return this;
    }

    /**
     * Whether to enable automatic dark/light mode adjustment based on StatusBar and NavigationBar colors.
     *
     * @param isEnable true to enable, default is false
     * @return the immersion bar
     */
    public ImmersionBar autoDarkModeEnable(boolean isEnable) {
        return this.autoDarkModeEnable(isEnable, 0.2f);
    }

    /**
     * Whether to enable automatic dark/light mode adjustment based on StatusBar and NavigationBar colors.
     * Auto dark mode enable immersion bar.
     *
     * @param isEnable          the is enable
     * @param autoDarkModeAlpha the auto dark mode alpha
     * @return the immersion bar
     */
    public ImmersionBar autoDarkModeEnable(boolean isEnable, @FloatRange(from = 0f, to = 1f) float autoDarkModeAlpha) {
        mBarParams.autoStatusBarDarkModeEnable = isEnable;
        mBarParams.autoStatusBarDarkModeAlpha = autoDarkModeAlpha;
        mBarParams.autoNavigationBarDarkModeEnable = isEnable;
        mBarParams.autoNavigationBarDarkModeAlpha = autoDarkModeAlpha;
        return this;
    }

    /**
     * Whether to enable automatic dark/light mode adjustment based on StatusBar color.
     * Auto status bar dark mode enable immersion bar.
     *
     * @param isEnable the is enable
     * @return the immersion bar
     */
    public ImmersionBar autoStatusBarDarkModeEnable(boolean isEnable) {
        return this.autoStatusBarDarkModeEnable(isEnable, 0.2f);
    }

    /**
     * Whether to enable automatic dark/light mode adjustment based on StatusBar color.
     * Auto status bar dark mode enable immersion bar.
     *
     * @param isEnable          the is enable
     * @param autoDarkModeAlpha the auto dark mode alpha
     * @return the immersion bar
     */
    public ImmersionBar autoStatusBarDarkModeEnable(boolean isEnable, @FloatRange(from = 0f, to = 1f) float autoDarkModeAlpha) {
        mBarParams.autoStatusBarDarkModeEnable = isEnable;
        mBarParams.autoStatusBarDarkModeAlpha = autoDarkModeAlpha;
        return this;
    }

    /**
     * Whether to enable automatic dark/light mode adjustment based on StatusBar color.
     * Auto navigation bar dark mode enable immersion bar.
     *
     * @param isEnable the is enable
     * @return the immersion bar
     */
    public ImmersionBar autoNavigationBarDarkModeEnable(boolean isEnable) {
        return this.autoNavigationBarDarkModeEnable(isEnable, 0.2f);
    }

    /**
     * Whether to enable automatic dark/light mode adjustment based on NavigationBar color.
     * Auto navigation bar dark mode enable immersion bar.
     *
     * @param isEnable          the is enable
     * @param autoDarkModeAlpha the auto dark mode alpha
     * @return the immersion bar
     */
    public ImmersionBar autoNavigationBarDarkModeEnable(boolean isEnable, @FloatRange(from = 0f, to = 1f) float autoDarkModeAlpha) {
        mBarParams.autoNavigationBarDarkModeEnable = isEnable;
        mBarParams.autoNavigationBarDarkModeAlpha = autoDarkModeAlpha;
        return this;
    }

    /**
     * Status bar font dark or light.
     *
     * @param isDarkFont true for dark
     * @return the immersion bar
     */
    public ImmersionBar statusBarDarkFont(boolean isDarkFont) {
        return statusBarDarkFont(isDarkFont, 0.2f);
    }

    /**
     * Status bar font dark or light, setting status bar alpha based on whether device supports status bar font color change.
     * Status bar dark font immersion bar.
     *
     * @param isDarkFont  the is dark font
     * @param statusAlpha the status alpha; if status bar font color change is not supported, statusAlpha can specify status bar alpha, useful e.g. for white status bar
     * @return the immersion bar
     */
    public ImmersionBar statusBarDarkFont(boolean isDarkFont, @FloatRange(from = 0f, to = 1f) float statusAlpha) {
        mBarParams.statusBarDarkFont = isDarkFont;
        if (isDarkFont && !isSupportStatusBarDarkFont()) {
            mBarParams.statusBarAlpha = statusAlpha;
        } else {
            mBarParams.flymeOSStatusBarFontColor = mBarParams.flymeOSStatusBarFontTempColor;
            mBarParams.statusBarAlpha = mBarParams.statusBarTempAlpha;
        }
        return this;
    }

    /**
     * Navigation bar icons dark or light, supported only on Android O and above.
     * Navigation bar dark icon immersion bar.
     *
     * @param isDarkIcon the is dark icon
     * @return the immersion bar
     */
    public ImmersionBar navigationBarDarkIcon(boolean isDarkIcon) {
        return navigationBarDarkIcon(isDarkIcon, 0.2f);
    }

    /**
     * Navigation bar icons dark or light, supported only on Android O and above, setting navigation bar alpha based on whether device supports navigation bar icon color change.
     * Navigation bar dark icon immersion bar.
     *
     * @param isDarkIcon      the is dark icon
     * @param navigationAlpha the navigation alpha; if navigation bar icon color change is not supported, navigationAlpha can specify navigation bar alpha, useful e.g. for white navigation bar
     * @return the immersion bar
     */
    public ImmersionBar navigationBarDarkIcon(boolean isDarkIcon, @FloatRange(from = 0f, to = 1f) float navigationAlpha) {
        mBarParams.navigationBarDarkIconSpecified = true;
        setNavigationBarDarkIconInternal(isDarkIcon, navigationAlpha);
        return this;
    }

    private void setNavigationBarDarkIconInternal(boolean isDarkIcon,
                                                  @FloatRange(from = 0f, to = 1f) float navigationAlpha) {
        mBarParams.navigationBarDarkIcon = isDarkIcon;
        if (isDarkIcon && !isSupportNavigationIconDark()) {
            mBarParams.navigationBarAlpha = navigationAlpha;
        } else {
            mBarParams.navigationBarAlpha = mBarParams.navigationBarTempAlpha;
        }
    }

    /**
     * Modify Flyme OS status bar font color, takes precedence over statusBarDarkFont(boolean isDarkFont).
     * Flyme os status bar font color immersion bar.
     *
     * @param flymeOSStatusBarFontColor the flyme os status bar font color
     * @return the immersion bar
     */
    public ImmersionBar flymeOSStatusBarFontColor(@ColorRes int flymeOSStatusBarFontColor) {
        mBarParams.flymeOSStatusBarFontColor = ContextCompat.getColor(mActivity, flymeOSStatusBarFontColor);
        mBarParams.flymeOSStatusBarFontTempColor = mBarParams.flymeOSStatusBarFontColor;
        return this;
    }

    /**
     * Modify Flyme OS status bar font color, takes precedence over statusBarDarkFont(boolean isDarkFont).
     * Flyme os status bar font color immersion bar.
     *
     * @param flymeOSStatusBarFontColor the flyme os status bar font color
     * @return the immersion bar
     */
    public ImmersionBar flymeOSStatusBarFontColor(String flymeOSStatusBarFontColor) {
        mBarParams.flymeOSStatusBarFontColor = Color.parseColor(flymeOSStatusBarFontColor);
        mBarParams.flymeOSStatusBarFontTempColor = mBarParams.flymeOSStatusBarFontColor;
        return this;
    }

    /**
     * Modify Flyme OS status bar font color, takes precedence over statusBarDarkFont(boolean isDarkFont).
     * Flyme os status bar font color immersion bar.
     *
     * @param flymeOSStatusBarFontColor the flyme os status bar font color
     * @return the immersion bar
     */
    public ImmersionBar flymeOSStatusBarFontColorInt(@ColorInt int flymeOSStatusBarFontColor) {
        mBarParams.flymeOSStatusBarFontColor = flymeOSStatusBarFontColor;
        mBarParams.flymeOSStatusBarFontTempColor = mBarParams.flymeOSStatusBarFontColor;
        return this;
    }

    /**
     * Hide navigation bar or status bar.
     *
     * @param barHide the bar hide
     * @return the immersion bar
     */
    public ImmersionBar hideBar(BarHide barHide) {
        mBarParams.barHide = barHide;
        if (Build.VERSION.SDK_INT == Version.KITKAT || OSUtils.isEMUI3_x()) {
            mBarParams.hideNavigationBar = (mBarParams.barHide == BarHide.FLAG_HIDE_NAVIGATION_BAR) ||
                    (mBarParams.barHide == BarHide.FLAG_HIDE_BAR);
        }
        return this;
    }


    /**
     * Resolve layout and status bar overlap. This method calls system view's setFitsSystemWindows method.
     * Once window has focus, setting to false will not take effect. Not used by default; do not call or set to false for gradient status bar and top images.
     * Apply system fits immersion bar.
     *
     * @param applySystemFits the apply system fits
     * @return the immersion bar
     */
    public ImmersionBar applySystemFits(boolean applySystemFits) {
        mBarParams.fitsLayoutOverlapEnable = !applySystemFits;
        setFitsSystemWindows(mActivity, applySystemFits);
        return this;
    }

    /**
     * Resolve layout and status bar overlap.
     *
     * @param fits the fits
     * @return the immersion bar
     */
    public ImmersionBar fitsSystemWindows(boolean fits) {
        mBarParams.fits = fits;
        if (mBarParams.fits) {
            if (mFitsStatusBarType == FLAG_FITS_DEFAULT) {
                mFitsStatusBarType = FLAG_FITS_SYSTEM_WINDOWS;
            }
        } else {
            mFitsStatusBarType = FLAG_FITS_DEFAULT;
        }
        return this;
    }

    /**
     * Resolve layout and status bar overlap, supporting swipe back.
     * Fits system windows immersion bar.
     *
     * @param fits         the fits
     * @param contentColor the content color overall background color
     * @return the immersion bar
     */
    public ImmersionBar fitsSystemWindows(boolean fits, @ColorRes int contentColor) {
        return fitsSystemWindowsInt(fits, ContextCompat.getColor(mActivity, contentColor));
    }

    /**
     * Resolve layout and status bar overlap, supporting swipe back.
     * Fits system windows immersion bar.
     *
     * @param fits                  the fits
     * @param contentColor          the content color overall background color
     * @param contentColorTransform the content color transform transformed overall background color
     * @param contentAlpha          the content alpha overall background alpha
     * @return the immersion bar
     */
    public ImmersionBar fitsSystemWindows(boolean fits, @ColorRes int contentColor
            , @ColorRes int contentColorTransform, @FloatRange(from = 0f, to = 1f) float contentAlpha) {
        return fitsSystemWindowsInt(fits, ContextCompat.getColor(mActivity, contentColor),
                ContextCompat.getColor(mActivity, contentColorTransform), contentAlpha);
    }

    /**
     * Resolve layout and status bar overlap, supporting swipe back.
     * Fits system windows int immersion bar.
     *
     * @param fits         the fits
     * @param contentColor the content color overall background color
     * @return the immersion bar
     */
    public ImmersionBar fitsSystemWindowsInt(boolean fits, @ColorInt int contentColor) {
        return fitsSystemWindowsInt(fits, contentColor, Color.BLACK, 0);
    }

    /**
     * Resolve layout and status bar overlap, supporting swipe back.
     * Fits system windows int immersion bar.
     *
     * @param fits                  the fits
     * @param contentColor          the content color overall background color
     * @param contentColorTransform the content color transform transformed overall background color
     * @param contentAlpha          the content alpha overall background alpha
     * @return the immersion bar
     */
    public ImmersionBar fitsSystemWindowsInt(boolean fits, @ColorInt int contentColor
            , @ColorInt int contentColorTransform, @FloatRange(from = 0f, to = 1f) float contentAlpha) {
        mBarParams.fits = fits;
        mBarParams.contentColor = contentColor;
        mBarParams.contentColorTransform = contentColorTransform;
        mBarParams.contentAlpha = contentAlpha;
        if (mBarParams.fits) {
            if (mFitsStatusBarType == FLAG_FITS_DEFAULT) {
                mFitsStatusBarType = FLAG_FITS_SYSTEM_WINDOWS;
            }
        } else {
            mFitsStatusBarType = FLAG_FITS_DEFAULT;
        }
        mContentView.setBackgroundColor(ColorUtils.blendARGB(mBarParams.contentColor,
                mBarParams.contentColorTransform, mBarParams.contentAlpha));
        return this;
    }

    /**
     * Whether status bar and layout overlap can be fixed; default is true, applies only to ImmersionBar#statusBarView, ImmersionBar#titleBar, ImmersionBar#titleBarMarginTop.
     * Fits layout overlap enable immersion bar.
     *
     * @param fitsLayoutOverlapEnable the fits layout overlap enable
     * @return the immersion bar
     */
    public ImmersionBar fitsLayoutOverlapEnable(boolean fitsLayoutOverlapEnable) {
        mBarParams.fitsLayoutOverlapEnable = fitsLayoutOverlapEnable;
        return this;
    }

    /**
     * Dynamically configure status bar layout with status bar height.
     *
     * @param view the view
     * @return the immersion bar
     */
    public ImmersionBar statusBarView(View view) {
        if (view == null) {
            return this;
        }
        mBarParams.statusBarView = view;
        if (mFitsStatusBarType == FLAG_FITS_DEFAULT) {
            mFitsStatusBarType = FLAG_FITS_STATUS;
        }
        return this;
    }

    /**
     * Dynamically configure status bar layout with status bar height, used only in Activity.
     *
     * @param viewId the view id
     * @return the immersion bar
     */
    public ImmersionBar statusBarView(@IdRes int viewId) {
        return statusBarView(mActivity.findViewById(viewId));
    }

    /**
     * Dynamically configure status bar layout with status bar height.
     * Status bar view immersion bar.
     *
     * @param viewId   the view id
     * @param rootView the root view
     * @return the immersion bar
     */
    public ImmersionBar statusBarView(@IdRes int viewId, View rootView) {
        return statusBarView(rootView.findViewById(viewId));
    }

    /**
     * Another method to resolve status bar and layout top overlap.
     * Title bar immersion bar.
     *
     * @param view the view
     * @return the immersion bar
     */
    public ImmersionBar titleBar(View view) {
        if (view == null) {
            return this;
        }
        return titleBar(view, true);
    }

    /**
     * Another method to resolve status bar and layout top overlap.
     * Title bar immersion bar.
     *
     * @param view                          the view
     * @param statusBarColorTransformEnable the status bar flag; default is true; false indicates status bar does not support color change, true indicates support
     * @return the immersion bar
     */
    public ImmersionBar titleBar(View view, boolean statusBarColorTransformEnable) {
        if (view == null) {
            return this;
        }
        if (mFitsStatusBarType == FLAG_FITS_DEFAULT) {
            mFitsStatusBarType = FLAG_FITS_TITLE;
        }
        mBarParams.titleBarView = view;
        mBarParams.statusBarColorEnabled = statusBarColorTransformEnable;
        return this;
    }

    /**
     * Another method to resolve status bar and layout top overlap, supports Activity only.
     * Title bar immersion bar.
     *
     * @param viewId the view id
     * @return the immersion bar
     */
    public ImmersionBar titleBar(@IdRes int viewId) {
        return titleBar(viewId, true);
    }

    /**
     * Title bar immersion bar.
     *
     * @param viewId                        the view id
     * @param statusBarColorTransformEnable the status bar flag
     * @return the immersion bar
     */
    public ImmersionBar titleBar(@IdRes int viewId, boolean statusBarColorTransformEnable) {
        if (mSupportFragment != null && mSupportFragment.getView() != null) {
            return titleBar(mSupportFragment.getView().findViewById(viewId), statusBarColorTransformEnable);
        } else if (mFragment != null && mFragment.getView() != null) {
            return titleBar(mFragment.getView().findViewById(viewId), statusBarColorTransformEnable);
        } else {
            return titleBar(mActivity.findViewById(viewId), statusBarColorTransformEnable);
        }
    }

    /**
     * Title bar immersion bar.
     *
     * @param viewId   the view id
     * @param rootView the root view
     * @return the immersion bar
     */
    public ImmersionBar titleBar(@IdRes int viewId, View rootView) {
        return titleBar(rootView.findViewById(viewId), true);
    }

    /**
     * Another method to resolve status bar and layout top overlap, supports any view.
     * Title bar immersion bar.
     *
     * @param viewId                        the view id
     * @param rootView                      the root view
     * @param statusBarColorTransformEnable the status bar flag; default is true; false indicates status bar does not support color change, true indicates support
     * @return the immersion bar
     */
    public ImmersionBar titleBar(@IdRes int viewId, View rootView, boolean statusBarColorTransformEnable) {
        return titleBar(rootView.findViewById(viewId), statusBarColorTransformEnable);
    }

    /**
     * Set title bar top distance to status bar height.
     * Title bar margin top immersion bar.
     *
     * @param viewId the view id title bar resource id
     * @return the immersion bar
     */
    public ImmersionBar titleBarMarginTop(@IdRes int viewId) {
        if (mSupportFragment != null && mSupportFragment.getView() != null) {
            return titleBarMarginTop(mSupportFragment.getView().findViewById(viewId));
        } else if (mFragment != null && mFragment.getView() != null) {
            return titleBarMarginTop(mFragment.getView().findViewById(viewId));
        } else {
            return titleBarMarginTop(mActivity.findViewById(viewId));
        }
    }

    /**
     * Set title bar top distance to status bar height.
     * Title bar margin top immersion bar.
     *
     * @param viewId   the view id title bar resource id
     * @param rootView the root view layout view
     * @return the immersion bar
     */
    public ImmersionBar titleBarMarginTop(@IdRes int viewId, View rootView) {
        return titleBarMarginTop(rootView.findViewById(viewId));
    }

    /**
     * Set title bar top distance to status bar height.
     * Title bar margin top immersion bar.
     *
     * @param view the view title bar view to modify
     * @return the immersion bar
     */
    public ImmersionBar titleBarMarginTop(View view) {
        if (view == null) {
            return this;
        }
        if (mFitsStatusBarType == FLAG_FITS_DEFAULT) {
            mFitsStatusBarType = FLAG_FITS_TITLE_MARGIN_TOP;
        }
        mBarParams.titleBarView = view;
        return this;
    }

    /**
     * Support screen with ActionBar; calling this method renders layout starting below ActionBar.
     * Support action bar immersion bar.
     *
     * @param isSupportActionBar the is support action bar
     * @return the immersion bar
     */
    public ImmersionBar supportActionBar(boolean isSupportActionBar) {
        mBarParams.isSupportActionBar = isSupportActionBar;
        return this;
    }

    /**
     * Status bar color transform enable immersion bar.
     *
     * @param statusBarColorTransformEnable the status bar flag
     * @return the immersion bar
     */
    public ImmersionBar statusBarColorTransformEnable(boolean statusBarColorTransformEnable) {
        mBarParams.statusBarColorEnabled = statusBarColorTransformEnable;
        return this;
    }

    /**
     * Reset all parameters with one click.
     * Reset immersion bar.
     *
     * @return the immersion bar
     */
    public ImmersionBar reset() {
        mBarParams = createDefaultBarParams();
        mFitsStatusBarType = FLAG_FITS_DEFAULT;
        return this;
    }

    /**
     * Set tag for a page to identify its bar properties.
     * Add tag bar tag.
     *
     * @param tag the tag
     * @return the bar tag
     */
    public ImmersionBar addTag(String tag) {
        if (isEmpty(tag)) {
            throw new IllegalArgumentException("tag cannot be empty");
        }
        BarParams barParams = mBarParams.clone();
        mTagMap.put(tag, barParams);
        return this;
    }

    /**
     * Restore parameters to a previous call according to tag.
     * Recover immersion bar.
     *
     * @param tag the tag
     * @return the immersion bar
     */
    public ImmersionBar getTag(String tag) {
        if (isEmpty(tag)) {
            throw new IllegalArgumentException("tag cannot be empty");
        }
        BarParams barParams = mTagMap.get(tag);
        if (barParams != null) {
            mBarParams = barParams.clone();
        }
        return this;
    }

    /**
     * Resolve soft keyboard and bottom input view conflict, default is false.
     * Keyboard enable immersion bar.
     *
     * @param enable the enable
     * @return the immersion bar
     */
    public ImmersionBar keyboardEnable(boolean enable) {
        return keyboardEnable(enable, mBarParams.keyboardMode);
    }

    /**
     * Resolve soft keyboard and bottom input view conflict, default is false.
     *
     * @param enable       the enable
     * @param keyboardMode the keyboard mode
     * @return the immersion bar
     */
    public ImmersionBar keyboardEnable(boolean enable, int keyboardMode) {
        mBarParams.keyboardEnable = enable;
        mBarParams.keyboardMode = keyboardMode;
        mKeyboardTempEnable = enable;
        return this;
    }

    /**
     * Modify keyboard mode.
     * Keyboard mode immersion bar.
     *
     * @param keyboardMode the keyboard mode
     * @return the immersion bar
     */
    public ImmersionBar keyboardMode(int keyboardMode) {
        mBarParams.keyboardMode = keyboardMode;
        return this;
    }

    /**
     * Callback listener for soft keyboard show/hide.
     * Sets on keyboard listener.
     *
     * @param onKeyboardListener the on keyboard listener
     * @return the on keyboard listener
     * @deprecated Use {@link #addOnKeyboardChangedListener(OnKeyboardChangedListener)} instead.
     */
    @Deprecated
    public ImmersionBar setOnKeyboardListener(@Nullable OnKeyboardListener onKeyboardListener) {
        if (mBarParams.onKeyboardListener == null) {
            mBarParams.onKeyboardListener = onKeyboardListener;
        }
        return this;
    }

    /**
     * Navigation bar visibility listener.
     * Sets on navigation bar listener.
     *
     * @param onNavigationBarListener the on navigation bar listener
     * @return the on navigation bar listener
     * @deprecated Use {@link #addOnNavigationBarChangedListener(OnNavigationBarChangedListener)} instead.
     * For backward compatibility, listeners set here are still dispatched uniformly via BarProperties snapshot,
     * invoked on initial initialization and when navigation bar visibility, height, width, or navigation type changes.
     */
    @Deprecated
    public ImmersionBar setOnNavigationBarListener(OnNavigationBarListener onNavigationBarListener) {
        if (onNavigationBarListener != null) {
            if (mBarParams.onNavigationBarListener == null) {
                mBarParams.onNavigationBarListener = onNavigationBarListener;
            }
        } else {
            if (mBarParams.onNavigationBarListener != null) {
                mBarParams.onNavigationBarListener = null;
            }
        }
        return this;
    }

    /**
     * Status bar visibility listener.
     * Sets on status bar listener.
     *
     * @param onStatusBarListener the on status bar listener
     * @return the immersion bar
     * @deprecated Use {@link #addOnStatusBarChangedListener(OnStatusBarChangedListener)} instead.
     * For backward compatibility, listeners set here are still dispatched uniformly via BarProperties snapshot,
     * invoked on initial initialization and when status bar visibility or height changes.
     */
    @Deprecated
    public ImmersionBar setOnStatusBarListener(@Nullable OnStatusBarListener onStatusBarListener) {
        if (onStatusBarListener != null) {
            if (mBarParams.onStatusBarListener == null) {
                mBarParams.onStatusBarListener = onStatusBarListener;
            }
        } else {
            if (mBarParams.onStatusBarListener != null) {
                mBarParams.onStatusBarListener = null;
            }
        }
        return this;
    }


    /**
     * Add bar property change listener. Triggered on initial initialization and when bar properties change.
     *
     * @param listener the bar properties changed listener
     * @return the immersion bar
     */
    public ImmersionBar addOnBarPropertiesChangedListener(@NonNull OnBarPropertiesChangedListener listener) {
        mOnBarPropertiesChangedListeners.add(listener);
        return this;
    }

    /**
     * Remove bar property change listener.
     *
     * @param listener the bar properties changed listener
     * @return the immersion bar
     */
    public ImmersionBar removeOnBarPropertiesChangedListener(@NonNull OnBarPropertiesChangedListener listener) {
        mOnBarPropertiesChangedListeners.remove(listener);
        return this;
    }

    /**
     * Add soft keyboard change listener. Triggered when soft keyboard visibility or height changes.
     *
     * @param listener the keyboard changed listener
     * @return the immersion bar
     */
    public ImmersionBar addOnKeyboardChangedListener(@NonNull OnKeyboardChangedListener listener) {
        mOnKeyboardChangedListeners.add(listener);
        return this;
    }

    /**
     * Remove soft keyboard change listener.
     *
     * @param listener the keyboard changed listener
     * @return the immersion bar
     */
    public ImmersionBar removeOnKeyboardChangedListener(@NonNull OnKeyboardChangedListener listener) {
        mOnKeyboardChangedListeners.remove(listener);
        return this;
    }

    /**
     * Add status bar change listener. Triggered on initial initialization and when status bar visibility or height changes.
     *
     * @param listener the status bar changed listener
     * @return the immersion bar
     */
    public ImmersionBar addOnStatusBarChangedListener(@NonNull OnStatusBarChangedListener listener) {
        mOnStatusBarChangedListeners.add(listener);
        return this;
    }

    /**
     * Remove status bar change listener.
     *
     * @param listener the status bar changed listener
     * @return the immersion bar
     */
    public ImmersionBar removeOnStatusBarChangedListener(@NonNull OnStatusBarChangedListener listener) {
        mOnStatusBarChangedListeners.remove(listener);
        return this;
    }

    /**
     * Add navigation bar change listener. Triggered on initial initialization and when navigation bar visibility, height, width, or navigation type changes.
     *
     * @param listener the navigation bar changed listener
     * @return the immersion bar
     */
    public ImmersionBar addOnNavigationBarChangedListener(@NonNull OnNavigationBarChangedListener listener) {
        mOnNavigationBarChangedListeners.add(listener);
        return this;
    }

    /**
     * Remove navigation bar change listener.
     *
     * @param listener the navigation bar changed listener
     * @return the immersion bar
     */
    public ImmersionBar removeOnNavigationBarChangedListener(@NonNull OnNavigationBarChangedListener listener) {
        mOnNavigationBarChangedListeners.remove(listener);
        return this;
    }

    @SuppressWarnings("deprecation")
    void dispatchOnKeyboardChanged(boolean isPopup, int keyboardHeight) {
        Keyboard keyboard = new Keyboard(isPopup, keyboardHeight);
        for (OnKeyboardChangedListener listener : mOnKeyboardChangedListeners) {
            listener.onKeyboardChanged(keyboard);
        }
        BarParams barParams = mBarParams;
        if (barParams == null) {
            return;
        }
        OnKeyboardListener onKeyboardListener = barParams.onKeyboardListener;
        if (onKeyboardListener != null) {
            onKeyboardListener.onKeyboardChange(isPopup, keyboardHeight);
        }
    }

    void dispatchOnBarPropertiesChanged(@NonNull BarProperties barProperties) {
        for (OnBarPropertiesChangedListener listener : mOnBarPropertiesChangedListeners) {
            listener.onBarPropertiesChanged(barProperties);
        }
        BarParams barParams = mBarParams;
        if (barParams == null) {
            return;
        }
        OnBarListener onBarListener = barParams.onBarListener;
        if (onBarListener != null) {
            onBarListener.onBarChange(barProperties);
        }
    }

    /**
     * Dispatch status bar change: first invokes OnStatusBarChangedListener set (including internal default fake status bar synchronization callback), then invokes deprecated OnStatusBarListener.
     * Whether to invoke is determined by ImmersionDelegate comparing successive snapshots (visibility or height change); here it purely performs dispatch.
     */
    void dispatchOnStatusBarChanged(@NonNull StatusBar statusBar) {
        for (OnStatusBarChangedListener listener : mOnStatusBarChangedListeners) {
            listener.onStatusBarChanged(statusBar);
        }
        BarParams barParams = mBarParams;
        if (barParams == null) {
            return;
        }
        OnStatusBarListener onStatusBarListener = barParams.onStatusBarListener;
        if (onStatusBarListener != null) {
            onStatusBarListener.onStatusBarChange(statusBar.isVisible());
        }
    }

    /**
     * Dispatch navigation bar change: first invokes OnNavigationBarChangedListener set (including internal default fake navigation bar synchronization callback), then invokes deprecated OnNavigationBarListener.
     * Whether to invoke is determined by ImmersionDelegate comparing successive snapshots (visibility, height, width, or navigation type change); here it purely performs dispatch.
     */
    void dispatchOnNavigationBarChanged(@NonNull NavigationBar navigationBar) {
        for (OnNavigationBarChangedListener listener : mOnNavigationBarChangedListeners) {
            listener.onNavigationBarChanged(navigationBar);
        }
        BarParams barParams = mBarParams;
        if (barParams == null) {
            return;
        }
        OnNavigationBarListener onNavigationBarListener = barParams.onNavigationBarListener;
        if (onNavigationBarListener != null) {
            onNavigationBarListener.onNavigationBarChange(navigationBar.isVisible(), navigationBar.getType());
        }
    }

    /**
     * Bar listener, triggered on first call and configuration/orientation changes, e.g. solving notch covering layout in landscape mode on orientation switch.
     * Sets on bar listener.
     *
     * @param onBarListener the on bar listener
     * @return the on bar listener
     * @deprecated Use {@link #addOnBarPropertiesChangedListener(OnBarPropertiesChangedListener)}
     * and {@link #removeOnBarPropertiesChangedListener(OnBarPropertiesChangedListener)} instead.
     */
    @Deprecated
    public ImmersionBar setOnBarListener(OnBarListener onBarListener) {
        if (onBarListener != null) {
            if (mBarParams.onBarListener == null) {
                mBarParams.onBarListener = onBarListener;
            }
        } else {
            if (mBarParams.onBarListener != null) {
                mBarParams.onBarListener = null;
            }
        }
        return this;
    }

    /**
     * Whether navigation bar color can be modified; default is true.
     * Priority: navigationBarEnable > navigationBarWithKitkatEnable > navigationBarWithEMUI3Enable
     * Navigation bar enable immersion bar.
     *
     * @param navigationBarEnable the enable
     * @return the immersion bar
     */
    public ImmersionBar navigationBarEnable(boolean navigationBarEnable) {
        mBarParams.navigationBarEnable = navigationBarEnable;
        return this;
    }

    /**
     * Whether navigation bar color on Android 4.4 devices can be modified; default is true.
     * Priority: navigationBarEnable > navigationBarWithKitkatEnable > navigationBarWithEMUI3Enable
     *
     * @param navigationBarWithKitkatEnable the navigation bar with kitkat enable
     * @return the immersion bar
     */
    public ImmersionBar navigationBarWithKitkatEnable(boolean navigationBarWithKitkatEnable) {
        mBarParams.navigationBarWithKitkatEnable = navigationBarWithKitkatEnable;
        return this;
    }

    /**
     * Whether navigation bar color on Huawei EMUI 3.1 can be modified; default is true.
     * Priority: navigationBarEnable > navigationBarWithKitkatEnable > navigationBarWithEMUI3Enable
     * Navigation bar with emui 3 enable immersion bar.
     *
     * @param navigationBarWithEMUI3Enable the navigation bar with emui 3 1 enable
     * @return the immersion bar
     */
    public ImmersionBar navigationBarWithEMUI3Enable(boolean navigationBarWithEMUI3Enable) {
        // Whether navigation bar on EMUI 3 series devices can be modified
        if (OSUtils.isEMUI3_x()) {
            mBarParams.navigationBarWithEMUI3Enable = navigationBarWithEMUI3Enable;
            mBarParams.navigationBarWithKitkatEnable = navigationBarWithEMUI3Enable;
        }
        return this;
    }

    /**
     * Whether immersion can be used; if already true and changed to false, previous immersion effect will not disappear, but subsequent immersion effects will not take effect.
     * Bar enable immersion bar.
     *
     * @param barEnable the bar enable
     * @return the immersion bar
     */
    public ImmersionBar barEnable(boolean barEnable) {
        mBarParams.barEnable = barEnable;
        return this;
    }

    private static RequestManagerRetriever getRetriever() {
        return RequestManagerRetriever.getInstance();
    }

    private static boolean isEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }
}
