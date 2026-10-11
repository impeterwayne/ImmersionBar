package com.gyf.immersionbar.sample.fragment;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.appcompat.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.gyf.immersionbar.ImmersionBar;
import com.gyf.immersionbar.sample.R;


/**
 * Base Fragment without immersion
 *
 * @author geyifeng
 * @date 2017/4/7
 */
public abstract class BaseFragment extends Fragment {

    protected String mTag = this.getClass().getSimpleName();

    protected Activity mActivity;
    protected View mRootView;
    protected Toolbar toolbar;
    protected View statusBarView;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mActivity = (Activity) context;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initDataBeforeView(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (mRootView == null) {
            mRootView = inflater.inflate(getLayoutId(), container, false);
        } else {
            ViewGroup viewGroup = (ViewGroup) mRootView.getParent();
            if (viewGroup != null) {
                viewGroup.removeView(mRootView);
            }
        }
        return mRootView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViewBinding(view);
        statusBarView = view.findViewById(R.id.status_bar_view);
        toolbar = view.findViewById(R.id.toolbar);
        fitsLayoutOverlap();
        initData();
        initView();
        setListener();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Why reconfigure layout overlap on screen rotation? Because status bar height may change on orientation switch, so call fix method again if using static methods
        fitsLayoutOverlap();
    }

    protected void initDataBeforeView(Bundle savedInstanceState) {

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

    private void fitsLayoutOverlap() {
        if (statusBarView != null) {
            ImmersionBar.setStatusBarView(this, statusBarView);
        } else {
            ImmersionBar.setTitleBar(this, toolbar);
        }
    }
}
