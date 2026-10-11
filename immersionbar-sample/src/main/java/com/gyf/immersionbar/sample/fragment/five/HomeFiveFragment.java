package com.gyf.immersionbar.sample.fragment.five;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import android.view.View;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.gyf.immersionbar.ImmersionBar;
import com.gyf.immersionbar.sample.R;
import com.gyf.immersionbar.sample.databinding.FragmentTwoHomeBinding;
import com.gyf.immersionbar.sample.utils.Utils;


/**
 * @author geyifeng
 * @date 2017/7/20
 */
public class HomeFiveFragment extends BaseFiveFragment {

    private FragmentTwoHomeBinding binding;
    public static HomeFiveFragment newInstance() {
        Bundle args = new Bundle();
        HomeFiveFragment fragment = new HomeFiveFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_two_home;
    }

    @Override
    public void initImmersionBar() {
        super.initImmersionBar();
        ImmersionBar.with(this).navigationBarColor(R.color.colorPrimary).init();
    }

    @SuppressLint("SetTextI18n")
    @Override
    protected void initView() {
        super.initView();
        binding.text.setText(getString(R.string.text_home_five_desc));
        Glide.with(this).asBitmap().load(Utils.getPic())
                .apply(new RequestOptions().placeholder(R.mipmap.test))
                .into(binding.iv);
    }
    @Override
    protected void initViewBinding(View view) {
        binding = FragmentTwoHomeBinding.bind(view);
    }

}
