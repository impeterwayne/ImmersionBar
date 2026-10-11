package com.gyf.immersionbar.sample.activity;

import android.annotation.SuppressLint;
import androidx.appcompat.widget.Toolbar;
import android.widget.TextView;

import com.gyf.immersionbar.ImmersionBar;
import com.gyf.immersionbar.sample.R;
import com.gyf.immersionbar.sample.databinding.ActivityOver5Binding;


/**
 * @author geyifeng
 * @date 2017/5/8
 */
public class Over5Activity extends BaseActivity {

    private ActivityOver5Binding binding;
    @Override
    protected int getLayoutId() {
        return R.layout.activity_over5;
    }

    @Override
    protected void initImmersionBar() {
        super.initImmersionBar();
        ImmersionBar.with(this).titleBar(binding.toolbar)
                .navigationBarColor(R.color.colorPrimary)
                .keyboardEnable(true)
                .init();
    }

    @SuppressLint("SetTextI18n")
    @Override
    protected void initView() {
        binding.text.setText(getString(R.string.text_over5_desc));
    }
    @Override
    protected void initViewBinding() {
        binding = ActivityOver5Binding.bind(getContentView());
    }

}
