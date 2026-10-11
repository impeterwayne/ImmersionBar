package com.gyf.immersionbar.sample.activity;

import android.annotation.SuppressLint;
import androidx.appcompat.widget.Toolbar;
import android.widget.TextView;

import com.gyf.immersionbar.ImmersionBar;
import com.gyf.immersionbar.sample.R;
import com.gyf.immersionbar.sample.databinding.ActivityOver6Binding;


/**
 * @author geyifeng
 * @date 2017/5/8
 */
public class Over6Activity extends BaseActivity {

    private ActivityOver6Binding binding;
    @Override
    protected int getLayoutId() {
        return R.layout.activity_over6;
    }

    @Override
    protected void initImmersionBar() {
        super.initImmersionBar();
        ImmersionBar.with(this).navigationBarColor(R.color.colorPrimary)
                .titleBarMarginTop(R.id.toolbar)
                .statusBarColor(R.color.colorPrimary)
                .keyboardEnable(true)
                .init();

        // Or use static method to set
        //ImmersionBar.setTitleBarMarginTop(this,binding.toolbar);
    }


    @SuppressLint("SetTextI18n")
    @Override
    protected void initView() {
        binding.text.setText(getString(R.string.text_over6_desc));
    }
    @Override
    protected void initViewBinding() {
        binding = ActivityOver6Binding.bind(getContentView());
    }

}
