package com.gyf.immersionbar.sample.activity;

import android.os.Bundle;
import com.google.android.material.tabs.TabLayout;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.Toolbar;

import com.gyf.immersionbar.ImmersionBar;
import com.gyf.immersionbar.sample.AppManager;
import com.gyf.immersionbar.sample.R;
import com.gyf.immersionbar.sample.adapter.TabAdapter;

import java.util.ArrayList;
import java.util.List;

import me.imid.swipebacklayout.lib.app.SwipeBackActivity;

/**
 * @author geyifeng
 * @date 2017/7/4
 */
public class TabLayoutActivity extends SwipeBackActivity {

    private List<String> mData;
    private TabAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppManager.getInstance().addActivity(this);
        setContentView(R.layout.activity_tab_layout);
        initData(1);
        initView();
        ImmersionBar.with(this)
                .statusBarView(R.id.view)
                .navigationBarColor(R.color.cool_green_normal)
                .init();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        AppManager.getInstance().removeActivity(this);
    }

    private void initData(int pager) {
        mData = new ArrayList<>();
        for (int i = 1; i < 50; i++) {
            mData.add(getString(R.string.text_pager_item_format, pager, i));
        }
    }

    private void initView() {
        // Set Toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle("");
        //setSupportActionBar(toolbar);// Replace system actionBar

        // Set TabLayout
        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabLayout);
        for (int i = 1; i < 7; i++) {
            tabLayout.addTab(tabLayout.newTab().setText("TAB" + i));
        }
        // TabLayout selection listener
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                // Update RecyclerView when tab changes
                initData(tab.getPosition() + 1);
                mAdapter.setNewData(mData);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });
        // Set RecyclerView
        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        mAdapter = new TabAdapter();
        recyclerView.setAdapter(mAdapter);
        mAdapter.setNewData(mData);
    }
}
