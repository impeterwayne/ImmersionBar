package com.gyf.immersionbar.ktx

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.view.View
import android.view.Window
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import com.gyf.immersionbar.ImmersionBar
import com.gyf.immersionbar.NotchCallback

/**
 * @author geyifeng
 * @date 2019/3/27 5:45 PM
 */

/** Initializes ImmersionBar. */
@JvmOverloads
inline fun Activity.immersionBar(isOnly: Boolean = false, block: ImmersionBar.() -> Unit) =
    ImmersionBar.with(this, isOnly).apply { block(this) }.init()

@JvmOverloads
inline fun Fragment.immersionBar(isOnly: Boolean = false, block: ImmersionBar.() -> Unit) =
    ImmersionBar.with(this, isOnly).apply { block(this) }.init()

@JvmOverloads
inline fun android.app.Fragment.immersionBar(
    isOnly: Boolean = false,
    block: ImmersionBar.() -> Unit
) = ImmersionBar.with(this, isOnly).apply { block(this) }.init()

@JvmOverloads
inline fun DialogFragment.immersionBar(isOnly: Boolean = false, block: ImmersionBar.() -> Unit) =
    ImmersionBar.with(this, isOnly).apply { block(this) }.init()

@JvmOverloads
inline fun android.app.DialogFragment.immersionBar(
    isOnly: Boolean = false,
    block: ImmersionBar.() -> Unit
) = ImmersionBar.with(this, isOnly).apply { block(this) }.init()

@JvmOverloads
inline fun Dialog.immersionBar(isOnly: Boolean = false, block: ImmersionBar.() -> Unit) =
    ImmersionBar.with(this, isOnly).apply { block(this) }.init()

@Deprecated(
    message = "Dialog can resolve Activity automatically. Use dialog.immersionBar(isOnly, block) instead.",
    replaceWith = ReplaceWith("immersionBar(isOnly, block)")
)
@JvmOverloads
inline fun Dialog.immersionBar(
    activity: Activity,
    isOnly: Boolean = false,
    block: ImmersionBar.() -> Unit
) = ImmersionBar.with(activity, this, isOnly).apply { block(this) }.init()

@JvmOverloads
inline fun Activity.immersionBar(
    dialog: Dialog,
    isOnly: Boolean = false,
    block: ImmersionBar.() -> Unit
) = ImmersionBar.with(this, dialog, isOnly).apply { block(this) }.init()

@JvmOverloads
inline fun Fragment.immersionBar(
    dialog: Dialog,
    isOnly: Boolean = false,
    block: ImmersionBar.() -> Unit
) = activity?.run { ImmersionBar.with(this, dialog, isOnly).apply { block(this) }.init() } ?: Unit

@JvmOverloads
inline fun android.app.Fragment.immersionBar(
    dialog: Dialog,
    isOnly: Boolean = false,
    block: ImmersionBar.() -> Unit
) = activity?.run { ImmersionBar.with(this, dialog, isOnly).apply { block(this) }.init() } ?: Unit

@JvmOverloads
fun Activity.immersionBar(isOnly: Boolean = false) = immersionBar(isOnly) { }

@JvmOverloads
fun Fragment.immersionBar(isOnly: Boolean = false) = immersionBar(isOnly) { }

@JvmOverloads
fun android.app.Fragment.immersionBar(isOnly: Boolean = false) = immersionBar(isOnly) { }

@JvmOverloads
fun DialogFragment.immersionBar(isOnly: Boolean = false) = immersionBar(isOnly) { }

@JvmOverloads
fun android.app.DialogFragment.immersionBar(isOnly: Boolean = false) = immersionBar(isOnly) { }

@JvmOverloads
fun Dialog.immersionBar(isOnly: Boolean = false) = immersionBar(isOnly) {}

