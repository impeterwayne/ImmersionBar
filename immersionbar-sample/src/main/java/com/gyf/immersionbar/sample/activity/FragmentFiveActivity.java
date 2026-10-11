package com.gyf.immersionbar.sample.activity;

import android.os.Bundle;
import androidx.annotation.Nullable;

import com.gyf.immersionbar.ImmersionBar;
import com.gyf.immersionbar.sample.AppManager;
import com.gyf.immersionbar.sample.R;
import com.gyf.immersionbar.sample.fragment.five.MainFragment;

import me.yokeyword.fragmentation.SupportActivity;
import me.yokeyword.fragmentation.anim.DefaultHorizontalAnimator;
import me.yokeyword.fragmentation.anim.FragmentAnimator;

/**
 * @author geyifeng
 * @date 2017/8/12
 */
public class FragmentFiveActivity extends SupportActivity {


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppManager.getInstance().addActivity(this);
        setContentView(R.layout.activity_fragmentation);
        ImmersionBar.with(this).init();
        if (findFragment(MainFragment.class) == null) {
            loadRootFragment(R.id.content, MainFragment.newInstance());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        AppManager.getInstance().removeActivity(this);
    }

    @Override
    public void onBackPressedSupport() {
        // For back press logic in 4 category main Fragments, handled in their onBackPressedSupport
        super.onBackPressedSupport();
    }

    @Override
    public FragmentAnimator onCreateFragmentAnimator() {
        // Set horizontal animation (same as Android 4.x animation)
        return new DefaultHorizontalAnimator();
    }
}
