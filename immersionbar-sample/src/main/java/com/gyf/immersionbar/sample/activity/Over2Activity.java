package com.gyf.immersionbar.sample.activity;

import android.annotation.SuppressLint;
import android.widget.TextView;

import com.gyf.immersionbar.ImmersionBar;
import com.gyf.immersionbar.sample.R;
import com.gyf.immersionbar.sample.databinding.ActivityOver2Binding;


/**
 * @author geyifeng
 * @date 2017/5/8
 */

public class Over2Activity extends BaseActivity {

    private ActivityOver2Binding binding;
    @Override
    protected int getLayoutId() {
        return R.layout.activity_over2;
    }

    @Override
    protected void initImmersionBar() {
        super.initImmersionBar();
        ImmersionBar.with(this).barColor(R.color.colorPrimary).init();
    }

    @SuppressLint("SetTextI18n")
    @Override
    protected void initView() {
        super.initView();
        binding.text.setText(getString(R.string.text_over2_desc));
    }
    @Override
    protected void initViewBinding() {
        binding = ActivityOver2Binding.bind(getContentView());
    }

}