@Deprecated(
    message = "Dialog can resolve Activity automatically. Use dialog.immersionBar(isOnly) instead.",
    replaceWith = ReplaceWith("immersionBar(isOnly)")
)
@JvmOverloads
fun Dialog.immersionBar(activity: Activity, isOnly: Boolean = false) =
    immersionBar(activity, isOnly) {}

@JvmOverloads
fun Activity.immersionBar(dialog: Dialog, isOnly: Boolean = false) = immersionBar(dialog, isOnly) {}

@JvmOverloads
fun Fragment.immersionBar(dialog: Dialog, isOnly: Boolean = false) = immersionBar(dialog, isOnly) {}

@JvmOverloads
fun android.app.Fragment.immersionBar(dialog: Dialog, isOnly: Boolean = false) =
    immersionBar(dialog, isOnly) {}

/** Dialog destroy extensions. */
@Deprecated(
    message = "Dialog ImmersionBar is destroyed automatically when dialog is dismissed. Manual destroy is no longer required."
)
@JvmOverloads
fun Activity.destroyImmersionBar(dialog: Dialog, isOnly: Boolean = false) =
    ImmersionBar.destroy(this, dialog, isOnly)

@Deprecated(
    message = "Dialog ImmersionBar is destroyed automatically when dialog is dismissed. Manual destroy is no longer required."
)
@JvmOverloads
fun Fragment.destroyImmersionBar(dialog: Dialog, isOnly: Boolean = false) =
    activity?.run { ImmersionBar.destroy(this, dialog, isOnly) } ?: Unit

@Deprecated(
    message = "Dialog ImmersionBar is destroyed automatically when dialog is dismissed. Manual destroy is no longer required."
)
@JvmOverloads
fun android.app.Fragment.destroyImmersionBar(dialog: Dialog, isOnly: Boolean = false) =
    activity?.run { ImmersionBar.destroy(this, dialog, isOnly) } ?: Unit

/** BarProperties extensions. */
val Activity.barProperties get() = ImmersionBar.getBarProperties(this)
val Fragment.barProperties get() = ImmersionBar.getBarProperties(this)
val android.app.Fragment.barProperties get() = ImmersionBar.getBarProperties(this)
val Context.barProperties get() = ImmersionBar.getBarProperties(this)
val Window.barProperties get() = ImmersionBar.getBarProperties(this)
val View.barProperties get() = ImmersionBar.getBarProperties(this)
val Dialog.barProperties get() = ImmersionBar.getBarProperties(this)

/** Status bar info extensions. */
val Activity.statusBar get() = ImmersionBar.getStatusBar(this)
val Fragment.statusBar get() = ImmersionBar.getStatusBar(this)
val android.app.Fragment.statusBar get() = ImmersionBar.getStatusBar(this)
val Context.statusBar get() = ImmersionBar.getStatusBar(this)
val Window.statusBar get() = ImmersionBar.getStatusBar(this)
val View.statusBar get() = ImmersionBar.getStatusBar(this)
val Dialog.statusBar get() = ImmersionBar.getStatusBar(this)

/** Navigation bar info extensions. */
val Activity.navigationBar get() = ImmersionBar.getNavigationBar(this)
val Fragment.navigationBar get() = ImmersionBar.getNavigationBar(this)
val android.app.Fragment.navigationBar get() = ImmersionBar.getNavigationBar(this)
val Context.navigationBar get() = ImmersionBar.getNavigationBar(this)
val Window.navigationBar get() = ImmersionBar.getNavigationBar(this)
val View.navigationBar get() = ImmersionBar.getNavigationBar(this)
val Dialog.navigationBar get() = ImmersionBar.getNavigationBar(this)

