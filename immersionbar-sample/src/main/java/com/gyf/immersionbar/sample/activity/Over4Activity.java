package com.gyf.immersionbar.sample.activity;

import android.annotation.SuppressLint;
import android.view.View;
import android.widget.TextView;

import com.gyf.immersionbar.ImmersionBar;
import com.gyf.immersionbar.sample.R;
import com.gyf.immersionbar.sample.databinding.ActivityOver4Binding;


/**
 * @author geyifeng
 * @date 2017/5/8
 */
public class Over4Activity extends BaseActivity {

    private ActivityOver4Binding binding;
    @Override
    protected int getLayoutId() {
        return R.layout.activity_over4;
    }

    @Override
    protected void initImmersionBar() {
        super.initImmersionBar();
        ImmersionBar.with(this).statusBarView(binding.statusBarView)
                .navigationBarColor(R.color.colorPrimary)
                .keyboardEnable(true)
                .init();
    }

    @SuppressLint("SetTextI18n")
    @Override
    protected void initView() {
        binding.text.setText(getString(R.string.text_over4_desc));
    }
    @Override
    protected void initViewBinding() {
        binding = ActivityOver4Binding.bind(getContentView());
    }

}
