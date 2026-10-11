package com.gyf.immersionbar.sample.fragment.three;

import android.view.View;
import com.google.android.material.tabs.TabLayout;
import androidx.viewpager.widget.ViewPager;

import com.gyf.immersionbar.sample.R;
import com.gyf.immersionbar.sample.databinding.FragmentThreeCategoryBinding;
import com.gyf.immersionbar.sample.adapter.NewsAdapter;
import com.gyf.immersionbar.sample.fragment.BaseFragment;


/**
 * @author geyifeng
 * @date 2017/5/12
 */
public class CategoryThreeFragment extends BaseFragment {

    private FragmentThreeCategoryBinding binding;
    private String[] mTabName;

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_three_category;
    }

    @Override
    protected void initView() {
        super.initView();
        mTabName = new String[]{
                getString(R.string.tab_follow),
                getString(R.string.tab_headlines),
                getString(R.string.tab_video),
                getString(R.string.tab_entertainment),
                getString(R.string.tab_sports),
                getString(R.string.tab_new_era),
                getString(R.string.tab_top_news),
                getString(R.string.tab_qna),
                getString(R.string.tab_jokes),
                getString(R.string.tab_local),
                getString(R.string.tab_open_course),
                getString(R.string.tab_finance),
                getString(R.string.tab_tech),
                getString(R.string.tab_auto)
        };
        for (String s : mTabName) {
            binding.tabLayout.addTab(binding.tabLayout.newTab().setText(s));
        }
        binding.viewPager.setAdapter(new NewsAdapter(getChildFragmentManager(), mTabName));
        binding.tabLayout.setupWithViewPager(binding.viewPager);
    }
    @Override
    protected void initViewBinding(View view) {
        binding = FragmentThreeCategoryBinding.bind(view);
    }

}