/** Status bar height extensions. */
val Activity.statusBarHeight get() = ImmersionBar.getStatusBarHeight(this)
val Fragment.statusBarHeight get() = ImmersionBar.getStatusBarHeight(this)
val android.app.Fragment.statusBarHeight get() = ImmersionBar.getStatusBarHeight(this)
val Context.statusBarHeight get() = ImmersionBar.getStatusBarHeight(this)
val Window.statusBarHeight get() = ImmersionBar.getStatusBarHeight(this)
val View.statusBarHeight get() = ImmersionBar.getStatusBarHeight(this)
val Dialog.statusBarHeight get() = ImmersionBar.getStatusBarHeight(this)

/** Navigation bar size extensions. */
val Activity.navigationBarHeight get() = ImmersionBar.getNavigationBarHeight(this)
val Fragment.navigationBarHeight get() = ImmersionBar.getNavigationBarHeight(this)
val android.app.Fragment.navigationBarHeight get() = ImmersionBar.getNavigationBarHeight(this)
val Context.navigationBarHeight get() = ImmersionBar.getNavigationBarHeight(this)
val Window.navigationBarHeight get() = ImmersionBar.getNavigationBarHeight(this)
val View.navigationBarHeight get() = ImmersionBar.getNavigationBarHeight(this)
val Dialog.navigationBarHeight get() = ImmersionBar.getNavigationBarHeight(this)

val Activity.navigationBarWidth get() = ImmersionBar.getNavigationBarWidth(this)
val Fragment.navigationBarWidth get() = ImmersionBar.getNavigationBarWidth(this)
val android.app.Fragment.navigationBarWidth get() = ImmersionBar.getNavigationBarWidth(this)
val Context.navigationBarWidth get() = ImmersionBar.getNavigationBarWidth(this)
val Window.navigationBarWidth get() = ImmersionBar.getNavigationBarWidth(this)
val View.navigationBarWidth get() = ImmersionBar.getNavigationBarWidth(this)
val Dialog.navigationBarWidth get() = ImmersionBar.getNavigationBarWidth(this)

/** ActionBar height extensions. */
val Activity.actionBarHeight get() = ImmersionBar.getActionBarHeight(this)
val Fragment.actionBarHeight get() = ImmersionBar.getActionBarHeight(this)
val android.app.Fragment.actionBarHeight get() = ImmersionBar.getActionBarHeight(this)
val Context.actionBarHeight get() = ImmersionBar.getActionBarHeight(this)
val Window.actionBarHeight get() = ImmersionBar.getActionBarHeight(this)
val View.actionBarHeight get() = ImmersionBar.getActionBarHeight(this)
val Dialog.actionBarHeight get() = ImmersionBar.getActionBarHeight(this)

/** Checks whether navigation bar exists. */
val Activity.hasNavigationBar get() = ImmersionBar.hasNavigationBar(this)
val Fragment.hasNavigationBar get() = ImmersionBar.hasNavigationBar(this)
val android.app.Fragment.hasNavigationBar get() = ImmersionBar.hasNavigationBar(this)
val Context.hasNavigationBar get() = ImmersionBar.hasNavigationBar(this)
val Window.hasNavigationBar get() = ImmersionBar.hasNavigationBar(this)
val View.hasNavigationBar get() = ImmersionBar.hasNavigationBar(this)
val Dialog.hasNavigationBar get() = ImmersionBar.hasNavigationBar(this)

/** Checks whether notch screen exists. */
val Activity.hasNotchScreen get() = ImmersionBar.hasNotchScreen(this)
val Fragment.hasNotchScreen get() = ImmersionBar.hasNotchScreen(this)
val android.app.Fragment.hasNotchScreen get() = ImmersionBar.hasNotchScreen(this)
val Context.hasNotchScreen get() = ImmersionBar.hasNotchScreen(this)
val Window.hasNotchScreen get() = ImmersionBar.hasNotchScreen(this)
val View.hasNotchScreen get() = ImmersionBar.hasNotchScreen(this)
val Dialog.hasNotchScreen get() = ImmersionBar.hasNotchScreen(this)

