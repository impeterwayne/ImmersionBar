package com.gyf.immersionbar.sample.activity;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.gyf.immersionbar.ImmersionBar;
import com.gyf.immersionbar.sample.R;
import com.gyf.immersionbar.sample.databinding.ActivityWhiteStatusBarBinding;


/**
 * @author geyifeng
 * @date 2017/6/2
 */
public class WhiteBarActivity extends BaseActivity {

    private ActivityWhiteStatusBarBinding binding;
    @Override
    protected int getLayoutId() {
        return R.layout.activity_white_status_bar;
    }

    @Override
    protected void initImmersionBar() {
        super.initImmersionBar();
        ImmersionBar.with(this).titleBar(R.id.toolbar).navigationBarColorInt(Color.WHITE).init();
    }

    @SuppressLint("SetTextI18n")
    @Override
    protected void setListener() {
        binding.btn.setOnClickListener(v -> {
            ImmersionBar.with(this)
                    .statusBarDarkFont(true)
                    .navigationBarDarkIcon(true)
                    .init();
            binding.text.setText(getString(R.string.text_white_bar_explanation));
            binding.btn.setVisibility(View.GONE);
        });
    }
    @Override
    protected void initViewBinding() {
        binding = ActivityWhiteStatusBarBinding.bind(getContentView());
    }

}
