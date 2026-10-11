package com.gyf.immersionbar.sample.activity;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.gyf.immersionbar.ImmersionBar;
import com.gyf.immersionbar.sample.AppManager;
import com.gyf.immersionbar.sample.R;

/**
 * Activity base class
 *
 * @author geyifeng
 * @date 2017/5/9
 */
public abstract class BaseActivity extends AppCompatActivity {

    protected String mTag = this.getClass().getSimpleName();

    protected Activity mActivity;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppManager.getInstance().addActivity(this);
        mActivity = this;
        setContentView(getLayoutId());
        // Bind views
        initViewBinding();
        // Initialize ImmersionBar
        initImmersionBar();
        // Initialize data
        initData();
        // Bind views and data
        initView();
        // Set listeners
        setListener();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        AppManager.getInstance().removeActivity(this);
    }

    /**
     * Initialize ViewBinding
     */
    protected void initViewBinding() {
    }

    protected View getContentView() {
        ViewGroup content = findViewById(android.R.id.content);
        return content.getChildAt(0);
    }

    /**
     * Subclasses set layout Id
     *
     * @return the layout id
     */
    protected abstract int getLayoutId();

    /**
     * Initialize ImmersionBar
     * Init immersion bar.
     */
    protected void initImmersionBar() {
        // Set common ImmersionBar style
        ImmersionBar.with(this).navigationBarColor(R.color.colorPrimary).init();
    }

    protected void initData() {
    }

    protected void initView() {
    }

    protected void setListener() {
    }
}