/** Notch height extensions. */
val Activity.notchHeight get() = ImmersionBar.getNotchHeight(this)
val Fragment.notchHeight get() = ImmersionBar.getNotchHeight(this)
val android.app.Fragment.notchHeight get() = ImmersionBar.getNotchHeight(this)
val Context.notchHeight get() = ImmersionBar.getNotchHeight(this)
val Window.notchHeight get() = ImmersionBar.getNotchHeight(this)
val View.notchHeight get() = ImmersionBar.getNotchHeight(this)
val Dialog.notchHeight get() = ImmersionBar.getNotchHeight(this)

fun Activity.getNotchHeight(callback: NotchCallback) = ImmersionBar.getNotchHeight(this, callback)
fun Fragment.getNotchHeight(callback: NotchCallback) = ImmersionBar.getNotchHeight(this, callback)
fun android.app.Fragment.getNotchHeight(callback: NotchCallback) =
    ImmersionBar.getNotchHeight(this, callback)

fun Context.getNotchHeight(callback: NotchCallback) = ImmersionBar.getNotchHeight(this, callback)
fun Window.getNotchHeight(callback: NotchCallback) = ImmersionBar.getNotchHeight(this, callback)
fun View.getNotchHeight(callback: NotchCallback) = ImmersionBar.getNotchHeight(this, callback)
fun Dialog.getNotchHeight(callback: NotchCallback) = ImmersionBar.getNotchHeight(this, callback)

/** Checks whether status bar dark font is supported. */
val isSupportStatusBarDarkFont get() = ImmersionBar.isSupportStatusBarDarkFont()

/** Checks whether navigation bar dark icon is supported. */
val isSupportNavigationIconDark get() = ImmersionBar.isSupportNavigationIconDark()

/** Checks whether the view uses fitsSystemWindows. */
val View.checkFitsSystemWindows get() = ImmersionBar.checkFitsSystemWindows(this)

/** Checks whether navigation bar is at the bottom. */
val Activity.isNavigationAtBottom get() = ImmersionBar.isNavigationAtBottom(this)
val Fragment.isNavigationAtBottom get() = ImmersionBar.isNavigationAtBottom(this)
val android.app.Fragment.isNavigationAtBottom get() = ImmersionBar.isNavigationAtBottom(this)
val Context.isNavigationAtBottom get() = ImmersionBar.isNavigationAtBottom(this)
val Window.isNavigationAtBottom get() = ImmersionBar.isNavigationAtBottom(this)
val View.isNavigationAtBottom get() = ImmersionBar.isNavigationAtBottom(this)
val Dialog.isNavigationAtBottom get() = ImmersionBar.isNavigationAtBottom(this)

/** Checks whether gesture navigation is active. */
val Activity.isGesture get() = ImmersionBar.isGesture(this)
val Fragment.isGesture get() = ImmersionBar.isGesture(this)
val android.app.Fragment.isGesture get() = ImmersionBar.isGesture(this)
val Context.isGesture get() = ImmersionBar.isGesture(this)
val Window.isGesture get() = ImmersionBar.isGesture(this)
val View.isGesture get() = ImmersionBar.isGesture(this)
val Dialog.isGesture get() = ImmersionBar.isGesture(this)

/** Fits statusBarView extensions. */
fun Activity.fitsStatusBarView(view: View) = ImmersionBar.setStatusBarView(this, view)
fun Fragment.fitsStatusBarView(view: View) = ImmersionBar.setStatusBarView(this, view)
fun android.app.Fragment.fitsStatusBarView(view: View) = ImmersionBar.setStatusBarView(this, view)
fun Context.fitsStatusBarView(view: View) = ImmersionBar.setStatusBarView(this, view)
fun Window.fitsStatusBarView(view: View) = ImmersionBar.setStatusBarView(this, view)
fun View.fitsStatusBarView(view: View) = ImmersionBar.setStatusBarView(this, view)
fun Dialog.fitsStatusBarView(view: View) = ImmersionBar.setStatusBarView(this, view)

