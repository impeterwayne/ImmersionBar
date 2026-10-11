package com.gyf.immersionbar.sample.fragment.five;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.gyf.immersionbar.ImmersionBar;
import com.gyf.immersionbar.sample.R;

import me.yokeyword.fragmentation.SupportFragment;

/**
 * Implement immersion based on Fragmentation framework. If you want to implement immersion in Fragment, please do so in onSupportVisible.
 * As for resolving layout overlap issues, it is recommended to use method 4 or 5 in readme, refer to onViewCreated method
 *
 * @author geyifeng
 * @date 2017/8/12
 */
public abstract class BaseFiveFragment extends SupportFragment {
    protected Activity mActivity;
    protected View mRootView;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mActivity = (Activity) context;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mRootView = inflater.inflate(getLayoutId(), container, false);
        return mRootView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViewBinding(view);
        View titleBar = view.findViewById(setTitleBar());
        ImmersionBar.setTitleBar(mActivity, titleBar);
        View statusBarView = view.findViewById(setStatusBarView());
        ImmersionBar.setStatusBarView(mActivity, statusBarView);
        initData();
        initView();
        setListener();
    }

    protected int setTitleBar() {
        return R.id.toolbar;
    }

    protected int setStatusBarView() {
        return 0;
    }

    @Override
    public void onSupportVisible() {
        super.onSupportVisible();
        // Please implement immersion in onSupportVisible
        if (isImmersionBarEnabled()) {
            initImmersionBar();
        }
    }

    public void initImmersionBar() {
        ImmersionBar.with(this).keyboardEnable(true).init();
    }

    private boolean isImmersionBarEnabled() {
        return true;
    }


    /**
     * Initialize ViewBinding
     */
    protected void initViewBinding(View view) {
    }

    /**
     * Gets layout id.
     *
     * @return the layout id
     */
    protected abstract int getLayoutId();

    /**
     * Initialize data
     */
    protected void initData() {

    }

    /**
     * Bind views and data
     */
    protected void initView() {

    }

    /**
     * Set listeners
     */
    protected void setListener() {

    }
}
