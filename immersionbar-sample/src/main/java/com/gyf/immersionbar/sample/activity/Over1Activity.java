package com.gyf.immersionbar.sample.activity;

import android.annotation.SuppressLint;
import android.widget.TextView;

import com.gyf.immersionbar.ImmersionBar;
import com.gyf.immersionbar.sample.R;
import com.gyf.immersionbar.sample.databinding.ActivityOver1Binding;


/**
 * @author geyifeng
 * @date 2017/5/8
 */
public class Over1Activity extends BaseActivity {

    private ActivityOver1Binding binding;
    @Override
    protected int getLayoutId() {
        return R.layout.activity_over1;
    }

    @Override
    protected void initImmersionBar() {
        super.initImmersionBar();
        ImmersionBar.with(this).navigationBarColor(R.color.colorPrimary).keyboardEnable(true).init();
    }

    @SuppressLint("SetTextI18n")
    @Override
    protected void initView() {
        binding.text.setText(getString(R.string.text_over1_desc));
    }
    @Override
    protected void initViewBinding() {
        binding = ActivityOver1Binding.bind(getContentView());
    }

}