/** Fits titleBar extensions. */
fun Activity.fitsTitleBar(vararg view: View) = ImmersionBar.setTitleBar(this, *view)
fun Fragment.fitsTitleBar(vararg view: View) = ImmersionBar.setTitleBar(this, *view)
fun android.app.Fragment.fitsTitleBar(vararg view: View) = ImmersionBar.setTitleBar(this, *view)
fun Context.fitsTitleBar(vararg view: View) = ImmersionBar.setTitleBar(this, *view)
fun Window.fitsTitleBar(vararg view: View) = ImmersionBar.setTitleBar(this, *view)
fun View.fitsTitleBar(vararg view: View) = ImmersionBar.setTitleBar(this, *view)
fun Dialog.fitsTitleBar(vararg view: View) = ImmersionBar.setTitleBar(this, *view)

fun Activity.fitsTitleBarMarginTop(vararg view: View) =
    ImmersionBar.setTitleBarMarginTop(this, *view)

fun Fragment.fitsTitleBarMarginTop(vararg view: View) =
    ImmersionBar.setTitleBarMarginTop(this, *view)

fun android.app.Fragment.fitsTitleBarMarginTop(vararg view: View) =
    ImmersionBar.setTitleBarMarginTop(this, *view)

fun Context.fitsTitleBarMarginTop(vararg view: View) =
    ImmersionBar.setTitleBarMarginTop(this, *view)

fun Window.fitsTitleBarMarginTop(vararg view: View) =
    ImmersionBar.setTitleBarMarginTop(this, *view)

fun View.fitsTitleBarMarginTop(vararg view: View) =
    ImmersionBar.setTitleBarMarginTop(this, *view)

fun Dialog.fitsTitleBarMarginTop(vararg view: View) =
    ImmersionBar.setTitleBarMarginTop(this, *view)

/** Hides status bar. */
fun Activity.hideStatusBar() = ImmersionBar.hideStatusBar(this)
fun Fragment.hideStatusBar() = ImmersionBar.hideStatusBar(this)
fun android.app.Fragment.hideStatusBar() = ImmersionBar.hideStatusBar(this)
fun Context.hideStatusBar() = ImmersionBar.hideStatusBar(this)
fun Window.hideStatusBar() = ImmersionBar.hideStatusBar(this)
fun View.hideStatusBar() = ImmersionBar.hideStatusBar(this)
fun Dialog.hideStatusBar() = ImmersionBar.hideStatusBar(this)

/** Shows status bar. */
fun Activity.showStatusBar() = ImmersionBar.showStatusBar(this)
fun Fragment.showStatusBar() = ImmersionBar.showStatusBar(this)
fun android.app.Fragment.showStatusBar() = ImmersionBar.showStatusBar(this)
fun Context.showStatusBar() = ImmersionBar.showStatusBar(this)
fun Window.showStatusBar() = ImmersionBar.showStatusBar(this)
fun View.showStatusBar() = ImmersionBar.showStatusBar(this)
fun Dialog.showStatusBar() = ImmersionBar.showStatusBar(this)

/** Fits system windows to prevent layout overlap with system bars (irreversible). */
fun Activity.setFitsSystemWindows() = ImmersionBar.setFitsSystemWindows(this)
fun Fragment.setFitsSystemWindows() = ImmersionBar.setFitsSystemWindows(this)
fun android.app.Fragment.setFitsSystemWindows() = ImmersionBar.setFitsSystemWindows(this)
fun Context.setFitsSystemWindows() = ImmersionBar.setFitsSystemWindows(this)
fun Window.setFitsSystemWindows() = ImmersionBar.setFitsSystemWindows(this)
fun View.setFitsSystemWindows() = ImmersionBar.setFitsSystemWindows(this)
fun Dialog.setFitsSystemWindows() = ImmersionBar.setFitsSystemWindows(this)



