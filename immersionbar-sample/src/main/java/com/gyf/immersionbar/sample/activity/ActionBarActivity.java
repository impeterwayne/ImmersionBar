package com.gyf.immersionbar.sample.activity;

import android.annotation.SuppressLint;
import androidx.appcompat.app.ActionBar;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.gyf.immersionbar.ImmersionBar;
import com.gyf.immersionbar.sample.R;
import com.gyf.immersionbar.sample.databinding.ActivityActionBarBinding;
import com.gyf.immersionbar.sample.utils.Utils;


/**
 * @author geyifeng
 * @date 2017/6/2
 */

public class ActionBarActivity extends BaseActivity {

    private ActivityActionBarBinding binding;
    @Override
    protected int getLayoutId() {
        return R.layout.activity_action_bar;
    }

    @Override
    protected void initImmersionBar() {
        super.initImmersionBar();
        ImmersionBar.with(this).keyboardEnable(true).init();
    }

    @SuppressLint("SetTextI18n")
    @Override
    protected void initView() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(getString(R.string.title_action_bar));
        }
        binding.text.setText(getString(R.string.text_action_bar_desc1));
        Glide.with(this).asBitmap().load(Utils.getPic())
                .apply(new RequestOptions().placeholder(R.mipmap.test))
                .into(binding.iv);
    }

    @SuppressLint("SetTextI18n")
    @Override
    protected void setListener() {
        binding.btn.setOnClickListener(v -> {
            ImmersionBar.with(this)
                    .supportActionBar(true)
                    .statusBarColor(R.color.colorPrimary)
                    .init();
            binding.text.setText(getString(R.string.text_action_bar_desc2));
            binding.btn.setText(getString(R.string.text_solved));
        });
    }
    @Override
    protected void initViewBinding() {
        binding = ActivityActionBarBinding.bind(getContentView());
    }

}
