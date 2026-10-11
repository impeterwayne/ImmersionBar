package com.gyf.immersionbar.sample.activity;

import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.PopupWindow;

import com.gyf.immersionbar.ImmersionBar;
import com.gyf.immersionbar.OSUtils;
import com.gyf.immersionbar.sample.R;
import com.gyf.immersionbar.sample.utils.Utils;


/**
 * Use with PopupWindow
 * The type Popup activity.
 *
 * @author geyifeng
 */
public class PopupActivity extends BaseActivity {

    private View mPopupView;
    private PopupWindow mPopupWindow;
    private int mCurPosition;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_popup;
    }

    @Override
    protected void initImmersionBar() {
        super.initImmersionBar();
        ImmersionBar.with(this)
                .titleBar(R.id.toolbar)
                // Huawei phones can manually hide navigation bar, so adapt for it
                .addOnNavigationBarChangedListener(navigationBar -> updatePopupView())
                .init();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Adapt for screen orientation switch
        updatePopupWindow();
        updatePopupView();
    }

    /**
     * On view click.
     *
     * @param view the view
     */
    @Override
    protected void setListener() {
        findViewById(R.id.btn_full).setOnClickListener(this::onViewClick);
        findViewById(R.id.btn_top).setOnClickListener(this::onViewClick);
        findViewById(R.id.btn_bottom).setOnClickListener(this::onViewClick);
        findViewById(R.id.btn_left).setOnClickListener(this::onViewClick);
        findViewById(R.id.btn_right).setOnClickListener(this::onViewClick);
    }

    public void onViewClick(View view) {
        Integer[] widthAndHeight = Utils.getWidthAndHeight(getWindow());
        mCurPosition = view.getId();
        switch (view.getId()) {
            case R.id.btn_full:
                showPopup(Gravity.NO_GRAVITY, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, R.style.RightAnimation);
                break;
            case R.id.btn_top:
                showPopup(Gravity.TOP, ViewGroup.LayoutParams.MATCH_PARENT, widthAndHeight[1] / 2, R.style.TopAnimation);
                break;
            case R.id.btn_bottom:
                showPopup(Gravity.BOTTOM, ViewGroup.LayoutParams.MATCH_PARENT, widthAndHeight[1] / 2, R.style.BottomAnimation);
                break;
            case R.id.btn_left:
                showPopup(Gravity.START, widthAndHeight[0] / 2, ViewGroup.LayoutParams.MATCH_PARENT, R.style.LeftAnimation);
                break;
            case R.id.btn_right:
                showPopup(Gravity.END, widthAndHeight[0] / 2, ViewGroup.LayoutParams.MATCH_PARENT, R.style.RightAnimation);
                break;
            default:
                break;
        }
    }

    /**
     * Pop up PopupWindow
     * Show popup.
     *
     * @param gravity        the gravity
     * @param width          the width
     * @param height         the height
     * @param animationStyle the animation style
     */
    private void showPopup(int gravity, int width, int height, int animationStyle) {
        mPopupView = LayoutInflater.from(mActivity).inflate(R.layout.popup_demo, null);
        mPopupWindow = new PopupWindow(mPopupView, width, height);
        // The following attributes respond to click outside or physical back key dismiss
        mPopupWindow.setOutsideTouchable(true);
        mPopupWindow.setFocusable(true);
        mPopupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        // In immersion mode, the following two attributes do not take effect
        mPopupWindow.setInputMethodMode(PopupWindow.INPUT_METHOD_NEEDED);
        mPopupWindow.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        // Key method: allows layout to extend to status bar and navigation bar
        mPopupWindow.setClippingEnabled(false);
        // Set animation
        mPopupWindow.setAnimationStyle(animationStyle);
        // Show popup
        mPopupWindow.showAtLocation(getWindow().getDecorView(), gravity, 0, 0);
        // Background alpha value after showing
        backgroundAlpha(0.5f);
        // Restore background alpha value after dismiss
        mPopupWindow.setOnDismissListener(() -> backgroundAlpha(1f));
        // Adapt for layout covered by status bar and navigation bar after showing popup
        updatePopupView();
    }

    /**
     * Adjust PopupWindow position and size, otherwise orientation switch alters position and size
     * Update popup window.
     */
    private void updatePopupWindow() {
        Integer[] widthAndHeight = Utils.getWidthAndHeight(getWindow());
        switch (mCurPosition) {
            case R.id.btn_top:
            case R.id.btn_bottom:
                mPopupWindow.update(0, 0, ViewGroup.LayoutParams.MATCH_PARENT, widthAndHeight[1] / 2);
                break;
            case R.id.btn_left:
            case R.id.btn_right:
                mPopupWindow.update(0, 0, widthAndHeight[0] / 2, ViewGroup.LayoutParams.MATCH_PARENT);
                break;
            default:
                break;
        }
    }

    /**
     * Adjust Margins of view inside PopupWindow to adapt for layout covered by navigation bar
     * Update popup view.
     */
    private void updatePopupView() {
        int navigationBarHeight = ImmersionBar.getNavigationBarHeight(this);
        int navigationBarWidth = ImmersionBar.getNavigationBarWidth(this);
        if (mPopupView != null) {
            ImmersionBar.setTitleBar(this, mPopupView.findViewById(R.id.toolbar));
            View rlContent = mPopupView.findViewById(R.id.rlContent);
            mPopupView.post(() -> {
                boolean isPortrait;
                boolean isLandscapeLeft;
                int rotation = getWindowManager().getDefaultDisplay().getRotation();
                if (rotation == Surface.ROTATION_90) {
                    isPortrait = false;
                    isLandscapeLeft = true;
                } else if (rotation == Surface.ROTATION_270) {
                    isPortrait = false;
                    isLandscapeLeft = false;
                } else {
                    isPortrait = true;
                    isLandscapeLeft = false;
                }
                FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) rlContent.getLayoutParams();
                switch (mCurPosition) {
                    case R.id.btn_full:
                    case R.id.btn_top:
                    case R.id.btn_bottom:
                        if (isPortrait) {
                            layoutParams.setMargins(0, 0, 0, navigationBarHeight);
                        } else {
                            if (isLandscapeLeft) {
                                layoutParams.setMargins(0, 0, navigationBarWidth, 0);
                            } else {
                                if (OSUtils.isEMUI3_x()) {
                                    layoutParams.setMargins(0, 0, navigationBarWidth, 0);
                                } else {
                                    layoutParams.setMargins(navigationBarWidth, 0, 0, 0);
                                }
                            }
                        }
                        break;
                    case R.id.btn_left:
                        if (isPortrait) {
                            layoutParams.setMargins(0, 0, 0, navigationBarHeight);
                        } else {
                            if (isLandscapeLeft) {
                                layoutParams.setMargins(0, 0, 0, 0);
                            } else {
                                if (OSUtils.isEMUI3_x()) {
                                    layoutParams.setMargins(0, 0, navigationBarWidth, 0);
                                } else {
                                    layoutParams.setMargins(navigationBarWidth, 0, 0, 0);
                                }
                            }
                        }
                        break;
                    case R.id.btn_right:
                        if (isPortrait) {
                            layoutParams.setMargins(0, 0, 0, navigationBarHeight);
                        } else {
                            if (isLandscapeLeft) {
                                layoutParams.setMargins(0, 0, navigationBarWidth, 0);
                            } else {
                                if (OSUtils.isEMUI3_x()) {
                                    layoutParams.setMargins(0, 0, navigationBarWidth, 0);
                                } else {
                                    layoutParams.setMargins(0, 0, 0, 0);
                                }
                            }
                        }
                        break;
                    default:
                        break;
                }
                rlContent.setLayoutParams(layoutParams);
            });
        }
    }

    /**
     * Set background alpha value when showing popup
     *
     * @param bgAlpha 0f - 1f
     */
    public void backgroundAlpha(float bgAlpha) {
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.alpha = bgAlpha;
        getWindow().setAttributes(lp);
    }
}
