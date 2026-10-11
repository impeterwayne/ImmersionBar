package com.gyf.immersionbar.sample.activity;

import android.annotation.SuppressLint;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.gyf.immersionbar.BarParams;
import com.gyf.immersionbar.ImmersionBar;
import com.gyf.immersionbar.sample.R;
import com.gyf.immersionbar.sample.databinding.ActivityOver3Binding;


/**
 * @author geyifeng
 * @date 2017/5/8
 */
public class Over3Activity extends BaseActivity {

    private ActivityOver3Binding binding;
    @Override
    protected int getLayoutId() {
        return R.layout.activity_over3;
    }

    @Override
    protected void initImmersionBar() {
        super.initImmersionBar();
        ImmersionBar.with(this).fitsSystemWindows(true)
                .statusBarColor(R.color.colorPrimary)
                .navigationBarColor(R.color.colorPrimary)
                .keyboardEnable(true)
                .init();
    }

    @SuppressLint("SetTextI18n")
    @Override
    protected void initView() {
        binding.text.setText(getString(R.string.text_over3_desc));
    }

    @SuppressLint("SetTextI18n")
    @Override
    protected void setListener() {
        findViewById(R.id.btn).setOnClickListener(this::onClick);
    }

    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn:
                BarParams barParams = ImmersionBar.with(this).getBarParams();
                if (barParams.fits) {
                    ImmersionBar.with(this).fitsSystemWindows(false).transparentStatusBar().init();
                    binding.btn.setText(getString(R.string.text_fits_system_windows_demo, false));
                } else {
                    ImmersionBar.with(this).fitsSystemWindows(true).statusBarColor(R.color.colorPrimary).init();
                    binding.btn.setText(getString(R.string.text_fits_system_windows_demo, true));
                }
                break;
            default:
                break;
        }
    }
    @Override
    protected void initViewBinding() {
        binding = ActivityOver3Binding.bind(getContentView());
    }

}
